package com.zubora.taijuki.domain

import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.data.GraphPeriod
import com.zubora.taijuki.data.Stamp
import com.zubora.taijuki.data.StampMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GraphMathTest {
    private val today = LocalDate.of(2026, 10, 5)

    private fun entries(vararg days: Pair<LocalDate, Double>, stamps: Map<LocalDate, List<Stamp>> = emptyMap()) =
        days.associate { (d, w) -> d to Entry(d, w, stamps[d] ?: emptyList(), "") }

    @Test
    fun yAxisUsesWholeKilogramsForAWideRange() {
        val (min, max, ticks) = niceYAxis(77.5, 80.2)
        assertEquals(listOf("77", "78", "79", "80", "81"), ticks.map { it.label })
        assertEquals(77.0, min, 1e-9)
        assertEquals(81.0, max, 1e-9)
    }

    @Test
    fun yAxisKeepsAtLeastOneKilogramSoSmallWobblesStaySmall() {
        val (_, _, ticks) = niceYAxis(77.7, 78.0)
        assertEquals(listOf("77.0", "77.5", "78.0", "78.5"), ticks.map { it.label })
    }

    @Test
    fun yAxisHandlesAFlatLine() {
        val (min, max, ticks) = niceYAxis(60.0, 60.0)
        assertTrue(ticks.size in 2..5)
        assertTrue(min < 60.0 && max > 60.0)
    }

    @Test
    fun weekShowsEveryDay() {
        val labels = dateTicks(LocalDate.of(2026, 9, 29), today).map { it.second }
        assertEquals(listOf("9/29", "9/30", "10/1", "10/2", "10/3", "10/4", "10/5"), labels)
    }

    @Test
    fun monthShowsWeeklyDatesEndingToday() {
        val labels = dateTicks(LocalDate.of(2026, 9, 6), today).map { it.second }
        assertEquals(listOf("9/7", "9/14", "9/21", "9/28", "10/5"), labels)
    }

    @Test
    fun twoMonthsShowFirstAndFifteenth() {
        val labels = dateTicks(LocalDate.of(2026, 8, 5), today).map { it.second }
        assertEquals(listOf("8/15", "9/1", "9/15", "10/1"), labels)
    }

    @Test
    fun multiYearSpanStaysSparseAndNamesTheYear() {
        val ticks = dateTicks(LocalDate.of(2025, 1, 10), LocalDate.of(2027, 1, 5))
        assertTrue(ticks.size <= 7)
        assertEquals(listOf("7月", "2026年", "7月", "2027年"), ticks.map { it.second })
    }

    @Test
    fun pointsAreSpacedByDateNotByIndex() {
        val start = today.minusDays(10)
        val data = buildGraphData(
            entries(start to 80.0, start.plusDays(1) to 79.8, today to 79.0),
            GraphPeriod.All, StampMode.All, today,
        )
        assertEquals(listOf(0f, 0.1f, 1f), data.points.map { it.xFrac })
    }

    @Test
    fun periodWithNoRecordsIsEmpty() {
        val data = buildGraphData(entries(today.minusDays(20) to 80.0), GraphPeriod.Week, StampMode.All, today)
        assertTrue(data.points.isEmpty())
    }

    @Test
    fun periodDoesNotStretchBackBeforeTheFirstRecord() {
        val data = buildGraphData(
            entries(today.minusDays(3) to 80.0, today to 79.5),
            GraphPeriod.Month, StampMode.All, today,
        )
        assertEquals(0f, data.points.first().xFrac)
        assertEquals(1f, data.points.last().xFrac)
    }

    @Test
    fun averageCoversTheSevenDaysEndingOnThatDate() {
        val all = entries(
            today.minusDays(7) to 90.0, // 8 days back from today: outside today's window
            today.minusDays(6) to 80.0,
            today.minusDays(2) to 79.0,
            today to 78.0,
        )
        assertEquals(79.0, trailingAverage(all, today), 1e-9)
        // The window reaches before the visible range, so a week view starts on the true trend.
        val week = buildGraphData(all, GraphPeriod.Week, StampMode.All, today)
        assertEquals(85.0, week.points.first().average, 1e-9)
    }

    @Test
    fun markerIsTheDaysFirstStampAndTheLegendFollowsTheUsersOrder() {
        val yesterday = today.minusDays(1)
        val all = entries(
            yesterday to 80.0,
            today to 79.5,
            stamps = mapOf(yesterday to listOf(TestStamps.gaishoku), today to listOf(TestStamps.benzuu, TestStamps.tabesugi)),
        )
        val data = buildGraphData(all, GraphPeriod.All, StampMode.All, today)
        assertEquals(listOf(TestStamps.gaishoku, TestStamps.benzuu), data.points.map { it.marker })
        // benzuu comes before gaishoku in the user's order, whichever day shows up first.
        assertEquals(listOf(TestStamps.benzuu, TestStamps.gaishoku), data.legendStamps)

        val hidden = buildGraphData(all, GraphPeriod.All, StampMode.None, today)
        assertTrue(hidden.points.all { it.marker == null })
        assertTrue(hidden.legendStamps.isEmpty())
    }

    @Test
    fun theOldKeyStampSettingNowShowsEveryStamp() {
        // "key" (主要3種) was saved by the version with six fixed stamps.
        assertEquals(StampMode.All, StampMode.fromId("key"))
        assertEquals(StampMode.None, StampMode.fromId("none"))
    }

    @Test
    fun singleRecordSitsInTheMiddle() {
        val data = buildGraphData(entries(today to 70.0), GraphPeriod.All, StampMode.All, today)
        assertEquals(0.5f, data.points.single().xFrac)
    }

    @Test
    fun diffLabelSaysPreviousRecordWhenDaysWereSkipped() {
        assertEquals("前日比", dayDiffLabel(entries(today.minusDays(1) to 80.0, today to 79.5)))
        assertEquals("前回比", dayDiffLabel(entries(today.minusDays(2) to 80.0, today to 79.5)))
        assertEquals("前日比", dayDiffLabel(entries(today to 79.5)))
    }
}
