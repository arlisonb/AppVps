package com.vpsguardian.app.presentation.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpsguardian.app.domain.model.Service
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.domain.repository.ServiceRepository
import com.vpsguardian.app.presentation.theme.StatusAttention
import com.vpsguardian.app.presentation.theme.StatusOffline
import com.vpsguardian.app.presentation.theme.StatusOnline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    fun getServices(vpsId: Long): StateFlow<List<Service>> =
        serviceRepository.getServicesByVps(vpsId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    vpsId: Long,
    onBack: () -> Unit,
    onServiceClick: (String) -> Unit,
    viewModel: ServicesViewModel = hiltViewModel()
) {
    val services by viewModel.getServices(vpsId).collectAsState()
    var filterStatus by remember { mutableStateOf<ServiceStatus?>(null) }

    val filtered = services.filter { filterStatus == null || it.status == filterStatus }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Serviços (${services.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filterStatus == ServiceStatus.ONLINE,
                    onClick = { filterStatus = ServiceStatus.ONLINE },
                    label = { Text("Online") }
                )
                FilterChip(
                    selected = filterStatus == ServiceStatus.OFFLINE,
                    onClick = { filterStatus = ServiceStatus.OFFLINE },
                    label = { Text("Offline") }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { service ->
                    ServiceCard(
                        service = service,
                        onClick = { onServiceClick(service.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(service: Service, onClick: () -> Unit) {
    val statusColor = when (service.status) {
        ServiceStatus.ONLINE -> StatusOnline
        ServiceStatus.ERROR -> StatusAttention
        else -> StatusOffline
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = service.type.name.take(2),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${service.type.name} • ${service.initMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (service.port != null) {
                    Text(
                        "Porta: ${service.port}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    service.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                if (service.cpuPercent > 0) {
                    Text(
                        "CPU ${service.cpuPercent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
