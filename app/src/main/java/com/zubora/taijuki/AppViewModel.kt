package com.zubora.taijuki

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zubora.taijuki.data.AppSettings
import com.zubora.taijuki.data.AvatarStore
import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.data.GraphPeriod
import com.zubora.taijuki.data.StampMode
import com.zubora.taijuki.data.toDomain
import com.zubora.taijuki.data.toEntity
import com.zubora.taijuki.domain.keypadSeed
import com.zubora.taijuki.domain.parseCsv
import com.zubora.taijuki.domain.round1
import com.zubora.taijuki.reminder.AlarmScheduler
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.StampType
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
import java.time.YearMonth

enum class Tab(val label: String) {
    Input("入力"), Calendar("カレンダー"), Graph("グラフ"), Settings("設定")
}

data class DayModalState(
    val date: LocalDate,
    val weight: String,
    val stamps: List<StampType>,
    val memo: String,
    val isExisting: Boolean,
)

data class NotifState(val visible: Boolean = false, val text: String = "")

data class UiState(
    val loadingInitial: Boolean = true,
    val activeTab: Tab = Tab.Input,
    val settings: AppSettings = AppSettings(),
    val entries: Map<LocalDate, Entry> = emptyMap(),
    val today: LocalDate = LocalDate.now(),

    val obHeight: String = "",
    val obWeight: String = "",
    val obTarget: String = "",
    val obError: Boolean = false,

    val keypadValue: String = "",
    val selectedStamps: List<StampType> = emptyList(),
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
            combine(entryDao.observeAll(), settingsRepository.settings) { rows, settings ->
                rows.associate { LocalDate.parse(it.date) to it.toDomain() } to settings
            }.collect { (entries, settings) ->
                val seedDrafts = !draftsSeeded
                if (seedDrafts) draftsSeeded = true
                _uiState.update { s ->
                    s.copy(
                        loadingInitial = false,
                        entries = entries,
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

    fun toggleStamp(stamp: StampType) {
        _uiState.update { s ->
            val has = stamp in s.selectedStamps
            s.copy(selectedStamps = if (has) s.selectedStamps - stamp else s.selectedStamps + stamp)
        }
    }

    fun toggleMemo() = _uiState.update { it.copy(memoOpen = !it.memoOpen) }
    fun setMemoValue(v: String) = _uiState.update { it.copy(memoValue = v) }

    fun saveToday() {
        val s = _uiState.value
        val value = s.keypadValue.toDoubleOrNull()
        if (value == null || value <= 0) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            delay(550)
            entryDao.upsert(Entry(s.today, round1(value), s.selectedStamps, s.memoValue).toEntity())
            _uiState.update {
                it.copy(saving = false, forceKeypadToday = false, notif = NotifState(true, "記録しました。よくできました"))
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

    fun toggleDayModalStamp(stamp: StampType) {
        _uiState.update { s ->
            val dm = s.dayModal ?: return@update s
            val has = stamp in dm.stamps
            s.copy(dayModal = dm.copy(stamps = if (has) dm.stamps - stamp else dm.stamps + stamp))
        }
    }

    fun saveDayModal() {
        val dm = _uiState.value.dayModal ?: return
        val value = dm.weight.toDoubleOrNull() ?: return
        if (value <= 0) return
        viewModelScope.launch {
            entryDao.upsert(Entry(dm.date, round1(value), dm.stamps, dm.memo).toEntity())
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

    fun previewNotif() {
        viewModelScope.launch {
            _uiState.update { it.copy(notif = NotifState(true, "カレンダーの今日のマスが空いてますよ")) }
            delay(3200)
            _uiState.update { it.copy(notif = NotifState()) }
        }
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
            val entries = withContext(Dispatchers.Default) { parseCsv(csv) }
            withContext(Dispatchers.IO) { entries.forEach { entryDao.upsert(it.toEntity()) } }
            showNotif(
                if (entries.isEmpty()) "読み込めるデータがありませんでした" else "${entries.size}件のデータを読み込みました",
            )
        }
    }

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
