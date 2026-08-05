package com.vpsguardian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vpsguardian.app.data.local.entity.AlertEntity
import com.vpsguardian.app.data.local.entity.HistoryEntity
import com.vpsguardian.app.data.local.entity.ServiceEntity
import com.vpsguardian.app.data.local.entity.VpsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VpsDao {
    @Query("SELECT * FROM vps_servers ORDER BY name ASC")
    fun getAll(): Flow<List<VpsEntity>>

    @Query("SELECT * FROM vps_servers WHERE id = :id")
    fun getById(id: Long): Flow<VpsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vps: VpsEntity): Long

    @Update
    suspend fun update(vps: VpsEntity)

    @Query("DELETE FROM vps_servers WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services WHERE vpsId = :vpsId ORDER BY name ASC")
    fun getByVps(vpsId: Long): Flow<List<ServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<ServiceEntity>)

    @Query("DELETE FROM services WHERE vpsId = :vpsId")
    suspend fun deleteByVps(vpsId: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    fun getAll(limit: Int = 100): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE vpsId = :vpsId ORDER BY timestamp DESC LIMIT :limit")
    fun getByVps(vpsId: Long, limit: Int = 100): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: HistoryEntity)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE read = 0 ORDER BY timestamp DESC")
    fun getUnread(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    @Query("UPDATE alerts SET read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM alerts")
    suspend fun clearAll()
}
