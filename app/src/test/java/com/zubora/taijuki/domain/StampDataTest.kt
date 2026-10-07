package com.zubora.taijuki.domain

import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.data.StampSeed
import com.zubora.taijuki.data.toDomain
import com.zubora.taijuki.data.toEntity
import com.zubora.taijuki.ui.theme.StampType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StampDataTest {
    @Test
    fun startsWithTheStampsThatGetUsed() {
        val active = StampSeed.defaults.filter { it.active }.sortedBy { it.sortOrder }.map { it.label }
        assertEquals(listOf("食べすぎ", "便通", "外食・コンビニ"), active)
    }

    @Test
    fun keepsEveryOriginalStampSoOldDaysStillShowThem() {
        val ids = StampSeed.defaults.map { it.id }
        StampType.entries.forEach { assertTrue(it.id, it.id in ids) }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(StampSeed.defaults.size, StampSeed.defaults.map { it.sortOrder }.toSet().size)
    }

    @Test
    fun originalsDrawTheirIconAndNewOnesShowAnEmoji() {
        val stamps = StampSeed.defaults.map { it.toDomain() }
        assertEquals(StampType.Tabesugi, stamps.first { it.id == "tabesugi" }.builtin)
        val gaishoku = stamps.first { it.id == "gaishoku" }
        assertEquals(null, gaishoku.builtin)
        assertEquals("🍽️", gaishoku.emoji)
    }

    @Test
    fun entriesStoreStampIdsAndTheRecordTime() {
        val date = LocalDate.of(2026, 10, 6)
        val entry = Entry(date, 65.5, listOf(TestStamps.tabesugi, TestStamps.gaishoku), "", recordedAt = 123L)
        val row = entry.toEntity()
        assertEquals("tabesugi,gaishoku", row.stamps)
        assertEquals(123L, row.recordedAt)
        assertEquals(entry, row.toDomain(TestStamps.all.associateBy { it.id }))
    }

    @Test
    fun csvKeepsStampNamesIncludingOnesThisInstallDoesNotKnow() {
        val date = LocalDate.of(2026, 10, 6)
        val csv = buildCsv(listOf(Entry(date, 65.5, listOf(TestStamps.tabesugi, TestStamps.gaishoku), "ラーメン")))
        assertEquals("日付,体重(kg),スタンプ,メモ\n2026-10-06,65.5,食べすぎ 外食・コンビニ,ラーメン", csv)

        val rows = parseCsv(csv + "\n2026-10-07,65.3,ラーメン,")
        assertEquals(listOf("食べすぎ", "外食・コンビニ"), rows[0].stampLabels)
        assertEquals(listOf("ラーメン"), rows[1].stampLabels)
    }
}
