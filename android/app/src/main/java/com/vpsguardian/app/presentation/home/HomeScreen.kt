package com.vpsguardian.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.data.ssh.SshRepository
import com.vpsguardian.app.domain.model.Service
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.presentation.components.ServiceIconMapper
import com.vpsguardian.app.presentation.components.ServiceTypeIcon
import com.vpsguardian.app.presentation.theme.StatusOffline
import com.vpsguardian.app.presentation.theme.StatusOnline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val sshRepository: SshRepository
) : ViewModel() {

    val session = sessionManager.session
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        val current = sessionManager.session.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            sshRepository.connect(current.credentials)
                .onSuccess { sessionManager.setSession(it) }
            _isRefreshing.value = false
        }
    }

    fun logout() = sessionManager.clearSession()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var filterStatus by remember { mutableStateOf<ServiceStatus?>(null) }

    val services = session?.services.orEmpty()
    val filtered = services.filter { filterStatus == null || it.status == filterStatus }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session?.hostname ?: "VPS", fontWeight = FontWeight.Bold)
                        Text(
                            "${session?.credentials?.ip} • ${session?.osInfo ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    IconButton(onClick = { viewModel.logout(); onLogout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    VpsMetricsCard(
                        cpu = session?.cpuPercent ?: 0f,
                        ram = session?.ramPercent ?: 0f,
                        disk = session?.diskPercent ?: 0f,
                        uptime = session?.uptime ?: "",
                        totalServices = services.size,
                        online = services.count { it.status == ServiceStatus.ONLINE }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = filterStatus == null, onClick = { filterStatus = null }, label = { Text("Todos (${services.size})") })
                        FilterChip(selected = filterStatus == ServiceStatus.ONLINE, onClick = { filterStatus = ServiceStatus.ONLINE }, label = { Text("Online") })
                        FilterChip(selected = filterStatus == ServiceStatus.OFFLINE, onClick = { filterStatus = ServiceStatus.OFFLINE }, label = { Text("Offline") })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sistemas Detectados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(filtered, key = { it.id }) { service ->
                    ServiceItemCard(service)
                }
            }
        }
    }
}

@Composable
private fun VpsMetricsCard(
    cpu: Float, ram: Float, disk: Float, uptime: String,
    totalServices: Int, online: Int
) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricLabel("CPU", cpu)
                MetricLabel("RAM", ram)
                MetricLabel("Disco", disk)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Uptime: $uptime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text("$online/$totalServices serviços online", style = MaterialTheme.typography.labelSmall, color = StatusOnline)
        }
    }
}

@Composable
private fun MetricLabel(label: String, percent: Float) {
    Column(modifier = Modifier.width(100.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
        Text("${percent.toInt()}%", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ServiceItemCard(service: Service) {
    val statusColor = if (service.status == ServiceStatus.ONLINE) StatusOnline else StatusOffline
    val iconColor = ServiceIconMapper.colorFor(service.type)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ServiceTypeIcon(type = service.type, modifier = Modifier.size(40.dp), tint = iconColor)

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    service.type.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = iconColor
                )
                Text(
                    "${service.initMethod}${service.port?.let { " • porta $it" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Text(
                service.status.name,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
