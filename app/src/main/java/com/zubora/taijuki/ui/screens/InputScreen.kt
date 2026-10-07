package com.zubora.taijuki.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.AppViewModel
import com.zubora.taijuki.UiState
import com.zubora.taijuki.domain.ReminderSuggestion
import com.zubora.taijuki.domain.WeekProgress
import com.zubora.taijuki.domain.comebackMessage
import com.zubora.taijuki.domain.daysSinceLastRecord
import com.zubora.taijuki.domain.formatDateJa
import com.zubora.taijuki.domain.suggestReminderTime
import com.zubora.taijuki.domain.toFixed1
import com.zubora.taijuki.domain.weekProgress
import com.zubora.taijuki.reminder.AlarmScheduler
import com.zubora.taijuki.ui.components.CheckGlyph
import com.zubora.taijuki.ui.components.MemoTextArea
import com.zubora.taijuki.ui.components.NumericKeypad
import com.zubora.taijuki.ui.components.PencilGlyph
import com.zubora.taijuki.ui.components.StampPickerGrid
import com.zubora.taijuki.ui.components.pickerStamps
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography
import com.zubora.taijuki.ui.theme.StampIcon
import com.zubora.taijuki.ui.theme.accentDark
import com.zubora.taijuki.ui.theme.accentFaint

@Composable
fun InputScreen(uiState: UiState, viewModel: AppViewModel) {
    if (uiState.showKeypadForToday) {
        KeypadContent(uiState, viewModel)
    } else {
        AlreadyLoggedContent(uiState, viewModel)
    }
}

@Composable
private fun KeypadContent(uiState: UiState, viewModel: AppViewModel) {
    val accent = uiState.accent
    val scale by animateFloatAsState(if (uiState.saving) 0.97f else 1f, label = "saveScale")
    val comeback = remember(uiState.entries, uiState.today) {
        if (uiState.todayEntry != null) null else comebackMessage(daysSinceLastRecord(uiState.entries.keys, uiState.today))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp)) {
            Text(
                formatDateJa(uiState.today),
                style = AppTypography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary),
            )
            Spacer(Modifier.height(2.dp))
            Text("今日の体重を記録", style = AppTypography.titleMedium.copy(fontSize = 19.sp, color = AppColors.TextPrimary))
            if (comeback != null) {
                Text(
                    comeback,
                    modifier = Modifier.padding(top = 4.dp),
                    style = AppTypography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = accentDark(accent)),
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 16.dp)
                .fillMaxWidth()
                .background(AppColors.Card, RoundedCornerShape(20.dp))
                .border(1.dp, AppColors.BorderCard, RoundedCornerShape(20.dp))
                .padding(vertical = 22.dp, horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (uiState.keypadValue.isEmpty()) "--" else uiState.keypadValue,
                    style = AppTypography.titleLarge.copy(fontSize = 50.sp, color = AppColors.TextPrimary),
                )
                Text(
                    "kg",
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                    style = AppTypography.labelMedium.copy(fontSize = 19.sp, color = AppColors.TextSecondary),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            StampPickerGrid(
                stamps = pickerStamps(uiState.stamps, uiState.selectedStamps),
                selected = uiState.selectedStamps,
                onToggle = viewModel::toggleStamp,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            if (!uiState.memoOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.clickable(onClick = viewModel::toggleMemo),
                    ) {
                        PencilGlyph(tint = AppColors.TextPlaceholder, modifier = Modifier.size(13.dp))
                        Text("メモを追加", style = AppTypography.bodySmall.copy(fontSize = 12.sp, color = AppColors.TextPlaceholder))
                    }
                    EditStampsLink(onClick = viewModel::openStampManager)
                }
            } else {
                MemoTextArea(
                    value = uiState.memoValue,
                    onValueChange = viewModel::setMemoValue,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }

            NumericKeypad(onDigit = viewModel::keypadPress, onBackspace = viewModel::keypadBackspace)

            Button(
                onClick = viewModel::saveToday,
                enabled = uiState.canSaveToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 20.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.saving) accentDark(accent) else accent,
                    contentColor = AppColors.Card,
                    disabledContainerColor = AppColors.BorderLight,
                    disabledContentColor = AppColors.TextPlaceholder,
                ),
            ) {
                Text(
                    if (uiState.saving) "保存中…" else "保存する",
                    style = AppTypography.titleMedium.copy(fontSize = 16.sp),
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun AlreadyLoggedContent(uiState: UiState, viewModel: AppViewModel) {
    val accent = uiState.accent
    val entry = uiState.todayEntry
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(60.dp).clip(CircleShape).background(accentFaint(accent)),
            contentAlignment = Alignment.Center,
        ) {
            CheckGlyph(color = accent, modifier = Modifier.size(30.dp), strokeWidthFraction = 0.1f)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "${formatDateJa(uiState.today)} · 記録済み",
            style = AppTypography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                entry?.weight?.toFixed1() ?: "",
                style = AppTypography.titleLarge.copy(fontSize = 46.sp, color = AppColors.TextPrimary),
            )
            Text(
                " kg",
                modifier = Modifier.padding(bottom = 6.dp),
                style = AppTypography.labelMedium.copy(fontSize = 17.sp, color = AppColors.TextSecondary),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            entry?.stamps?.forEach { stamp ->
                StampIcon(stamp, tint = stamp.color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = viewModel::editTodayAgain,
                shape = RoundedCornerShape(100),
                border = BorderStroke(1.dp, AppColors.BorderDashed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentDark(accent)),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
            ) {
                Text("編集する", style = AppTypography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            Button(
                onClick = viewModel::goToCalendarTab,
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = AppColors.Card),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
            ) {
                Text("カレンダーを見る", style = AppTypography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }

        val week = remember(uiState.entries, uiState.today) { weekProgress(uiState.entries.keys, uiState.today) }
        Spacer(Modifier.height(28.dp))
        WeekProgressCard(week, accent)

        val settings = uiState.settings
        val suggestion = remember(uiState.entries, settings.reminderTime, uiState.today) {
            suggestReminderTime(uiState.entries.values, AlarmScheduler.parseReminderTime(settings.reminderTime), uiState.today)
        }
        if (settings.reminderEnabled && suggestion != null && suggestion.label != settings.dismissedReminderSuggestion) {
            Spacer(Modifier.height(12.dp))
            ReminderSuggestionCard(
                suggestion = suggestion,
                accent = accent,
                onAccept = { viewModel.acceptReminderSuggestion(suggestion.time) },
                onDismiss = { viewModel.dismissReminderSuggestion(suggestion.time) },
            )
        }
    }
}

private val ReminderSuggestion.label: String get() = "%02d:%02d".format(time.hour, time.minute)

@Composable
private fun EditStampsLink(onClick: () -> Unit) {
    Text(
        "スタンプを編集",
        modifier = Modifier.clickable(onClick = onClick),
        style = AppTypography.bodySmall.copy(fontSize = 12.sp, color = AppColors.TextPlaceholder),
    )
}

/** This week against the 週3回 target — a target that can be met, unlike an unbroken streak. */
@Composable
private fun WeekProgressCard(week: WeekProgress, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.BorderCard, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("今週の記録", style = AppTypography.labelMedium.copy(fontSize = 12.sp, color = AppColors.TextSecondary))
            repeat(week.target) { i ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (i < week.count) accent else AppColors.BorderLight),
                )
            }
            Text(
                if (week.count <= week.target) "${week.count}/${week.target}回" else "${week.count}回",
                style = AppTypography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary),
            )
        }
        Text(
            if (week.achieved) "今週の目安（週${week.target}回）達成！あとは気が向いたらでOK" else "あと${week.remaining}回で今週の目安（週${week.target}回）",
            modifier = Modifier.padding(top = 6.dp),
            style = AppTypography.bodySmall.copy(fontSize = 12.sp, color = if (week.achieved) accentDark(accent) else AppColors.TextSecondary),
        )
    }
}

@Composable
private fun ReminderSuggestionCard(
    suggestion: ReminderSuggestion,
    accent: Color,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentFaint(accent), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            "いつも${suggestion.usualHour}時台に記録しています",
            style = AppTypography.bodySmall.copy(fontSize = 12.5.sp, color = AppColors.TextSecondary),
        )
        Text(
            "通知を ${suggestion.label} にしますか？",
            modifier = Modifier.padding(top = 2.dp),
            style = AppTypography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary),
        )
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = AppColors.Card),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("${suggestion.label} にする", style = AppTypography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold))
            }
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(100),
                border = BorderStroke(1.dp, AppColors.BorderDashed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("このまま", style = AppTypography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
