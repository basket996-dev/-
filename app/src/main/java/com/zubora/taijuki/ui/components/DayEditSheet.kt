package com.zubora.taijuki.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.AppViewModel
import com.zubora.taijuki.DayModalState
import com.zubora.taijuki.domain.formatDateJa
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditSheet(state: DayModalState, accent: Color, viewModel: AppViewModel) {
    ModalBottomSheet(
        onDismissRequest = viewModel::closeDayModal,
        containerColor = AppColors.Surface,
        scrimColor = AppColors.ModalScrim,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 26.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.BorderDashed),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                formatDateJa(state.date),
                style = AppTypography.titleSmall.copy(fontSize = 14.sp, color = AppColors.TextPrimary),
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                DashedNumberField(
                    value = state.weight,
                    onValueChange = viewModel::setDayModalWeight,
                    placeholder = "--",
                    fontSize = 34.sp,
                    modifier = Modifier.width(140.dp),
                )
                Text(
                    "kg",
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                    style = AppTypography.labelMedium.copy(fontSize = 16.sp, color = AppColors.TextSecondary),
                )
            }
            Spacer(Modifier.height(16.dp))

            StampPickerGrid(
                selected = state.stamps,
                onToggle = viewModel::toggleDayModalStamp,
                iconSize = 17.dp,
            )
            Spacer(Modifier.height(14.dp))

            MemoTextArea(
                value = state.memo,
                onValueChange = viewModel::setDayModalMemo,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (state.isExisting) {
                    TextButton(onClick = viewModel::deleteDayModal) {
                        Text(
                            "削除",
                            style = AppTypography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.Negative),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = viewModel::closeDayModal,
                    border = BorderStroke(1.dp, AppColors.BorderDashed),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary),
                ) {
                    Text("キャンセル", style = AppTypography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = viewModel::saveDayModal,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = AppColors.Card),
                ) {
                    Text("保存", style = AppTypography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
