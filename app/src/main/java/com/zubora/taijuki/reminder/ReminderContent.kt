package com.zubora.taijuki.reminder

import com.zubora.taijuki.ZuboraApplication
import com.zubora.taijuki.domain.ReminderCopy
import com.zubora.taijuki.domain.reminderCopy
import com.zubora.taijuki.domain.weekStart
import java.time.LocalDate

/** Builds the reminder's wording from what's in the database right now. */
object ReminderContent {
    suspend fun load(app: ZuboraApplication, today: LocalDate = LocalDate.now()): ReminderCopy {
        val dao = app.database.entryDao()
        val latest = dao.getLatest()
        val weekCount = dao.countBetween(weekStart(today).toString(), today.toString())
        return reminderCopy(latest?.date?.let(LocalDate::parse), latest?.weight, today, weekCount)
    }
}
