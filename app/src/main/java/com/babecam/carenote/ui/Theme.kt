package com.babecam.carenote.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 상태 표시용 색 (변화 있음 = 주황, 보고 필요 = 빨강) */
data class StatusColors(
    val warn: Color, val warnSoft: Color,
    val alert: Color, val alertSoft: Color,
    val calm: Color, val calmSoft: Color,
)

private val LightStatus = StatusColors(
    warn = Color(0xFF9A5B0E), warnSoft = Color(0xFFFBEBD3),
    alert = Color(0xFFA63A2E), alertSoft = Color(0xFFF8DEDA),
    calm = Color(0xFF56696C), calmSoft = Color(0xFFE7ECEB),
)
private val DarkStatus = StatusColors(
    warn = Color(0xFFE9B062), warnSoft = Color(0xFF3A2C17),
    alert = Color(0xFFF08A7C), alertSoft = Color(0xFF3D221F),
    calm = Color(0xFF9AAEAB), calmSoft = Color(0xFF1E2B2D),
)

val LocalStatus = staticCompositionLocalOf { LightStatus }

private val Light = lightColorScheme(
    primary = Color(0xFF1D6B5F), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEEE9), onPrimaryContainer = Color(0xFF0F4A41),
    background = Color(0xFFEEF2F1), onBackground = Color(0xFF17282B),
    surface = Color.White, onSurface = Color(0xFF17282B),
    surfaceVariant = Color(0xFFF5F8F7), onSurfaceVariant = Color(0xFF56696C),
    outline = Color(0xFFD3DDDA), outlineVariant = Color(0xFFD3DDDA),
    error = Color(0xFFA63A2E),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF5DBFAE), onPrimary = Color(0xFF0B1A17),
    primaryContainer = Color(0xFF1C3632), onPrimaryContainer = Color(0xFFBFE6DD),
    background = Color(0xFF0F1A1C), onBackground = Color(0xFFE4ECEA),
    surface = Color(0xFF172427), onSurface = Color(0xFFE4ECEA),
    surfaceVariant = Color(0xFF132022), onSurfaceVariant = Color(0xFF9AAEAB),
    outline = Color(0xFF2A3B3E), outlineVariant = Color(0xFF2A3B3E),
    error = Color(0xFFF08A7C),
)

@Composable
fun CareTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalStatus provides if (dark) DarkStatus else LightStatus) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
    }
}
