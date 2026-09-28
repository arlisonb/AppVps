package com.vpsguardian.app.data.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) { prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply() }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) { prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply() }

    var monitorIntervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL, 15).coerceAtLeast(15)
        set(value) { prefs.edit().putInt(KEY_INTERVAL, value.coerceAtLeast(15)).apply() }

    var ramThreshold: Int
        get() = prefs.getInt(KEY_RAM, 85)
        set(value) { prefs.edit().putInt(KEY_RAM, value).apply() }

    var diskThreshold: Int
        get() = prefs.getInt(KEY_DISK, 90)
        set(value) { prefs.edit().putInt(KEY_DISK, value).apply() }

    companion object {
        const val PREFS = "vps_guardian_settings"
        private const val KEY_BIOMETRIC = "biometric"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_INTERVAL = "interval_min"
        private const val KEY_RAM = "ram_threshold"
        private const val KEY_DISK = "disk_threshold"
    }
}
