package com.vpsguardian.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpsguardian.app.domain.model.VpsServer
import com.vpsguardian.app.domain.repository.VpsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val vpsList: List<VpsServer> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val vpsRepository: VpsRepository
) : ViewModel() {

    val vpsList: StateFlow<List<VpsServer>> = vpsRepository.getAllVps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            vpsRepository.syncAllVps()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = e.message ?: "Erro ao sincronizar"
                    )
                }
        }
    }

    fun addVps(name: String, ip: String, port: Int, token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val vps = VpsServer(name = name, ip = ip, port = port, token = token)
            val id = vpsRepository.addVps(vps)
            vpsRepository.syncVps(id)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun deleteVps(id: Long) {
        viewModelScope.launch {
            vpsRepository.deleteVps(id)
        }
    }
}
