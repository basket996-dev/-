package com.zubora.taijuki

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zubora.taijuki.data.AppSettings
import com.zubora.taijuki.data.AvatarStore
import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.data.GraphPeriod
import com.zubora.taijuki.data.Stamp
import com.zubora.taijuki.data.StampMode
import com.zubora.taijuki.data.toDomain
import com.zubora.taijuki.data.toEntity
import com.zubora.taijuki.domain.COMEBACK_GAP_DAYS
import com.zubora.taijuki.domain.daysSinceLastRecord
import com.zubora.taijuki.domain.firstGrapheme
import com.zubora.taijuki.domain.keypadSeed
import com.zubora.taijuki.domain.parseCsv
import com.zubora.taijuki.domain.round1
import com.zubora.taijuki.domain.sanitizeStampLabel
import com.zubora.taijuki.reminder.AlarmScheduler
import com.zubora.taijuki.reminder.NotificationHelper
import com.zubora.taijuki.reminder.ReminderContent
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.CustomStampColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

enum class Tab(val label: String) {
    Input("入力"), Calendar("カレンダー"), Graph("グラフ"), Settings("設定")
}

data class DayModalState(
    val date: LocalDate,
    val weight: String,
    val stamps: List<Stamp>,
    val memo: String,
    val isExisting: Boolean,
)

data class NotifState(val visible: Boolean = false, val text: String = "")

data class UiState(
    val loadingInitial: Boolean = true,
    val activeTab: Tab = Tab.Input,
    val settings: AppSettings = AppSettings(),
    val entries: Map<LocalDate, Entry> = emptyMap(),
    /** Every stamp, active or put away, in the user's order. */
    val stamps: List<Stamp> = emptyList(),
    val stampManagerOpen: Boolean = false,
    val today: LocalDate = LocalDate.now(),

    val obHeight: String = "",
    val obWeight: String = "",
    val obTarget: String = "",
    val obError: Boolean = false,

    val keypadValue: String = "",
    val selectedStamps: List<Stamp> = emptyList(),
    val memoOpen: Boolean = false,
    val memoValue: String = "",
    val forceKeypadToday: Boolean = false,
    val saving: Boolean = false,

    val calendarMonth: YearMonth = YearMonth.now(),

    val dayModal: DayModalState? = null,

    val settingsHeightDraft: String = "",
    val settingsTargetDraft: String = "",
    val settingsSaved: Boolean = false,

    val notif: NotifState = NotifState(),

    val avatarBitmap: Bitmap? = null,
) {
    val accent: Color get() = AppColors.AccentOptions.getOrElse(settings.accentIndex) { AppColors.DefaultAccent }
    val canSaveToday: Boolean get() = keypadValue.toDoubleOrNull()?.let { it > 0 } == true
    val todayEntry: Entry? get() = entries[today]
    val showKeypadForToday: Boolean get() = todayEntry == null || forceKeypadToday
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ZuboraApplication
    private val entryDao = app.database.entryDao()
    private val stampDao = app.database.stampDao()
    private val settingsRepository = app.settingsRepository

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var draftsSeeded = false

    init {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { AvatarStore.load(app) }
            if (bitmap != null) _uiState.update { it.copy(avatarBitmap = bitmap) }
        }
        viewModelScope.launch {
            combine(entryDao.observeAll(), stampDao.observeAll(), settingsRepository.settings) { rows, stampRows, settings ->
                val stamps = stampRows.map { it.toDomain() }
                val stampsById = stamps.associateBy { it.id }
                Triple(rows.associate { LocalDate.parse(it.date) to it.toDomain(stampsById) }, stamps, settings)
            }.collect { (entries, stamps, settings) ->
                val seedDrafts = !draftsSeeded
                if (seedDrafts) draftsSeeded = true
                _uiState.update { s ->
                    s.copy(
                        loadingInitial = false,
                        entries = entries,
                        stamps = stamps,
                        settings = settings,
                        settingsHeightDraft = if (seedDrafts) keypadSeed(settings.heightCm) else s.settingsHeightDraft,
                        settingsTargetDraft = if (seedDrafts) keypadSeed(settings.targetWeight) else s.settingsTargetDraft,
                    )
                }
            }
        }
    }

    // ---- onboarding ----
    fun setObHeight(v: String) = _uiState.update { it.copy(obHeight = v, obError = false) }
    fun setObWeight(v: String) = _uiState.update { it.copy(obWeight = v, obError = false) }
    fun setObTarget(v: String) = _uiState.update { it.copy(obTarget = v, obError = false) }

    fun pickAvatar(uri: Uri) {
        viewModelScope.launch {
            val saved = AvatarStore.save(app, uri)
            if (saved) {
                settingsRepository.setHasAvatar(true)
                val bitmap = withContext(Dispatchers.IO) { AvatarStore.load(app) }
                _uiState.update { it.copy(avatarBitmap = bitmap) }
            }
        }
    }

    fun completeOnboarding() {
        val s = _uiState.value
        val h = s.obHeight.toDoubleOrNull()
        val w = s.obWeight.toDoubleOrNull()
        val t = s.obTarget.toDoubleOrNull()
        if (h == null || w == null || t == null) {
            _uiState.update { it.copy(obError = true) }
            return
        }
        viewModelScope.launch { settingsRepository.completeOnboarding(heightCm = h, targetWeight = t, startWeight = w) }
        _uiState.update {
            it.copy(obError = false, settingsHeightDraft = keypadSeed(h), settingsTargetDraft = keypadSeed(t))
        }
    }

    // ---- tabs ----
    fun setActiveTab(tab: Tab) = _uiState.update { it.copy(activeTab = tab) }
    fun goToCalendarTab() = setActiveTab(Tab.Calendar)

    // ---- input keypad ----
    fun keypadPress(ch: String) {
        _uiState.update { s ->
            val v = s.keypadValue
            when {
                v.length >= 5 -> s
                ch == "." && v.contains(".") -> s
                ch == "." && v.isEmpty() -> s.copy(keypadValue = "0.")
                else -> s.copy(keypadValue = v + ch)
            }
        }
    }

    fun keypadBackspace() = _uiState.update { it.copy(keypadValue = it.keypadValue.dropLast(1)) }

    fun toggleStamp(stamp: Stamp) {
        _uiState.update { s -> s.copy(selectedStamps = s.selectedStamps.toggled(stamp)) }
    }

    fun toggleMemo() = _uiState.update { it.copy(memoOpen = !it.memoOpen) }
    fun setMemoValue(v: String) = _uiState.update { it.copy(memoValue = v) }

    fun saveToday() {
        val s = _uiState.value
        val value = s.keypadValue.toDoubleOrNull()
        if (value == null || value <= 0) return
        _uiState.update { it.copy(saving = true) }
        val gap = daysSinceLastRecord(s.entries.keys, s.today)
        val recordedAt = s.todayEntry?.recordedAt ?: System.currentTimeMillis()
        viewModelScope.launch {
            delay(550)
            entryDao.upsert(Entry(s.today, round1(value), s.selectedStamps, s.memoValue, recordedAt).toEntity())
            NotificationHelper.cancelReminder(app)
            val message = if (s.todayEntry == null && gap != null && gap >= COMEBACK_GAP_DAYS) {
                "おかえりなさい！${gap}日ぶりの記録です"
            } else {
                "記録しました。よくできました"
            }
            _uiState.update {
                it.copy(saving = false, forceKeypadToday = false, notif = NotifState(true, message))
            }
            delay(2800)
            _uiState.update { it.copy(notif = NotifState()) }
        }
    }

    fun editTodayAgain() {
        val s = _uiState.value
        val e = s.todayEntry
        _uiState.update {
            it.copy(
                forceKeypadToday = true,
                keypadValue = e?.weight?.let(::keypadSeed) ?: "",
                selectedStamps = e?.stamps ?: emptyList(),
                memoValue = e?.memo ?: "",
                memoOpen = !(e?.memo.isNullOrBlank()),
            )
        }
    }

    // ---- calendar ----
    fun changeMonth(delta: Long) = _uiState.update { it.copy(calendarMonth = it.calendarMonth.plusMonths(delta)) }
    fun changeYear(delta: Long) = _uiState.update { it.copy(calendarMonth = it.calendarMonth.plusYears(delta)) }

    // ---- day modal ----
    fun openDayModal(date: LocalDate) {
        val e = _uiState.value.entries[date]
        _uiState.update {
            it.copy(
                dayModal = DayModalState(
                    date = date,
                    weight = e?.weight?.let(::keypadSeed) ?: "",
                    stamps = e?.stamps ?: emptyList(),
                    memo = e?.memo ?: "",
                    isExisting = e != null,
                ),
            )
        }
    }

    fun setDayModalWeight(v: String) = _uiState.update { it.copy(dayModal = it.dayModal?.copy(weight = v)) }
    fun setDayModalMemo(v: String) = _uiState.update { it.copy(dayModal = it.dayModal?.copy(memo = v)) }

    fun toggleDayModalStamp(stamp: Stamp) {
        _uiState.update { s ->
            val dm = s.dayModal ?: return@update s
            s.copy(dayModal = dm.copy(stamps = dm.stamps.toggled(stamp)))
        }
    }

    fun saveDayModal() {
        val s = _uiState.value
        val dm = s.dayModal ?: return
        val value = dm.weight.toDoubleOrNull() ?: return
        if (value <= 0) return
        // Only a same-day save says when the user weighs in; filling in a past day doesn't.
        val recordedAt = s.entries[dm.date]?.recordedAt ?: if (dm.date == s.today) System.currentTimeMillis() else null
        viewModelScope.launch {
            entryDao.upsert(Entry(dm.date, round1(value), dm.stamps, dm.memo, recordedAt).toEntity())
            _uiState.update { it.copy(dayModal = null) }
        }
    }

    fun deleteDayModal() {
        val dm = _uiState.value.dayModal ?: return
        viewModelScope.launch {
            entryDao.deleteByDate(dm.date.toString())
            _uiState.update { it.copy(dayModal = null) }
        }
    }

    fun closeDayModal() = _uiState.update { it.copy(dayModal = null) }

    // ---- settings ----
    fun setSettingsHeightDraft(v: String) = _uiState.update { it.copy(settingsHeightDraft = v) }
    fun setSettingsTargetDraft(v: String) = _uiState.update { it.copy(settingsTargetDraft = v) }

    fun saveSettingsGoal() {
        val s = _uiState.value
        val h = s.settingsHeightDraft.toDoubleOrNull() ?: s.settings.heightCm
        val t = s.settingsTargetDraft.toDoubleOrNull() ?: s.settings.targetWeight
        viewModelScope.launch {
            settingsRepository.updateGoal(h, t)
            _uiState.update { it.copy(settingsSaved = true) }
            delay(1800)
            _uiState.update { it.copy(settingsSaved = false) }
        }
    }

    fun setAccentIndex(index: Int) {
        viewModelScope.launch { settingsRepository.setAccentIndex(index) }
    }

    fun setGraphPeriod(period: GraphPeriod) {
        viewModelScope.launch { settingsRepository.setGraphPeriod(period) }
    }

    fun setGraphStampMode(mode: StampMode) {
        viewModelScope.launch { settingsRepository.setGraphStampMode(mode) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val time = _uiState.value.settings.reminderTime
            settingsRepository.setReminder(enabled, time)
            if (enabled) AlarmScheduler.schedule(app, AlarmScheduler.parseReminderTime(time))
            else AlarmScheduler.cancel(app)
        }
    }

    fun setReminderTime(time: String) {
        viewModelScope.launch {
            val enabled = _uiState.value.settings.reminderEnabled
            settingsRepository.setReminder(enabled, time)
            if (enabled) AlarmScheduler.schedule(app, AlarmScheduler.parseReminderTime(time))
        }
    }

    /** Sends the real reminder now, so the type-in-the-notification flow can be tried any time. */
    fun previewNotif() {
        val granted = ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            showNotif("通知が許可されていません")
            return
        }
        viewModelScope.launch {
            val copy = withContext(Dispatchers.IO) { ReminderContent.load(app, _uiState.value.today) }
            NotificationHelper.showReminder(app, copy)
            showNotif("通知を送りました。「記録する」から入力できます")
        }
    }

    fun acceptReminderSuggestion(time: LocalTime) {
        val value = "%02d:%02d".format(time.hour, time.minute)
        viewModelScope.launch {
            settingsRepository.setReminder(true, value)
            AlarmScheduler.schedule(app, time)
            showNotif("通知を $value にしました")
        }
    }

    fun dismissReminderSuggestion(time: LocalTime) {
        viewModelScope.launch { settingsRepository.setDismissedReminderSuggestion("%02d:%02d".format(time.hour, time.minute)) }
    }

    fun showNotif(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(notif = NotifState(true, text)) }
            delay(2800)
            _uiState.update { it.copy(notif = NotifState()) }
        }
    }

    fun importCsv(csv: String) {
        viewModelScope.launch {
            val rows = withContext(Dispatchers.Default) { parseCsv(csv) }
            withContext(Dispatchers.IO) {
                // Stamp names this install doesn't know (e.g. ones made on another phone) become new stamps.
                val stamps = stampDao.getAll().map { it.toDomain() }.toMutableList()
                val unknown = rows.flatMap { it.stampLabels }.distinct().filter { label -> stamps.none { it.label == label } }
                unknown.forEach { label -> stamps += newStamp(label, emoji = "🏷️", existing = stamps) }
                if (unknown.isNotEmpty()) stampDao.upsertAll(renumbered(stamps))
                val byLabel = stamps.associateBy { it.label }
                rows.forEach { row ->
                    // The CSV has no record time, so keep whatever this install already knows.
                    val existing = entryDao.getByDate(row.date.toString())
                    val entry = Entry(row.date, row.weight, row.stampLabels.mapNotNull(byLabel::get), row.memo, existing?.recordedAt)
                    entryDao.upsert(entry.toEntity())
                }
            }
            showNotif(
                if (rows.isEmpty()) "読み込めるデータがありませんでした" else "${rows.size}件のデータを読み込みました",
            )
        }
    }

    // ---- stamps ----
    fun openStampManager() = _uiState.update { it.copy(stampManagerOpen = true) }
    fun closeStampManager() = _uiState.update { it.copy(stampManagerOpen = false) }

    fun addStamp(rawLabel: String, rawEmoji: String) {
        val label = sanitizeStampLabel(rawLabel) ?: return
        val emoji = firstGrapheme(rawEmoji) ?: return
        val stamps = _uiState.value.stamps
        val sameName = stamps.firstOrNull { it.label == label }
        when {
            sameName == null -> saveStamps(stamps.withNewActive(newStamp(label, emoji, stamps)))
            sameName.active -> showNotif("「$label」はもうあります")
            else -> setStampActive(sameName.id, true)
        }
    }

    fun updateStamp(id: String, rawLabel: String, rawEmoji: String) {
        val label = sanitizeStampLabel(rawLabel) ?: return
        val stamps = _uiState.value.stamps
        if (stamps.any { it.id != id && it.label == label }) {
            showNotif("「$label」はもうあります")
            return
        }
        saveStamps(
            stamps.map { s ->
                if (s.id != id) s
                // The original six keep their drawn icon; only stamps the user made carry an emoji.
                else s.copy(label = label, emoji = if (s.builtin == null) firstGrapheme(rawEmoji) ?: s.emoji else null)
            },
        )
    }

    /** Putting a stamp away hides it from input; the days that used it keep showing it. */
    fun setStampActive(id: String, active: Boolean) {
        val stamps = _uiState.value.stamps
        val target = stamps.firstOrNull { it.id == id } ?: return
        val rest = stamps.filter { it.id != id }
        saveStamps(
            if (active) rest.withNewActive(target.copy(active = true))
            else rest.filter { it.active } + target.copy(active = false) + rest.filter { !it.active },
        )
    }

    fun moveStamp(id: String, delta: Int) {
        val active = _uiState.value.stamps.filter { it.active }.toMutableList()
        val from = active.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in active.indices) return
        active.add(to, active.removeAt(from))
        saveStamps(active + _uiState.value.stamps.filter { !it.active })
    }

    private fun saveStamps(ordered: List<Stamp>) {
        viewModelScope.launch { stampDao.upsertAll(renumbered(ordered)) }
    }

    private fun renumbered(ordered: List<Stamp>) = ordered.mapIndexed { i, s -> s.copy(sortOrder = i).toEntity() }

    private fun List<Stamp>.withNewActive(stamp: Stamp): List<Stamp> =
        filter { it.active } + stamp + filter { !it.active }

    private fun newStamp(label: String, emoji: String, existing: List<Stamp>): Stamp {
        val madeByUser = existing.count { it.builtin == null }
        return Stamp(
            id = "u${System.currentTimeMillis()}${existing.size}",
            label = label,
            emoji = emoji,
            color = CustomStampColors[madeByUser % CustomStampColors.size],
            active = true,
            sortOrder = existing.size,
        )
    }

    private fun List<Stamp>.toggled(stamp: Stamp): List<Stamp> =
        if (any { it.id == stamp.id }) filter { it.id != stamp.id } else this + stamp

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(application) as T
                }
            }
    }
}
