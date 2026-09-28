package com.vpsguardian.app.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vpsguardian.app.data.session.AppSettingsStore
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleMonitoring(context, replace = true)
        }
    }

    companion object {
        fun scheduleMonitoring(context: Context, minutes: Int? = null, replace: Boolean = false) {
            val interval = (minutes ?: context.getSharedPreferences(AppSettingsStore.PREFS, Context.MODE_PRIVATE)
                .getInt("interval_min", 15)).coerceAtLeast(15)

            val workRequest = PeriodicWorkRequestBuilder<MonitorWorker>(
                interval.toLong(), TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "vps_monitor",
                if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
