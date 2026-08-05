package com.vpsguardian.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vps_servers")
data class VpsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val ip: String,
    val port: Int = 8443,
    val encryptedToken: String,
    val location: String = "",
    val osName: String = "",
    val osVersion: String = "",
    val icon: String = "server",
    val colorHex: String = "#2196F3",
    val groupName: String = "",
    val status: String = "DISCONNECTED",
    val cpuPercent: Float = 0f,
    val ramPercent: Float = 0f,
    val diskPercent: Float = 0f,
    val uptimeSeconds: Long = 0,
    val lastBoot: String = "",
    val servicesOnline: Int = 0,
    val servicesOffline: Int = 0,
    val servicesError: Int = 0,
    val temperature: Float? = null,
    val lastSync: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val vpsId: Long,
    val name: String,
    val type: String,
    val icon: String = "server",
    val status: String,
    val initMethod: String = "",
    val port: Int? = null,
    val restartCommand: String = "",
    val healthCheckUrl: String? = null,
    val uptimeSeconds: Long = 0,
    val pid: Int? = null,
    val cpuPercent: Float = 0f,
    val memoryMb: Float = 0f,
    val memoryPercent: Float = 0f,
    val version: String? = null,
    val containerId: String? = null,
    val lastRestart: Long = 0
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val vpsId: Long,
    val eventType: String,
    val target: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val vpsId: Long,
    val alertType: String,
    val severity: String,
    val title: String,
    val message: String,
    val serviceName: String? = null,
    val value: Float? = null,
    val threshold: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
