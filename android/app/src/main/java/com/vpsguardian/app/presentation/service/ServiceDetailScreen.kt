package com.vpsguardian.app.presentation.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.presentation.components.ServiceTypeIcon
import com.vpsguardian.app.presentation.theme.StatusAttention
import com.vpsguardian.app.presentation.theme.StatusOffline
import com.vpsguardian.app.presentation.theme.StatusOnline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    onBack: () -> Unit,
    viewModel: ServiceDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val service = uiState.service ?: return
    var showRestartDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.logs.size) {
        if (uiState.logs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logs.lastIndex)
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Reiniciar sistema?") },
            text = {
                Text("Apenas \"${service.name}\" será reiniciado. A VPS continua ligada — só este sistema para e volta.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    viewModel.restart()
                }) {
                    Text("Reiniciar", color = StatusAttention)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(service.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleAutoRefresh() }) {
                        Icon(
                            if (uiState.autoRefresh) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.autoRefresh) "Pausar" else "Retomar"
                        )
                    }
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ServiceHeaderCard(uiState)
            }

            if (uiState.isWppConnect) {
                item {
                    WppConnectHealthCard(uiState)
                }
            }

            item {
                RuntimeMetricsCard(uiState)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Logs em tempo real",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    uiState.lastUpdate.takeIf { it > 0 }?.let { ts ->
                        Text(
                            formatTime(ts),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (uiState.isLoading && uiState.logs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.logs.isEmpty()) {
                item {
                    Text(
                        "Nenhum log disponível",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                itemsIndexed(uiState.logs, key = { idx, _ -> "$idx-${uiState.lastUpdate}" }) { _, line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = logColor(line)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showRestartDialog = true },
                    enabled = !uiState.isRestarting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAttention)
                ) {
                    if (uiState.isRestarting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Aguardando o sistema voltar...")
                    } else {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Reiniciar sistema")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.start() },
                        enabled = !uiState.isRestarting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Iniciar")
                    }
                    OutlinedButton(
                        onClick = { viewModel.stop() },
                        enabled = !uiState.isRestarting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Parar")
                    }
                }

                uiState.restartMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = StatusOnline, style = MaterialTheme.typography.bodySmall)
                }
                uiState.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = StatusOffline, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ServiceHeaderCard(uiState: ServiceDetailUiState) {
    val service = uiState.service ?: return
    val statusColor = when (uiState.status) {
        ServiceStatus.ONLINE -> StatusOnline
        ServiceStatus.ERROR -> StatusOffline
        ServiceStatus.STARTING -> StatusAttention
        else -> StatusOffline
    }

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ServiceTypeIcon(type = service.type, modifier = Modifier.size(48.dp))
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(service.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${uiState.projectName} • ${service.initMethod}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    uiState.status.name,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                if (uiState.autoRefresh) {
                    Text(
                        "Atualizando a cada 4s (1 conexão)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusOnline.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WppConnectHealthCard(uiState: ServiceDetailUiState) {
    val bgColor = when {
        uiState.whatsappConnected == true -> StatusOnline.copy(alpha = 0.12f)
        uiState.whatsappConnected == false -> StatusOffline.copy(alpha = 0.12f)
        uiState.isHealthy -> StatusOnline.copy(alpha = 0.12f)
        else -> StatusAttention.copy(alpha = 0.12f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("WPPConnect", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            when (uiState.whatsappConnected) {
                true -> Text("✓ WhatsApp conectado", color = StatusOnline, fontWeight = FontWeight.SemiBold)
                false -> Text("✗ WhatsApp desconectado", color = StatusOffline, fontWeight = FontWeight.SemiBold)
                null -> Text("? Sessão não confirmada", color = StatusAttention)
            }
            Text(
                if (uiState.apiOnline) "API: online" else "API: sem resposta",
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.apiOnline) StatusOnline else StatusAttention
            )

            if (uiState.healthMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(uiState.healthMessage, style = MaterialTheme.typography.bodySmall)
            }

            uiState.port?.let { port ->
                Spacer(modifier = Modifier.height(4.dp))
                Text("Porta: $port", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun RuntimeMetricsCard(uiState: ServiceDetailUiState) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricItem("CPU", "${uiState.cpuPercent.toInt()}%")
            MetricItem("RAM", if (uiState.memoryMb > 0) "${uiState.memoryMb.toInt()} MB" else "—")
            MetricItem("Uptime", uiState.uptime.ifBlank { "—" })
            MetricItem("PID", uiState.pid?.toString() ?: "—")
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

private fun logColor(line: String): Color {
    val lower = line.lowercase()
    return when {
        "error" in lower || "fatal" in lower || "failed" in lower -> StatusOffline
        "warn" in lower -> StatusAttention
        "connected" in lower || "success" in lower -> StatusOnline
        else -> Color.Unspecified
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
