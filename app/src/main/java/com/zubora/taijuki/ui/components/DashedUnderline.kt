package com.zubora.taijuki.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Mirrors the design's `border-bottom: 2px dashed …` notebook-style input underline. */
fun Modifier.dashedBottomBorder(color: Color, thickness: Dp = 2.dp): Modifier = this.drawBehind {
    val strokeWidthPx = thickness.toPx()
    val y = size.height - strokeWidthPx / 2
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidthPx,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f),
    )
}
