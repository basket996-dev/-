package com.zubora.taijuki.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * accentDark / accentFaint mirror the prototype's
 * `color-mix(in srgb, accent 78%, black)` / `color-mix(in srgb, accent 14%, transparent)`.
 */
fun accentDark(accent: Color): Color = lerp(accent, Color.Black, 0.22f)
fun accentFaint(accent: Color, alpha: Float = 0.14f): Color = accent.copy(alpha = alpha)

/**
 * The design is a single fixed warm/cream look with no authored dark variant,
 * so the palette is intentionally not theme-switched by system dark mode.
 */
@Composable
fun ZuboraTheme(
    accentColor: Color = AppColors.DefaultAccent,
    content: @Composable () -> Unit,
) {
    val colorScheme = lightColorScheme(
        primary = accentColor,
        onPrimary = AppColors.Card,
        secondary = accentDark(accentColor),
        background = AppColors.Surface,
        onBackground = AppColors.TextPrimary,
        surface = AppColors.Card,
        onSurface = AppColors.TextPrimary,
        surfaceVariant = AppColors.CardAlt,
        onSurfaceVariant = AppColors.TextSecondary,
        outline = AppColors.BorderCard,
        error = AppColors.Negative,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
