package com.zubora.taijuki.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.zubora.taijuki.AppViewModel
import com.zubora.taijuki.UiState
import com.zubora.taijuki.backup.DriveBackup
import com.zubora.taijuki.data.Entry
import com.zubora.taijuki.data.GraphPeriod
import com.zubora.taijuki.data.StampMode
import com.zubora.taijuki.domain.buildCsv
import com.zubora.taijuki.reminder.AlarmScheduler
import com.zubora.taijuki.ui.components.BoxNumberField
import com.zubora.taijuki.ui.components.CheckGlyph
import com.zubora.taijuki.ui.components.SegmentedControl
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography
import com.zubora.taijuki.ui.theme.StampIcon
import com.zubora.taijuki.ui.theme.accentDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(uiState: UiState, viewModel: AppViewModel) {
    val accent = uiState.accent
    val settings = uiState.settings
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setReminderEnabled(true)
    }
    val onReminderToggle: (Boolean) -> Unit = { enable ->
        when {
            !enable -> viewModel.setReminderEnabled(false)
            Build.VERSION.SDK_INT < 33 -> viewModel.setReminderEnabled(true)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ->
                viewModel.setReminderEnabled(true)
            else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "設定",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp),
            style = AppTypography.titleMedium.copy(fontSize = 19.sp, color = AppColors.TextPrimary),
        )

        SettingsCard {
            SectionTitle("グラフ表示")
            FieldLabel("期間")
            SegmentedControl(
                options = GraphPeriod.entries,
                selected = settings.graphPeriod,
                label = { it.label },
                onSelect = viewModel::setGraphPeriod,
                accent = accent,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            FieldLabel("行動スタンプの表示")
            SegmentedControl(
                options = StampMode.entries,
                selected = settings.graphStampMode,
                label = { it.label },
                onSelect = viewModel::setGraphStampMode,
                accent = accent,
            )
        }

        SettingsCard {
            SectionTitle("見た目", bottomPadding = 14.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AppColors.AccentOptions.forEachIndexed { index, color ->
                    val selected = settings.accentIndex == index
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (selected) 2.5.dp else 0.dp, AppColors.TextPrimary, CircleShape)
                            .clickable { viewModel.setAccentIndex(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) CheckGlyph(color = AppColors.Card, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        SettingsCard {
            SectionTitle("目標設定", bottomPadding = 14.dp)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    FieldLabel("身長 (cm)", bottomPadding = 5.dp)
                    BoxNumberField(
                        value = uiState.settingsHeightDraft,
                        onValueChange = viewModel::setSettingsHeightDraft,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column {
                    FieldLabel("目標体重 (kg)", bottomPadding = 5.dp)
                    BoxNumberField(
                        value = uiState.settingsTargetDraft,
                        onValueChange = viewModel::setSettingsTargetDraft,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Button(
                onClick = viewModel::saveSettingsGoal,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = AppColors.Card),
            ) {
                Text(
                    if (uiState.settingsSaved) "保存しました" else "保存する",
                    style = AppTypography.labelLarge.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                )
            }
        }

        SettingsCard {
            SectionTitle("スタンプ", bottomPadding = 10.dp)
            val active = uiState.stamps.filter { it.active }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (active.isEmpty()) {
                        Text("入力画面に出していません", style = AppTypography.bodySmall.copy(fontSize = 12.sp, color = AppColors.TextPlaceholder))
                    }
                    active.forEach { StampIcon(it, modifier = Modifier.size(20.dp)) }
                }
                OutlinedButton(
                    onClick = viewModel::openStampManager,
                    shape = RoundedCornerShape(100),
                    border = BorderStroke(1.dp, AppColors.BorderDashed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentDark(accent)),
                ) {
                    Text("編集する", style = AppTypography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold))
                }
            }
        }

        SettingsCard {
            SectionTitle("通知", bottomPadding = 14.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("リマインド通知", style = AppTypography.bodyMedium.copy(fontSize = 13.5.sp, color = AppColors.TextPrimary))
                NotebookSwitch(checked = settings.reminderEnabled, onCheckedChange = onReminderToggle, accent = accent)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("時刻", style = AppTypography.bodyMedium.copy(fontSize = 13.5.sp, color = AppColors.TextPrimary))
                ReminderTimeField(time = settings.reminderTime, onTimeChange = viewModel::setReminderTime)
            }
            OutlinedButton(
                onClick = viewModel::previewNotif,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppColors.BorderDashed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentDark(accent)),
            ) {
                Text("通知を送ってみる（そこから記録もできます）", style = AppTypography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold))
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .fillMaxWidth()
                .background(AppColors.Card, RoundedCornerShape(18.dp))
                .border(1.dp, AppColors.BorderCard, RoundedCornerShape(18.dp))
                .padding(18.dp),
        ) {
            SectionTitle("データ", bottomPadding = 12.dp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CsvExportRow(
                    entries = uiState.entries.values,
                    accent = accent,
                    onExported = { viewModel.showNotif("CSVを書き出しました") },
                )
                CsvImportRow(
                    accent = accent,
                    onImport = viewModel::importCsv,
                )
                DriveBackupRow(
                    entries = uiState.entries.values,
                    accent = accent,
                    onResult = { success ->
                        viewModel.showNotif(
                            if (success) "Googleドライブに保存しました" else "バックアップに失敗しました。設定をご確認ください",
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(vertical = 14.dp)
            .fillMaxWidth()
            .background(AppColors.Card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.BorderCard, RoundedCornerShape(18.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun SectionTitle(text: String, bottomPadding: Dp = 12.dp) {
    Text(
        text,
        modifier = Modifier.padding(bottom = bottomPadding),
        style = AppTypography.titleSmall.copy(fontSize = 13.sp, color = AppColors.TextPrimary),
    )
}

@Composable
private fun FieldLabel(text: String, bottomPadding: Dp = 6.dp) {
    Text(
        text,
        modifier = Modifier.padding(bottom = bottomPadding),
        style = AppTypography.labelSmall.copy(fontSize = 11.5.sp, color = AppColors.TextSecondary),
    )
}

@Composable
private fun CsvExportRow(entries: Collection<Entry>, accent: Color, onExported: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(buildCsv(entries).toByteArray()) }
            }
            onExported()
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("CSV出力", style = AppTypography.bodySmall.copy(fontSize = 13.sp, color = AppColors.TextPrimary))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .border(1.dp, accent, RoundedCornerShape(100))
                .clickable { launcher.launch("zubora_taijuki_${LocalDate.now()}.csv") }
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text("書き出す", style = AppTypography.labelSmall.copy(fontSize = 10.sp, color = accentDark(accent)))
        }
    }
}

@Composable
private fun CsvImportRow(accent: Color, onImport: (String) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (content != null) onImport(content)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("CSV読み込み", style = AppTypography.bodySmall.copy(fontSize = 13.sp, color = AppColors.TextPrimary))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .border(1.dp, accent, RoundedCornerShape(100))
                .clickable { launcher.launch("text/*") }
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text("読み込む", style = AppTypography.labelSmall.copy(fontSize = 10.sp, color = accentDark(accent)))
        }
    }
}

@Composable
private fun DriveBackupRow(entries: Collection<Entry>, accent: Color, onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun runUpload(account: GoogleSignInAccount) {
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching { DriveBackup.upload(context, account, buildCsv(entries)) }
                    .onFailure { Log.e("DriveBackup", "upload failed", it) }
                    .isSuccess
            }
            onResult(success)
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val account = try {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
        } catch (e: ApiException) {
            Log.e("DriveBackup", "sign-in failed, statusCode=${e.statusCode}", e)
            null
        }
        if (account != null) runUpload(account) else onResult(false)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Googleドライブへバックアップ", style = AppTypography.bodySmall.copy(fontSize = 13.sp, color = AppColors.TextPrimary))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .border(1.dp, accent, RoundedCornerShape(100))
                .clickable {
                    val existing = DriveBackup.lastSignedInAccount(context)
                    if (existing != null) runUpload(existing) else signInLauncher.launch(DriveBackup.signInClient(context).signInIntent)
                }
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text("バックアップする", style = AppTypography.labelSmall.copy(fontSize = 10.sp, color = accentDark(accent)))
        }
    }
}

@Composable
private fun NotebookSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, accent: Color) {
    val knobOffset by animateDpAsState(if (checked) 20.dp else 2.dp, label = "knobOffset")
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(100))
            .background(if (checked) accent else AppColors.BorderCard)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset, y = 2.dp)
                .size(22.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(AppColors.Card),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeField(time: String, onTimeChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .background(AppColors.InputBackground, RoundedCornerShape(10.dp))
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(10.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(time, style = AppTypography.bodyMedium.copy(fontSize = 13.5.sp, color = AppColors.TextPrimary))
    }

    if (showDialog) {
        val parsed = AlarmScheduler.parseReminderTime(time)
        val state = rememberTimePickerState(initialHour = parsed.hour, initialMinute = parsed.minute, is24Hour = true)
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = AppColors.Surface) {
                Column(modifier = Modifier.padding(20.dp)) {
                    TimePicker(state = state)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDialog = false }) {
                            Text("キャンセル", color = AppColors.TextSecondary)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = {
                            onTimeChange("%02d:%02d".format(state.hour, state.minute))
                            showDialog = false
                        }) {
                            Text("OK", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
