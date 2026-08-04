package com.zubora.taijuki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.zubora.taijuki.Tab

private fun round(width: Float) = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Bottom nav tab icons, ported from the prototype's 24x24 inline SVGs. */
@Composable
fun NavIcon(tab: Tab, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            when (tab) {
                Tab.Input -> drawPencil(tint, strokeWidth = 1.7f, withTip = true)
                Tab.Calendar -> drawCalendarGlyph(tint)
                Tab.Graph -> drawGraphGlyph(tint)
                Tab.Settings -> drawSettingsGlyph(tint)
            }
        }
    }
}

/** Small standalone pencil, reused by the input tab's "メモを追加" toggle. */
@Composable
fun PencilGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPencil(tint, strokeWidth = 1.8f, withTip = false)
        }
    }
}

/** A simple checkmark, shared by the notification banner badge and the "already logged" confirmation. */
@Composable
fun CheckGlyph(color: Color, modifier: Modifier = Modifier, strokeWidthFraction: Float = 0.14f) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.55f)
            lineTo(size.width * 0.42f, size.height * 0.78f)
            lineTo(size.width * 0.85f, size.height * 0.22f)
        }
        drawPath(
            path,
            color,
            style = Stroke(width = size.width * strokeWidthFraction, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun DrawScope.drawPencil(color: Color, strokeWidth: Float, withTip: Boolean) {
    val body = Path().apply {
        moveTo(4f, 20f); lineTo(5f, 16f); lineTo(16f, 5f); lineTo(19f, 8f); lineTo(8f, 19f); close()
    }
    drawPath(body, color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
    if (withTip) {
        drawLine(color, Offset(14f, 7f), Offset(17f, 10f), strokeWidth = strokeWidth)
    }
}

private fun DrawScope.drawCalendarGlyph(color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(4f, 5f),
        size = Size(16f, 15f),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(width = 1.7f),
    )
    drawLine(color, Offset(4f, 10f), Offset(20f, 10f), strokeWidth = 1.7f, cap = StrokeCap.Round)
    drawLine(color, Offset(8f, 3f), Offset(8f, 7f), strokeWidth = 1.7f, cap = StrokeCap.Round)
    drawLine(color, Offset(16f, 3f), Offset(16f, 7f), strokeWidth = 1.7f, cap = StrokeCap.Round)
}

private fun DrawScope.drawGraphGlyph(color: Color) {
    drawLine(color, Offset(4f, 19f), Offset(4f, 5f), strokeWidth = 1.7f, cap = StrokeCap.Round)
    drawLine(color, Offset(4f, 19f), Offset(20f, 19f), strokeWidth = 1.7f, cap = StrokeCap.Round)
    val trend = Path().apply {
        moveTo(6f, 15f); lineTo(10f, 11f); lineTo(13f, 14f); lineTo(18f, 8f)
    }
    drawPath(trend, color, style = round(1.7f))
}

private fun DrawScope.drawSettingsGlyph(color: Color) {
    drawCircle(color, radius = 3f, center = Offset(12f, 12f), style = Stroke(width = 1.7f))
    val spokes = listOf(
        Offset(12f, 3f) to Offset(12f, 6f),
        Offset(12f, 18f) to Offset(12f, 21f),
        Offset(3f, 12f) to Offset(6f, 12f),
        Offset(18f, 12f) to Offset(21f, 12f),
        Offset(5.8f, 5.8f) to Offset(7.9f, 7.9f),
        Offset(16.1f, 16.1f) to Offset(18.2f, 18.2f),
        Offset(5.8f, 18.2f) to Offset(7.9f, 16.1f),
        Offset(16.1f, 7.9f) to Offset(18.2f, 5.8f),
    )
    spokes.forEach { (start, end) ->
        drawLine(color, start, end, strokeWidth = 1.7f, cap = StrokeCap.Round)
    }
}
