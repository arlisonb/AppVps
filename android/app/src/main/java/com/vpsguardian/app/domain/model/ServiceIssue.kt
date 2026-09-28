package com.vpsguardian.app.domain.model

data class ServiceIssue(
    val projectId: String,
    val projectName: String,
    val serviceName: String,
    val type: ServiceType,
    val status: ServiceStatus,
    val message: String,
    val severity: IssueSeverity
) {
    val isSystemMetric: Boolean get() = projectId == ServiceIssueDetector.VPS_ID
}

enum class IssueSeverity {
    CRITICAL,
    WARNING
}

object ServiceIssueDetector {

    const val VPS_ID = "vps"

    fun detect(
        session: VpsSession,
        ramThreshold: Int = 85,
        diskThreshold: Int = 90
    ): List<ServiceIssue> {
        return detect(
            projects = session.projects,
            wppHealth = session.wppHealth,
            ramPercent = session.ramPercent,
            diskPercent = session.diskPercent,
            ramThreshold = ramThreshold,
            diskThreshold = diskThreshold
        )
    }

    fun detect(
        projects: List<Project>,
        wppHealth: Map<String, WppHealthSnapshot> = emptyMap(),
        ramPercent: Float = 0f,
        diskPercent: Float = 0f,
        ramThreshold: Int = 85,
        diskThreshold: Int = 90
    ): List<ServiceIssue> {
        val issues = mutableListOf<ServiceIssue>()

        if (ramPercent >= ramThreshold) {
            issues.add(
                ServiceIssue(
                    projectId = VPS_ID,
                    projectName = "VPS",
                    serviceName = "ram",
                    type = ServiceType.CUSTOM,
                    status = ServiceStatus.UNKNOWN,
                    message = "RAM em ${ramPercent.toInt()}% (limite $ramThreshold%)",
                    severity = if (ramPercent >= 92) IssueSeverity.CRITICAL else IssueSeverity.WARNING
                )
            )
        }
        if (diskPercent >= diskThreshold) {
            issues.add(
                ServiceIssue(
                    projectId = VPS_ID,
                    projectName = "VPS",
                    serviceName = "disk",
                    type = ServiceType.CUSTOM,
                    status = ServiceStatus.UNKNOWN,
                    message = "Disco em ${diskPercent.toInt()}% (limite $diskThreshold%)",
                    severity = if (diskPercent >= 95) IssueSeverity.CRITICAL else IssueSeverity.WARNING
                )
            )
        }

        projects.forEach { project ->
            project.services.forEach { service ->
                if (service.name == "Repositório" && service.initMethod == "git") return@forEach

                val healthKey = "${project.id}:${service.name}"
                val wpp = wppHealth[healthKey]

                when (service.status) {
                    ServiceStatus.OFFLINE, ServiceStatus.STOPPED -> {
                        issues.add(
                            ServiceIssue(
                                projectId = project.id,
                                projectName = project.name,
                                serviceName = service.name,
                                type = service.type,
                                status = service.status,
                                message = "Sistema parado",
                                severity = IssueSeverity.CRITICAL
                            )
                        )
                    }
                    ServiceStatus.ERROR -> {
                        issues.add(
                            ServiceIssue(
                                projectId = project.id,
                                projectName = project.name,
                                serviceName = service.name,
                                type = service.type,
                                status = service.status,
                                message = "Sistema com erro",
                                severity = IssueSeverity.CRITICAL
                            )
                        )
                    }
                    ServiceStatus.STARTING -> {
                        issues.add(
                            ServiceIssue(
                                projectId = project.id,
                                projectName = project.name,
                                serviceName = service.name,
                                type = service.type,
                                status = service.status,
                                message = "Sistema iniciando",
                                severity = IssueSeverity.WARNING
                            )
                        )
                    }
                    else -> Unit
                }

                if (wpp != null && wpp.whatsappConnected == false) {
                    issues.add(
                        ServiceIssue(
                            projectId = project.id,
                            projectName = project.name,
                            serviceName = service.name,
                            type = service.type,
                            status = service.status,
                            message = wpp.message.ifBlank { "WhatsApp desconectado" },
                            severity = IssueSeverity.CRITICAL
                        )
                    )
                }
            }

            if (project.hasPendingPush) {
                issues.add(
                    ServiceIssue(
                        projectId = project.id,
                        projectName = project.name,
                        serviceName = "git-push",
                        type = ServiceType.CUSTOM,
                        status = ServiceStatus.UNKNOWN,
                        message = "${project.git.commitsAhead} commit(s) aguardando push",
                        severity = IssueSeverity.WARNING
                    )
                )
            }
        }

        return issues.sortedBy { it.severity.ordinal }
    }
}

data class WppHealthSnapshot(
    val healthy: Boolean,
    val whatsappConnected: Boolean?,
    val message: String
)
