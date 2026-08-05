package com.vpsguardian.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vpsguardian.app.data.local.dao.AlertDao
import com.vpsguardian.app.data.local.dao.HistoryDao
import com.vpsguardian.app.data.local.dao.ServiceDao
import com.vpsguardian.app.data.local.dao.VpsDao
import com.vpsguardian.app.data.local.entity.AlertEntity
import com.vpsguardian.app.data.local.entity.HistoryEntity
import com.vpsguardian.app.data.local.entity.ServiceEntity
import com.vpsguardian.app.data.local.entity.VpsEntity

@Database(
    entities = [
        VpsEntity::class,
        ServiceEntity::class,
        HistoryEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VpsDatabase : RoomDatabase() {
    abstract fun vpsDao(): VpsDao
    abstract fun serviceDao(): ServiceDao
    abstract fun historyDao(): HistoryDao
    abstract fun alertDao(): AlertDao
}
