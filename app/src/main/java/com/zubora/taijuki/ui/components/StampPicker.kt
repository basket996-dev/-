package com.zubora.taijuki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography
import com.zubora.taijuki.ui.theme.StampIcon
import com.zubora.taijuki.ui.theme.StampType

@Composable
fun StampButton(
    stamp: StampType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    labelFontSize: TextUnit = 9.5.sp,
) {
    val bg = if (selected) stamp.color else AppColors.Card
    val fg = if (selected) AppColors.Card else stamp.color
    val border = if (selected) stamp.color else AppColors.BorderCard
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
    ) {
        StampIcon(stamp, tint = fg, modifier = Modifier.size(iconSize))
        Text(
            stamp.label,
            style = AppTypography.labelSmall.copy(fontSize = labelFontSize, fontWeight = FontWeight.Medium, color = fg),
        )
    }
}

/** The 6-across stamp toggle grid, shared by the input keypad and the day-edit sheet. */
@Composable
fun StampPickerGrid(
    selected: List<StampType>,
    onToggle: (StampType) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StampType.entries.forEach { stamp ->
            StampButton(
                stamp = stamp,
                selected = stamp in selected,
                onClick = { onToggle(stamp) },
                iconSize = iconSize,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
