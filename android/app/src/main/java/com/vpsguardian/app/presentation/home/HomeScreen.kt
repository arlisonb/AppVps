package com.vpsguardian.app.presentation.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpsguardian.app.data.monitor.VpsMonitor
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.domain.model.IssueSeverity
import com.vpsguardian.app.domain.model.Project
import com.vpsguardian.app.domain.model.ServiceIssue
import com.vpsguardian.app.domain.model.WppHealthSnapshot
import com.vpsguardian.app.presentation.components.ProjectIconMapper
import com.vpsguardian.app.presentation.theme.StatusAttention
import com.vpsguardian.app.presentation.theme.StatusOffline
import com.vpsguardian.app.presentation.theme.StatusOnline
import com.vpsguardian.app.workers.BootReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val issues: List<ServiceIssue> = emptyList(),
    val wppHealth: Map<String, WppHealthSnapshot> = emptyMap()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val vpsMonitor: VpsMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val session = sessionManager.session
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.session.collect { session ->
                if (session != null) {
                    val result = vpsMonitor.fromSession(session)
                    _uiState.value = HomeUiState(
                        issues = result.issues,
                        wppHealth = result.wppHealth
                    )
                } else {
                    _uiState.value = HomeUiState()
                }
            }
        }
    }

    fun refresh() {
        val current = sessionManager.session.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            vpsMonitor.runCheck(current.credentials, notify = false)
                .onSuccess { result ->
                    _uiState.value = HomeUiState(
                        issues = result.issues,
                        wppHealth = result.wppHealth
                    )
                }
            _isRefreshing.value = false
        }
    }

    fun logout() {
        sessionManager.clearSession()
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("vps_monitor")
    }

    companion object {
        fun scheduleMonitoring(context: Context) {
            BootReceiver.scheduleMonitoring(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProjectClick: (String) -> Unit,
    onServiceClick: (projectId: String, serviceName: String) -> Unit,
    onLogout: () -> Unit,
    onSettings: () -> Unit = {},
    onHistory: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val projects = session?.projects.orEmpty()
    val criticalIssues = uiState.issues.filter { it.severity == IssueSeverity.CRITICAL }
    val firstCritical = criticalIssues.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session?.hostname ?: "VPS", fontWeight = FontWeight.Bold)
                        Text(
                            "${session?.credentials?.ip} • ${projects.size} projetos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onHistory) {
                        Icon(Icons.Default.History, contentDescription = "Histórico")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.issues.isNotEmpty()) {
                    item {
                        ProblemsAlertCard(
                            issues = uiState.issues,
                            onGoToFirst = {
                                firstCritical?.takeIf { !it.isSystemMetric }?.let { issue ->
                                    if (issue.serviceName != "git-push") {
                                        onServiceClick(issue.projectId, issue.serviceName)
                                    } else {
                                        onProjectClick(issue.projectId)
                                    }
                                }
                            },
                            onIssueClick = { issue ->
                                when {
                                    issue.isSystemMetric -> Unit
                                    issue.serviceName == "git-push" -> onProjectClick(issue.projectId)
                                    else -> onServiceClick(issue.projectId, issue.serviceName)
                                }
                            }
                        )
                    }
                }

                item {
                    VpsMetricsCard(
                        cpu = session?.cpuPercent ?: 0f,
                        ram = session?.ramPercent ?: 0f,
                        disk = session?.diskPercent ?: 0f,
                        swap = session?.swapPercent ?: 0f,
                        uptime = session?.uptime ?: "",
                        lastCheck = session?.lastCheck ?: 0L
                    )
                }

                if (session?.topProcesses.orEmpty().isNotEmpty()) {
                    item {
                        TopProcessesCard(session?.topProcesses.orEmpty())
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Meus Sistemas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (projects.isEmpty()) {
                    item {
                        Text(
                            "Nenhum projeto git encontrado em /var/www, /opt, /home...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                items(projects, key = { it.id }) { project ->
                    ProjectCard(project = project, onClick = { onProjectClick(project.id) })
                }
            }
        }
    }
}

@Composable
private fun ProblemsAlertCard(
    issues: List<ServiceIssue>,
    onGoToFirst: () -> Unit,
    onIssueClick: (ServiceIssue) -> Unit
) {
    val critical = issues.count { it.severity == IssueSeverity.CRITICAL }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StatusOffline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = StatusAttention, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        "Precisa de atenção",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusAttention
                    )
                    Text(
                        "$critical crítico(s) • ${issues.size} alerta(s)",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            issues.take(4).forEach { issue ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIssueClick(issue) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${issue.projectName} — ${issue.serviceName}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            issue.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (issue.severity == IssueSeverity.CRITICAL) StatusOffline else StatusAttention
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
                }
            }

            if (critical > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onGoToFirst, modifier = Modifier.fillMaxWidth()) {
                    Text("Ir para o que está quebrado")
                }
            }
        }
    }
}

@Composable
private fun VpsMetricsCard(
    cpu: Float,
    ram: Float,
    disk: Float,
    swap: Float,
    uptime: String,
    lastCheck: Long
) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricLabel("CPU", cpu)
                MetricLabel("RAM", ram, warn = ram >= 85f)
                MetricLabel("Disco", disk, warn = disk >= 90f)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Uptime: $uptime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            if (swap > 0f) {
                Text("Swap: ${swap.toInt()}%", style = MaterialTheme.typography.labelSmall, color = if (swap > 50f) StatusAttention else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            if (lastCheck > 0) {
                val ago = ((System.currentTimeMillis() - lastCheck) / 1000).coerceAtLeast(0)
                val label = when {
                    ago < 60 -> "há ${ago}s"
                    ago < 3600 -> "há ${ago / 60} min"
                    else -> "há ${ago / 3600} h"
                }
                Text("Última verificação $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun MetricLabel(label: String, percent: Float, warn: Boolean = false) {
    Column(modifier = Modifier.width(100.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (warn) StatusAttention else MaterialTheme.colorScheme.primary
        )
        Text("${percent.toInt()}%", style = MaterialTheme.typography.labelSmall, color = if (warn) StatusAttention else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TopProcessesCard(processes: List<com.vpsguardian.app.domain.model.ProcessInfo>) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quem usa mais RAM", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            processes.take(6).forEach { proc ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(proc.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("${proc.memoryMb.toInt()} MB  ${proc.memoryPercent.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit) {
    val icon = ProjectIconMapper.iconFor(project.name, project.path)
    val statusColor = when {
        project.servicesOffline > 0 -> StatusOffline
        project.hasPendingPush -> StatusAttention
        else -> StatusOnline
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgedBox(
                badge = {
                    if (project.hasPendingPush || project.servicesOffline > 0) {
                        Badge(containerColor = if (project.servicesOffline > 0) StatusOffline else StatusAttention) {
                            Text(
                                if (project.servicesOffline > 0) "${project.servicesOffline}" else "${project.git.commitsAhead}"
                            )
                        }
                    }
                }
            ) {
                Icon(icon.icon, null, modifier = Modifier.size(48.dp), tint = icon.color)
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${project.servicesOnline} online • ${project.servicesOffline} offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
                if (project.hasPendingPush) {
                    Text(
                        "↑ ${project.git.commitsAhead} push pendente",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusAttention,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (project.git.uncommittedFiles > 0) {
                    Text(
                        "● ${project.git.uncommittedFiles} alteração(ões) local",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusAttention
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (project.overallOnline) "ONLINE" else "ATENÇÃO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
