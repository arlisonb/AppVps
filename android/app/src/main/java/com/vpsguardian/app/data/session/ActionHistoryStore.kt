package com.vpsguardian.app.data.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ActionEvent(
    val id: String,
    val type: String,
    val target: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class ActionHistoryStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("vps_guardian_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun add(type: String, target: String, description: String) {
        val current = load().toMutableList()
        current.add(
            0,
            ActionEvent(
                id = System.currentTimeMillis().toString(),
                type = type,
                target = target,
                description = description
            )
        )
        prefs.edit().putString(KEY, json.encodeToString(current.take(50))).apply()
    }

    fun load(): List<ActionEvent> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ActionEvent>>(raw) }.getOrDefault(emptyList())
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "events"
    }
}
