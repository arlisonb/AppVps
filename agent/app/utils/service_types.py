"""Mapeamento de tipos de serviço e ícones."""

from app.models.schemas import ServiceType

# Padrões para detecção automática de tipo
SERVICE_PATTERNS: dict[ServiceType, list[str]] = {
    ServiceType.NGINX: ["nginx"],
    ServiceType.APACHE: ["apache2", "httpd", "apache"],
    ServiceType.POSTGRESQL: ["postgres", "postgresql"],
    ServiceType.MYSQL: ["mysql"],
    ServiceType.MARIADB: ["mariadb"],
    ServiceType.MONGODB: ["mongod", "mongodb"],
    ServiceType.REDIS: ["redis"],
    ServiceType.RABBITMQ: ["rabbitmq"],
    ServiceType.TRAEFIK: ["traefik"],
    ServiceType.MINIO: ["minio"],
    ServiceType.PORTAINER: ["portainer"],
    ServiceType.WPPCONNECT: ["wppconnect", "wpp-connect"],
    ServiceType.EVOLUTION_API: ["evolution-api", "evolution_api", "evolution"],
    ServiceType.TYPEBOT: ["typebot"],
    ServiceType.SUPABASE: ["supabase"],
    ServiceType.OPENWEBUI: ["open-webui", "openwebui"],
    ServiceType.OLLAMA: ["ollama"],
    ServiceType.FASTAPI: ["fastapi", "uvicorn"],
    ServiceType.NODEJS: ["node", "nodejs"],
    ServiceType.PYTHON: ["python", "gunicorn"],
    ServiceType.REACT: ["react"],
    ServiceType.NEXTJS: ["next", "nextjs"],
}

SERVICE_ICONS: dict[ServiceType, str] = {
    ServiceType.DOCKER: "docker",
    ServiceType.DOCKER_COMPOSE: "docker_compose",
    ServiceType.PM2: "pm2",
    ServiceType.SYSTEMD: "systemd",
    ServiceType.FASTAPI: "fastapi",
    ServiceType.NODEJS: "nodejs",
    ServiceType.PYTHON: "python",
    ServiceType.REACT: "react",
    ServiceType.NEXTJS: "nextjs",
    ServiceType.REDIS: "redis",
    ServiceType.POSTGRESQL: "postgresql",
    ServiceType.MYSQL: "mysql",
    ServiceType.MARIADB: "mariadb",
    ServiceType.MONGODB: "mongodb",
    ServiceType.NGINX: "nginx",
    ServiceType.APACHE: "apache",
    ServiceType.TRAEFIK: "traefik",
    ServiceType.RABBITMQ: "rabbitmq",
    ServiceType.MINIO: "minio",
    ServiceType.PORTAINER: "portainer",
    ServiceType.WPPCONNECT: "wppconnect",
    ServiceType.EVOLUTION_API: "evolution_api",
    ServiceType.TYPEBOT: "typebot",
    ServiceType.SUPABASE: "supabase",
    ServiceType.OPENWEBUI: "openwebui",
    ServiceType.OLLAMA: "ollama",
    ServiceType.CUSTOM: "server",
}


def detect_service_type(name: str, image: str = "", command: str = "") -> ServiceType:
    """Detecta tipo de serviço baseado em nome, imagem ou comando."""
    search_text = f"{name} {image} {command}".lower()

    for service_type, patterns in SERVICE_PATTERNS.items():
        for pattern in patterns:
            if pattern in search_text:
                return service_type

    return ServiceType.CUSTOM


def get_service_icon(service_type: ServiceType) -> str:
    return SERVICE_ICONS.get(service_type, "server")


def get_default_restart_command(service_type: ServiceType, name: str) -> str:
    """Retorna comando de restart padrão por tipo."""
    commands = {
        ServiceType.DOCKER: f"docker restart {name}",
        ServiceType.DOCKER_COMPOSE: f"docker compose -p {name} restart",
        ServiceType.PM2: f"pm2 restart {name}",
        ServiceType.SYSTEMD: f"systemctl restart {name}",
        ServiceType.NGINX: "systemctl restart nginx",
        ServiceType.APACHE: "systemctl restart apache2",
        ServiceType.POSTGRESQL: "systemctl restart postgresql",
        ServiceType.MYSQL: "systemctl restart mysql",
        ServiceType.MARIADB: "systemctl restart mariadb",
        ServiceType.MONGODB: "systemctl restart mongod",
        ServiceType.REDIS: "systemctl restart redis",
    }
    return commands.get(service_type, f"systemctl restart {name}")
