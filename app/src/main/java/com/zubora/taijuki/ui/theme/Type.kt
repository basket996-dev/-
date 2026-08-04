package com.zubora.taijuki.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The design calls for Zen Maru Gothic (a warm, rounded Google Font, loaded via
 * @import in the HTML prototype). Pulling that in on Android means either the
 * downloadable-fonts provider — which needs an exact Google-published signing
 * certificate baked into the app as a long base64 constant, unverifiable here
 * and silently wrong if mistyped — or bundling a font file, which needs the
 * binary asset itself. Neither is available in this build environment, so
 * this uses the platform default family. Swap AppFontFamily below for a
 * bundled or downloadable Zen Maru Gothic when one is available; every text
 * style in the app already reads from this single constant.
 */
val AppFontFamily: FontFamily = FontFamily.Default

val AppTypography = Typography(
    bodyLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    titleLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)
