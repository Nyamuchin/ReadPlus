package com.readplus.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ============================================================
// Miuix (HyperOS / MIUI) 色彩规范
// - 主色：MIUI 蓝 #3482FF
// - 卡片：纯白 / 纯黑，扁平
// - 圆角更大（在 Theme.kt 中定义 Shapes）
// ============================================================

val MiuixLightColorScheme = lightColorScheme(
    primary = Color(0xFF3482FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E3FF),
    onPrimaryContainer = Color(0xFF001B3D),

    secondary = Color(0xFF565E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),

    tertiary = Color(0xFF715573),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFBDAFB),
    onTertiaryContainer = Color(0xFF29132D),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF191C20),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFF0F3F7),
    surfaceContainerHigh = Color(0xFFEAEDF1),
    surfaceContainerHighest = Color(0xFFE4E7EB),

    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),

    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFF0F0F4),
    inversePrimary = Color(0xFFAEC6FF),

    scrim = Color(0xFF000000)
)

val MiuixDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF00458E),
    onPrimaryContainer = Color(0xFFD7E3FF),

    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283041),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),

    tertiary = Color(0xFFDEBCDF),
    onTertiary = Color(0xFF402843),
    tertiaryContainer = Color(0xFF583E5B),
    onTertiaryContainer = Color(0xFFFBDAFB),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF111318),
    onBackground = Color(0xFFE1E2E8),

    surface = Color(0xFF1A1C20),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),

    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),

    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),

    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2E3135),
    inversePrimary = Color(0xFF3482FF),

    scrim = Color(0xFF000000)
)

// ============================================================
// Material 3 fallback（设备不支持动态色时）
// 2025 Expressive 色彩规范的核心：更饱和的 accent、更生动的层级
// ============================================================

val ExpressiveLightColorScheme = lightColorScheme(
    primary = Color(0xFF6B4EA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF22005D),

    secondary = Color(0xFFA23F75),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD8E7),
    onSecondaryContainer = Color(0xFF3E0027),

    tertiary = Color(0xFFB8563D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBD0),
    onTertiaryContainer = Color(0xFF400C00),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE8E0EB),
    onSurfaceVariant = Color(0xFF49454E),

    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCBC4CF),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF322F35),
    inverseOnSurface = Color(0xFFF5EFF7),
    inversePrimary = Color(0xFFCEBDFF)
)

val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFCEBDFF),
    onPrimary = Color(0xFF390E7D),
    primaryContainer = Color(0xFF522A8F),
    onPrimaryContainer = Color(0xFFEADDFF),

    secondary = Color(0xFFFFAFD4),
    onSecondary = Color(0xFF5E113D),
    secondaryContainer = Color(0xFF7C2955),
    onSecondaryContainer = Color(0xFFFFD8E7),

    tertiary = Color(0xFFFFB59F),
    onTertiary = Color(0xFF63200E),
    tertiaryContainer = Color(0xFF843623),
    onTertiaryContainer = Color(0xFFFFDBD0),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF141218),
    onBackground = Color(0xFFE7E0E8),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE7E0E8),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCBC4CF),

    outline = Color(0xFF958E99),
    outlineVariant = Color(0xFF49454E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE7E0E8),
    inverseOnSurface = Color(0xFF322F35),
    inversePrimary = Color(0xFF6B4EA8)
)