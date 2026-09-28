package com.vpsguardian.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.vpsguardian.app.BuildConfig
import com.vpsguardian.app.data.session.AppSettingsStore
import com.vpsguardian.app.data.session.HostKeyStore
import com.vpsguardian.app.workers.BootReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val biometric: Boolean = true,
    val notifications: Boolean = true,
    val intervalMin: Int = 15,
    val ramThreshold: Int = 85,
    val diskThreshold: Int = 90,
    val hostKeyCleared: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: AppSettingsStore,
    private val hostKeyStore: HostKeyStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _ui = MutableStateFlow(read())
    val ui = _ui.asStateFlow()

    private fun read() = SettingsUiState(
        biometric = settings.biometricEnabled,
        notifications = settings.notificationsEnabled,
        intervalMin = settings.monitorIntervalMinutes,
        ramThreshold = settings.ramThreshold,
        diskThreshold = settings.diskThreshold
    )

    fun setBiometric(value: Boolean) {
        settings.biometricEnabled = value
        _ui.value = _ui.value.copy(biometric = value)
    }

    fun setNotifications(value: Boolean) {
        settings.notificationsEnabled = value
        _ui.value = _ui.value.copy(notifications = value)
    }

    fun cycleInterval() {
        val next = when (settings.monitorIntervalMinutes) {
            15 -> 30
            30 -> 60
            else -> 15
        }
        settings.monitorIntervalMinutes = next
        BootReceiver.scheduleMonitoring(context, next, replace = true)
        _ui.value = _ui.value.copy(intervalMin = next)
    }

    fun cycleRam() {
        val next = when (settings.ramThreshold) {
            80 -> 85
            85 -> 90
            else -> 80
        }
        settings.ramThreshold = next
        _ui.value = _ui.value.copy(ramThreshold = next)
    }

    fun cycleDisk() {
        val next = when (settings.diskThreshold) {
            85 -> 90
            90 -> 95
            else -> 85
        }
        settings.diskThreshold = next
        _ui.value = _ui.value.copy(diskThreshold = next)
    }

    fun clearHostKeys() {
        hostKeyStore.clear()
        _ui.value = _ui.value.copy(hostKeyCleared = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSection("Segurança") {
                SettingsToggle(Icons.Default.Fingerprint, "Biometria / PIN ao abrir", ui.biometric) {
                    viewModel.setBiometric(it)
                }
                SettingsItem(Icons.Default.Key, "Chave SSH da VPS", if (ui.hostKeyCleared) "Removida — será salva no próximo login" else "Toque para esquecer (se a VPS mudou)") {
                    viewModel.clearHostKeys()
                }
            }

            SettingsSection("Monitoramento") {
                SettingsToggle(Icons.Default.Notifications, "Notificações", ui.notifications) {
                    viewModel.setNotifications(it)
                }
                SettingsItem(Icons.Default.Timer, "Intervalo dos alertas", "${ui.intervalMin} minutos") {
                    viewModel.cycleInterval()
                }
                SettingsItem(Icons.Default.Storage, "Alerta de RAM", "A partir de ${ui.ramThreshold}%") {
                    viewModel.cycleRam()
                }
                SettingsItem(Icons.Default.Storage, "Alerta de disco", "A partir de ${ui.diskThreshold}%") {
                    viewModel.cycleDisk()
                }
            }

            SettingsSection("Sobre") {
                SettingsItem(Icons.Default.Info, "Versão", BuildConfig.VERSION_NAME) {}
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
