package com.vpsguardian.app.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder

class MonitorForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground service para monitoramento contínuo em background
        return START_STICKY
    }
}
