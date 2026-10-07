package com.zubora.taijuki.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class GraphPeriod(val id: String, val label: String, val days: Int?) {
    Week("week", "1週間", 7),
    Month("month", "1ヶ月", 30),
    ThreeMonth("3month", "3ヶ月", 90),
    All("all", "全期間", null);

    companion object {
        fun fromId(id: String?): GraphPeriod = entries.firstOrNull { it.id == id } ?: Month
    }
}

enum class StampMode(val id: String, val label: String) {
    None("none", "非表示"),
    All("all", "すべて");

    companion object {
        // The old "key" (主要3種) mode was tied to the fixed six stamps; it now shows everything.
        fun fromId(id: String?): StampMode = entries.firstOrNull { it.id == id } ?: All
    }
}

data class AppSettings(
    val onboardingDone: Boolean = false,
    val heightCm: Double = 165.0,
    val targetWeight: Double = 60.0,
    val startWeight: Double = 60.0,
    val accentIndex: Int = 0,
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "21:00",
    val graphPeriod: GraphPeriod = GraphPeriod.Month,
    val graphStampMode: StampMode = StampMode.All,
    val hasAvatar: Boolean = false,
    /** A reminder-time suggestion ("HH:mm") the user said no to, so it isn't offered again. */
    val dismissedReminderSuggestion: String? = null,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val HEIGHT_CM = floatPreferencesKey("height_cm")
        val TARGET_WEIGHT = floatPreferencesKey("target_weight")
        val START_WEIGHT = floatPreferencesKey("start_weight")
        val ACCENT_INDEX = intPreferencesKey("accent_index")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val GRAPH_PERIOD = stringPreferencesKey("graph_period")
        val GRAPH_STAMP_MODE = stringPreferencesKey("graph_stamp_mode")
        val HAS_AVATAR = booleanPreferencesKey("has_avatar")
        val DISMISSED_REMINDER_SUGGESTION = stringPreferencesKey("dismissed_reminder_suggestion")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
            heightCm = (p[Keys.HEIGHT_CM] ?: 165f).toDouble(),
            targetWeight = (p[Keys.TARGET_WEIGHT] ?: 60f).toDouble(),
            startWeight = (p[Keys.START_WEIGHT] ?: 60f).toDouble(),
            accentIndex = p[Keys.ACCENT_INDEX] ?: 0,
            reminderEnabled = p[Keys.REMINDER_ENABLED] ?: true,
            reminderTime = p[Keys.REMINDER_TIME] ?: "21:00",
            graphPeriod = GraphPeriod.fromId(p[Keys.GRAPH_PERIOD]),
            graphStampMode = StampMode.fromId(p[Keys.GRAPH_STAMP_MODE]),
            hasAvatar = p[Keys.HAS_AVATAR] ?: false,
            dismissedReminderSuggestion = p[Keys.DISMISSED_REMINDER_SUGGESTION],
        )
    }

    suspend fun completeOnboarding(heightCm: Double, targetWeight: Double, startWeight: Double) {
        context.dataStore.edit { p ->
            p[Keys.ONBOARDING_DONE] = true
            p[Keys.HEIGHT_CM] = heightCm.toFloat()
            p[Keys.TARGET_WEIGHT] = targetWeight.toFloat()
            p[Keys.START_WEIGHT] = startWeight.toFloat()
        }
    }

    suspend fun updateGoal(heightCm: Double, targetWeight: Double) {
        context.dataStore.edit { p ->
            p[Keys.HEIGHT_CM] = heightCm.toFloat()
            p[Keys.TARGET_WEIGHT] = targetWeight.toFloat()
        }
    }

    suspend fun setAccentIndex(index: Int) {
        context.dataStore.edit { p -> p[Keys.ACCENT_INDEX] = index }
    }

    suspend fun setReminder(enabled: Boolean, time: String) {
        context.dataStore.edit { p ->
            p[Keys.REMINDER_ENABLED] = enabled
            p[Keys.REMINDER_TIME] = time
        }
    }

    suspend fun setGraphPeriod(period: GraphPeriod) {
        context.dataStore.edit { p -> p[Keys.GRAPH_PERIOD] = period.id }
    }

    suspend fun setGraphStampMode(mode: StampMode) {
        context.dataStore.edit { p -> p[Keys.GRAPH_STAMP_MODE] = mode.id }
    }

    suspend fun setHasAvatar(has: Boolean) {
        context.dataStore.edit { p -> p[Keys.HAS_AVATAR] = has }
    }

    suspend fun setDismissedReminderSuggestion(time: String) {
        context.dataStore.edit { p -> p[Keys.DISMISSED_REMINDER_SUGGESTION] = time }
    }
}
