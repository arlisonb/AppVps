package com.vpsguardian.app.presentation.project

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vpsguardian.app.domain.model.GitPushStatus
import com.vpsguardian.app.domain.model.ProjectService
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.presentation.components.ProjectIconMapper
import com.vpsguardian.app.presentation.components.ServiceTypeIcon
import com.vpsguardian.app.presentation.theme.StatusAttention
import com.vpsguardian.app.presentation.theme.StatusOffline
import com.vpsguardian.app.presentation.theme.StatusOnline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    onServiceClick: (projectId: String, serviceName: String) -> Unit = { _, _ -> },
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val project = uiState.project ?: return
    val icon = ProjectIconMapper.iconFor(project.name, project.path)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    uiState.restartTarget?.let { service ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestart() },
            title = { Text("Reiniciar ${service.name}?") },
            text = { Text("Apenas este sistema será reiniciado. A VPS continua ligada.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestart() }) {
                    Text("Reiniciar", color = StatusAttention)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestart() }) { Text("Cancelar") }
            }
        )
    }

    uiState.healthDialog?.let { (name, message) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissHealthDialog() },
            title = { Text("Health — $name") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissHealthDialog() }) { Text("OK") }
            }
        )
    }

    if (uiState.quickLogs != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissQuickLogs() },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Logs — ${uiState.quickLogsFor}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                uiState.quickLogs.orEmpty().forEach { line ->
                    Text(
                        line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshProject() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon.icon, null, modifier = Modifier.size(56.dp), tint = icon.color)
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(project.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(project.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(
                                "${project.servicesOnline} online • ${project.servicesOffline} offline",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (project.servicesOffline == 0) StatusOnline else StatusAttention
                            )
                        }
                    }
                }
            }

            item {
                GitStatusCard(
                    project,
                    uiState,
                    onPush = { viewModel.push() },
                    onCommitAndPush = { viewModel.commitAndPush() }
                )
            }

            uiState.actionMessage?.let { msg ->
                item { Text(msg, color = StatusOnline, style = MaterialTheme.typography.bodySmall) }
            }
            uiState.actionError?.let { msg ->
                item { Text(msg, color = StatusOffline, style = MaterialTheme.typography.bodySmall) }
            }

            item {
                Text("Serviços do Projeto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(project.services, key = { it.name }) { service ->
                ProjectServiceCard(
                    service = service,
                    isLoading = uiState.actionLoading == service.name,
                    onClick = { onServiceClick(project.id, service.name) },
                    onRestart = { viewModel.requestRestart(service) },
                    onStart = { viewModel.startService(service) },
                    onStop = { viewModel.stopService(service) },
                    onHealth = { viewModel.quickHealth(service) },
                    onLogs = { viewModel.quickLogs(service) }
                )
            }
        }
    }
}

@Composable
private fun GitStatusCard(
    project: com.vpsguardian.app.domain.model.Project,
    uiState: ProjectDetailUiState,
    onPush: () -> Unit,
    onCommitAndPush: () -> Unit
) {
    val git = project.git
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (project.hasPendingPush) StatusAttention.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Git — ${git.branch}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            when (git.status) {
                GitPushStatus.UP_TO_DATE -> Text("✓ Tudo publicado", color = StatusOnline)
                GitPushStatus.COMMITS_PENDING -> Text("↑ ${git.commitsAhead} commit(s) prontos para push", color = StatusAttention)
                GitPushStatus.CHANGES_UNCOMMITTED -> Text("● ${git.uncommittedFiles} arquivo(s) alterados localmente", color = StatusAttention)
                GitPushStatus.BOTH_PENDING -> Text("↑ ${git.commitsAhead} commit(s) + ${git.uncommittedFiles} alteração(ões)", color = StatusAttention)
                GitPushStatus.NO_REMOTE -> Text("Sem remote origin configurado", color = StatusOffline)
                GitPushStatus.NOT_A_REPO -> Text("Não é repositório git", color = StatusOffline)
            }
            if (git.pendingCommits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                git.pendingCommits.forEach { commit ->
                    Text("• $commit", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (git.lastCommitMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Último commit: ${git.lastCommitMessage}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onPush, enabled = !uiState.isPushing && project.hasPendingPush, modifier = Modifier.fillMaxWidth()) {
                if (uiState.isPushing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Push para GitHub")
                }
            }
            if (project.git.uncommittedFiles > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCommitAndPush,
                    enabled = !uiState.isPushing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Commit + push (${project.git.uncommittedFiles} arquivo(s))")
                }
            }
            uiState.pushResult?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = StatusOnline, style = MaterialTheme.typography.bodySmall)
            }
            uiState.pushError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = StatusOffline, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProjectServiceCard(
    service: ProjectService,
    isLoading: Boolean,
    onClick: () -> Unit,
    onRestart: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onHealth: () -> Unit,
    onLogs: () -> Unit
) {
    val color = if (service.status == ServiceStatus.ONLINE) StatusOnline else StatusOffline
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceTypeIcon(type = service.type, modifier = Modifier.size(36.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(service.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${service.initMethod}${service.port?.let { " • :$it" } ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(service.status.name, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onStart, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PlayArrow, "Iniciar", tint = StatusOnline, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Stop, "Parar", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onRestart, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.RestartAlt, "Reiniciar sistema", tint = StatusAttention, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onHealth, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Favorite, "Health", tint = StatusOnline, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onLogs, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Article, "Logs", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
