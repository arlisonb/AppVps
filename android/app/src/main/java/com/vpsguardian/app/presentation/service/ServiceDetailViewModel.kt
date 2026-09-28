package com.vpsguardian.app.presentation.service

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpsguardian.app.data.session.ActionHistoryStore
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.data.ssh.ServiceControl
import com.vpsguardian.app.data.ssh.ServiceOperations
import com.vpsguardian.app.domain.model.ProjectService
import com.vpsguardian.app.domain.model.ServiceStatus
import com.vpsguardian.app.domain.model.ServiceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceDetailUiState(
    val service: ProjectService? = null,
    val projectName: String = "",
    val logs: List<String> = emptyList(),
    val healthMessage: String = "",
    val isHealthy: Boolean = false,
    val whatsappConnected: Boolean? = null,
    val apiOnline: Boolean = false,
    val port: Int? = null,
    val uptime: String = "",
    val memoryMb: Float = 0f,
    val cpuPercent: Float = 0f,
    val pid: Int? = null,
    val status: ServiceStatus = ServiceStatus.UNKNOWN,
    val isLoading: Boolean = true,
    val isRestarting: Boolean = false,
    val autoRefresh: Boolean = true,
    val lastUpdate: Long = 0L,
    val error: String? = null,
    val restartMessage: String? = null
) {
    val isWppConnect: Boolean
        get() = service?.type == ServiceType.WPPCONNECT ||
            service?.name?.lowercase()?.contains("whatsapp") == true ||
            service?.name?.lowercase()?.contains("wppconnect") == true
}

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val serviceOperations: ServiceOperations,
    private val history: ActionHistoryStore
) : ViewModel() {

    private val projectId: String = savedStateHandle["projectId"] ?: ""
    private val serviceName: String = java.net.URLDecoder.decode(
        savedStateHandle["serviceName"] ?: "",
        "UTF-8"
    )

    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadService()
        startMonitoring()
    }

    private fun loadService() {
        val session = sessionManager.session.value ?: return
        val project = session.projects.find { it.id == projectId } ?: return
        val service = project.services.find { it.name == serviceName } ?: return
        _uiState.value = _uiState.value.copy(
            service = service,
            projectName = project.name,
            status = service.status
        )
    }

    fun startMonitoring() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            refreshOnce()
            while (isActive && _uiState.value.autoRefresh) {
                delay(4_000)
                if (!_uiState.value.isRestarting) refreshOnce()
            }
        }
    }

    fun toggleAutoRefresh() {
        val enabled = !_uiState.value.autoRefresh
        _uiState.value = _uiState.value.copy(autoRefresh = enabled)
        if (enabled) startMonitoring() else pollingJob?.cancel()
    }

    fun refreshAll() {
        viewModelScope.launch { refreshOnce() }
    }

    private suspend fun refreshOnce() {
        val session = sessionManager.session.value ?: return
        val service = _uiState.value.service ?: return
        if (_uiState.value.logs.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = true)
        }

        serviceOperations.snapshot(session.credentials, service)
            .onSuccess { snap ->
                _uiState.value = _uiState.value.copy(
                    logs = snap.logs.ifEmpty { _uiState.value.logs },
                    status = snap.runtime.status,
                    uptime = snap.runtime.uptime,
                    memoryMb = snap.runtime.memoryMb,
                    cpuPercent = snap.runtime.cpuPercent,
                    pid = snap.runtime.pid,
                    isHealthy = snap.health.healthy,
                    healthMessage = snap.health.message,
                    whatsappConnected = snap.health.whatsappConnected,
                    apiOnline = snap.health.apiOnline,
                    port = snap.health.port ?: service.port,
                    isLoading = false,
                    lastUpdate = System.currentTimeMillis(),
                    error = null
                )
            }
            .onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Falha ao atualizar"
                )
            }
    }

    fun restart() {
        control(ServiceControl.RESTART)
    }

    fun start() {
        control(ServiceControl.START)
    }

    fun stop() {
        control(ServiceControl.STOP)
    }

    private fun control(action: ServiceControl) {
        val session = sessionManager.session.value ?: return
        val service = _uiState.value.service ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRestarting = true,
                restartMessage = null,
                error = null
            )

            val result = if (action == ServiceControl.RESTART) {
                serviceOperations.restartAndWait(session.credentials, service)
            } else {
                serviceOperations.control(session.credentials, service, action)
            }

            result.onSuccess { msg ->
                history.add(
                    type = action.name.lowercase(),
                    target = service.name,
                    description = msg
                )
                _uiState.value = _uiState.value.copy(
                    isRestarting = false,
                    restartMessage = msg
                )
                refreshOnce()
            }.onFailure { e ->
                history.add(action.name.lowercase(), service.name, e.message ?: "Falha")
                _uiState.value = _uiState.value.copy(
                    isRestarting = false,
                    error = e.message ?: "Falha na ação"
                )
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
