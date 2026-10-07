package com.zubora.taijuki.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.zubora.taijuki.ZuboraApplication
import com.zubora.taijuki.data.TodayRecorder
import com.zubora.taijuki.domain.parseQuickWeight
import com.zubora.taijuki.widget.WeightWidget
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
                val copy = TodayRecorder.save(app.database.entryDao(), weight, today)
                NotificationHelper.showRecorded(app, copy)
                WeightWidget.refresh(app)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
