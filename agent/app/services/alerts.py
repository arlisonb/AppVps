"""Verificação de health check e alertas."""

import time
from datetime import datetime, timezone

import httpx

from app.config import Settings
from app.models.schemas import AlertEvent, HealthCheckResponse, ServiceInfo, ServiceStatus, SystemMetrics


class AlertService:
    """Gera alertas baseados em métricas e status de serviços."""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def check_metrics(self, hostname: str, metrics: SystemMetrics) -> list[AlertEvent]:
        alerts: list[AlertEvent] = []

        if metrics.cpu_percent >= self.settings.cpu_alert_threshold:
            alerts.append(AlertEvent(
                alert_type="cpu_high",
                severity="warning",
                title="CPU Alta",
                message=f"CPU em {metrics.cpu_percent}% (limite: {self.settings.cpu_alert_threshold}%)",
                vps_hostname=hostname,
                value=metrics.cpu_percent,
                threshold=self.settings.cpu_alert_threshold,
            ))

        if metrics.ram_percent >= self.settings.ram_alert_threshold:
            alerts.append(AlertEvent(
                alert_type="ram_high",
                severity="warning",
                title="RAM Alta",
                message=f"RAM em {metrics.ram_percent}% (limite: {self.settings.ram_alert_threshold}%)",
                vps_hostname=hostname,
                value=metrics.ram_percent,
                threshold=self.settings.ram_alert_threshold,
            ))

        if metrics.disk.percent >= self.settings.disk_alert_threshold:
            alerts.append(AlertEvent(
                alert_type="disk_high",
                severity="warning",
                title="Disco Cheio",
                message=f"Disco em {metrics.disk.percent}% (limite: {self.settings.disk_alert_threshold}%)",
                vps_hostname=hostname,
                value=metrics.disk.percent,
                threshold=self.settings.disk_alert_threshold,
            ))

        if metrics.temperature_celsius and metrics.temperature_celsius >= self.settings.temp_alert_threshold:
            alerts.append(AlertEvent(
                alert_type="temperature_high",
                severity="critical",
                title="Temperatura Alta",
                message=f"Temperatura em {metrics.temperature_celsius}°C",
                vps_hostname=hostname,
                value=metrics.temperature_celsius,
                threshold=self.settings.temp_alert_threshold,
            ))

        return alerts

    def check_services(self, hostname: str, services: list[ServiceInfo]) -> list[AlertEvent]:
        alerts: list[AlertEvent] = []

        for svc in services:
            if svc.status == ServiceStatus.OFFLINE:
                alerts.append(AlertEvent(
                    alert_type="service_offline",
                    severity="critical",
                    title="Serviço Offline",
                    message=f"O serviço {svc.name} está offline",
                    vps_hostname=hostname,
                    service_name=svc.name,
                ))
            elif svc.status == ServiceStatus.ERROR:
                alerts.append(AlertEvent(
                    alert_type="service_error",
                    severity="warning",
                    title="Serviço com Erro",
                    message=f"O serviço {svc.name} reportou erro",
                    vps_hostname=hostname,
                    service_name=svc.name,
                ))

        return alerts


class HealthCheckService:
    """Executa health checks HTTP em serviços."""

    async def check(self, service: ServiceInfo) -> HealthCheckResponse:
        if not service.health_check_url:
            return HealthCheckResponse(
                service_id=service.id,
                service_name=service.name,
                url=None,
                healthy=service.status.value == "online",
                message="Sem URL de health check configurada",
            )

        start = time.monotonic()
        try:
            async with httpx.AsyncClient(timeout=10.0, verify=False) as client:
                response = await client.get(service.health_check_url)
                elapsed_ms = (time.monotonic() - start) * 1000

                return HealthCheckResponse(
                    service_id=service.id,
                    service_name=service.name,
                    url=service.health_check_url,
                    healthy=200 <= response.status_code < 400,
                    status_code=response.status_code,
                    response_time_ms=round(elapsed_ms, 2),
                    message="OK" if response.status_code < 400 else f"HTTP {response.status_code}",
                )
        except Exception as exc:
            elapsed_ms = (time.monotonic() - start) * 1000
            return HealthCheckResponse(
                service_id=service.id,
                service_name=service.name,
                url=service.health_check_url,
                healthy=False,
                response_time_ms=round(elapsed_ms, 2),
                message=str(exc),
            )
