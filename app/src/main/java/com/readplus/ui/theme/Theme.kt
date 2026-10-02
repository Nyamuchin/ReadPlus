package com.readplus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.readplus.data.preferences.ThemeStyle

// ============================================================
// Material 3 Expressive Shapes（2025）
// 大圆角是 Expressive 的核心视觉特征
// ============================================================
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// ============================================================
// Miuix Shapes（HyperOS/MIUI）：圆角进一步放大，卡片化
// ============================================================
private val MiuixShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun ReadPlusTheme(
    themeStyle: ThemeStyle = ThemeStyle.MATERIAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeStyle) {
        ThemeStyle.MATERIAL -> materialColorScheme(darkTheme)
        ThemeStyle.MIUIX -> if (darkTheme) MiuixDarkColorScheme else MiuixLightColorScheme
    }
    val shapes = when (themeStyle) {
        ThemeStyle.MATERIAL -> ExpressiveShapes
        ThemeStyle.MIUIX -> MiuixShapes
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}

/**
 * Material 3 色彩方案。
 *
 * - 设备支持动态色（Android 12+）：使用系统提取的 Tonal Spot 调色板
 * - 不支持：使用 Expressive 规范的内置色板
 *
 * Tonal Spot 是 Material You 的默认调色板风格，
 * 从种子色提取低饱和、低对比度的 5 个色调。
 */
@Composable
private fun materialColorScheme(darkTheme: Boolean) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) ExpressiveDarkColorScheme else ExpressiveLightColorScheme
    }