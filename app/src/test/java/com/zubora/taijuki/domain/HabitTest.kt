package com.zubora.taijuki.domain

import com.zubora.taijuki.data.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class HabitTest {
    private val today = LocalDate.of(2026, 10, 6) // a Tuesday
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test
    fun readsWeightsTypedIntoTheNotification() {
        assertEquals(65.5, parseQuickWeight("65.5")!!, 1e-9)
        assertEquals(65.5, parseQuickWeight("65,5")!!, 1e-9)
        assertEquals(65.5, parseQuickWeight("６５．５")!!, 1e-9)
        assertEquals(65.5, parseQuickWeight("65.5kg")!!, 1e-9)
        assertEquals(65.5, parseQuickWeight("65。5")!!, 1e-9)
        assertEquals(65.0, parseQuickWeight(" 65 ")!!, 1e-9)
        assertEquals(65.5, parseQuickWeight("65.46")!!, 1e-9)
    }

    @Test
    fun rejectsWhatIsNotAWeight() {
        listOf(null, "", "abc", "65..5", "7.75", "775", "-65").forEach { assertNull(it, parseQuickWeight(it)) }
    }

    @Test
    fun weeksStartOnSundayLikeTheCalendar() {
        assertEquals(LocalDate.of(2026, 10, 4), weekStart(today))
        assertEquals(LocalDate.of(2026, 10, 4), weekStart(LocalDate.of(2026, 10, 4)))
        assertEquals(LocalDate.of(2026, 10, 4), weekStart(LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun countsThisWeekAgainstTheTarget() {
        val dates = listOf(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 6))
        val week = weekProgress(dates, today)
        assertEquals(2, week.count) // 10/3 was last week
        assertEquals(1, week.remaining)
        assertFalse(week.achieved)
        assertTrue(weekProgress(dates + LocalDate.of(2026, 10, 5), today).achieved)
    }

    @Test
    fun gapIsMeasuredToTheRecordBeforeToday() {
        val dates = listOf(LocalDate.of(2026, 10, 2), today)
        assertEquals(4, daysSinceLastRecord(dates, today))
        assertNull(daysSinceLastRecord(listOf(today), today))
        assertNull(comebackMessage(2))
        assertEquals("3日ぶりですね。今日の1回だけでOK", comebackMessage(3))
    }

    @Test
    fun reminderWordingDependsOnTheGapAndTheWeek() {
        assertEquals("今日の体重を記録しましょう", reminderCopy(null, null, today, 0).title)

        val comeback = reminderCopy(LocalDate.of(2026, 10, 2), 65.7, today, 0)
        assertEquals("4日ぶりでも大丈夫", comeback.title)
        assertEquals("今日の1回だけでOK。前回 65.7kg（10/2）", comeback.text)

        val done = reminderCopy(LocalDate.of(2026, 10, 5), 65.7, today, 3)
        assertEquals("今週はもう3回記録しています", done.title)

        val normal = reminderCopy(LocalDate.of(2026, 10, 5), 65.7, today, 1)
        assertEquals("今日の体重を記録しましょう", normal.title)
        assertEquals("前回 65.7kg（10/5）・今週あと2回で目安の週3回", normal.text)
    }

    @Test
    fun confirmationComparesWithThePreviousRecord() {
        assertEquals("はじめての記録です。おつかれさまでした", recordedCopy(65.5, null, null, today).text)
        val next = recordedCopy(65.5, LocalDate.of(2026, 10, 5), 65.7, today)
        assertEquals("65.5kg を記録しました", next.title)
        assertEquals("前回より -0.2kg", next.text)
        assertEquals("5日ぶりの記録です。おかえりなさい（前回より +0.3kg）", recordedCopy(66.0, LocalDate.of(2026, 10, 1), 65.7, today).text)
    }

    private fun recordedAt(date: LocalDate, time: String) =
        Entry(date, 65.7, emptyList(), "", LocalDateTime.of(date, LocalTime.parse(time)).atZone(tokyo).toInstant().toEpochMilli())

    @Test
    fun suggestsTheUsualRecordingTimeRoundedDownToTheHalfHour() {
        val entries = listOf(
            recordedAt(LocalDate.of(2026, 10, 5), "11:49"),
            recordedAt(LocalDate.of(2026, 10, 3), "11:10"),
            recordedAt(LocalDate.of(2026, 9, 28), "12:20"),
        )
        val suggestion = suggestReminderTime(entries, LocalTime.of(21, 0), today, tokyo)!!
        assertEquals(LocalTime.of(11, 30), suggestion.time)
        assertEquals(11, suggestion.usualHour)
    }

    @Test
    fun noSuggestionWithoutEnoughSameDayRecords() {
        val twoOnly = listOf(recordedAt(LocalDate.of(2026, 10, 5), "11:49"), recordedAt(LocalDate.of(2026, 10, 3), "11:10"))
        assertNull(suggestReminderTime(twoOnly, LocalTime.of(21, 0), today, tokyo))

        // Filled in the next day: says nothing about when the user weighs in.
        val backfilled = Entry(
            LocalDate.of(2026, 10, 4), 65.7, emptyList(), "",
            LocalDateTime.of(2026, 10, 5, 11, 0).atZone(tokyo).toInstant().toEpochMilli(),
        )
        assertNull(suggestReminderTime(twoOnly + backfilled, LocalTime.of(21, 0), today, tokyo))

        // Older than 30 days doesn't count either.
        val old = recordedAt(LocalDate.of(2026, 8, 20), "11:00")
        assertNull(suggestReminderTime(twoOnly + old, LocalTime.of(21, 0), today, tokyo))
    }

    @Test
    fun noSuggestionWhenTheReminderIsAlreadyClose() {
        val entries = listOf(
            recordedAt(LocalDate.of(2026, 10, 5), "11:49"),
            recordedAt(LocalDate.of(2026, 10, 3), "11:10"),
            recordedAt(LocalDate.of(2026, 9, 28), "12:20"),
        )
        assertNull(suggestReminderTime(entries, LocalTime.of(11, 0), today, tokyo))
        assertTrue(suggestReminderTime(entries, LocalTime.of(10, 0), today, tokyo) != null)
    }

    @Test
    fun keypadKeepsAWeightShapedValue() {
        assertEquals("7", keypadAppend("", "7"))
        assertEquals("0.", keypadAppend("", "."))
        assertEquals("65.", keypadAppend("65.", "."))
        assertEquals("65.45", keypadAppend("65.4", "5"))
        assertEquals("65.45", keypadAppend("65.45", "6"))
    }

    @Test
    fun widgetSaysWhetherTodayIsDone() {
        val recorded = widgetState(65.4, LocalDate.of(2026, 10, 5), 65.7, today, 2)
        assertTrue(recorded.recordedToday)
        assertEquals("今日 65.4kg", recorded.headline)
        assertEquals("前回より -0.3kg", recorded.detail)

        val pending = widgetState(null, LocalDate.of(2026, 10, 5), 65.7, today, 1)
        assertFalse(pending.recordedToday)
        assertEquals("今日はまだです", pending.headline)
        assertEquals("前回 65.7kg（10/5）", pending.detail)
        assertEquals(1, pending.week.count)

        assertEquals("4日ぶりでも大丈夫", widgetState(null, LocalDate.of(2026, 10, 2), 65.7, today, 0).headline)
        assertEquals("まだ記録がありません", widgetState(null, null, null, today, 0).headline)
    }

    @Test
    fun stampNamesStayCsvSafe() {
        assertEquals("ラーメン", sanitizeStampLabel(" ラーメン "))
        assertEquals("外食・コンビニ", sanitizeStampLabel("外食 コンビニ"))
        assertEquals("麺・パン", sanitizeStampLabel("麺,パン"))
        assertNull(sanitizeStampLabel("   "))
        assertEquals(STAMP_LABEL_MAX, sanitizeStampLabel("あいうえおかきくけこ")!!.length)
    }

    @Test
    fun emojiBoxKeepsOneWholeEmoji() {
        assertEquals("🍜", firstGrapheme("🍜🍤"))
        assertEquals("🍤", lastGrapheme("🍜🍤"))
        assertEquals("🍽️", firstGrapheme("🍽️"))
        assertNull(firstGrapheme("  "))
        assertNull(lastGrapheme(""))
    }
}
