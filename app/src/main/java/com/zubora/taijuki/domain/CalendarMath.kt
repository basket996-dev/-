package com.zubora.taijuki.domain

import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.ui.theme.StampType
import java.time.LocalDate
import java.time.YearMonth

data class CalendarCell(
    val date: LocalDate?,
    val dayLabel: String,
    val inMonth: Boolean,
    val isToday: Boolean,
    val hasEntry: Boolean,
    val weightDisplay: String,
    val stamps: List<StampType>,
    val hasMemo: Boolean,
)

/** 42 cells (6 weeks), Sunday-first, matching the design's fixed-height grid. */
fun buildCalendarCells(month: YearMonth, entries: Map<LocalDate, Entry>, today: LocalDate): List<CalendarCell> {
    val firstOfMonth = month.atDay(1)
    val startOffset = firstOfMonth.dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()
    return (0 until 42).map { i ->
        val dayNum = i - startOffset + 1
        val inMonth = dayNum in 1..daysInMonth
        val date = if (inMonth) month.atDay(dayNum) else null
        val entry = date?.let { entries[it] }
        CalendarCell(
            date = date,
            dayLabel = if (inMonth) dayNum.toString() else "",
            inMonth = inMonth,
            isToday = date == today,
            hasEntry = entry != null,
            weightDisplay = entry?.weight?.toFixed1() ?: "",
            stamps = entry?.stamps ?: emptyList(),
            hasMemo = !(entry?.memo.isNullOrBlank()),
        )
    }
}
