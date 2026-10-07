package com.zubora.taijuki.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.zubora.taijuki.ZuboraApplication
import com.zubora.taijuki.data.EntryEntity
import com.zubora.taijuki.domain.parseQuickWeight
import com.zubora.taijuki.domain.recordedCopy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Saves the weight typed into the reminder's 記録する box as today's record. */
class QuickRecordReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val typed = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(NotificationHelper.KEY_QUICK_WEIGHT)
        val app = context.applicationContext as ZuboraApplication
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val weight = parseQuickWeight(typed)
                if (weight == null) {
                    // Re-post with the reply box still there, so a typo is one more try away.
                    NotificationHelper.showReminder(app, ReminderContent.load(app, today), error = "数字で入れてください（例 77.5）")
                    return@launch
                }
                val dao = app.database.entryDao()
                val existing = dao.getByDate(today.toString())
                val previous = dao.getLatestBefore(today.toString())
                // Keep any stamps or memo already on today; only the weight comes from the notification.
                dao.upsert(
                    (existing ?: EntryEntity(date = today.toString(), weight = weight, stamps = "", memo = "")).copy(
                        weight = weight,
                        recordedAt = existing?.recordedAt ?: System.currentTimeMillis(),
                    ),
                )
                val copy = recordedCopy(weight, previous?.date?.let(LocalDate::parse), previous?.weight, today)
                NotificationHelper.showRecorded(app, copy)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
