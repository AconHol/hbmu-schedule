package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode

/**
 * 定义一个用于全局同步深色模式状态的 Local 变量
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * 外部调用的快捷主题函数
 * 自动根据 AppSettingsModel 处理所有主题逻辑
 */
@Composable
fun ShiguangScheduleTheme(
    settings: AppSettingsModel,
    content: @Composable () -> Unit
) {
    val darkTheme = when (settings.themeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        ShiguangScheduleTheme(
            darkTheme = darkTheme,
            dynamicColor = settings.useDynamicColor,
            customLightPrimary = Color(settings.customPrimaryColor.light),
            customDarkPrimary = Color(settings.customPrimaryColor.dark),
            themeMode = settings.themeMode,
            content = content
        )
    }
}

/**
 * 核心主题实现函数（跨平台通用）
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
fun ShiguangScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    customLightPrimary: Color = DefaultThemeColor,
    customDarkPrimary: Color = DefaultThemeColor,
    themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = rememberColorScheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        customLightPrimary = customLightPrimary,
        customDarkPrimary = customDarkPrimary
    )

    // 应用平台特定的窗口与系统栏外观控制
    SetupPlatformThemeEffects(
        colorScheme = colorScheme,
        darkTheme = darkTheme,
        themeMode = themeMode
    )

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp)
        ),
        motionScheme = MotionScheme.expressive(),
        content = { CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) { content() } }
    )
}

/**
 * 共享的 MaterialKolor 动态配色方案生成函数
 */
@Composable
fun rememberMaterialKolorScheme(
    darkTheme: Boolean,
    seedColor: Color,
    style: PaletteStyle = PaletteStyle.Expressive
): ColorScheme {
    return rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        style = style
    )
}

/**
 * 平台特定的配色生成声明
 */
@Composable
expect fun rememberColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    customLightPrimary: Color,
    customDarkPrimary: Color
): ColorScheme

/**
 * 平台特定的窗口与系统栏外观控制声明
 */
@Composable
expect fun SetupPlatformThemeEffects(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
    themeMode: AppThemeMode
)

/**
 * 平台特定的能力：当前系统/平台是否支持 Dynamic Color (动态取色)
 */
expect val supportsDynamicColor: Boolean
