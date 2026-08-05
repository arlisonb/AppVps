"""Coleta de logs de serviços."""

from app.models.schemas import LogsRequest, LogsResponse, ServiceInfo, ServiceType
from app.utils.command import run_command


class LogsService:
    """Obtém logs de serviços Docker, PM2, Systemd."""

    async def get_logs(
        self,
        request: LogsRequest,
        services: list[ServiceInfo],
    ) -> LogsResponse:
        service = next((s for s in services if s.id == request.service_id), None)
        if not service:
            return LogsResponse(
                service_id=request.service_id,
                service_name="unknown",
                lines=[],
                total_lines=0,
            )

        lines = await self._fetch_logs(service, request.lines, request.since)
        filtered = self._filter_lines(lines, request.search)

        truncated = len(lines) > request.lines
        result_lines = filtered[:request.lines]

        return LogsResponse(
            service_id=service.id,
            service_name=service.name,
            lines=result_lines,
            total_lines=len(result_lines),
            truncated=truncated,
        )

    async def _fetch_logs(
        self,
        service: ServiceInfo,
        lines: int,
        since: str | None,
    ) -> list[str]:
        since_arg = f"--since {since}" if since else ""

        if service.type == ServiceType.DOCKER or service.init_method == "docker":
            cmd = f"docker logs --tail {lines} {since_arg} {service.name} 2>&1"
        elif service.type == ServiceType.PM2 or service.init_method == "pm2":
            cmd = f"pm2 logs {service.name} --nostream --lines {lines} 2>&1"
        elif service.init_method == "systemd":
            cmd = f"journalctl -u {service.name} -n {lines} --no-pager 2>&1"
        else:
            cmd = f"journalctl -n {lines} --no-pager 2>&1"

        code, stdout, stderr = await run_command(cmd, timeout=30)
        output = stdout if stdout else stderr
        return output.splitlines() if output else []

    def _filter_lines(self, lines: list[str], search: str | None) -> list[str]:
        if not search:
            return lines
        search_lower = search.lower()
        return [line for line in lines if search_lower in line.lower()]
