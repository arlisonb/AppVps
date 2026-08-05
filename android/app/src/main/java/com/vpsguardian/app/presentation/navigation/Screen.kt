package com.vpsguardian.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Services : Screen("services/{vpsId}") {
        fun createRoute(vpsId: Long) = "services/$vpsId"
    }
    data object ServiceDetail : Screen("service/{vpsId}/{serviceId}") {
        fun createRoute(vpsId: Long, serviceId: String) = "service/$vpsId/$serviceId"
    }
    data object AddVps : Screen("add_vps")
    data object VpsDetail : Screen("vps/{vpsId}") {
        fun createRoute(vpsId: Long) = "vps/$vpsId"
    }
    data object Metrics : Screen("metrics/{vpsId}") {
        fun createRoute(vpsId: Long) = "metrics/$vpsId"
    }
    data object Logs : Screen("logs/{vpsId}/{serviceId}") {
        fun createRoute(vpsId: Long, serviceId: String) = "logs/$vpsId/$serviceId"
    }
    data object History : Screen("history")
    data object Alerts : Screen("alerts")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(Screen.History, "Histórico", Icons.Filled.History, Icons.Outlined.History),
    BottomNavItem(Screen.Alerts, "Alertas", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem(Screen.Settings, "Config", Icons.Filled.Settings, Icons.Outlined.Settings),
)
