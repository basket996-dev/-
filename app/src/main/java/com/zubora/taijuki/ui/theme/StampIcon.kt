package com.zubora.taijuki.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.zubora.taijuki.data.Stamp

/**
 * Hand-drawn stamp icons, ported from the prototype's inline SVG paths (24x24
 * viewBox) into Compose Path/arc calls. One vector per stamp, scaled to
 * whatever size it's placed at — unlike the HTML source, which hand-simplified
 * a couple of icons (undou's head, ganbatta's ribbons) for the tiny calendar
 * cells, a vector path scales cleanly so a single definition covers every
 * context (keypad, day-sheet, calendar cell, graph markers).
 */
@Composable
fun StampIcon(
    stamp: Stamp,
    modifier: Modifier = Modifier,
    tint: Color = stamp.color,
) {
    val builtin = stamp.builtin
    if (builtin != null) {
        BuiltinStampIcon(builtin, modifier, tint)
    } else {
        EmojiStampIcon(stamp.emoji ?: stamp.label.take(1), modifier)
    }
}

/** A stamp the user made: its emoji, sized to the same box the drawn icons fill. Emoji keep their own colors. */
@Composable
private fun EmojiStampIcon(emoji: String, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val size = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.85f).toSp() }
        Text(emoji, style = TextStyle(fontSize = size, lineHeight = size, textAlign = TextAlign.Center))
    }
}

@Composable
private fun BuiltinStampIcon(
    type: StampType,
    modifier: Modifier,
    tint: Color,
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            when (type) {
                StampType.Tabesugi -> drawTabesugi(tint)
                StampType.Osake -> drawOsake(tint)
                StampType.Undou -> drawUndou(tint)
                StampType.Benzuu -> drawBenzuu(tint)
                StampType.Gohoubi -> drawGohoubi(tint)
                StampType.Ganbatta -> drawGanbatta(tint)
            }
        }
    }
}

private fun round(width: Float) = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

// 食べすぎ (overeating) — a bowl with two small steam wisps.
private fun DrawScope.drawTabesugi(color: Color) {
    val bowl = Path().apply {
        moveTo(4f, 13f)
        cubicTo(4f, 17f, 7.6f, 19f, 12f, 19f)
        cubicTo(16.4f, 19f, 20f, 17f, 20f, 13f)
    }
    drawPath(bowl, color, style = round(1.7f))
    drawLine(color, Offset(3f, 13f), Offset(21f, 13f), strokeWidth = 1.7f, cap = StrokeCap.Round)
    val steam1 = Path().apply { moveTo(9f, 8f); cubicTo(9f, 7f, 9.8f, 6f, 10f, 5f) }
    val steam2 = Path().apply { moveTo(15f, 8f); cubicTo(15f, 7f, 15.6f, 6f, 15.8f, 5f) }
    drawPath(steam1, color, style = round(1.4f))
    drawPath(steam2, color, style = round(1.4f))
}

// お酒 (alcohol) — a coupe glass.
private fun DrawScope.drawOsake(color: Color) {
    drawLine(color, Offset(7f, 4f), Offset(17f, 4f), strokeWidth = 1.7f, cap = StrokeCap.Round)
    val cup = Path().apply {
        moveTo(7f, 4f)
        cubicTo(7f, 8f, 8f, 11f, 12f, 11f)
        cubicTo(16f, 11f, 17f, 8f, 17f, 4f)
    }
    drawPath(cup, color, style = round(1.7f))
    drawLine(color, Offset(12f, 11f), Offset(12f, 18f), strokeWidth = 1.7f, cap = StrokeCap.Round)
    drawLine(color, Offset(8.5f, 20f), Offset(15.5f, 20f), strokeWidth = 1.7f, cap = StrokeCap.Round)
}

// 運動 (exercise) — a running figure.
private fun DrawScope.drawUndou(color: Color) {
    drawCircle(color, radius = 1.8f, center = Offset(14.2f, 4.6f), style = Stroke(width = 1.5f))
    val front = Path().apply {
        moveTo(14f, 7.5f); lineTo(11.5f, 10.5f); lineTo(13.5f, 13f); lineTo(12.5f, 19f)
    }
    val back = Path().apply {
        moveTo(14f, 7.5f); lineTo(17f, 9.5f); lineTo(16f, 13.5f); lineTo(19f, 16f)
    }
    drawPath(front, color, style = round(1.7f))
    drawPath(back, color, style = round(1.7f))
    drawLine(color, Offset(11.5f, 10.5f), Offset(8f, 11.5f), strokeWidth = 1.7f, cap = StrokeCap.Round)
}

// 便通 (bowel movement) — a tapering swirl, three stacked semicircle arcs.
private fun DrawScope.drawBenzuu(color: Color) {
    val swirl = Path().apply {
        moveTo(12f, 4f)
        arcTo(Rect(8f, 4f, 16f, 12f), startAngleDegrees = 270f, sweepAngleDegrees = 180f, forceMoveTo = false)
        arcTo(Rect(9f, 6f, 15f, 12f), startAngleDegrees = 90f, sweepAngleDegrees = 180f, forceMoveTo = false)
        arcTo(Rect(10f, 6f, 14f, 10f), startAngleDegrees = 270f, sweepAngleDegrees = 180f, forceMoveTo = false)
    }
    drawPath(swirl, color, style = round(1.7f))
}

// ご褒美 (treat) — a party-hat triangle with a dot.
private fun DrawScope.drawGohoubi(color: Color) {
    val triangle = Path().apply {
        moveTo(4f, 18f); lineTo(12f, 6f); lineTo(20f, 18f); close()
    }
    drawPath(triangle, color, style = Stroke(width = 1.7f, join = StrokeJoin.Round))
    drawCircle(color, radius = 1.2f, center = Offset(12f, 6f))
    drawLine(color, Offset(4f, 18f), Offset(20f, 18f), strokeWidth = 1.7f, cap = StrokeCap.Round)
}

// がんばった (did well) — a badge with a checkmark and two ribbon tails.
private fun DrawScope.drawGanbatta(color: Color) {
    drawCircle(color, radius = 4.5f, center = Offset(12f, 9f), style = Stroke(width = 1.7f))
    val leftRibbon = Path().apply {
        moveTo(9.3f, 13f); lineTo(6.5f, 19.8f); lineTo(9.5f, 18.5f); lineTo(11f, 21.2f)
    }
    val rightRibbon = Path().apply {
        moveTo(14.7f, 13f); lineTo(17.5f, 19.8f); lineTo(14.5f, 18.5f); lineTo(13f, 21.2f)
    }
    val check = Path().apply {
        moveTo(10f, 9f); lineTo(11.3f, 10.3f); lineTo(14f, 8f)
    }
    drawPath(leftRibbon, color, style = round(1.7f))
    drawPath(rightRibbon, color, style = round(1.7f))
    drawPath(check, color, style = round(1.7f))
}
