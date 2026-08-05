package com.vpsguardian.app.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleMonitoring(context)
        }
    }

    companion object {
        fun scheduleMonitoring(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MonitorWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "vps_monitor",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
