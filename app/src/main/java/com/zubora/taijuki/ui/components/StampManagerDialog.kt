package com.zubora.taijuki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zubora.taijuki.AppViewModel
import com.zubora.taijuki.data.Stamp
import com.zubora.taijuki.domain.STAMP_LABEL_MAX
import com.zubora.taijuki.domain.firstGrapheme
import com.zubora.taijuki.domain.lastGrapheme
import com.zubora.taijuki.domain.sanitizeStampLabel
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography
import com.zubora.taijuki.ui.theme.StampIcon
import com.zubora.taijuki.ui.theme.accentDark

/** A starting point for the emoji box; any emoji typed from the keyboard works too. */
private val EMOJI_IDEAS = listOf("🍜", "🍤", "🍙", "🍞", "🍰", "🍫", "🍺", "🏪", "🍽️", "🥗", "🏃", "💤")

/**
 * Make, rename, reorder and put away stamps. Put-away stamps leave the input
 * screen but keep showing on the days that used them, so nothing is lost.
 */
@Composable
fun StampManagerDialog(stamps: List<Stamp>, accent: Color, viewModel: AppViewModel) {
    val active = stamps.filter { it.active }
    val putAway = stamps.filter { !it.active }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = viewModel::closeStampManager,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppColors.Surface,
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 680.dp),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "スタンプ",
                        modifier = Modifier.weight(1f),
                        style = AppTypography.titleMedium.copy(fontSize = 17.sp, color = AppColors.TextPrimary),
                    )
                    SmallAction("閉じる", AppColors.TextSecondary, onClick = viewModel::closeStampManager)
                }

                SectionLabel("入力画面に出すもの")
                if (active.isEmpty()) {
                    Hint("ありません。下で作るか、しまったものを戻してください")
                }
                active.forEachIndexed { i, stamp ->
                    if (editingId == stamp.id) {
                        StampEditor(
                            initialLabel = stamp.label,
                            initialEmoji = stamp.emoji.orEmpty(),
                            emojiEditable = stamp.builtin == null,
                            accent = accent,
                            confirmLabel = "保存",
                            onConfirm = { label, emoji ->
                                viewModel.updateStamp(stamp.id, label, emoji)
                                editingId = null
                            },
                            onCancel = { editingId = null },
                        )
                    } else {
                        StampRow(stamp) {
                            SmallAction("↑", AppColors.TextSecondary, enabled = i > 0) { viewModel.moveStamp(stamp.id, -1) }
                            SmallAction("↓", AppColors.TextSecondary, enabled = i < active.lastIndex) { viewModel.moveStamp(stamp.id, 1) }
                            SmallAction("編集", accentDark(accent)) { editingId = stamp.id }
                            SmallAction("しまう", AppColors.TextSecondary) { viewModel.setStampActive(stamp.id, false) }
                        }
                    }
                }

                SectionLabel("新しく作る")
                StampEditor(
                    initialLabel = "",
                    initialEmoji = "",
                    emojiEditable = true,
                    accent = accent,
                    confirmLabel = "追加する",
                    onConfirm = viewModel::addStamp,
                    onCancel = null,
                )

                if (putAway.isNotEmpty()) {
                    SectionLabel("しまってあるもの")
                    Hint("入力画面には出ませんが、押した日のカレンダーやグラフには表示されます")
                    putAway.forEach { stamp ->
                        StampRow(stamp) {
                            SmallAction("戻す", accentDark(accent)) { viewModel.setStampActive(stamp.id, true) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
        style = AppTypography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        modifier = Modifier.padding(bottom = 6.dp),
        style = AppTypography.bodySmall.copy(fontSize = 11.sp, color = AppColors.TextPlaceholder),
    )
}

@Composable
private fun StampRow(stamp: Stamp, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .background(AppColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.BorderCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StampIcon(stamp, modifier = Modifier.size(22.dp))
        Text(
            stamp.label,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            maxLines = 1,
            style = AppTypography.bodyMedium.copy(fontSize = 13.5.sp, color = AppColors.TextPrimary),
        )
        actions()
    }
}

@Composable
private fun SmallAction(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp),
        style = AppTypography.labelLarge.copy(
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) color else AppColors.BorderLight,
        ),
    )
}

/** Name + emoji boxes. The original six keep their drawn icon, so only their name can change. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StampEditor(
    initialLabel: String,
    initialEmoji: String,
    emojiEditable: Boolean,
    accent: Color,
    confirmLabel: String,
    onConfirm: (label: String, emoji: String) -> Unit,
    onCancel: (() -> Unit)?,
) {
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }
    var emoji by remember(initialEmoji) { mutableStateOf(initialEmoji) }
    val canConfirm = sanitizeStampLabel(label) != null && (!emojiEditable || firstGrapheme(emoji) != null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .background(AppColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, accent, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (emojiEditable) {
                BoxTextField(
                    value = emoji,
                    onValueChange = { emoji = lastGrapheme(it).orEmpty() },
                    placeholder = "絵文字",
                    modifier = Modifier.width(76.dp),
                )
            }
            BoxTextField(
                value = label,
                onValueChange = { label = it },
                placeholder = "名前（${STAMP_LABEL_MAX}文字まで）",
                modifier = Modifier.weight(1f),
            )
        }
        if (emojiEditable) {
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                EMOJI_IDEAS.forEach { idea ->
                    Text(
                        idea,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (idea == emoji) AppColors.BorderLight else Color.Transparent)
                            .clickable { emoji = idea }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        style = AppTypography.bodyMedium.copy(fontSize = 20.sp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onCancel != null) SmallAction("やめる", AppColors.TextSecondary, onClick = onCancel)
            Button(
                onClick = {
                    onConfirm(label, emoji)
                    if (onCancel == null) {
                        label = ""
                        emoji = ""
                    }
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = AppColors.Card,
                    disabledContainerColor = AppColors.BorderLight,
                    disabledContentColor = AppColors.TextPlaceholder,
                ),
            ) {
                Text(confirmLabel, style = AppTypography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
