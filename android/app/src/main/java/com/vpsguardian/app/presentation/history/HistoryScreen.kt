package com.vpsguardian.app.presentation.history

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryItem(
    val id: String,
    val eventType: String,
    val target: String,
    val description: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val sampleHistory = listOf(
        HistoryItem("1", "reboot", "producao", "VPS Produção foi reiniciada", System.currentTimeMillis() - 3600000),
        HistoryItem("2", "restart", "nginx", "Nginx reiniciado com sucesso", System.currentTimeMillis() - 7200000),
        HistoryItem("3", "alert", "cpu", "CPU acima de 90%", System.currentTimeMillis() - 10800000),
        HistoryItem("4", "login", "app", "Login realizado", System.currentTimeMillis() - 86400000),
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Histórico") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleHistory, key = { it.id }) { item ->
                HistoryCard(item)
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${item.eventType.uppercase()} • ${item.target}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = dateFormat.format(Date(item.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
