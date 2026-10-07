package com.zubora.taijuki.data

import com.zubora.taijuki.domain.ReminderCopy
import com.zubora.taijuki.domain.recordedCopy
import java.time.LocalDate

/** Saving just a weight for today, from outside the app: the reminder's reply box and the widget's keypad. */
object TodayRecorder {
    /** Keeps any stamps or memo already on today. Returns the "saved" wording, comparing with the previous record. */
    suspend fun save(dao: EntryDao, weight: Double, today: LocalDate = LocalDate.now()): ReminderCopy {
        val existing = dao.getByDate(today.toString())
        val previous = dao.getLatestBefore(today.toString())
        dao.upsert(
            (existing ?: EntryEntity(date = today.toString(), weight = weight, stamps = "", memo = "")).copy(
                weight = weight,
                recordedAt = existing?.recordedAt ?: System.currentTimeMillis(),
            ),
        )
        return recordedCopy(weight, previous?.date?.let(LocalDate::parse), previous?.weight, today)
    }
}
