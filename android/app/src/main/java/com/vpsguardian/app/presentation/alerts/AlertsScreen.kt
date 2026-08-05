package com.vpsguardian.app.presentation.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vpsguardian.app.presentation.theme.StatusAttention
import com.vpsguardian.app.presentation.theme.StatusOffline

data class AlertItem(
    val id: String,
    val title: String,
    val message: String,
    val severity: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen() {
    val sampleAlerts = listOf(
        AlertItem("1", "CPU Alta", "VPS Produção: CPU em 95%", "warning", System.currentTimeMillis() - 1800000),
        AlertItem("2", "Serviço Offline", "WPPConnect está offline", "critical", System.currentTimeMillis() - 3600000),
        AlertItem("3", "VPS Reiniciada", "VPS Produção foi reiniciada", "critical", System.currentTimeMillis() - 7200000),
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Alertas") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleAlerts, key = { it.id }) { alert ->
                AlertCard(alert)
            }
        }
    }
}

@Composable
private fun AlertCard(alert: AlertItem) {
    val color = if (alert.severity == "critical") StatusOffline else StatusAttention

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
