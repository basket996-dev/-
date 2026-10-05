package com.zubora.taijuki.domain

import androidx.compose.ui.graphics.Color
import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.ui.theme.StampType
import java.time.LocalDate

data class MemoryItem(
    val date: LocalDate,
    val yearsAgo: Int,
    val yearLabel: String,
    val dateLabel: String,
    val weightDisplay: String,
    val memo: String,
    val diffText: String,
    val diffColor: Color,
)

/** Same date, every year back as far as the data goes. */
fun buildMemories(entries: Map<LocalDate, Entry>, today: LocalDate, currentWeight: Double): List<MemoryItem> {
    val oldestYear = entries.keys.minOfOrNull { it.year } ?: return emptyList()
    val maxYearsAgo = today.year - oldestYear
    if (maxYearsAgo < 1) return emptyList()
    return (1..maxYearsAgo).mapNotNull { yearsAgo ->
        val date = today.minusYears(yearsAgo.toLong())
        val e = entries[date] ?: return@mapNotNull null
        val diff = round1(currentWeight - e.weight)
        MemoryItem(
            date = date,
            yearsAgo = yearsAgo,
            yearLabel = "${yearsAgo}年前の今日",
            dateLabel = formatDateJa(date),
            weightDisplay = e.weight.toFixed1(),
            memo = e.memo,
            diffText = memoryDiffText(diff),
            diffColor = diffColor(diff),
        )
    }
}

data class MemoFeedItem(
    val date: LocalDate,
    val dateLabel: String,
    val weightDisplay: String,
    val memo: String,
)

/** Recent (<=200 days old) entries with a memo, newest first — older history lives in 思い出 instead. */
fun buildMemoFeed(entries: Map<LocalDate, Entry>, today: LocalDate): List<MemoFeedItem> {
    val cutoff = today.minusDays(200)
    return entries.entries
        .filter { (date, e) -> e.memo.isNotBlank() && date >= cutoff }
        .sortedByDescending { it.key }
        .take(6)
        .map { (date, e) -> MemoFeedItem(date, formatDateJa(date), e.weight.toFixed1(), e.memo) }
}

data class WeeklyStampCount(val stamp: StampType, val count: Int)

fun buildWeeklyStampCounts(entries: Map<LocalDate, Entry>, today: LocalDate): List<WeeklyStampCount> {
    val cutoff = today.minusDays(6)
    val counts = mutableMapOf<StampType, Int>()
    entries.forEach { (date, e) ->
        if (date in cutoff..today) {
            e.stamps.forEach { s -> counts[s] = (counts[s] ?: 0) + 1 }
        }
    }
    return StampType.entries.mapNotNull { s ->
        val c = counts[s] ?: 0
        if (c > 0) WeeklyStampCount(s, c) else null
    }
}

fun currentWeight(entries: Map<LocalDate, Entry>, fallback: Double): Double =
    entries.entries.maxByOrNull { it.key }?.value?.weight ?: fallback

/** Most-recent-entry minus second-most-recent, by whatever dates actually have entries (gaps allowed). */
fun dayDiff(entries: Map<LocalDate, Entry>): Double? {
    if (entries.size < 2) return null
    val newestTwo = entries.entries.sortedByDescending { it.key }
    return round1(newestTwo[0].value.weight - newestTwo[1].value.weight)
}

/** [dayDiff] compares the two newest entries, so it's only a true 前日比 when they're on consecutive days. */
fun dayDiffLabel(entries: Map<LocalDate, Entry>): String {
    val newestTwo = entries.keys.sortedDescending().take(2)
    return if (newestTwo.size < 2 || newestTwo[1].plusDays(1) == newestTwo[0]) "前日比" else "前回比"
}

fun computeBmi(weightKg: Double, heightCm: Double): String {
    val heightM = heightCm / 100.0
    return (weightKg / (heightM * heightM)).toFixed1()
}
