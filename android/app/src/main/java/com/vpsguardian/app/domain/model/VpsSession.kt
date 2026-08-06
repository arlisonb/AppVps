package com.vpsguardian.app.domain.model

data class VpsCredentials(
    val ip: String,
    val username: String = "root",
    val password: String,
    val sshPort: Int = 22
)

data class VpsSession(
    val credentials: VpsCredentials,
    val hostname: String = "",
    val osInfo: String = "",
    val uptime: String = "",
    val cpuPercent: Float = 0f,
    val ramPercent: Float = 0f,
    val diskPercent: Float = 0f,
    val services: List<Service> = emptyList(),
    val connectedAt: Long = System.currentTimeMillis()
)
