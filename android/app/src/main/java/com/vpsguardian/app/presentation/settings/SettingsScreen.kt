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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var biometricEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configurações") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSection("Aparência") {
                SettingsItem(Icons.Default.DarkMode, "Tema", "Escuro") {}
                SettingsItem(Icons.Default.Language, "Idioma", "Português") {}
            }

            SettingsSection("Monitoramento") {
                SettingsItem(Icons.Default.Refresh, "Intervalo de atualização", "30 segundos") {}
                SettingsItem(Icons.Default.Timer, "Tempo do cache", "60 segundos") {}
            }

            SettingsSection("Notificações") {
                SettingsToggle(Icons.Default.Notifications, "Notificações", notificationsEnabled) {
                    notificationsEnabled = it
                }
                SettingsToggle(Icons.Default.Notifications, "Sons", soundEnabled) {
                    soundEnabled = it
                }
                SettingsToggle(Icons.Default.Notifications, "Vibração", vibrationEnabled) {
                    vibrationEnabled = it
                }
            }

            SettingsSection("Segurança") {
                SettingsToggle(Icons.Default.Fingerprint, "Biometria", biometricEnabled) {
                    biometricEnabled = it
                }
            }

            SettingsSection("Sobre") {
                SettingsItem(Icons.Default.Refresh, "Versão", "1.0.0") {}
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
