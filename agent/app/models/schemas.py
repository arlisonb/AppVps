"""Schemas Pydantic para API do agente."""

from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class ServiceType(str, Enum):
    DOCKER = "docker"
    DOCKER_COMPOSE = "docker_compose"
    PM2 = "pm2"
    SYSTEMD = "systemd"
    FASTAPI = "fastapi"
    NODEJS = "nodejs"
    PYTHON = "python"
    REACT = "react"
    NEXTJS = "nextjs"
    REDIS = "redis"
    POSTGRESQL = "postgresql"
    MYSQL = "mysql"
    MARIADB = "mariadb"
    MONGODB = "mongodb"
    NGINX = "nginx"
    APACHE = "apache"
    TRAEFIK = "traefik"
    RABBITMQ = "rabbitmq"
    MINIO = "minio"
    PORTAINER = "portainer"
    WPPCONNECT = "wppconnect"
    EVOLUTION_API = "evolution_api"
    TYPEBOT = "typebot"
    SUPABASE = "supabase"
    OPENWEBUI = "openwebui"
    OLLAMA = "ollama"
    CUSTOM = "custom"


class ServiceStatus(str, Enum):
    ONLINE = "online"
    OFFLINE = "offline"
    ERROR = "error"
    STARTING = "starting"
    STOPPED = "stopped"
    UNKNOWN = "unknown"


class VpsStatus(str, Enum):
    ONLINE = "online"
    ATTENTION = "attention"
    OFFLINE = "offline"
    DISCONNECTED = "disconnected"


class RestartType(str, Enum):
    SERVICE = "service"
    VPS = "vps"
    DOCKER = "docker"
    PM2 = "pm2"
    CONTAINER = "container"
    COMPOSE = "compose"
    NGINX = "nginx"
    DATABASE = "database"
    CUSTOM = "custom"


class TokenRequest(BaseModel):
    token: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int


class RefreshRequest(BaseModel):
    refresh_token: str


class ServiceInfo(BaseModel):
    id: str
    name: str
    type: ServiceType
    icon: str = "server"
    status: ServiceStatus
    init_method: str = ""
    port: int | None = None
    ports: list[int] = Field(default_factory=list)
    restart_command: str = ""
    health_check_url: str | None = None
    last_execution: datetime | None = None
    uptime_seconds: int = 0
    pid: int | None = None
    cpu_percent: float = 0.0
    memory_mb: float = 0.0
    memory_percent: float = 0.0
    version: str | None = None
    container_id: str | None = None
    compose_project: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class NetworkMetrics(BaseModel):
    bytes_sent: int = 0
    bytes_recv: int = 0
    packets_sent: int = 0
    packets_recv: int = 0
    upload_mbps: float = 0.0
    download_mbps: float = 0.0


class DiskMetrics(BaseModel):
    total_gb: float = 0.0
    used_gb: float = 0.0
    free_gb: float = 0.0
    percent: float = 0.0
    read_bytes: int = 0
    write_bytes: int = 0
    io_read_mbps: float = 0.0
    io_write_mbps: float = 0.0


class SystemMetrics(BaseModel):
    cpu_percent: float = 0.0
    cpu_count: int = 0
    ram_total_mb: float = 0.0
    ram_used_mb: float = 0.0
    ram_percent: float = 0.0
    swap_total_mb: float = 0.0
    swap_used_mb: float = 0.0
    swap_percent: float = 0.0
    disk: DiskMetrics = Field(default_factory=DiskMetrics)
    network: NetworkMetrics = Field(default_factory=NetworkMetrics)
    temperature_celsius: float | None = None
    process_count: int = 0
    load_average: list[float] = Field(default_factory=list)
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class VpsInfo(BaseModel):
    hostname: str
    os_name: str
    os_version: str
    kernel: str
    architecture: str
    uptime_seconds: int
    last_boot: datetime
    boot_id: str
    ip_addresses: list[str] = Field(default_factory=list)
    status: VpsStatus = VpsStatus.ONLINE
    metrics: SystemMetrics = Field(default_factory=SystemMetrics)
    services_online: int = 0
    services_offline: int = 0
    services_error: int = 0


class InventoryResponse(BaseModel):
    vps: VpsInfo
    services: list[ServiceInfo]
    discovered_at: datetime = Field(default_factory=datetime.utcnow)


class RestartRequest(BaseModel):
    restart_type: RestartType
    target: str
    custom_command: str | None = None
    force: bool = False


class RestartResponse(BaseModel):
    success: bool
    message: str
    target: str
    executed_at: datetime = Field(default_factory=datetime.utcnow)


class LogsRequest(BaseModel):
    service_id: str
    lines: int = 500
    search: str | None = None
    since: str | None = None


class LogsResponse(BaseModel):
    service_id: str
    service_name: str
    lines: list[str]
    total_lines: int
    truncated: bool = False


class HealthCheckResponse(BaseModel):
    service_id: str
    service_name: str
    url: str | None
    healthy: bool
    status_code: int | None = None
    response_time_ms: float | None = None
    message: str = ""


class HistoryEntry(BaseModel):
    id: str
    event_type: str
    target: str
    description: str
    metadata: dict[str, Any] = Field(default_factory=dict)
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class BootDetectionResponse(BaseModel):
    reboot_detected: bool
    previous_boot_id: str | None = None
    current_boot_id: str
    offline_services: list[str] = Field(default_factory=list)
    message: str = ""


class AlertEvent(BaseModel):
    alert_type: str
    severity: str
    title: str
    message: str
    vps_hostname: str
    service_name: str | None = None
    value: float | None = None
    threshold: float | None = None
    timestamp: datetime = Field(default_factory=datetime.utcnow)
