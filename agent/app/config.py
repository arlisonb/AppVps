"""Configurações do agente VPS Guardian."""

from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "VPS Guardian Agent"
    app_version: str = "1.0.0"
    host: str = "0.0.0.0"
    port: int = 8443

    # Segurança
    jwt_secret: str = "change-me-in-production-use-openssl-rand-hex-32"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 60
    api_token: str = "change-me-agent-token"
    rate_limit_per_minute: int = 120
    max_failed_attempts: int = 5
    blacklist_duration_minutes: int = 30

    # TLS
    ssl_certfile: str | None = None
    ssl_keyfile: str | None = None

    # Monitoramento
    boot_id_file: Path = Path("/proc/sys/kernel/random/boot_id")
    state_file: Path = Path("/var/lib/vps-guardian/state.json")
    history_dir: Path = Path("/var/lib/vps-guardian/history")
    metrics_interval_seconds: int = 5

    # Limites de alerta
    cpu_alert_threshold: float = 90.0
    ram_alert_threshold: float = 90.0
    disk_alert_threshold: float = 90.0
    temp_alert_threshold: float = 80.0


@lru_cache
def get_settings() -> Settings:
    return Settings()
