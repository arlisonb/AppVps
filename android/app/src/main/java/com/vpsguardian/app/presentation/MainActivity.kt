package com.vpsguardian.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vpsguardian.app.data.session.SessionManager
import com.vpsguardian.app.domain.model.AppTheme
import com.vpsguardian.app.presentation.home.HomeScreen
import com.vpsguardian.app.presentation.login.LoginScreen
import com.vpsguardian.app.presentation.navigation.Screen
import com.vpsguardian.app.presentation.theme.VpsGuardianTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VpsGuardianTheme(appTheme = AppTheme.DARK) {
                VpsGuardianApp(sessionManager)
            }
        }
    }
}

@Composable
fun VpsGuardianApp(sessionManager: SessionManager) {
    val navController = rememberNavController()
    val session by sessionManager.session.collectAsState()
    val startDestination = if (session != null) Screen.Home.route else Screen.Login.route

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
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
