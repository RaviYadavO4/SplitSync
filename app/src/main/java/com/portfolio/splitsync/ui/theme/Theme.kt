package com.portfolio.splitsync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand & Financial Semantic Colors
val DeepSlate900 = Color(0xFF0F172A)
val DeepSlate800 = Color(0xFF1E293B)
val DeepSlate700 = Color(0xFF334155)
val Alabaster50 = Color(0xFFF8FAFC)
val PureWhite = Color(0xFFFFFFFF)

val ElectricEmerald = Color(0xFF10B981) // You are owed (+)
val WarmCoral = Color(0xFFF43F5E)       // You owe (-)
val AmberWarning = Color(0xFFF59E0B)    // Offline / Pending Sync

private val DarkColorScheme = darkColorScheme(
    primary = ElectricEmerald,
    onPrimary = DeepSlate900,
    secondary = Color(0xFF38BDF8),
    background = DeepSlate900,
    surface = DeepSlate800,
    surfaceVariant = DeepSlate700,
    onBackground = Alabaster50,
    onSurface = Alabaster50,
    error = WarmCoral
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricEmerald,
    onPrimary = PureWhite,
    secondary = Color(0xFF0284C7),
    background = Alabaster50,
    surface = PureWhite,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = DeepSlate900,
    onSurface = DeepSlate900,
    error = WarmCoral
)

@Composable
fun SplitSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
