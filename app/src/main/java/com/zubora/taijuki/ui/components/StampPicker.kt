package com.zubora.taijuki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.data.Stamp
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography
import com.zubora.taijuki.ui.theme.StampIcon

@Composable
fun StampButton(
    stamp: Stamp,
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
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = AppTypography.labelSmall.copy(fontSize = labelFontSize, fontWeight = FontWeight.Medium, color = fg),
        )
    }
}

/** The stamps offered for a day: the ones on the input screen, plus any put-away ones already on that day. */
fun pickerStamps(all: List<Stamp>, selected: List<Stamp>): List<Stamp> =
    all.filter { s -> s.active || selected.any { it.id == s.id } }

private const val MAX_COLUMNS = 6

/**
 * The stamp toggle grid, shared by the input keypad and the day-edit sheet.
 * A few stamps get wide buttons; past six they wrap, six to a row.
 */
@Composable
fun StampPickerGrid(
    stamps: List<Stamp>,
    selected: List<Stamp>,
    onToggle: (Stamp) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
) {
    if (stamps.isEmpty()) return
    val columns = stamps.size.coerceAtMost(MAX_COLUMNS)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stamps.chunked(columns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { stamp ->
                    StampButton(
                        stamp = stamp,
                        selected = selected.any { it.id == stamp.id },
                        onClick = { onToggle(stamp) },
                        iconSize = iconSize,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep a short last row's buttons the same width as the rows above.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
