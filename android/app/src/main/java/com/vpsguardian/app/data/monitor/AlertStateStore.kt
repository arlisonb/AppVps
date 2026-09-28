package com.vpsguardian.app.data.monitor

import android.content.Context
import com.vpsguardian.app.domain.model.ServiceIssue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AlertChange(
    val issue: ServiceIssue,
    val type: ChangeType
)

enum class ChangeType {
    NEW_PROBLEM,
    RECOVERED
}

@Singleton
class AlertStateStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("vps_alert_states", Context.MODE_PRIVATE)

    fun processChanges(issues: List<ServiceIssue>, silent: Boolean = false): List<AlertChange> {
        if (silent) {
            issues.forEach { issue ->
                prefs.edit().putString("$PREFIX${issueKey(issue)}", issue.message).apply()
            }
            return emptyList()
        }
        val changes = mutableListOf<AlertChange>()
        val currentKeys = mutableSetOf<String>()

        issues.forEach { issue ->
            val key = issueKey(issue)
            currentKeys.add(key)
            val prev = prefs.getString(key, null)
            val state = issue.message
            if (prev != state) {
                prefs.edit().putString(key, state).apply()
                if (prev == null) {
                    changes.add(AlertChange(issue, ChangeType.NEW_PROBLEM))
                }
            }
        }

        val staleKeys = prefs.all.keys
            .filter { it.startsWith(PREFIX) }
            .map { it.removePrefix(PREFIX) }
            .filter { it !in currentKeys }

        staleKeys.forEach { key ->
            val parts = key.split("|", limit = 3)
            if (parts.size == 3) {
                changes.add(
                    AlertChange(
                        issue = ServiceIssue(
                            projectId = parts[0],
                            projectName = parts[1],
                            serviceName = parts[2],
                            type = com.vpsguardian.app.domain.model.ServiceType.CUSTOM,
                            status = com.vpsguardian.app.domain.model.ServiceStatus.ONLINE,
                            message = "Recuperado",
                            severity = com.vpsguardian.app.domain.model.IssueSeverity.WARNING
                        ),
                        type = ChangeType.RECOVERED
                    )
                )
            }
            prefs.edit().remove("$PREFIX$key").apply()
        }

        return changes
    }

    private fun issueKey(issue: ServiceIssue): String =
        "${issue.projectId}|${issue.projectName}|${issue.serviceName}"

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFIX = "k_"
    }
}
