package com.vpsguardian.app.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vpsguardian.app.presentation.components.VpsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onVpsClick: (Long) -> Unit,
    onAddVps: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val vpsList by viewModel.vpsList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VPS Guardian") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVps) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar VPS")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshAll() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                vpsList.isEmpty() -> {
                    EmptyState(onAddVps = onAddVps)
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vpsList, key = { it.id }) { vps ->
                            VpsCard(
                                vps = vps,
                                onClick = { onVpsClick(vps.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAddVps: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "Nenhuma VPS cadastrada",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Toque no botão + para adicionar seu primeiro servidor",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
