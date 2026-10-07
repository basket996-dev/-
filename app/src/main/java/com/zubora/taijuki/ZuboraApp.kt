package com.zubora.taijuki

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zubora.taijuki.reminder.AlarmScheduler
import com.zubora.taijuki.ui.components.BottomNavBar
import com.zubora.taijuki.ui.components.DayEditSheet
import com.zubora.taijuki.ui.components.NotificationBanner
import com.zubora.taijuki.ui.components.StampManagerDialog
import com.zubora.taijuki.ui.screens.CalendarScreen
import com.zubora.taijuki.ui.screens.GraphScreen
import com.zubora.taijuki.ui.screens.InputScreen
import com.zubora.taijuki.ui.screens.OnboardingScreen
import com.zubora.taijuki.ui.screens.SettingsScreen
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.ZuboraTheme

@Composable
fun ZuboraApp() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(application))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ZuboraTheme(accentColor = uiState.accent) {
        when {
            uiState.loadingInitial -> Box(modifier = Modifier.fillMaxSize().background(AppColors.Surface))
            !uiState.settings.onboardingDone -> OnboardingScreen(uiState, viewModel)
            else -> AppShell(uiState, viewModel)
        }
    }
}

@Composable
private fun AppShell(uiState: UiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way — the reminder alarm was already armed below */ }

    // The reminder toggle defaults to on, but nothing has requested notification
    // permission or armed the alarm until the user actually visits Settings.
    // Check once per app open so a user who never touches the toggle still gets
    // reminders instead of a switch that looks on but silently does nothing.
    LaunchedEffect(Unit) {
        if (uiState.settings.reminderEnabled) {
            val hasPermission = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            AlarmScheduler.schedule(context, AlarmScheduler.parseReminderTime(uiState.settings.reminderTime))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                ) {
                    when (uiState.activeTab) {
                        Tab.Input -> InputScreen(uiState, viewModel)
                        Tab.Calendar -> CalendarScreen(uiState, viewModel)
                        Tab.Graph -> GraphScreen(uiState, viewModel)
                        Tab.Settings -> SettingsScreen(uiState, viewModel)
                    }
                }
                NotificationBanner(
                    visible = uiState.notif.visible,
                    text = uiState.notif.text,
                    accent = uiState.accent,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(10.dp),
                )
            }
            BottomNavBar(activeTab = uiState.activeTab, accent = uiState.accent, onSelect = viewModel::setActiveTab)
        }
    }

    if (uiState.stampManagerOpen) {
        StampManagerDialog(stamps = uiState.stamps, accent = uiState.accent, viewModel = viewModel)
    }

    uiState.dayModal?.let { dayModal ->
        DayEditSheet(state = dayModal, stamps = uiState.stamps, accent = uiState.accent, viewModel = viewModel)
    }
}
