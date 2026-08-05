"""Descoberta automática de serviços na VPS."""

import hashlib
from datetime import datetime, timezone

from app.models.schemas import ServiceInfo, ServiceStatus, ServiceType
from app.utils.command import extract_port, normalize_service_name, run_command, safe_json_loads
from app.utils.service_types import detect_service_type, get_default_restart_command, get_service_icon


class ServiceDiscovery:
    """Descobre serviços Docker, PM2, Systemd e processos."""

    async def discover_all(self) -> list[ServiceInfo]:
        services: list[ServiceInfo] = []
        services.extend(await self._discover_systemd())
        services.extend(await self._discover_docker())
        services.extend(await self._discover_docker_compose())
        services.extend(await self._discover_pm2())
        services.extend(await self._discover_listening_ports())

        # Deduplicar por nome+tipo
        seen: set[str] = set()
        unique: list[ServiceInfo] = []
        for svc in services:
            key = f"{svc.type}:{svc.name}"
            if key not in seen:
                seen.add(key)
                unique.append(svc)

        return unique

    def _make_id(self, name: str, svc_type: ServiceType) -> str:
        raw = f"{svc_type.value}:{name}"
        return hashlib.md5(raw.encode()).hexdigest()[:12]

    async def _discover_systemd(self) -> list[ServiceInfo]:
        services: list[ServiceInfo] = []
        code, stdout, _ = await run_command(
            "systemctl list-units --type=service --all --no-pager --plain --no-legend"
        )
        if code != 0:
            return services

        for line in stdout.splitlines():
            parts = line.split()
            if len(parts) < 4:
                continue
            name = parts[0].replace(".service", "")
            active = parts[2]
            if active not in ("active", "failed", "inactive"):
                continue
            if name.endswith(("@", "user@", "system-")) or "@" in name:
                continue

            svc_type = detect_service_type(name)
            status = ServiceStatus.ONLINE if active == "active" else (
                ServiceStatus.ERROR if active == "failed" else ServiceStatus.OFFLINE
            )

            services.append(ServiceInfo(
                id=self._make_id(name, ServiceType.SYSTEMD),
                name=name,
                type=svc_type if svc_type != ServiceType.CUSTOM else ServiceType.SYSTEMD,
                icon=get_service_icon(svc_type),
                status=status,
                init_method="systemd",
                restart_command=get_default_restart_command(ServiceType.SYSTEMD, name),
                metadata={"unit": f"{name}.service", "load_state": parts[3] if len(parts) > 3 else ""},
            ))

        return services

    async def _discover_docker(self) -> list[ServiceInfo]:
        services: list[ServiceInfo] = []
        code, stdout, _ = await run_command(
            'docker ps -a --format "{{json .}}"'
        )
        if code != 0:
            return services

        for line in stdout.splitlines():
            data = safe_json_loads(line)
            if not data:
                continue

            name = data.get("Names", data.get("Name", "unknown"))
            image = data.get("Image", "")
            state = data.get("State", "").lower()
            ports_str = data.get("Ports", "")
            container_id = data.get("ID", "")[:12]

            svc_type = detect_service_type(name, image)
            port = extract_port(ports_str)

            status_map = {
                "running": ServiceStatus.ONLINE,
                "exited": ServiceStatus.OFFLINE,
                "dead": ServiceStatus.ERROR,
                "restarting": ServiceStatus.STARTING,
            }

            services.append(ServiceInfo(
                id=self._make_id(name, ServiceType.DOCKER),
                name=name.lstrip("/"),
                type=svc_type if svc_type != ServiceType.CUSTOM else ServiceType.DOCKER,
                icon=get_service_icon(svc_type),
                status=status_map.get(state, ServiceStatus.UNKNOWN),
                init_method="docker",
                port=port,
                ports=[port] if port else [],
                restart_command=f"docker restart {name.lstrip('/')}",
                container_id=container_id,
                version=image.split(":")[-1] if ":" in image else None,
                metadata={"image": image, "state": state},
            ))

        return services

    async def _discover_docker_compose(self) -> list[ServiceInfo]:
        services: list[ServiceInfo] = []
        code, stdout, _ = await run_command("docker compose ls --format json 2>/dev/null || docker-compose ls --format json 2>/dev/null")
        if code != 0:
            return services

        projects = safe_json_loads(stdout, [])
        if isinstance(projects, dict):
            projects = [projects]

        for project in projects if isinstance(projects, list) else []:
            name = project.get("Name", project.get("name", ""))
            if not name:
                continue

            svc_type = detect_service_type(name)
            services.append(ServiceInfo(
                id=self._make_id(name, ServiceType.DOCKER_COMPOSE),
                name=name,
                type=ServiceType.DOCKER_COMPOSE,
                icon=get_service_icon(svc_type),
                status=ServiceStatus.ONLINE,
                init_method="docker_compose",
                restart_command=f"docker compose -p {name} restart",
                compose_project=name,
                metadata=project,
            ))

        return services

    async def _discover_pm2(self) -> list[ServiceInfo]:
        services: list[ServiceInfo] = []
        code, stdout, _ = await run_command("pm2 jlist 2>/dev/null")
        if code != 0:
            return services

        processes = safe_json_loads(stdout, [])
        if not isinstance(processes, list):
            return processes

        for proc in processes:
            name = proc.get("name", "unknown")
            pm2_env = proc.get("pm2_env", {})
            monit = proc.get("monit", {})
            status_str = pm2_env.get("status", "stopped")

            status_map = {
                "online": ServiceStatus.ONLINE,
                "stopped": ServiceStatus.STOPPED,
                "stopping": ServiceStatus.OFFLINE,
                "launching": ServiceStatus.STARTING,
                "errored": ServiceStatus.ERROR,
            }

            svc_type = detect_service_type(name, command=pm2_env.get("pm_exec_path", ""))
            pid = proc.get("pid")

            services.append(ServiceInfo(
                id=self._make_id(name, ServiceType.PM2),
                name=name,
                type=svc_type if svc_type != ServiceType.CUSTOM else ServiceType.PM2,
                icon=get_service_icon(svc_type),
                status=status_map.get(status_str, ServiceStatus.UNKNOWN),
                init_method="pm2",
                restart_command=f"pm2 restart {name}",
                pid=pid if pid and pid > 0 else None,
                cpu_percent=float(monit.get("cpu", 0)),
                memory_mb=float(monit.get("memory", 0)) / 1_048_576,
                uptime_seconds=int((datetime.now(timezone.utc).timestamp() - pm2_env.get("created_at", 0) / 1000)) if pm2_env.get("created_at") else 0,
                metadata={"pm_id": proc.get("pm_id"), "exec_mode": pm2_env.get("exec_mode")},
            ))

        return services

    async def _discover_listening_ports(self) -> list[ServiceInfo]:
        """Descobre portas em escuta via ss."""
        services: list[ServiceInfo] = []
        code, stdout, _ = await run_command("ss -tulpn 2>/dev/null")
        if code != 0:
            return services

        known_ports: set[int] = set()
        for line in stdout.splitlines()[1:]:
            parts = line.split()
            if len(parts) < 5:
                continue

            local_addr = parts[4]
            port = extract_port(local_addr)
            if not port or port in known_ports:
                continue
            known_ports.add(port)

            process_info = parts[-1] if len(parts) > 5 else ""
            name = normalize_service_name(process_info.split(",")[-1].replace('"', "") if process_info else f"port_{port}")

            if not name or name.startswith("port_"):
                continue

            svc_type = detect_service_type(name, command=process_info)
            services.append(ServiceInfo(
                id=self._make_id(f"{name}_{port}", ServiceType.CUSTOM),
                name=name,
                type=svc_type,
                icon=get_service_icon(svc_type),
                status=ServiceStatus.ONLINE,
                init_method="process",
                port=port,
                ports=[port],
                metadata={"process": process_info, "address": local_addr},
            ))

        return services

    async def enrich_with_metrics(self, services: list[ServiceInfo]) -> list[ServiceInfo]:
        """Enriquece serviços com métricas de CPU/RAM via ps aux."""
        import psutil

        for svc in services:
            if svc.pid:
                try:
                    proc = psutil.Process(svc.pid)
                    svc.cpu_percent = round(proc.cpu_percent(), 2)
                    mem = proc.memory_info()
                    svc.memory_mb = round(mem.rss / 1_048_576, 2)
                    svc.memory_percent = round(proc.memory_percent(), 2)
                    create_time = datetime.fromtimestamp(proc.create_time(), tz=timezone.utc)
                    svc.uptime_seconds = int((datetime.now(timezone.utc) - create_time).total_seconds())
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    pass

        return services
