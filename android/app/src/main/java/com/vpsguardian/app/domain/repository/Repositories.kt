package com.vpsguardian.app.domain.repository

import com.vpsguardian.app.domain.model.Alert
import com.vpsguardian.app.domain.model.AppSettings
import com.vpsguardian.app.domain.model.HistoryEvent
import com.vpsguardian.app.domain.model.Service
import com.vpsguardian.app.domain.model.SystemMetrics
import com.vpsguardian.app.domain.model.VpsServer
import kotlinx.coroutines.flow.Flow

interface VpsRepository {
    fun getAllVps(): Flow<List<VpsServer>>
    fun getVpsById(id: Long): Flow<VpsServer?>
    suspend fun addVps(vps: VpsServer): Long
    suspend fun updateVps(vps: VpsServer)
    suspend fun deleteVps(id: Long)
    suspend fun syncVps(id: Long): Result<VpsServer>
    suspend fun syncAllVps(): Result<List<VpsServer>>
}

interface ServiceRepository {
    fun getServicesByVps(vpsId: Long): Flow<List<Service>>
    suspend fun refreshServices(vpsId: Long): Result<List<Service>>
    suspend fun restartService(vpsId: Long, serviceId: String, type: String): Result<String>
    suspend fun startService(vpsId: Long, serviceId: String): Result<String>
    suspend fun stopService(vpsId: Long, serviceId: String): Result<String>
    suspend fun getLogs(vpsId: Long, serviceId: String, lines: Int, search: String?): Result<List<String>>
    suspend fun healthCheck(vpsId: Long, serviceId: String): Result<Boolean>
}

interface HistoryRepository {
    fun getHistory(vpsId: Long? = null, limit: Int = 100): Flow<List<HistoryEvent>>
    suspend fun addEvent(event: HistoryEvent)
}

interface AlertRepository {
    fun getAlerts(unreadOnly: Boolean = false): Flow<List<Alert>>
    suspend fun markAsRead(alertId: String)
    suspend fun clearAll()
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
}

interface MetricsRepository {
    suspend fun getMetrics(vpsId: Long): Result<SystemMetrics>
}
