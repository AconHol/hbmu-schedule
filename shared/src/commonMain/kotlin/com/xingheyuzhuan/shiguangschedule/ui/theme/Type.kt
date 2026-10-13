// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size.sp, lineHeight = height.sp, fontWeight = weight, letterSpacing = 0.sp)

val Typography = Typography(
    displayLarge = type(52, 60, FontWeight.Bold), displayMedium = type(44, 52, FontWeight.Bold), displaySmall = type(36, 44, FontWeight.Bold),
    headlineLarge = type(32, 40, FontWeight.Bold), headlineMedium = type(28, 36, FontWeight.SemiBold), headlineSmall = type(24, 32, FontWeight.SemiBold),
    titleLarge = type(22, 30, FontWeight.SemiBold), titleMedium = type(16, 24, FontWeight.SemiBold), titleSmall = type(14, 20, FontWeight.SemiBold),
    bodyLarge = type(16, 26), bodyMedium = type(14, 22), bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.SemiBold), labelMedium = type(12, 18, FontWeight.Medium), labelSmall = type(11, 16, FontWeight.Medium)
)
