package com.vpsguardian.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.vpsguardian.app.domain.model.AppTheme

private val GreenOnline = Color(0xFF4CAF50)
private val YellowAttention = Color(0xFFFFC107)
private val RedOffline = Color(0xFFF44336)
private val GrayDisconnected = Color(0xFF9E9E9E)

val StatusOnline = GreenOnline
val StatusAttention = YellowAttention
val StatusOffline = RedOffline
val StatusDisconnected = GrayDisconnected

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004880),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF21262D),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
    error = RedOffline,
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004880),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF1A1A1A),
    onBackground = Color(0xFFE6EDF3),
    onSurface = Color(0xFFE6EDF3),
    error = RedOffline,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFFE65100),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE0E0E0),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    error = RedOffline,
)

@Composable
fun VpsGuardianTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.AMOLED -> AmoledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
