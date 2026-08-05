package com.vpsguardian.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(val token: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class VpsInfoDto(
    val hostname: String,
    @SerialName("os_name") val osName: String,
    @SerialName("os_version") val osVersion: String,
    val kernel: String,
    val architecture: String,
    @SerialName("uptime_seconds") val uptimeSeconds: Long,
    @SerialName("last_boot") val lastBoot: String,
    @SerialName("boot_id") val bootId: String,
    @SerialName("ip_addresses") val ipAddresses: List<String> = emptyList(),
    val status: String = "online",
    val metrics: SystemMetricsDto = SystemMetricsDto(),
    @SerialName("services_online") val servicesOnline: Int = 0,
    @SerialName("services_offline") val servicesOffline: Int = 0,
    @SerialName("services_error") val servicesError: Int = 0
)

@Serializable
data class SystemMetricsDto(
    @SerialName("cpu_percent") val cpuPercent: Float = 0f,
    @SerialName("cpu_count") val cpuCount: Int = 0,
    @SerialName("ram_total_mb") val ramTotalMb: Float = 0f,
    @SerialName("ram_used_mb") val ramUsedMb: Float = 0f,
    @SerialName("ram_percent") val ramPercent: Float = 0f,
    @SerialName("swap_total_mb") val swapTotalMb: Float = 0f,
    @SerialName("swap_used_mb") val swapUsedMb: Float = 0f,
    @SerialName("swap_percent") val swapPercent: Float = 0f,
    val disk: DiskMetricsDto = DiskMetricsDto(),
    val network: NetworkMetricsDto = NetworkMetricsDto(),
    @SerialName("temperature_celsius") val temperatureCelsius: Float? = null,
    @SerialName("process_count") val processCount: Int = 0,
    @SerialName("load_average") val loadAverage: List<Float> = emptyList(),
    val timestamp: String = ""
)

@Serializable
data class DiskMetricsDto(
    @SerialName("total_gb") val totalGb: Float = 0f,
    @SerialName("used_gb") val usedGb: Float = 0f,
    @SerialName("free_gb") val freeGb: Float = 0f,
    val percent: Float = 0f,
    @SerialName("io_read_mbps") val ioReadMbps: Float = 0f,
    @SerialName("io_write_mbps") val ioWriteMbps: Float = 0f
)

@Serializable
data class NetworkMetricsDto(
    @SerialName("upload_mbps") val uploadMbps: Float = 0f,
    @SerialName("download_mbps") val downloadMbps: Float = 0f
)

@Serializable
data class ServiceInfoDto(
    val id: String,
    val name: String,
    val type: String,
    val icon: String = "server",
    val status: String,
    @SerialName("init_method") val initMethod: String = "",
    val port: Int? = null,
    @SerialName("restart_command") val restartCommand: String = "",
    @SerialName("health_check_url") val healthCheckUrl: String? = null,
    @SerialName("uptime_seconds") val uptimeSeconds: Long = 0,
    val pid: Int? = null,
    @SerialName("cpu_percent") val cpuPercent: Float = 0f,
    @SerialName("memory_mb") val memoryMb: Float = 0f,
    @SerialName("memory_percent") val memoryPercent: Float = 0f,
    val version: String? = null,
    @SerialName("container_id") val containerId: String? = null
)

@Serializable
data class InventoryResponseDto(
    val vps: VpsInfoDto,
    val services: List<ServiceInfoDto>,
    @SerialName("discovered_at") val discoveredAt: String = ""
)

@Serializable
data class RestartRequestDto(
    @SerialName("restart_type") val restartType: String,
    val target: String,
    @SerialName("custom_command") val customCommand: String? = null,
    val force: Boolean = false
)

@Serializable
data class RestartResponseDto(
    val success: Boolean,
    val message: String,
    val target: String,
    @SerialName("executed_at") val executedAt: String = ""
)

@Serializable
data class LogsRequestDto(
    @SerialName("service_id") val serviceId: String,
    val lines: Int = 500,
    val search: String? = null
)

@Serializable
data class LogsResponseDto(
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
    val lines: List<String>,
    @SerialName("total_lines") val totalLines: Int,
    val truncated: Boolean = false
)

@Serializable
data class HealthCheckResponseDto(
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
    val url: String? = null,
    val healthy: Boolean,
    @SerialName("status_code") val statusCode: Int? = null,
    @SerialName("response_time_ms") val responseTimeMs: Float? = null,
    val message: String = ""
)

@Serializable
data class BootDetectionDto(
    @SerialName("reboot_detected") val rebootDetected: Boolean,
    @SerialName("previous_boot_id") val previousBootId: String? = null,
    @SerialName("current_boot_id") val currentBootId: String,
    @SerialName("offline_services") val offlineServices: List<String> = emptyList(),
    val message: String = ""
)

@Serializable
data class HistoryEntryDto(
    val id: String,
    @SerialName("event_type") val eventType: String,
    val target: String,
    val description: String,
    val timestamp: String = ""
)

@Serializable
data class AlertEventDto(
    @SerialName("alert_type") val alertType: String,
    val severity: String,
    val title: String,
    val message: String,
    @SerialName("vps_hostname") val vpsHostname: String,
    @SerialName("service_name") val serviceName: String? = null,
    val value: Float? = null,
    val threshold: Float? = null,
    val timestamp: String = ""
)
