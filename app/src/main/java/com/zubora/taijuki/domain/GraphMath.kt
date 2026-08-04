package com.zubora.taijuki.domain

import androidx.compose.ui.graphics.Color
import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.data.GraphPeriod
import com.zubora.taijuki.data.StampMode
import com.zubora.taijuki.ui.theme.StampType
import java.time.LocalDate

fun filterByPeriod(entries: Map<LocalDate, Entry>, period: GraphPeriod, today: LocalDate): Map<LocalDate, Entry> {
    val days = period.days ?: return entries
    val cutoff = today.minusDays((days - 1).toLong())
    return entries.filterKeys { it >= cutoff && it <= today }
}

data class GraphPoint(
    val date: LocalDate,
    val x: Float,
    val y: Float,
    val weight: Double,
    val hasStamp: Boolean,
    val stampColor: Color,
    val radius: Float,
    val isEndpoint: Boolean,
)

data class GraphGridLine(val y: Float, val label: String)

data class GraphData(
    val points: List<GraphPoint>,
    val gridLines: List<GraphGridLine>,
    val chartHeight: Float,
    val baselineY: Float,
    val chartWidth: Float,
)

private const val CHART_W = 376f
private const val CHART_H = 300f
private const val PAD_L = 34f
private const val PAD_R = 12f
private const val PAD_T = 26f
private const val PAD_B = 22f

private val EMPTY_GRAPH = GraphData(emptyList(), emptyList(), CHART_H, CHART_H - PAD_B, CHART_W)

fun buildGraphData(entries: Map<LocalDate, Entry>, stampMode: StampMode): GraphData {
    if (entries.isEmpty()) return EMPTY_GRAPH
    val dates = entries.keys.sorted()
    val weights = dates.map { entries.getValue(it).weight }
    val min = weights.min() - 0.6
    val max = weights.max() + 0.6
    val innerW = CHART_W - PAD_L - PAD_R
    val innerH = CHART_H - PAD_T - PAD_B
    val n = dates.size
    // weights.max()+0.6 / weights.min()-0.6 always leaves range >= 1.2, so this never divides by zero.
    val range = max - min

    fun toY(v: Double): Float = (PAD_T + innerH - ((v - min) / range) * innerH).toFloat()

    val points = dates.mapIndexed { i, date ->
        val e = entries.getValue(date)
        val x = if (n <= 1) PAD_L else PAD_L + (innerW * i) / (n - 1)
        val rawStamp = e.stamps.firstOrNull()
        val show = when (stampMode) {
            StampMode.None -> false
            StampMode.Key -> rawStamp != null && rawStamp in StampType.KeyStamps
            StampMode.All -> rawStamp != null
        }
        val isEndpoint = i == 0 || i == n - 1
        GraphPoint(
            date = date,
            x = x,
            y = toY(e.weight),
            weight = e.weight,
            hasStamp = show,
            stampColor = rawStamp?.color ?: Color.Transparent,
            radius = if (isEndpoint) 4f else 2.4f,
            isEndpoint = isEndpoint,
        )
    }
    val gridSteps = listOf(max, min + range / 2, min)
    val gridLines = gridSteps.map { v -> GraphGridLine(y = toY(v), label = v.toFixed1()) }
    return GraphData(points, gridLines, CHART_H, CHART_H - PAD_B, CHART_W)
}
