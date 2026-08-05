package com.vpsguardian.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vpsguardian.app.domain.model.AppTheme
import com.vpsguardian.app.presentation.addvps.AddVpsScreen
import com.vpsguardian.app.presentation.alerts.AlertsScreen
import com.vpsguardian.app.presentation.dashboard.DashboardScreen
import com.vpsguardian.app.presentation.history.HistoryScreen
import com.vpsguardian.app.presentation.navigation.Screen
import com.vpsguardian.app.presentation.navigation.bottomNavItems
import com.vpsguardian.app.presentation.services.ServicesScreen
import com.vpsguardian.app.presentation.settings.SettingsScreen
import com.vpsguardian.app.presentation.theme.VpsGuardianTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VpsGuardianTheme(appTheme = AppTheme.DARK) {
                VpsGuardianApp()
            }
        }
    }
}

@Composable
fun VpsGuardianApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.History.route,
        Screen.Alerts.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (currentRoute == item.screen.route) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onVpsClick = { vpsId ->
                        navController.navigate(Screen.Services.createRoute(vpsId))
                    },
                    onAddVps = {
                        navController.navigate(Screen.AddVps.route)
                    }
                )
            }

            composable(Screen.AddVps.route) {
                AddVpsScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable(Screen.Services.route) { backStackEntry ->
                val vpsId = backStackEntry.arguments?.getString("vpsId")?.toLongOrNull() ?: 0L
                ServicesScreen(
                    vpsId = vpsId,
                    onBack = { navController.popBackStack() },
                    onServiceClick = { serviceId ->
                        navController.navigate(Screen.ServiceDetail.createRoute(vpsId, serviceId))
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen()
            }

            composable(Screen.Alerts.route) {
                AlertsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
