package com.vpsguardian.app.data.session

import com.vpsguardian.app.domain.model.VpsSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val _session = MutableStateFlow<VpsSession?>(null)
    val session: StateFlow<VpsSession?> = _session.asStateFlow()

    val isLoggedIn: Boolean get() = _session.value != null

    fun setSession(session: VpsSession) {
        _session.value = session
    }

    fun clearSession() {
        _session.value = null
    }
}
