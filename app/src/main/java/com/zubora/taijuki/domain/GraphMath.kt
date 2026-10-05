package com.zubora.taijuki.domain

import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.data.GraphPeriod
import com.zubora.taijuki.data.StampMode
import com.zubora.taijuki.ui.theme.StampType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/** One recorded day. [xFrac] is its position on the date axis (0 = left edge, 1 = right edge). */
data class GraphPoint(
    val date: LocalDate,
    val weight: Double,
    val xFrac: Float,
    /** Mean of every entry in the 7 days ending on [date] — smooths out the day-to-day wobble. */
    val average: Double,
    /** The stamp drawn above the dot, after the 主要3種/すべて/非表示 setting is applied. */
    val marker: StampType?,
    val stamps: List<StampType>,
)

data class GraphYTick(val value: Double, val label: String)

data class GraphXTick(val xFrac: Float, val label: String)

data class GraphData(
    val points: List<GraphPoint>,
    val yMin: Double,
    val yMax: Double,
    val yTicks: List<GraphYTick>,
    val xTicks: List<GraphXTick>,
    /** Stamps that have a marker somewhere in view, in StampType order — the chart's legend. */
    val legendStamps: List<StampType>,
)

val EMPTY_GRAPH = GraphData(emptyList(), 0.0, 1.0, emptyList(), emptyList(), emptyList())

private const val AVERAGE_DAYS = 7L

/** A day-to-day wobble of a few hundred grams shouldn't fill the whole chart height. */
private const val MIN_Y_SPAN = 1.0
private const val MAX_Y_TICKS = 5
private val Y_STEPS = listOf(0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0)

private const val MAX_X_TICKS = 7

/**
 * Lays out the weight chart for [period]. Points sit on a real date axis, so a
 * week with no records shows up as a gap instead of being squeezed out.
 */
fun buildGraphData(
    entries: Map<LocalDate, Entry>,
    period: GraphPeriod,
    stampMode: StampMode,
    today: LocalDate,
): GraphData {
    if (entries.isEmpty()) return EMPTY_GRAPH
    val firstEver = entries.keys.min()
    val end = maxOf(today, entries.keys.max())
    val periodStart = period.days?.let { end.minusDays((it - 1).toLong()) } ?: firstEver
    // Nothing can exist before the first record, so don't draw an empty run-up to it.
    val start = maxOf(periodStart, firstEver)
    val visible = entries.keys.filter { it in start..end }.sorted()
    if (visible.isEmpty()) return EMPTY_GRAPH

    val spanDays = ChronoUnit.DAYS.between(start, end)
    fun xFrac(date: LocalDate): Float =
        if (spanDays == 0L) 0.5f else ChronoUnit.DAYS.between(start, date).toFloat() / spanDays

    val points = visible.map { date ->
        val e = entries.getValue(date)
        GraphPoint(
            date = date,
            weight = e.weight,
            xFrac = xFrac(date),
            average = trailingAverage(entries, date),
            marker = e.stamps.firstOrNull { stampMode.shows(it) },
            stamps = e.stamps,
        )
    }

    val values = points.flatMap { listOf(it.weight, it.average) }
    val (yMin, yMax, yTicks) = niceYAxis(values.min(), values.max())
    return GraphData(
        points = points,
        yMin = yMin,
        yMax = yMax,
        yTicks = yTicks,
        xTicks = dateTicks(start, end).map { GraphXTick(xFrac(it.first), it.second) },
        legendStamps = StampType.entries.filter { s -> points.any { it.marker == s } },
    )
}

private fun StampMode.shows(stamp: StampType): Boolean = when (this) {
    StampMode.None -> false
    StampMode.Key -> stamp in StampType.KeyStamps
    StampMode.All -> true
}

/** Reads from all entries, not just the visible window, so the line doesn't restart at the left edge. */
fun trailingAverage(entries: Map<LocalDate, Entry>, date: LocalDate): Double {
    val from = date.minusDays(AVERAGE_DAYS - 1)
    val window = entries.filterKeys { it in from..date }.values.map { it.weight }
    return window.average()
}

/** Round-numbered gridlines (77, 78, 79… or 77.5, 78.0…) instead of the raw data extremes. */
internal fun niceYAxis(dataMin: Double, dataMax: Double): Triple<Double, Double, List<GraphYTick>> {
    var lo = dataMin
    var hi = dataMax
    if (hi - lo < MIN_Y_SPAN) {
        val mid = (lo + hi) / 2
        lo = mid - MIN_Y_SPAN / 2
        hi = mid + MIN_Y_SPAN / 2
    }
    // Small margin so the extreme dots don't sit right on the frame.
    lo -= 0.1
    hi += 0.1
    for (step in Y_STEPS) {
        val first = floor(lo / step).roundToLong()
        val last = ceil(hi / step).roundToLong()
        if (last - first + 1 <= MAX_Y_TICKS) {
            val ticks = (first..last).map { k ->
                val v = round1(k * step)
                GraphYTick(v, if (step >= 1.0) v.roundToLong().toString() else v.toFixed1())
            }
            return Triple(ticks.first().value, ticks.last().value, ticks)
        }
    }
    // Only reachable for absurd ranges (> ~80kg of spread); fall back to the plain extremes.
    return Triple(lo, hi, listOf(GraphYTick(lo, lo.toFixed1()), GraphYTick(hi, hi.toFixed1())))
}

/**
 * Picks the densest calendar-aligned spacing that still fits [MAX_X_TICKS]:
 * every day / 2 days / week (counted back from the right edge so today is
 * labelled), then the 1st & 15th, then whole months.
 */
internal fun dateTicks(start: LocalDate, end: LocalDate): List<Pair<LocalDate, String>> {
    val spanDays = ChronoUnit.DAYS.between(start, end)
    for (step in listOf(1L, 2L, 7L)) {
        if (spanDays / step + 1 <= MAX_X_TICKS) {
            return generateSequence(end) { it.minusDays(step) }
                .takeWhile { it >= start }
                .toList()
                .reversed()
                .map { it to "${it.monthValue}/${it.dayOfMonth}" }
        }
    }
    val semiMonthly = generateSequence(start.withDayOfMonth(1)) { it.plusMonths(1) }
        .takeWhile { it <= end }
        .flatMap { sequenceOf(it, it.withDayOfMonth(15)) }
        .filter { it in start..end }
        .toList()
    if (semiMonthly.size <= MAX_X_TICKS) {
        return semiMonthly.map { it to "${it.monthValue}/${it.dayOfMonth}" }
    }
    val monthStarts = generateSequence(start.withDayOfMonth(1)) { it.plusMonths(1) }
        .takeWhile { it <= end }
        .filter { it >= start }
        .toList()
    for (monthStep in listOf(1, 2, 3, 6, 12)) {
        val picked = monthStarts.filter { (it.monthValue - 1) % monthStep == 0 }
        if (picked.size <= MAX_X_TICKS) {
            // January carries the year so a multi-year chart stays readable.
            return picked.map { it to if (it.monthValue == 1) "${it.year}年" else "${it.monthValue}月" }
        }
    }
    val years = monthStarts.filter { it.monthValue == 1 }
    val yearStep = (years.size + MAX_X_TICKS - 1) / MAX_X_TICKS
    return years.filterIndexed { i, _ -> i % yearStep == 0 }.map { it to "${it.year}年" }
}
