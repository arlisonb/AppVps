package com.vpsguardian.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.data.ssh.SshRepository
import com.vpsguardian.app.domain.model.VpsCredentials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sshRepository: SshRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(ip: String, username: String, password: String, sshPort: Int = 22) {
        if (ip.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(error = "IP e senha são obrigatórios")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val credentials = VpsCredentials(
                ip = ip.trim(),
                username = username.ifBlank { "root" }.trim(),
                password = password,
                sshPort = sshPort
            )

            sshRepository.connect(credentials)
                .onSuccess { session ->
                    sessionManager.setSession(session)
                    _uiState.value = LoginUiState(success = true)
                }
                .onFailure { e ->
                    _uiState.value = LoginUiState(
                        error = when {
                            e.message?.contains("Auth fail", ignoreCase = true) == true ->
                                "Senha incorreta ou usuário inválido"
                            e.message?.contains("connect", ignoreCase = true) == true ->
                                "Não foi possível conectar em $ip:$sshPort"
                            else -> e.message ?: "Erro ao conectar na VPS"
                        }
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
