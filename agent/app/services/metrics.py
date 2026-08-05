"""Coleta de métricas do sistema via psutil."""

import os
import time
from datetime import datetime, timezone

import psutil

from app.models.schemas import DiskMetrics, NetworkMetrics, SystemMetrics


class MetricsCollector:
    """Coleta métricas de CPU, RAM, disco, rede e temperatura."""

    def __init__(self) -> None:
        self._last_net_io: psutil._common.snetio | None = None
        self._last_disk_io: psutil._common.sdiskio | None = None
        self._last_io_time: float = 0.0

    def _get_temperature(self) -> float | None:
        try:
            temps = psutil.sensors_temperatures()
            if not temps:
                return None
            for sensor_list in temps.values():
                for sensor in sensor_list:
                    if sensor.current is not None:
                        return float(sensor.current)
        except (AttributeError, OSError):
            pass
        return None

    def _calc_io_rates(
        self,
        current_net: psutil._common.snetio,
        current_disk: psutil._common.sdiskio,
    ) -> tuple[float, float, float, float]:
        now = time.time()
        upload_mbps = download_mbps = io_read_mbps = io_write_mbps = 0.0

        if self._last_net_io and self._last_io_time:
            elapsed = now - self._last_io_time
            if elapsed > 0:
                upload_mbps = (
                    (current_net.bytes_sent - self._last_net_io.bytes_sent) * 8 / elapsed / 1_000_000
                )
                download_mbps = (
                    (current_net.bytes_recv - self._last_net_io.bytes_recv) * 8 / elapsed / 1_000_000
                )
                io_read_mbps = (
                    (current_disk.read_bytes - self._last_disk_io.read_bytes) / elapsed / 1_048_576
                )
                io_write_mbps = (
                    (current_disk.write_bytes - self._last_disk_io.write_bytes) / elapsed / 1_048_576
                )

        self._last_net_io = current_net
        self._last_disk_io = current_disk
        self._last_io_time = now

        return upload_mbps, download_mbps, io_read_mbps, io_write_mbps

    def collect(self) -> SystemMetrics:
        cpu_percent = psutil.cpu_percent(interval=0.5)
        mem = psutil.virtual_memory()
        swap = psutil.swap_memory()
        disk_usage = psutil.disk_usage("/")
        net_io = psutil.net_io_counters()
        disk_io = psutil.disk_io_counters() or psutil._common.sdiskio(0, 0, 0, 0, 0, 0, 0, 0, 0)

        upload_mbps, download_mbps, io_read_mbps, io_write_mbps = self._calc_io_rates(net_io, disk_io)

        load_avg = list(os.getloadavg()) if hasattr(os, "getloadavg") else []

        return SystemMetrics(
            cpu_percent=round(cpu_percent, 2),
            cpu_count=psutil.cpu_count() or 0,
            ram_total_mb=round(mem.total / 1_048_576, 2),
            ram_used_mb=round(mem.used / 1_048_576, 2),
            ram_percent=round(mem.percent, 2),
            swap_total_mb=round(swap.total / 1_048_576, 2),
            swap_used_mb=round(swap.used / 1_048_576, 2),
            swap_percent=round(swap.percent, 2),
            disk=DiskMetrics(
                total_gb=round(disk_usage.total / 1_073_741_824, 2),
                used_gb=round(disk_usage.used / 1_073_741_824, 2),
                free_gb=round(disk_usage.free / 1_073_741_824, 2),
                percent=round(disk_usage.percent, 2),
                read_bytes=disk_io.read_bytes,
                write_bytes=disk_io.write_bytes,
                io_read_mbps=round(io_read_mbps, 2),
                io_write_mbps=round(io_write_mbps, 2),
            ),
            network=NetworkMetrics(
                bytes_sent=net_io.bytes_sent,
                bytes_recv=net_io.bytes_recv,
                packets_sent=net_io.packets_sent,
                packets_recv=net_io.packets_recv,
                upload_mbps=round(upload_mbps, 2),
                download_mbps=round(download_mbps, 2),
            ),
            temperature_celsius=self._get_temperature(),
            process_count=len(psutil.pids()),
            load_average=load_avg,
            timestamp=datetime.now(timezone.utc),
        )
