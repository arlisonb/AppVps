"""Execução segura de comandos de restart."""

import uuid
from datetime import datetime, timezone

from app.models.schemas import RestartRequest, RestartResponse, RestartType
from app.utils.command import run_command

# Comandos permitidos por tipo (whitelist)
ALLOWED_RESTART_COMMANDS: dict[RestartType, list[str]] = {
    RestartType.VPS: ["sudo reboot", "sudo shutdown -r now"],
    RestartType.DOCKER: ["systemctl restart docker", "service docker restart"],
    RestartType.PM2: ["pm2 restart all"],
    RestartType.NGINX: ["systemctl restart nginx", "nginx -s reload"],
}

# Prefixos permitidos para comandos customizados
ALLOWED_COMMAND_PREFIXES = [
    "systemctl restart",
    "systemctl start",
    "systemctl stop",
    "docker restart",
    "docker start",
    "docker stop",
    "docker compose",
    "docker-compose",
    "pm2 restart",
    "pm2 start",
    "pm2 stop",
    "service ",
    "nginx -s reload",
]


class RestartService:
    """Executa restarts autorizados com validação de segurança."""

    async def execute(self, request: RestartRequest) -> RestartResponse:
        command = self._resolve_command(request)

        if not self._is_command_allowed(command):
            return RestartResponse(
                success=False,
                message=f"Comando não autorizado: {command}",
                target=request.target,
            )

        code, stdout, stderr = await run_command(command, timeout=120)

        if code == 0:
            return RestartResponse(
                success=True,
                message=stdout or f"Restart executado com sucesso: {request.target}",
                target=request.target,
            )

        return RestartResponse(
            success=False,
            message=stderr or f"Falha ao executar restart: código {code}",
            target=request.target,
        )

    def _resolve_command(self, request: RestartRequest) -> str:
        if request.restart_type == RestartType.CUSTOM:
            if not request.custom_command:
                raise ValueError("Comando customizado é obrigatório")
            return request.custom_command.strip()

        if request.restart_type == RestartType.SERVICE:
            return f"systemctl restart {request.target}"

        if request.restart_type == RestartType.CONTAINER:
            return f"docker restart {request.target}"

        if request.restart_type == RestartType.COMPOSE:
            return f"docker compose -p {request.target} restart"

        if request.restart_type == RestartType.DATABASE:
            db_commands = {
                "postgresql": "systemctl restart postgresql",
                "postgres": "systemctl restart postgresql",
                "mysql": "systemctl restart mysql",
                "mariadb": "systemctl restart mariadb",
                "mongodb": "systemctl restart mongod",
                "redis": "systemctl restart redis",
            }
            return db_commands.get(request.target.lower(), f"systemctl restart {request.target}")

        if request.restart_type == RestartType.PM2:
            return f"pm2 restart {request.target}"

        allowed = ALLOWED_RESTART_COMMANDS.get(request.restart_type, [])
        return allowed[0] if allowed else f"systemctl restart {request.target}"

    def _is_command_allowed(self, command: str) -> bool:
        command_lower = command.lower().strip()

        # Bloquear comandos perigosos
        dangerous = ["rm ", "mkfs", "dd ", "chmod 777", "> /dev", "wget", "curl |", "&&", "||", ";", "|", "`", "$("]
        for pattern in dangerous:
            if pattern in command_lower:
                return False

        for prefix in ALLOWED_COMMAND_PREFIXES:
            if command_lower.startswith(prefix):
                return True

        # Verificar whitelist exata
        for commands in ALLOWED_RESTART_COMMANDS.values():
            if command_lower in [c.lower() for c in commands]:
                return True

        return False
