package com.zubora.taijuki

import android.app.Application
import com.zubora.taijuki.data.AppDatabase
import com.zubora.taijuki.data.SettingsRepository
import com.zubora.taijuki.reminder.NotificationHelper

class ZuboraApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
    }
}
