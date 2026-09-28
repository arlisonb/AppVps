package com.vpsguardian.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vpsguardian.app.data.session.ActionHistoryStore
import com.vpsguardian.app.data.session.CredentialsStore
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.data.monitor.VpsMonitor
import com.vpsguardian.app.data.ssh.SshRepository
import com.vpsguardian.app.domain.model.VpsCredentials
import com.vpsguardian.app.presentation.home.HomeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class SavedLoginForm(
    val ip: String = "",
    val username: String = "root",
    val password: String = "",
    val sshPort: Int = 22
)

data class LoginUiState(
    val isLoading: Boolean = false,
    val isAutoLoggingIn: Boolean = false,
    val statusMessage: String? = null,
    val error: String? = null,
    val success: Boolean = false,
    val savedForm: SavedLoginForm? = null,
    val hasSavedLogin: Boolean = false,
    val rememberLogin: Boolean = true
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sshRepository: SshRepository,
    private val sessionManager: SessionManager,
    private val credentialsStore: CredentialsStore,
    private val vpsMonitor: VpsMonitor,
    private val history: ActionHistoryStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        loadSavedForm()
        if (sessionManager.session.value == null) {
            tryAutoLogin()
        }
    }

    fun loadSavedForm() {
        val saved = credentialsStore.load()
        _uiState.value = _uiState.value.copy(
            hasSavedLogin = saved != null,
            savedForm = saved?.let {
                SavedLoginForm(
                    ip = it.ip,
                    username = it.username,
                    password = it.password,
                    sshPort = it.sshPort
                )
            },
            rememberLogin = credentialsStore.isRememberEnabled()
        )
    }

    fun tryAutoLogin() {
        if (sessionManager.session.value != null) return
        val credentials = credentialsStore.load() ?: return
        if (!credentialsStore.isRememberEnabled()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isAutoLoggingIn = true,
                statusMessage = "Reconectando na VPS...",
                error = null
            )
            connect(credentials, saveOnSuccess = false)
        }
    }

    fun login(ip: String, username: String, password: String, sshPort: Int = 22) {
        if (ip.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "IP e senha são obrigatórios")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAutoLoggingIn = false)
            val credentials = VpsCredentials(
                ip = ip.trim(),
                username = username.ifBlank { "root" }.trim(),
                password = password,
                sshPort = sshPort
            )
            connect(credentials, saveOnSuccess = _uiState.value.rememberLogin)
        }
    }

    fun setRememberLogin(enabled: Boolean) {
        credentialsStore.setRememberEnabled(enabled)
        _uiState.value = _uiState.value.copy(rememberLogin = enabled)
        if (!enabled) {
            credentialsStore.clearPasswordOnly()
            _uiState.value = _uiState.value.copy(
                hasSavedLogin = false,
                savedForm = _uiState.value.savedForm?.copy(password = "")
            )
        }
    }

    fun forgetSavedLogin() {
        credentialsStore.clear()
        _uiState.value = _uiState.value.copy(
            hasSavedLogin = false,
            savedForm = null,
            rememberLogin = true,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun connect(credentials: VpsCredentials, saveOnSuccess: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isAutoLoggingIn = _uiState.value.isAutoLoggingIn,
            statusMessage = if (_uiState.value.isAutoLoggingIn) "Reconectando na VPS..."
            else "Conectando via SSH...",
            error = null
        )

        try {
            withTimeout(65_000) {
                _uiState.value = _uiState.value.copy(statusMessage = "Descobrindo sistemas...")
                sshRepository.connect(credentials)
                    .onSuccess { session ->
                        if (saveOnSuccess) {
                            credentialsStore.save(credentials)
                        }
                        sessionManager.setSession(session)
                        vpsMonitor.seedFromSession(session, notify = false)
                        history.add("login", session.hostname, "Conectou em ${credentials.ip}")
                        HomeViewModel.scheduleMonitoring(context)
                        _uiState.value = LoginUiState(
                            success = true,
                            hasSavedLogin = credentialsStore.hasCredentials()
                        )
                    }
                    .onFailure { e ->
                        _uiState.value = LoginUiState(
                            error = mapError(e, credentials.ip, credentials.sshPort),
                            savedForm = SavedLoginForm(
                                ip = credentials.ip,
                                username = credentials.username,
                                password = if (saveOnSuccess) credentials.password else "",
                                sshPort = credentials.sshPort
                            ),
                            hasSavedLogin = credentialsStore.hasCredentials(),
                            rememberLogin = credentialsStore.isRememberEnabled()
                        )
                    }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            _uiState.value = LoginUiState(
                error = "Tempo esgotado. Verifique IP, porta ${credentials.sshPort} e firewall da VPS.",
                savedForm = SavedLoginForm(
                    ip = credentials.ip,
                    username = credentials.username,
                    password = "",
                    sshPort = credentials.sshPort
                ),
                hasSavedLogin = credentialsStore.hasCredentials(),
                rememberLogin = credentialsStore.isRememberEnabled()
            )
        }
    }

    private fun mapError(e: Throwable, ip: String, sshPort: Int): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("Auth fail", ignoreCase = true) ->
                "Senha incorreta ou usuário inválido"
            msg.contains("timeout", ignoreCase = true) ||
                msg.contains("Timed out", ignoreCase = true) ->
                "Tempo esgotado ao conectar em $ip:$sshPort"
            msg.contains("Connection refused", ignoreCase = true) ->
                "Conexão recusada em $ip:$sshPort — SSH desativado?"
            msg.contains("connect", ignoreCase = true) ||
                msg.contains("Network is unreachable", ignoreCase = true) ->
                "Não foi possível conectar em $ip:$sshPort"
            else -> msg.ifBlank { "Erro ao conectar na VPS" }
        }
    }
}
