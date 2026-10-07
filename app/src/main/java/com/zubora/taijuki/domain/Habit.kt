package com.zubora.taijuki.domain

import com.zubora.taijuki.data.Entry
import java.text.BreakIterator
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Keeping the habit going without nagging: a weekly target instead of a daily
 * streak, a gentler tone after a gap, and a reminder that moves to when the
 * user actually weighs in.
 */

/** Three times a week is the bar — every day is the ideal, not the requirement. */
const val WEEKLY_TARGET = 3

/** From this many days without a record, the copy switches to "welcome back". */
const val COMEBACK_GAP_DAYS = 3

/** Sunday-first, like the calendar. */
fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

data class WeekProgress(val count: Int, val target: Int = WEEKLY_TARGET) {
    val achieved: Boolean get() = count >= target
    val remaining: Int get() = (target - count).coerceAtLeast(0)
}

fun weekProgress(dates: Collection<LocalDate>, today: LocalDate): WeekProgress {
    val from = weekStart(today)
    return WeekProgress(dates.count { it in from..today })
}

/** Days since the newest record before [today]; null if there's none. */
fun daysSinceLastRecord(dates: Collection<LocalDate>, today: LocalDate): Int? =
    dates.filter { it < today }.maxOrNull()?.let { ChronoUnit.DAYS.between(it, today).toInt() }

/** The line under 今日の体重を記録 after a gap — null when there's nothing to say. */
fun comebackMessage(gapDays: Int?): String? =
    if (gapDays != null && gapDays >= COMEBACK_GAP_DAYS) "${gapDays}日ぶりですね。今日の1回だけでOK" else null

data class ReminderCopy(val title: String, val text: String)

/** The reminder notification's wording, from the newest record and this week's count. */
fun reminderCopy(lastDate: LocalDate?, lastWeight: Double?, today: LocalDate, weekCount: Int): ReminderCopy {
    if (lastDate == null || lastWeight == null) {
        return ReminderCopy("今日の体重を記録しましょう", "ここに数字を入れるだけで記録できます")
    }
    val previous = "前回 ${lastWeight.toFixed1()}kg（${lastDate.monthValue}/${lastDate.dayOfMonth}）"
    val gap = ChronoUnit.DAYS.between(lastDate, today)
    val week = WeekProgress(weekCount)
    return when {
        gap >= COMEBACK_GAP_DAYS -> ReminderCopy("${gap}日ぶりでも大丈夫", "今日の1回だけでOK。$previous")
        week.achieved -> ReminderCopy("今週はもう${week.count}回記録しています", "測れたらでOK。$previous")
        else -> ReminderCopy("今日の体重を記録しましょう", "$previous・今週あと${week.remaining}回で目安の週${week.target}回")
    }
}

/** The notification shown once a weight typed into the reminder has been saved. */
fun recordedCopy(weight: Double, previousDate: LocalDate?, previousWeight: Double?, today: LocalDate): ReminderCopy {
    val title = "${weight.toFixed1()}kg を記録しました"
    if (previousDate == null || previousWeight == null) return ReminderCopy(title, "はじめての記録です。おつかれさまでした")
    val diff = "前回より ${dayDiffText(weight - previousWeight)}kg"
    val gap = ChronoUnit.DAYS.between(previousDate, today)
    return ReminderCopy(title, if (gap >= COMEBACK_GAP_DAYS) "${gap}日ぶりの記録です。おかえりなさい（$diff）" else diff)
}

/**
 * Reads what was typed into the notification: "77.5", "77,5", "７７．５",
 * "77.5kg"… Anything outside a plausible human weight is rejected.
 */
fun parseQuickWeight(input: CharSequence?): Double? {
    if (input == null) return null
    val s = Normalizer.normalize(input, Normalizer.Form.NFKC)
        .lowercase()
        .replace("kg", "")
        .replace('。', '.')
        .replace(',', '.')
        .replace('、', '.')
        .trim()
    if (!Regex("""\d{1,3}(\.\d+)?""").matches(s)) return null
    return round1(s.toDouble()).takeIf { it in 20.0..300.0 }
}

data class ReminderSuggestion(val time: LocalTime, val usualHour: Int)

private const val SUGGESTION_WINDOW_DAYS = 30L
private const val SUGGESTION_MIN_SAMPLES = 3

/**
 * Suggests moving the reminder to when the user really records: the median
 * time of same-day records in the last 30 days, rounded down to :00 or :30.
 * Null until there are 3 such records, or when the reminder is already within
 * an hour of it.
 */
fun suggestReminderTime(
    entries: Collection<Entry>,
    current: LocalTime,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): ReminderSuggestion? {
    val from = today.minusDays(SUGGESTION_WINDOW_DAYS)
    val minutes = entries
        .filter { it.date > from && it.date <= today }
        .mapNotNull { e ->
            val at = Instant.ofEpochMilli(e.recordedAt ?: return@mapNotNull null).atZone(zone).toLocalDateTime()
            // A day filled in later says nothing about when the user weighs in.
            if (at.toLocalDate() != e.date) null else at.hour * 60 + at.minute
        }
        .sorted()
    if (minutes.size < SUGGESTION_MIN_SAMPLES) return null
    val median = minutes[(minutes.size - 1) / 2]
    val suggested = LocalTime.of(median / 60, median % 60 / 30 * 30)
    val diff = abs(current.toSecondOfDay() - suggested.toSecondOfDay()) / 60
    if (minOf(diff, 24 * 60 - diff) < 60) return null
    return ReminderSuggestion(suggested, median / 60)
}

/** One key press on the weight keypad: at most 5 characters and one decimal point. */
fun keypadAppend(current: String, key: String): String = when {
    current.length >= 5 -> current
    key == "." && current.contains(".") -> current
    key == "." && current.isEmpty() -> "0."
    else -> current + key
}

/** What the home-screen widget says. */
data class WidgetState(
    val recordedToday: Boolean,
    val headline: String,
    val detail: String,
    val week: WeekProgress,
)

fun widgetState(
    todayWeight: Double?,
    previousDate: LocalDate?,
    previousWeight: Double?,
    today: LocalDate,
    weekCount: Int,
): WidgetState {
    val week = WeekProgress(weekCount)
    if (todayWeight != null) {
        val detail = if (previousWeight == null) "はじめての記録です" else "前回より ${dayDiffText(todayWeight - previousWeight)}kg"
        return WidgetState(true, "今日 ${todayWeight.toFixed1()}kg", detail, week)
    }
    if (previousDate == null || previousWeight == null) {
        return WidgetState(false, "まだ記録がありません", "体重を入れてみましょう", week)
    }
    val previous = "前回 ${previousWeight.toFixed1()}kg（${previousDate.monthValue}/${previousDate.dayOfMonth}）"
    val gap = ChronoUnit.DAYS.between(previousDate, today)
    val headline = if (gap >= COMEBACK_GAP_DAYS) "${gap}日ぶりでも大丈夫" else "今日はまだです"
    return WidgetState(false, headline, previous, week)
}

const val STAMP_LABEL_MAX = 8

/** Stamp names are joined with spaces in the CSV, so spaces and commas become "・". */
fun sanitizeStampLabel(raw: String): String? {
    val s = raw.trim().replace(Regex("""[\s,]+"""), "・").trim('・')
    return s.take(STAMP_LABEL_MAX).takeIf { it.isNotEmpty() }
}

/** The last visible character typed — so a new emoji typed into the box replaces the old one. */
fun lastGrapheme(raw: String): String? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    val it = BreakIterator.getCharacterInstance()
    it.setText(s)
    val end = it.last()
    val start = it.previous()
    return s.substring(if (start == BreakIterator.DONE) 0 else start, end)
}

/** The first visible character of what was typed in the emoji box — one emoji, even a multi-part one. */
fun firstGrapheme(raw: String): String? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    val it = BreakIterator.getCharacterInstance()
    it.setText(s)
    val end = it.next()
    return if (end == BreakIterator.DONE) s else s.substring(0, end)
}
