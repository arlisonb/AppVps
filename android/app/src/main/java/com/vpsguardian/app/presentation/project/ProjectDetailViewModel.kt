package com.vpsguardian.app.presentation.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpsguardian.app.data.session.ActionHistoryStore
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.data.ssh.GitOperations
import com.vpsguardian.app.data.ssh.ServiceControl
import com.vpsguardian.app.data.ssh.ServiceOperations
import com.vpsguardian.app.data.ssh.SshRepository
import com.vpsguardian.app.domain.model.Project
import com.vpsguardian.app.domain.model.ProjectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectDetailUiState(
    val project: Project? = null,
    val isPushing: Boolean = false,
    val pushResult: String? = null,
    val pushError: String? = null,
    val actionLoading: String? = null,
    val quickLogs: List<String>? = null,
    val quickLogsFor: String? = null,
    val healthDialog: Pair<String, String>? = null,
    val restartTarget: ProjectService? = null,
    val actionMessage: String? = null,
    val actionError: String? = null
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val gitOperations: GitOperations,
    private val sshRepository: SshRepository,
    private val serviceOperations: ServiceOperations,
    private val history: ActionHistoryStore
) : ViewModel() {

    private val projectId: String = savedStateHandle["projectId"] ?: ""

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.session.collect { session ->
                val project = session?.projects?.find { it.id == projectId }
                if (project != null) {
                    _uiState.value = _uiState.value.copy(project = project)
                }
            }
        }
    }

    fun push() {
        val session = sessionManager.session.value ?: return
        val project = _uiState.value.project ?: return
        val branch = project.git.branch.ifBlank { "main" }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPushing = true, pushError = null, pushResult = null)
            gitOperations.push(session.credentials, project.path, branch)
                .onSuccess { output ->
                    history.add("push", project.name, output.ifBlank { "Push realizado" })
                    refreshProject()
                    _uiState.value = _uiState.value.copy(
                        isPushing = false,
                        pushResult = output.ifBlank { "Push realizado com sucesso!" }
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isPushing = false,
                        pushError = e.message ?: "Falha no push"
                    )
                }
        }
    }

    fun commitAndPush() {
        val session = sessionManager.session.value ?: return
        val project = _uiState.value.project ?: return
        val branch = project.git.branch.ifBlank { "main" }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPushing = true, pushError = null, pushResult = null)
            gitOperations.commitAndPush(
                session.credentials,
                project.path,
                branch,
                "chore: sync from VPS Guardian"
            ).onSuccess { output ->
                history.add("commit-push", project.name, output.take(200))
                refreshProject()
                _uiState.value = _uiState.value.copy(
                    isPushing = false,
                    pushResult = output.ifBlank { "Commit e push realizados!" }
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isPushing = false,
                    pushError = e.message ?: "Falha no commit/push"
                )
            }
        }
    }

    fun refreshProject() {
        val session = sessionManager.session.value ?: return
        viewModelScope.launch {
            sshRepository.connect(session.credentials)
                .onSuccess { updated ->
                    sessionManager.setSession(updated)
                }
        }
    }

    fun requestRestart(service: ProjectService) {
        _uiState.value = _uiState.value.copy(restartTarget = service)
    }

    fun confirmRestart() {
        val session = sessionManager.session.value ?: return
        val service = _uiState.value.restartTarget ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                actionLoading = service.name,
                restartTarget = null,
                actionError = null,
                actionMessage = null
            )
            serviceOperations.restartAndWait(session.credentials, service)
                .onSuccess { msg ->
                    history.add("restart", service.name, msg)
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionMessage = msg
                    )
                    refreshProject()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionError = e.message ?: "Falha ao reiniciar"
                    )
                }
        }
    }

    fun startService(service: ProjectService) {
        control(service, ServiceControl.START)
    }

    fun stopService(service: ProjectService) {
        control(service, ServiceControl.STOP)
    }

    private fun control(service: ProjectService, action: ServiceControl) {
        val session = sessionManager.session.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = service.name, actionError = null)
            serviceOperations.control(session.credentials, service, action)
                .onSuccess { msg ->
                    history.add(action.name.lowercase(), service.name, msg)
                    _uiState.value = _uiState.value.copy(actionLoading = null, actionMessage = msg)
                    refreshProject()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionError = e.message ?: "Falha na ação"
                    )
                }
        }
    }

    fun dismissRestart() {
        _uiState.value = _uiState.value.copy(restartTarget = null)
    }

    fun quickHealth(service: ProjectService) {
        val session = sessionManager.session.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = service.name, actionError = null)
            serviceOperations.healthCheck(session.credentials, service)
                .onSuccess { health ->
                    val detail = buildString {
                        append(health.message)
                        health.port?.let { append("\nPorta: $it") }
                        append("\nAPI: ${if (health.apiOnline) "online" else "sem resposta"}")
                        health.whatsappConnected?.let { connected ->
                            append("\nWhatsApp: ${if (connected) "conectado" else "desconectado"}")
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        healthDialog = service.name to detail
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionError = e.message ?: "Falha no health check"
                    )
                }
        }
    }

    fun quickLogs(service: ProjectService) {
        val session = sessionManager.session.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = service.name, actionError = null)
            serviceOperations.getLogs(session.credentials, service, lines = 20)
                .onSuccess { logs ->
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        quickLogs = logs,
                        quickLogsFor = service.name
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        actionError = e.message ?: "Falha ao carregar logs"
                    )
                }
        }
    }

    fun dismissHealthDialog() {
        _uiState.value = _uiState.value.copy(healthDialog = null)
    }

    fun dismissQuickLogs() {
        _uiState.value = _uiState.value.copy(quickLogs = null, quickLogsFor = null)
    }
}
