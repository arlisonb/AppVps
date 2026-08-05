package com.vpsguardian.app.data.mapper

import com.vpsguardian.app.data.local.entity.AlertEntity
import com.vpsguardian.app.data.local.entity.HistoryEntity
import com.vpsguardian.app.data.local.entity.ServiceEntity
import com.vpsguardian.app.data.local.entity.VpsEntity
import com.vpsguardian.app.data.remote.dto.AlertEventDto
import com.vpsguardian.app.data.remote.dto.HistoryEntryDto
import com.vpsguardian.app.data.remote.dto.InventoryResponseDto
import com.vpsguardian.app.data.remote.dto.ServiceInfoDto
import com.vpsguardian.app.data.remote.dto.SystemMetricsDto
import com.vpsguardian.app.data.remote.dto.VpsInfoDto
import com.vpsguardian.app.domain.model.Alert
import com.vpsguardian.app.domain.model.HistoryEvent
import com.vpsguardian.app.domain.model.Service
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.domain.model.ServiceType
import com.vpsguardian.app.domain.model.SystemMetrics
import com.vpsguardian.app.domain.model.VpsServer
import com.vpsguardian.app.domain.model.VpsStatus
import java.util.UUID

object Mappers {

    fun VpsEntity.toDomain(decryptedToken: String = ""): VpsServer = VpsServer(
        id = id,
        name = name,
        description = description,
        ip = ip,
        port = port,
        token = decryptedToken,
        location = location,
        osName = osName,
        osVersion = osVersion,
        icon = icon,
        colorHex = colorHex,
        group = groupName,
        status = VpsStatus.valueOf(status),
        cpuPercent = cpuPercent,
        ramPercent = ramPercent,
        diskPercent = diskPercent,
        uptimeSeconds = uptimeSeconds,
        lastBoot = lastBoot,
        servicesOnline = servicesOnline,
        servicesOffline = servicesOffline,
        servicesError = servicesError,
        temperature = temperature,
        lastSync = lastSync,
        createdAt = createdAt
    )

    fun VpsServer.toEntity(encryptedToken: String): VpsEntity = VpsEntity(
        id = id,
        name = name,
        description = description,
        ip = ip,
        port = port,
        encryptedToken = encryptedToken,
        location = location,
        osName = osName,
        osVersion = osVersion,
        icon = icon,
        colorHex = colorHex,
        groupName = group,
        status = status.name,
        cpuPercent = cpuPercent,
        ramPercent = ramPercent,
        diskPercent = diskPercent,
        uptimeSeconds = uptimeSeconds,
        lastBoot = lastBoot,
        servicesOnline = servicesOnline,
        servicesOffline = servicesOffline,
        servicesError = servicesError,
        temperature = temperature,
        lastSync = lastSync,
        createdAt = createdAt
    )

    fun ServiceEntity.toDomain(): Service = Service(
        id = id,
        vpsId = vpsId,
        name = name,
        type = parseServiceType(type),
        icon = icon,
        status = ServiceStatus.valueOf(status),
        initMethod = initMethod,
        port = port,
        restartCommand = restartCommand,
        healthCheckUrl = healthCheckUrl,
        uptimeSeconds = uptimeSeconds,
        pid = pid,
        cpuPercent = cpuPercent,
        memoryMb = memoryMb,
        memoryPercent = memoryPercent,
        version = version,
        containerId = containerId,
        lastRestart = lastRestart
    )

    fun ServiceInfoDto.toEntity(vpsId: Long): ServiceEntity = ServiceEntity(
        id = id,
        vpsId = vpsId,
        name = name,
        type = type.uppercase(),
        icon = icon,
        status = status.uppercase(),
        initMethod = initMethod,
        port = port,
        restartCommand = restartCommand,
        healthCheckUrl = healthCheckUrl,
        uptimeSeconds = uptimeSeconds,
        pid = pid,
        cpuPercent = cpuPercent,
        memoryMb = memoryMb,
        memoryPercent = memoryPercent,
        version = version,
        containerId = containerId
    )

    fun VpsInfoDto.toVpsUpdate(existing: VpsServer): VpsServer = existing.copy(
        osName = osName,
        osVersion = osVersion,
        status = parseVpsStatus(status),
        cpuPercent = metrics.cpuPercent,
        ramPercent = metrics.ramPercent,
        diskPercent = metrics.disk.percent,
        uptimeSeconds = uptimeSeconds,
        lastBoot = lastBoot,
        servicesOnline = servicesOnline,
        servicesOffline = servicesOffline,
        servicesError = servicesError,
        temperature = metrics.temperatureCelsius,
        lastSync = System.currentTimeMillis()
    )

    fun SystemMetricsDto.toDomain(): SystemMetrics = SystemMetrics(
        cpuPercent = cpuPercent,
        cpuCount = cpuCount,
        ramTotalMb = ramTotalMb,
        ramUsedMb = ramUsedMb,
        ramPercent = ramPercent,
        swapTotalMb = swapTotalMb,
        swapUsedMb = swapUsedMb,
        swapPercent = swapPercent,
        diskTotalGb = disk.totalGb,
        diskUsedGb = disk.usedGb,
        diskPercent = disk.percent,
        uploadMbps = network.uploadMbps,
        downloadMbps = network.downloadMbps,
        temperature = temperatureCelsius,
        processCount = processCount
    )

    fun HistoryEntity.toDomain(): HistoryEvent = HistoryEvent(
        id = id,
        vpsId = vpsId,
        eventType = eventType,
        target = target,
        description = description,
        timestamp = timestamp
    )

    fun HistoryEntryDto.toEntity(vpsId: Long): HistoryEntity = HistoryEntity(
        id = id,
        vpsId = vpsId,
        eventType = eventType,
        target = target,
        description = description
    )

    fun AlertEntity.toDomain(): Alert = Alert(
        id = id,
        vpsId = vpsId,
        alertType = alertType,
        severity = severity,
        title = title,
        message = message,
        serviceName = serviceName,
        value = value,
        threshold = threshold,
        timestamp = timestamp,
        read = read
    )

    fun AlertEventDto.toEntity(vpsId: Long): AlertEntity = AlertEntity(
        id = UUID.randomUUID().toString().take(8),
        vpsId = vpsId,
        alertType = alertType,
        severity = severity,
        title = title,
        message = message,
        serviceName = serviceName,
        value = value,
        threshold = threshold
    )

    private fun parseVpsStatus(status: String): VpsStatus = try {
        VpsStatus.valueOf(status.uppercase())
    } catch (_: Exception) {
        VpsStatus.ONLINE
    }

    private fun parseServiceType(type: String): ServiceType = try {
        ServiceType.valueOf(type.uppercase())
    } catch (_: Exception) {
        ServiceType.CUSTOM
    }
}
