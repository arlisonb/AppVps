"""Rotas da API REST do agente VPS Guardian."""

from datetime import timedelta

from fastapi import APIRouter, Depends, HTTPException, Request

from app.config import Settings, get_settings
from app.models.schemas import (
    BootDetectionResponse,
    HealthCheckResponse,
    HistoryEntry,
    InventoryResponse,
    LogsRequest,
    LogsResponse,
    RefreshRequest,
    RestartRequest,
    RestartResponse,
    SystemMetrics,
    TokenRequest,
    TokenResponse,
)
from app.security.auth import (
    create_access_token,
    create_refresh_token,
    get_current_user,
    record_failed_attempt,
    verify_token,
)
from app.services.alerts import AlertService, HealthCheckService
from app.services.discovery import ServiceDiscovery
from app.services.history import HistoryService
from app.services.logs import LogsService
from app.services.metrics import MetricsCollector
from app.services.restart import RestartService
from app.services.system_info import SystemInfo

router = APIRouter()

# Instâncias singleton dos serviços
_discovery = ServiceDiscovery()
_metrics = MetricsCollector()
_restart = RestartService()
_logs = LogsService()
_health = HealthCheckService()


def _get_system_info(settings: Settings) -> SystemInfo:
    return SystemInfo(settings.state_file, settings.boot_id_file)


def _get_history(settings: Settings) -> HistoryService:
    return HistoryService(settings.history_dir)


def _get_alerts(settings: Settings) -> AlertService:
    return AlertService(settings)


# Cache de inventário
_cached_services: list = []
_cached_vps = None


@router.post("/auth/token", response_model=TokenResponse)
async def authenticate(
    request: Request,
    body: TokenRequest,
    settings: Settings = Depends(get_settings),
):
    """Autentica com token do agente e retorna JWT."""
    if body.token != settings.api_token:
        record_failed_attempt(request, settings)
        raise HTTPException(status_code=401, detail="Token inválido")

    access_token = create_access_token(
        {"sub": "vps-guardian-agent"},
        settings,
        timedelta(minutes=settings.jwt_expire_minutes),
    )
    refresh_token = create_refresh_token({"sub": "vps-guardian-agent"}, settings)

    history = _get_history(settings)
    history.record("login", "agent", "Autenticação bem-sucedida")

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=settings.jwt_expire_minutes * 60,
    )


@router.post("/auth/refresh", response_model=TokenResponse)
async def refresh_token(
    body: RefreshRequest,
    settings: Settings = Depends(get_settings),
):
    """Renova access token usando refresh token."""
    payload = verify_token(body.refresh_token, settings, "refresh")

    access_token = create_access_token(
        {"sub": payload["sub"]},
        settings,
        timedelta(minutes=settings.jwt_expire_minutes),
    )
    new_refresh = create_refresh_token({"sub": payload["sub"]}, settings)

    return TokenResponse(
        access_token=access_token,
        refresh_token=new_refresh,
        expires_in=settings.jwt_expire_minutes * 60,
    )


@router.get("/inventory", response_model=InventoryResponse)
async def get_inventory(
    user: dict = Depends(get_current_user),
    settings: Settings = Depends(get_settings),
):
    """Retorna inventário completo da VPS com descoberta automática."""
    global _cached_services, _cached_vps

    system_info = _get_system_info(settings)
    services = await _discovery.discover_all()
    services = await _discovery.enrich_with_metrics(services)
    vps = await system_info.build_vps_info(services)

    _cached_services = services
    _cached_vps = vps

    history = _get_history(settings)
    history.record("discovery", vps.hostname, f"Inventário atualizado: {len(services)} serviços")

    return InventoryResponse(vps=vps, services=services)


@router.get("/metrics", response_model=SystemMetrics)
async def get_metrics(user: dict = Depends(get_current_user)):
    """Retorna métricas em tempo real."""
    return _metrics.collect()


@router.get("/boot-check", response_model=BootDetectionResponse)
async def check_boot(
    user: dict = Depends(get_current_user),
    settings: Settings = Depends(get_settings),
):
    """Verifica se a VPS foi reiniciada."""
    system_info = _get_system_info(settings)
    services = _cached_services or await _discovery.discover_all()
    result = system_info.check_reboot(services)

    if result.reboot_detected:
        history = _get_history(settings)
        history.record(
            "reboot_detected",
            result.current_boot_id,
            result.message,
            {"offline_services": result.offline_services},
        )

    return result


@router.post("/restart", response_model=RestartResponse)
async def restart_service(
    body: RestartRequest,
    user: dict = Depends(get_current_user),
    settings: Settings = Depends(get_settings),
):
    """Executa restart autorizado de serviço ou sistema."""
    result = await _restart.execute(body)

    history = _get_history(settings)
    history.record(
        "restart" if result.success else "restart_failed",
        body.target,
        result.message,
        {"type": body.restart_type.value, "force": body.force},
    )

    return result


@router.post("/logs", response_model=LogsResponse)
async def get_logs(
    body: LogsRequest,
    user: dict = Depends(get_current_user),
):
    """Obtém logs de um serviço."""
    services = _cached_services or await _discovery.discover_all()
    return await _logs.get_logs(body, services)


@router.get("/health-check/{service_id}", response_model=HealthCheckResponse)
async def health_check(
    service_id: str,
    user: dict = Depends(get_current_user),
):
    """Executa health check em um serviço."""
    services = _cached_services or await _discovery.discover_all()
    service = next((s for s in services if s.id == service_id), None)
    if not service:
        raise HTTPException(status_code=404, detail="Serviço não encontrado")
    return await _health.check(service)


@router.get("/history", response_model=list[HistoryEntry])
async def get_history(
    limit: int = 100,
    event_type: str | None = None,
    target: str | None = None,
    user: dict = Depends(get_current_user),
    settings: Settings = Depends(get_settings),
):
    """Retorna histórico de eventos."""
    history = _get_history(settings)
    return history.get_history(limit, event_type, target)


@router.get("/alerts")
async def get_alerts(
    user: dict = Depends(get_current_user),
    settings: Settings = Depends(get_settings),
):
    """Retorna alertas ativos baseados em métricas e serviços."""
    alerts_service = _get_alerts(settings)
    system_info = _get_system_info(settings)
    services = _cached_services or await _discovery.discover_all()
    vps = _cached_vps or await system_info.build_vps_info(services)

    metric_alerts = alerts_service.check_metrics(vps.hostname, vps.metrics)
    service_alerts = alerts_service.check_services(vps.hostname, services)

    return metric_alerts + service_alerts
