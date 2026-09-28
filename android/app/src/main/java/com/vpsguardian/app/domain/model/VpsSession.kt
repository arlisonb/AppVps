package com.vpsguardian.app.domain.model

data class VpsCredentials(
    val ip: String,
    val username: String = "root",
    val password: String,
    val sshPort: Int = 22
)

data class ProcessInfo(
    val pid: Int = 0,
    val name: String,
    val memoryMb: Float = 0f,
    val memoryPercent: Float = 0f
)

data class VpsSession(
    val credentials: VpsCredentials,
    val hostname: String = "",
    val osInfo: String = "",
    val uptime: String = "",
    val cpuPercent: Float = 0f,
    val ramPercent: Float = 0f,
    val diskPercent: Float = 0f,
    val swapPercent: Float = 0f,
    val services: List<Service> = emptyList(),
    val projects: List<Project> = emptyList(),
    val topProcesses: List<ProcessInfo> = emptyList(),
    val wppHealth: Map<String, WppHealthSnapshot> = emptyMap(),
    val connectedAt: Long = System.currentTimeMillis(),
    val lastCheck: Long = System.currentTimeMillis()
)
