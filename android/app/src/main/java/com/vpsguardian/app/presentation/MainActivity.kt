package com.vpsguardian.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vpsguardian.app.data.session.AppSettingsStore
import com.vpsguardian.app.data.session.CredentialsStore
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.domain.model.AppTheme
import com.vpsguardian.app.notifications.NotificationHelper
import com.vpsguardian.app.presentation.history.HistoryScreen
import com.vpsguardian.app.presentation.home.HomeScreen
import com.vpsguardian.app.presentation.login.LoginScreen
import com.vpsguardian.app.presentation.navigation.Screen
import com.vpsguardian.app.presentation.project.ProjectDetailScreen
import com.vpsguardian.app.presentation.service.ServiceDetailScreen
import com.vpsguardian.app.presentation.settings.SettingsScreen
import com.vpsguardian.app.presentation.theme.VpsGuardianTheme
import com.vpsguardian.app.security.BiometricAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var settingsStore: AppSettingsStore
    @Inject lateinit var credentialsStore: CredentialsStore

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            VpsGuardianTheme(appTheme = AppTheme.DARK) {
                val needsLock = settingsStore.biometricEnabled && credentialsStore.hasCredentials()
                var unlocked by remember { mutableStateOf(!needsLock) }
                var lockError by remember { mutableStateOf<String?>(null) }
                val biometric = remember { BiometricAuth(this) }

                if (!unlocked) {
                    UnlockScreen(
                        error = lockError,
                        onUnlock = {
                            if (!biometric.isAvailable()) {
                                unlocked = true
                                return@UnlockScreen
                            }
                            biometric.authenticate(
                                onSuccess = { unlocked = true },
                                onError = { lockError = it }
                            )
                        }
                    )
                } else {
                    VpsGuardianApp(
                        sessionManager = sessionManager,
                        deepLinkProjectId = intent?.getStringExtra(NotificationHelper.EXTRA_PROJECT_ID),
                        deepLinkServiceName = intent?.getStringExtra(NotificationHelper.EXTRA_SERVICE_NAME)
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun UnlockScreen(error: String?, onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.height(72.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("VPS Guardian", style = MaterialTheme.typography.headlineMedium)
        Text("Desbloqueie para acessar a VPS", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onUnlock) { Text("Desbloquear") }
        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun VpsGuardianApp(
    sessionManager: SessionManager,
    deepLinkProjectId: String? = null,
    deepLinkServiceName: String? = null
) {
    val navController = rememberNavController()
    val session by sessionManager.session.collectAsState()
    val startDestination = if (session != null) Screen.Home.route else Screen.Login.route

    LaunchedEffect(deepLinkProjectId, deepLinkServiceName, session) {
        if (session == null || deepLinkProjectId.isNullOrBlank()) return@LaunchedEffect
        if (!deepLinkServiceName.isNullOrBlank()) {
            navController.navigate(
                Screen.ProjectServiceDetail.createRoute(deepLinkProjectId, deepLinkServiceName)
            )
        } else {
            navController.navigate(Screen.ProjectDetail.createRoute(deepLinkProjectId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                },
                onServiceClick = { projectId, serviceName ->
                    navController.navigate(
                        Screen.ProjectServiceDetail.createRoute(projectId, serviceName)
                    )
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(Screen.ProjectDetail.route) {
            ProjectDetailScreen(
                onBack = { navController.popBackStack() },
                onServiceClick = { projectId, serviceName ->
                    navController.navigate(
                        Screen.ProjectServiceDetail.createRoute(projectId, serviceName)
                    )
                }
            )
        }

        composable(Screen.ProjectServiceDetail.route) {
            ServiceDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
