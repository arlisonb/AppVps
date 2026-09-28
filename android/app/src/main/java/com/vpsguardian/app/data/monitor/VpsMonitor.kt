package com.vpsguardian.app.data.monitor

import com.vpsguardian.app.data.session.AppSettingsStore
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.data.ssh.SshRepository
import com.vpsguardian.app.domain.model.ServiceIssue
import com.vpsguardian.app.domain.model.ServiceIssueDetector
import com.vpsguardian.app.domain.model.VpsCredentials
import com.vpsguardian.app.domain.model.VpsSession
import com.vpsguardian.app.domain.model.WppHealthSnapshot
import com.vpsguardian.app.notifications.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton

data class MonitorResult(
    val issues: List<ServiceIssue>,
    val wppHealth: Map<String, WppHealthSnapshot> = emptyMap()
)

@Singleton
class VpsMonitor @Inject constructor(
    private val sshRepository: SshRepository,
    private val alertStateStore: AlertStateStore,
    private val notificationHelper: NotificationHelper,
    private val sessionManager: SessionManager,
    private val settings: AppSettingsStore
) {

    suspend fun runCheck(credentials: VpsCredentials, notify: Boolean = true): Result<MonitorResult> {
        return sshRepository.connect(credentials).mapCatching { session ->
            sessionManager.setSession(session)
            val result = fromSession(session)
            publish(result.issues, notify)
            result
        }.onFailure {
            if (notify && settings.notificationsEnabled) {
                notificationHelper.notifyVpsUnreachable(it.message ?: "VPS inacessível")
            }
        }
    }

    fun seedFromSession(session: VpsSession, notify: Boolean = false) {
        sessionManager.setSession(session)
        publish(fromSession(session).issues, notify)
    }

    fun fromSession(session: VpsSession): MonitorResult {
        val issues = ServiceIssueDetector.detect(
            session = session,
            ramThreshold = settings.ramThreshold,
            diskThreshold = settings.diskThreshold
        )
        return MonitorResult(issues = issues, wppHealth = session.wppHealth)
    }

    private fun publish(issues: List<ServiceIssue>, notify: Boolean) {
        if (notify && settings.notificationsEnabled) {
            alertStateStore.processChanges(issues, silent = false).forEach { change ->
                notificationHelper.notifyChange(change)
            }
        } else {
            alertStateStore.processChanges(issues, silent = true)
        }
    }
}
