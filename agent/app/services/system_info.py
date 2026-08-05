"""Informações do sistema e detecção de reinicialização."""

import json
import socket
from datetime import datetime, timezone
from pathlib import Path

from app.models.schemas import BootDetectionResponse, ServiceInfo, ServiceStatus, VpsInfo, VpsStatus
from app.services.metrics import MetricsCollector
from app.utils.command import run_command


class SystemInfo:
    """Coleta informações do host e detecta reboots."""

    def __init__(self, state_file: Path, boot_id_file: Path) -> None:
        self.state_file = state_file
        self.boot_id_file = boot_id_file
        self.metrics_collector = MetricsCollector()
        self._ensure_state_dir()

    def _ensure_state_dir(self) -> None:
        self.state_file.parent.mkdir(parents=True, exist_ok=True)

    async def get_hostname(self) -> str:
        code, stdout, _ = await run_command("hostname")
        return stdout if code == 0 else socket.gethostname()

    async def get_uname(self) -> tuple[str, str, str]:
        code, stdout, _ = await run_command("uname -srm")
        if code != 0:
            return "Linux", "unknown", "unknown"
        parts = stdout.split()
        kernel = parts[0] if parts else "Linux"
        version = parts[1] if len(parts) > 1 else "unknown"
        arch = parts[2] if len(parts) > 2 else "unknown"
        return kernel, version, arch

    async def get_uptime_seconds(self) -> int:
        code, stdout, _ = await run_command("cat /proc/uptime")
        if code == 0 and stdout:
            return int(float(stdout.split()[0]))
        return 0

    def get_boot_id(self) -> str:
        try:
            return self.boot_id_file.read_text().strip()
        except OSError:
            return "unknown"

    async def get_os_info(self) -> tuple[str, str]:
        try:
            os_release = Path("/etc/os-release").read_text()
            name = version = "Linux"
            for line in os_release.splitlines():
                if line.startswith("NAME="):
                    name = line.split("=", 1)[1].strip('"')
                elif line.startswith("VERSION="):
                    version = line.split("=", 1)[1].strip('"')
            return name, version
        except OSError:
            return "Linux", "unknown"

    def get_ip_addresses(self) -> list[str]:
        ips: list[str] = []
        try:
            for info in socket.getaddrinfo(socket.gethostname(), None):
                ip = info[4][0]
                if ":" not in ip and not ip.startswith("127."):
                    ips.append(ip)
        except socket.gaierror:
            pass
        return list(set(ips))

    async def build_vps_info(self, services: list[ServiceInfo]) -> VpsInfo:
        hostname = await self.get_hostname()
        kernel, kernel_version, arch = await self.get_uname()
        os_name, os_version = await self.get_os_info()
        uptime = await self.get_uptime_seconds()
        boot_id = self.get_boot_id()
        metrics = self.metrics_collector.collect()

        online = sum(1 for s in services if s.status == ServiceStatus.ONLINE)
        offline = sum(1 for s in services if s.status in (ServiceStatus.OFFLINE, ServiceStatus.STOPPED))
        error = sum(1 for s in services if s.status == ServiceStatus.ERROR)

        status = VpsStatus.ONLINE
        if metrics.cpu_percent > 90 or metrics.ram_percent > 90 or metrics.disk.percent > 90:
            status = VpsStatus.ATTENTION
        if error > 0:
            status = VpsStatus.ATTENTION

        last_boot = datetime.now(timezone.utc).replace(
            microsecond=0
        )
        if uptime > 0:
            from datetime import timedelta
            last_boot = datetime.now(timezone.utc) - timedelta(seconds=uptime)

        return VpsInfo(
            hostname=hostname,
            os_name=os_name,
            os_version=os_version,
            kernel=f"{kernel} {kernel_version}",
            architecture=arch,
            uptime_seconds=uptime,
            last_boot=last_boot,
            boot_id=boot_id,
            ip_addresses=self.get_ip_addresses(),
            status=status,
            metrics=metrics,
            services_online=online,
            services_offline=offline,
            services_error=error,
        )

    def _load_state(self) -> dict:
        try:
            if self.state_file.exists():
                return json.loads(self.state_file.read_text())
        except (json.JSONDecodeError, OSError):
            pass
        return {}

    def _save_state(self, state: dict) -> None:
        self.state_file.write_text(json.dumps(state, default=str))

    def check_reboot(self, services: list[ServiceInfo]) -> BootDetectionResponse:
        current_boot_id = self.get_boot_id()
        state = self._load_state()
        previous_boot_id = state.get("boot_id")
        expected_services = state.get("expected_services", [])

        reboot_detected = previous_boot_id is not None and previous_boot_id != current_boot_id

        offline_services: list[str] = []
        if reboot_detected:
            online_names = {s.name for s in services if s.status == ServiceStatus.ONLINE}
            for expected in expected_services:
                if expected not in online_names:
                    offline_services.append(expected)

        # Atualizar estado
        online_service_names = [s.name for s in services if s.status == ServiceStatus.ONLINE]
        self._save_state({
            "boot_id": current_boot_id,
            "expected_services": online_service_names,
            "last_check": datetime.now(timezone.utc).isoformat(),
        })

        message = ""
        if reboot_detected:
            if offline_services:
                message = f"VPS reiniciada. Serviços offline: {', '.join(offline_services)}"
            else:
                message = "VPS reiniciada. Todos os serviços estão online."

        return BootDetectionResponse(
            reboot_detected=reboot_detected,
            previous_boot_id=previous_boot_id,
            current_boot_id=current_boot_id,
            offline_services=offline_services,
            message=message,
        )
