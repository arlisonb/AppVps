package com.vpsguardian.app.data.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vpsguardian.app.domain.model.VpsCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "vps_guardian_credentials",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(credentials: VpsCredentials) {
        prefs.edit()
            .putString(KEY_IP, credentials.ip)
            .putString(KEY_USER, credentials.username)
            .putString(KEY_PASS, credentials.password)
            .putInt(KEY_PORT, credentials.sshPort)
            .putBoolean(KEY_HAS_CREDS, true)
            .putBoolean(KEY_REMEMBER, true)
            .apply()
    }

    fun setRememberEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER, enabled).apply()
    }

    fun isRememberEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER, true)

    /** Remove senha e flag, mantém IP/usuário para preenchimento. */
    fun clearPasswordOnly() {
        prefs.edit()
            .remove(KEY_PASS)
            .putBoolean(KEY_HAS_CREDS, false)
            .apply()
    }

    fun load(): VpsCredentials? {
        if (!prefs.getBoolean(KEY_HAS_CREDS, false)) return null
        val ip = prefs.getString(KEY_IP, null) ?: return null
        val pass = prefs.getString(KEY_PASS, null) ?: return null
        return VpsCredentials(
            ip = ip,
            username = prefs.getString(KEY_USER, "root") ?: "root",
            password = pass,
            sshPort = prefs.getInt(KEY_PORT, 22)
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasCredentials(): Boolean = prefs.getBoolean(KEY_HAS_CREDS, false)

    companion object {
        private const val KEY_IP = "ip"
        private const val KEY_USER = "user"
        private const val KEY_PASS = "pass"
        private const val KEY_PORT = "port"
        private const val KEY_HAS_CREDS = "has_creds"
        private const val KEY_REMEMBER = "remember_login"
    }
}
