package com.zubora.taijuki.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zubora.taijuki.ZuboraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Fires once at the configured reminder time; notifies only if today is still unlogged. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = appContext as ZuboraApplication
                val settings = app.settingsRepository.settings.first()
                if (settings.reminderEnabled) {
                    val todayEntry = app.database.entryDao().getByDate(LocalDate.now().toString())
                    if (todayEntry == null) {
                        NotificationHelper.showReminder(appContext)
                    }
                    // Exact daily alarms aren't repeating, so re-arm for tomorrow.
                    AlarmScheduler.schedule(appContext, AlarmScheduler.parseReminderTime(settings.reminderTime))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
