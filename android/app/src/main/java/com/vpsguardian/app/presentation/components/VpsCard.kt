package com.vpsguardian.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vpsguardian.app.domain.model.VpsServer
import com.vpsguardian.app.domain.model.VpsStatus
import com.vpsguardian.app.presentation.theme.StatusAttention
import com.vpsguardian.app.presentation.theme.StatusDisconnected
import com.vpsguardian.app.presentation.theme.StatusOffline
import com.vpsguardian.app.presentation.theme.StatusOnline

@Composable
fun VpsCard(
    vps: VpsServer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (vps.status) {
            VpsStatus.ONLINE -> StatusOnline
            VpsStatus.ATTENTION -> StatusAttention
            VpsStatus.OFFLINE -> StatusOffline
            VpsStatus.DISCONNECTED -> StatusDisconnected
        },
        label = "statusColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = vps.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${vps.ip}:${vps.port}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                StatusBadge(vps.status)
            }

            if (vps.osName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${vps.osName} ${vps.osVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MetricBar("CPU", vps.cpuPercent, Icons.Default.Memory, statusColorForPercent(vps.cpuPercent))
            Spacer(modifier = Modifier.height(8.dp))
            MetricBar("RAM", vps.ramPercent, Icons.Default.Storage, statusColorForPercent(vps.ramPercent))
            Spacer(modifier = Modifier.height(8.dp))
            MetricBar("Disco", vps.diskPercent, Icons.Default.Cloud, statusColorForPercent(vps.diskPercent))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ServiceCount("Online", vps.servicesOnline, StatusOnline)
                ServiceCount("Offline", vps.servicesOffline, StatusOffline)
                ServiceCount("Erro", vps.servicesError, StatusAttention)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatUptime(vps.uptimeSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBar(label: String, percent: Float, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "${percent.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun StatusBadge(status: VpsStatus) {
    val (text, color) = when (status) {
        VpsStatus.ONLINE -> "Online" to StatusOnline
        VpsStatus.ATTENTION -> "Atenção" to StatusAttention
        VpsStatus.OFFLINE -> "Offline" to StatusOffline
        VpsStatus.DISCONNECTED -> "Desconectado" to StatusDisconnected
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ServiceCount(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun statusColorForPercent(percent: Float): Color = when {
    percent >= 90f -> StatusOffline
    percent >= 70f -> StatusAttention
    else -> StatusOnline
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h"
        else -> "${seconds / 60}m"
    }
}
