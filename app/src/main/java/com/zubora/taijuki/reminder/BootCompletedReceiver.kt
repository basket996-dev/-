package com.zubora.taijuki.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zubora.taijuki.ZuboraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Re-arms the reminder alarm after a reboot clears all pending AlarmManager entries. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = appContext as ZuboraApplication
                val settings = app.settingsRepository.settings.first()
                if (settings.reminderEnabled) {
                    AlarmScheduler.schedule(appContext, AlarmScheduler.parseReminderTime(settings.reminderTime))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
