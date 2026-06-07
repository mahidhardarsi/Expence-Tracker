package com.expensetracker.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ── Color palette ─────────────────────────────────────────────────────────────

object AppColors {
    // Light
    val LightBackground = Color(0xFFF6F8FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightElevated = Color(0xFFF1F4F8)
    val LightPrimaryText = Color(0xFF0B1220)
    val LightSecondaryText = Color(0xFF4B5563)
    val LightMutedText = Color(0xFF9CA3AF)
    val LightBorder = Color(0xFFE5E7EB)

    // Dark
    val DarkBackground = Color(0xFF0D1117)
    val DarkSurface = Color(0xFF161B22)
    val DarkElevated = Color(0xFF1F2630)
    val DarkPrimaryText = Color(0xFFE6EDF3)
    val DarkSecondaryText = Color(0xFF9BA7B4)
    val DarkMutedText = Color(0xFF6E7681)
    val DarkBorder = Color(0xFF30363D)

    // Accent (shared)
    val AccentGreen = Color(0xFF2DA44E)
    val AccentBlue = Color(0xFF2563EB)
    val AccentRed = Color(0xFFDC2626)
    val AccentYellow = Color(0xFFD97706)
    val AccentPurple = Color(0xFF7C3AED)

    // Dark accent variants
    val DarkAccentGreen = Color(0xFF2EA043)
    val DarkAccentBlue = Color(0xFF3B82F6)
    val DarkAccentRed = Color(0xFFF85149)
    val DarkAccentYellow = Color(0xFFF59E0B)
    val DarkAccentPurple = Color(0xFFA371F7)
}

// ── Custom theme tokens ───────────────────────────────────────────────────────

@Immutable
data class AppThemeColors(
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val border: Color,
    val accentGreen: Color,
    val accentBlue: Color,
    val accentRed: Color,
    val accentYellow: Color,
    val accentPurple: Color,
    val creditTint: Color,
    val debitTint: Color,
    val activeReport: Color,
    val isDark: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    lightAppColors()
}

fun lightAppColors() = AppThemeColors(
    background = AppColors.LightBackground,
    surface = AppColors.LightSurface,
    elevatedSurface = AppColors.LightElevated,
    primaryText = AppColors.LightPrimaryText,
    secondaryText = AppColors.LightSecondaryText,
    mutedText = AppColors.LightMutedText,
    border = AppColors.LightBorder,
    accentGreen = AppColors.AccentGreen,
    accentBlue = AppColors.AccentBlue,
    accentRed = AppColors.AccentRed,
    accentYellow = AppColors.AccentYellow,
    accentPurple = AppColors.AccentPurple,
    creditTint = AppColors.AccentGreen.copy(alpha = 0.12f),
    debitTint = AppColors.AccentRed.copy(alpha = 0.10f),
    activeReport = AppColors.AccentBlue.copy(alpha = 0.12f),
    isDark = false
)

fun darkAppColors() = AppThemeColors(
    background = AppColors.DarkBackground,
    surface = AppColors.DarkSurface,
    elevatedSurface = AppColors.DarkElevated,
    primaryText = AppColors.DarkPrimaryText,
    secondaryText = AppColors.DarkSecondaryText,
    mutedText = AppColors.DarkMutedText,
    border = AppColors.DarkBorder,
    accentGreen = AppColors.DarkAccentGreen,
    accentBlue = AppColors.DarkAccentBlue,
    accentRed = AppColors.DarkAccentRed,
    accentYellow = AppColors.DarkAccentYellow,
    accentPurple = AppColors.DarkAccentPurple,
    creditTint = AppColors.DarkAccentGreen.copy(alpha = 0.18f),
    debitTint = AppColors.DarkAccentRed.copy(alpha = 0.20f),
    activeReport = AppColors.DarkAccentBlue.copy(alpha = 0.20f),
    isDark = true
)

// ── Material3 color scheme builders ──────────────────────────────────────────

private fun lightColorScheme() = lightColorScheme(
    primary = AppColors.AccentGreen,
    secondary = AppColors.AccentBlue,
    tertiary = AppColors.AccentPurple,
    background = AppColors.LightBackground,
    surface = AppColors.LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppColors.LightPrimaryText,
    onSurface = AppColors.LightPrimaryText,
    error = AppColors.AccentRed
)

private fun darkColorScheme() = darkColorScheme(
    primary = AppColors.DarkAccentGreen,
    secondary = AppColors.DarkAccentBlue,
    tertiary = AppColors.DarkAccentPurple,
    background = AppColors.DarkBackground,
    surface = AppColors.DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppColors.DarkPrimaryText,
    onSurface = AppColors.DarkPrimaryText,
    error = AppColors.DarkAccentRed
)

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) darkAppColors() else lightAppColors()
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}

// Convenience accessor
val MaterialTheme.appColors: AppThemeColors
    @Composable get() = LocalAppColors.current

val chartPalette = listOf(
    Color(0xFF2563EB), Color(0xFF2DA44E), Color(0xFF7C3AED),
    Color(0xFFD97706), Color(0xFFDC2626), Color(0xFF4C8DA6),
    Color(0xFF73849A), Color(0xFF5E8B5A)
)
