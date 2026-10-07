package com.zubora.taijuki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography

/** The 3×4 weight keypad, shared by the input tab and the widget's quick-record sheet. */
@Composable
fun NumericKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
    Column {
        keys.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                row.forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.Card)
                            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(14.dp))
                            .clickable { if (label == "⌫") onBackspace() else onDigit(label) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = AppTypography.titleMedium.copy(fontSize = 19.sp, color = AppColors.TextPrimary))
                    }
                }
            }
        }
    }
}
