package com.vpsguardian.app.domain.model

enum class VpsStatus {
    ONLINE, ATTENTION, OFFLINE, DISCONNECTED
}

enum class ServiceStatus {
    ONLINE, OFFLINE, ERROR, STARTING, STOPPED, UNKNOWN
}

enum class ServiceType {
    DOCKER, DOCKER_COMPOSE, PM2, SYSTEMD, FASTAPI, NODEJS, PYTHON,
    REACT, NEXTJS, REDIS, POSTGRESQL, MYSQL, MARIADB, MONGODB,
    NGINX, APACHE, TRAEFIK, RABBITMQ, MINIO, PORTAINER,
    WPPCONNECT, EVOLUTION_API, TYPEBOT, SUPABASE, OPENWEBUI, OLLAMA, CUSTOM
}

enum class RestartType {
    SERVICE, VPS, DOCKER, PM2, CONTAINER, COMPOSE, NGINX, DATABASE, CUSTOM
}

enum class AppTheme {
    LIGHT, DARK, AMOLED
}

data class VpsServer(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val ip: String,
    val port: Int = 8443,
    val token: String,
    val location: String = "",
    val osName: String = "",
    val osVersion: String = "",
    val icon: String = "server",
    val colorHex: String = "#2196F3",
    val group: String = "",
    val status: VpsStatus = VpsStatus.DISCONNECTED,
    val cpuPercent: Float = 0f,
    val ramPercent: Float = 0f,
    val diskPercent: Float = 0f,
    val uptimeSeconds: Long = 0,
    val lastBoot: String = "",
    val servicesOnline: Int = 0,
    val servicesOffline: Int = 0,
    val servicesError: Int = 0,
    val temperature: Float? = null,
    val lastSync: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Service(
    val id: String,
    val vpsId: Long,
    val name: String,
    val type: ServiceType,
    val icon: String = "server",
    val status: ServiceStatus,
    val initMethod: String = "",
    val port: Int? = null,
    val restartCommand: String = "",
    val healthCheckUrl: String? = null,
    val uptimeSeconds: Long = 0,
    val pid: Int? = null,
    val cpuPercent: Float = 0f,
    val memoryMb: Float = 0f,
    val memoryPercent: Float = 0f,
    val version: String? = null,
    val containerId: String? = null,
    val lastRestart: Long = 0
)

data class SystemMetrics(
    val cpuPercent: Float = 0f,
    val cpuCount: Int = 0,
    val ramTotalMb: Float = 0f,
    val ramUsedMb: Float = 0f,
    val ramPercent: Float = 0f,
    val swapTotalMb: Float = 0f,
    val swapUsedMb: Float = 0f,
    val swapPercent: Float = 0f,
    val diskTotalGb: Float = 0f,
    val diskUsedGb: Float = 0f,
    val diskPercent: Float = 0f,
    val uploadMbps: Float = 0f,
    val downloadMbps: Float = 0f,
    val temperature: Float? = null,
    val processCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class HistoryEvent(
    val id: String,
    val vpsId: Long,
    val eventType: String,
    val target: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Alert(
    val id: String,
    val vpsId: Long,
    val alertType: String,
    val severity: String,
    val title: String,
    val message: String,
    val serviceName: String? = null,
    val value: Float? = null,
    val threshold: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

data class AppSettings(
    val theme: AppTheme = AppTheme.DARK,
    val language: String = "pt",
    val refreshIntervalSeconds: Int = 30,
    val cacheDurationSeconds: Int = 60,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val sessionTimeoutMinutes: Int = 30
)
