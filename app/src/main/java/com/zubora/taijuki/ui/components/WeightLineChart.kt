package com.zubora.taijuki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.domain.GraphData
import com.zubora.taijuki.domain.GraphPoint
import com.zubora.taijuki.domain.formatDateJa
import com.zubora.taijuki.domain.round1
import com.zubora.taijuki.domain.toFixed1
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppFontFamily
import com.zubora.taijuki.ui.theme.accentFaint
import java.time.LocalDate
import kotlin.math.abs

private val CHART_HEIGHT = 250.dp
private val PAD_LEFT = 34.dp // room for the kg labels
private val PAD_RIGHT = 14.dp
private val PAD_TOP = 24.dp // room for stamp markers above the highest dot
private val PAD_BOTTOM = 26.dp // room for the date labels

/** Daily dots are drawn lighter than the 7-day line so the trend reads first. */
const val DAILY_DOT_ALPHA = 0.55f

private class PlotArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    private val yMin: Double,
    private val yMax: Double,
) {
    fun x(frac: Float): Float = left + frac * (right - left)
    fun y(value: Double): Float = (bottom - (value - yMin) / (yMax - yMin) * (bottom - top)).toFloat()
    fun dot(p: GraphPoint) = Offset(x(p.xFrac), y(p.weight))
}

private fun Density.plotArea(width: Float, height: Float, data: GraphData) = PlotArea(
    left = PAD_LEFT.toPx(),
    top = PAD_TOP.toPx(),
    right = width - PAD_RIGHT.toPx(),
    bottom = height - PAD_BOTTOM.toPx(),
    yMin = data.yMin,
    yMax = data.yMax,
)

private fun nearestIndex(data: GraphData, area: PlotArea, touchX: Float): Int? =
    data.points.indices.minByOrNull { abs(area.x(data.points[it].xFrac) - touchX) }

/**
 * The weight trend chart: a bold 7-day-average line over the day-by-day dots,
 * on a real date axis. Tap or drag across it to read any day's numbers.
 */
@Composable
fun WeightLineChart(graphData: GraphData, accent: Color, today: LocalDate, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    var selected by remember(graphData) { mutableStateOf<Int?>(null) }
    val styles = remember { ChartTextStyles() }
    val latest = graphData.points.lastOrNull()
    val description = latest?.let { "体重グラフ。最新は${formatDateJa(it.date)}の${it.weight.toFixed1()}kg" } ?: "体重グラフ"

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .semantics { contentDescription = description }
            .pointerInput(graphData) {
                detectTapGestures { offset ->
                    val area = plotArea(size.width.toFloat(), size.height.toFloat(), graphData)
                    val i = nearestIndex(graphData, area, offset.x)
                    selected = if (i == selected) null else i
                }
            }
            .pointerInput(graphData) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val area = plotArea(size.width.toFloat(), size.height.toFloat(), graphData)
                        selected = nearestIndex(graphData, area, offset.x)
                    },
                ) { change, _ ->
                    val area = plotArea(size.width.toFloat(), size.height.toFloat(), graphData)
                    selected = nearestIndex(graphData, area, change.position.x)
                }
            },
    ) {
        val area = plotArea(size.width, size.height, graphData)
        drawAxes(graphData, area, textMeasurer, styles)
        drawSeries(graphData, area, accent)
        val sel = selected?.let { graphData.points.getOrNull(it) }
        if (sel != null) {
            drawSelection(sel, area, accent, today, textMeasurer, styles)
        } else if (latest != null) {
            drawLatestCallout(latest, area, accent, textMeasurer, styles)
        }
    }
}

private class ChartTextStyles {
    val axis = TextStyle(fontFamily = AppFontFamily, fontSize = 10.5.sp, color = AppColors.TextSecondary)
    val callout = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AppColors.Card)
    val tipDate = TextStyle(fontFamily = AppFontFamily, fontSize = 11.sp, color = AppColors.TextSecondary)
    val tipWeight = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.TextPrimary)
    val tipSub = TextStyle(fontFamily = AppFontFamily, fontSize = 11.sp, color = AppColors.TextSecondary)
}

private fun DrawScope.drawAxes(data: GraphData, area: PlotArea, tm: TextMeasurer, styles: ChartTextStyles) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()), 0f)
    data.yTicks.forEach { t ->
        val y = area.y(t.value)
        drawLine(AppColors.BorderLight, Offset(area.left, y), Offset(area.right, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        val m = tm.measure(AnnotatedString(t.label), styles.axis)
        drawText(m, topLeft = Offset(area.left - 6.dp.toPx() - m.size.width, y - m.size.height / 2f))
    }
    drawLine(AppColors.BorderCard, Offset(area.left, area.bottom), Offset(area.right, area.bottom), strokeWidth = 1.dp.toPx())

    var lastLabelRight = Float.NEGATIVE_INFINITY
    data.xTicks.forEach { t ->
        val x = area.x(t.xFrac)
        drawLine(AppColors.BorderCard, Offset(x, area.bottom), Offset(x, area.bottom + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
        val m = tm.measure(AnnotatedString(t.label), styles.axis)
        val left = (x - m.size.width / 2f).coerceIn(0f, size.width - m.size.width)
        if (left < lastLabelRight + 4.dp.toPx()) return@forEach // never let two dates overlap
        drawText(m, topLeft = Offset(left, area.bottom + 6.dp.toPx()))
        lastLabelRight = left + m.size.width
    }
}

private fun DrawScope.drawSeries(data: GraphData, area: PlotArea, accent: Color) {
    val points = data.points
    if (points.size >= 2) {
        val average = Path().apply {
            points.forEachIndexed { i, p ->
                val x = area.x(p.xFrac)
                val y = area.y(p.average)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        val fill = Path().apply {
            addPath(average)
            lineTo(area.x(points.last().xFrac), area.bottom)
            lineTo(area.x(points.first().xFrac), area.bottom)
            close()
        }
        drawPath(fill, accentFaint(accent, 0.10f))
        drawPath(average, accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    points.forEachIndexed { i, p ->
        val c = area.dot(p)
        if (i == points.lastIndex) {
            drawCircle(accent, radius = 4.5.dp.toPx(), center = c)
            drawCircle(AppColors.Card, radius = 4.5.dp.toPx(), center = c, style = Stroke(width = 1.8.dp.toPx()))
        } else {
            drawCircle(accent.copy(alpha = DAILY_DOT_ALPHA), radius = 3.dp.toPx(), center = c)
        }
        p.marker?.let { stamp ->
            val m = Offset(c.x, c.y - 10.dp.toPx())
            drawCircle(stamp.color, radius = 3.6.dp.toPx(), center = m)
            drawCircle(AppColors.Card, radius = 3.6.dp.toPx(), center = m, style = Stroke(width = 1.2.dp.toPx()))
        }
    }
}

/** The latest weight as a small accent pill, on the side of the dot away from the average line. */
private fun DrawScope.drawLatestCallout(p: GraphPoint, area: PlotArea, accent: Color, tm: TextMeasurer, styles: ChartTextStyles) {
    val text = tm.measure(AnnotatedString("${p.weight.toFixed1()}kg"), styles.callout)
    val padH = 7.dp.toPx()
    val padV = 3.dp.toPx()
    val w = text.size.width + padH * 2
    val h = text.size.height + padV * 2
    val c = area.dot(p)
    val gap = 9.dp.toPx()
    val above = c.y - (if (p.marker != null) gap + 9.dp.toPx() else gap) - h
    val below = c.y + gap
    val lineIsAbove = p.average >= p.weight
    val top = when {
        lineIsAbove && below + h <= size.height -> below
        above >= 0f -> above
        else -> below
    }
    // Hang mostly to the left of the dot — the newest point usually sits at the right edge.
    val left = (c.x - w + 10.dp.toPx()).coerceIn(0f, size.width - w)
    drawRoundRect(accent, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(h / 2))
    drawText(text, topLeft = Offset(left + padH, top + padV))
}

private fun DrawScope.drawSelection(
    p: GraphPoint,
    area: PlotArea,
    accent: Color,
    today: LocalDate,
    tm: TextMeasurer,
    styles: ChartTextStyles,
) {
    val c = area.dot(p)
    drawLine(
        AppColors.TextPlaceholder,
        Offset(c.x, area.top),
        Offset(c.x, area.bottom),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f),
    )
    drawCircle(accent, radius = 5.5.dp.toPx(), center = c)
    drawCircle(AppColors.Card, radius = 5.5.dp.toPx(), center = c, style = Stroke(width = 2.dp.toPx()))

    val dateLabel = (if (p.date.year != today.year) "${p.date.year}年" else "") + formatDateJa(p.date)
    val lines: List<TextLayoutResult> = buildList {
        add(tm.measure(AnnotatedString(dateLabel), styles.tipDate))
        add(tm.measure(AnnotatedString("${p.weight.toFixed1()}kg"), styles.tipWeight))
        add(tm.measure(AnnotatedString("7日平均 ${round1(p.average).toFixed1()}kg"), styles.tipSub))
        if (p.stamps.isNotEmpty()) add(tm.measure(AnnotatedString(p.stamps.joinToString("・") { it.label }), styles.tipSub))
    }
    val pad = 8.dp.toPx()
    val w = lines.maxOf { it.size.width } + pad * 2
    val h = lines.sumOf { it.size.height } + pad * 2
    // Keep clear of the kg labels on the left when there's room.
    val maxLeft = (size.width - w).coerceAtLeast(0f)
    val left = (c.x - w / 2).coerceIn(minOf(area.left, maxLeft), maxLeft)
    // Sit at the top of the chart unless that would cover the selected dot.
    val top = if (c.y > h + 12.dp.toPx()) 0f else area.bottom - h - 4.dp.toPx()
    val radius = CornerRadius(10.dp.toPx())
    drawRoundRect(AppColors.Card, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = radius)
    drawRoundRect(AppColors.BorderCard, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = radius, style = Stroke(width = 1.dp.toPx()))
    var y = top + pad
    lines.forEach { line ->
        drawText(line, topLeft = Offset(left + pad, y))
        y += line.size.height
    }
}
