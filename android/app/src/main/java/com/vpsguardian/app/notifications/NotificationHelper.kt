package com.vpsguardian.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vpsguardian.app.R
import com.vpsguardian.app.data.monitor.AlertChange
import com.vpsguardian.app.data.monitor.ChangeType
import com.vpsguardian.app.domain.model.ServiceIssue
import com.vpsguardian.app.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        createChannel()
    }

    fun notifyChange(change: AlertChange) {
        when (change.type) {
            ChangeType.NEW_PROBLEM -> notifyIssue(change.issue)
            ChangeType.RECOVERED -> notifyRecovery(change.issue)
        }
    }

    fun notifyIssue(issue: ServiceIssue) {
        val title = when {
            issue.serviceName == "ram" -> "RAM alta na VPS"
            issue.serviceName == "disk" -> "Disco cheio na VPS"
            issue.serviceName == "git-push" -> "Push pendente — ${issue.projectName}"
            "whatsapp" in issue.serviceName.lowercase() || issue.type.name.contains("WPP") ->
                "WhatsApp — ${issue.projectName}"
            else -> "Sistema offline — ${issue.projectName}"
        }

        show(
            id = issue.hashCode(),
            title = title,
            body = "${issue.serviceName}: ${issue.message}",
            projectId = if (issue.isSystemMetric || issue.serviceName == "git-push") null else issue.projectId,
            serviceName = if (issue.isSystemMetric || issue.serviceName == "git-push") null else issue.serviceName
        )
    }

    fun notifyRecovery(issue: ServiceIssue) {
        show(
            id = issue.hashCode() + 1,
            title = "Recuperado — ${issue.projectName}",
            body = "${issue.serviceName} voltou ao normal",
            projectId = issue.projectId,
            serviceName = issue.serviceName
        )
    }

    fun notifyVpsUnreachable(message: String = "Não foi possível conectar na VPS") {
        show(
            id = NOTIFICATION_VPS_DOWN,
            title = "VPS inacessível",
            body = message,
            projectId = null,
            serviceName = null
        )
    }

    private fun show(
        id: Int,
        title: String,
        body: String,
        projectId: String?,
        serviceName: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            projectId?.let { putExtra(EXTRA_PROJECT_ID, it) }
            serviceName?.let { putExtra(EXTRA_SERVICE_NAME, it) }
        }

        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas VPS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos quando serviços caem ou WhatsApp desconecta"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "vps_alerts"
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_SERVICE_NAME = "service_name"
        private const val NOTIFICATION_VPS_DOWN = 9999
    }
}
