package com.zubora.taijuki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.domain.GraphData
import com.zubora.taijuki.domain.toFixed1
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppFontFamily
import com.zubora.taijuki.ui.theme.accentFaint

/**
 * The weight trend chart. Geometry (line/area/points/gridlines) is drawn in the
 * design's fixed 376x300 coordinate space and scaled to the actual canvas size —
 * mirroring the prototype's SVG viewBox. Text labels are drawn separately at a
 * FIXED font size, only their position scaled — the prototype had to move its
 * labels to HTML overlays for the same reason (SVG `<text>` couldn't hold the
 * templating engine's `{{ }}` holes), and a fixed-size label was the result;
 * Compose's Canvas can draw text directly, so that's reproduced directly here
 * rather than through the overlay workaround.
 */
@Composable
fun WeightLineChart(graphData: GraphData, accent: Color, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val gridLabelStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 10.5.sp,
        color = AppColors.TextSecondary,
    )
    val calloutStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = AppColors.TextPrimary,
    )
    val faint = accentFaint(accent)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(graphData.chartHeight.dp), // 1 design unit == 1dp, same reference scale as the prototype's CSS px
    ) {
        val scaleX = size.width / graphData.chartWidth
        val scaleY = size.height / graphData.chartHeight

        scale(scaleX, scaleY, pivot = Offset.Zero) {
            graphData.gridLines.forEach { g ->
                drawLine(
                    color = AppColors.BorderLight,
                    start = Offset(34f, g.y),
                    end = Offset(368f, g.y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f),
                )
            }

            val points = graphData.points
            if (points.isNotEmpty()) {
                val area = Path().apply {
                    moveTo(points.first().x, graphData.baselineY)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, graphData.baselineY)
                    close()
                }
                drawPath(area, faint)

                val line = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(line, accent, style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                points.forEach { p ->
                    drawCircle(accent, radius = p.radius, center = Offset(p.x, p.y))
                    drawCircle(AppColors.Card, radius = p.radius, center = Offset(p.x, p.y), style = Stroke(width = 1.5f))
                    if (p.hasStamp) {
                        val markerY = p.y - 11f
                        drawCircle(p.stampColor, radius = 3.2f, center = Offset(p.x, markerY))
                        drawCircle(AppColors.Card, radius = 3.2f, center = Offset(p.x, markerY), style = Stroke(width = 1f))
                    }
                }
            }
        }

        // Gridline value labels — fixed size, positioned at the scaled y.
        graphData.gridLines.forEach { g ->
            val measured = textMeasurer.measure(AnnotatedString(g.label), gridLabelStyle)
            val x = 8f * scaleX
            val y = g.y * scaleY - measured.size.height / 2f
            drawRect(
                color = AppColors.Card,
                topLeft = Offset(x - 2f, y),
                size = Size(measured.size.width + 4f, measured.size.height.toFloat()),
            )
            drawText(measured, topLeft = Offset(x, y))
        }

        // Start/current weight callouts on the first/last points.
        graphData.points.forEachIndexed { i, p ->
            if (!p.isEndpoint) return@forEachIndexed
            val isFirst = i == 0
            val measured = textMeasurer.measure(AnnotatedString("${p.weight.toFixed1()}kg"), calloutStyle)
            val px = p.x * scaleX
            val py = p.y * scaleY
            val x = if (isFirst) px + 2f else px - measured.size.width - 2f
            val y = py - measured.size.height / 2f
            drawText(measured, topLeft = Offset(x, y))
        }
    }
}
