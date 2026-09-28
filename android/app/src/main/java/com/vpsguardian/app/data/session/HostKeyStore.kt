package com.vpsguardian.app.data.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostKeyStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "vps_guardian_hostkeys",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun verifyOrSave(ip: String, port: Int, fingerprint: String) {
        if (fingerprint.isBlank()) return
        val key = "$ip:$port"
        val saved = prefs.getString(key, null)
        if (saved == null) {
            prefs.edit().putString(key, fingerprint).apply()
            return
        }
        if (saved != fingerprint) {
            throw Exception(
                "A chave SSH de $ip:$port mudou. Confirme o IP ou apague a chave em Configurações."
            )
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasKey(ip: String, port: Int): Boolean =
        prefs.contains("$ip:$port")
}
