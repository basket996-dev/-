package com.zubora.taijuki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.AppViewModel
import com.zubora.taijuki.UiState
import com.zubora.taijuki.domain.CalendarCell
import com.zubora.taijuki.domain.buildCalendarCells
import com.zubora.taijuki.domain.buildMemoFeed
import com.zubora.taijuki.domain.buildMemories
import com.zubora.taijuki.domain.currentWeight
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography
import com.zubora.taijuki.ui.theme.StampIcon
import com.zubora.taijuki.ui.theme.accentDark
import java.time.LocalDate

@Composable
fun CalendarScreen(uiState: UiState, viewModel: AppViewModel) {
    val accent = uiState.accent
    val accentDarkColor = accentDark(accent)

    val cells = remember(uiState.calendarMonth, uiState.entries, uiState.today) {
        buildCalendarCells(uiState.calendarMonth, uiState.entries, uiState.today)
    }
    val current = remember(uiState.entries, uiState.settings.startWeight) {
        currentWeight(uiState.entries, uiState.settings.startWeight)
    }
    val memories = remember(uiState.entries, uiState.today, current) {
        buildMemories(uiState.entries, uiState.today, current)
    }
    val memoFeed = remember(uiState.entries, uiState.today) {
        buildMemoFeed(uiState.entries, uiState.today)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "カレンダー",
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 4.dp),
            style = AppTypography.titleMedium.copy(fontSize = 19.sp, color = AppColors.TextPrimary),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavGlyphButton("«", accentDarkColor, fontSize = 15.sp, onClick = { viewModel.changeYear(-1) })
                NavGlyphButton("‹", accentDarkColor, fontSize = 18.sp, onClick = { viewModel.changeMonth(-1) })
            }
            Text(
                "${uiState.calendarMonth.year}年${uiState.calendarMonth.monthValue}月",
                style = AppTypography.titleSmall.copy(fontSize = 14.sp, color = AppColors.TextPrimary),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavGlyphButton("›", accentDarkColor, fontSize = 18.sp, onClick = { viewModel.changeMonth(1) })
                NavGlyphButton("»", accentDarkColor, fontSize = 15.sp, onClick = { viewModel.changeYear(1) })
            }
        }

        Column(modifier = Modifier.padding(horizontal = 10.dp).padding(bottom = 22.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                val labels = listOf("日" to AppColors.Sunday, "月" to AppColors.TextSecondary, "火" to AppColors.TextSecondary,
                    "水" to AppColors.TextSecondary, "木" to AppColors.TextSecondary, "金" to AppColors.TextSecondary, "土" to AppColors.Saturday)
                labels.forEach { (label, color) ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = AppTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color),
                    )
                }
            }
            cells.chunked(7).forEach { rowCells ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowCells.forEach { cell ->
                        CalendarDayCell(
                            cell = cell,
                            accent = accent,
                            accentDarkColor = accentDarkColor,
                            onClick = { viewModel.openDayModal(it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (memories.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth()
                    .background(AppColors.CardAlt, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Text(
                    "思い出",
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = AppTypography.titleSmall.copy(fontSize = 13.sp, color = AppColors.TextPrimary),
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    memories.forEach { mem ->
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.openDayModal(mem.date) },
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                                Text(mem.yearLabel, style = AppTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentDarkColor))
                                Text(mem.dateLabel, style = AppTypography.labelSmall.copy(fontSize = 10.5.sp, color = AppColors.TextSecondary))
                                Text("${mem.weightDisplay}kg", style = AppTypography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary))
                                Text("今より ${mem.diffText}", style = AppTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = mem.diffColor))
                            }
                            if (mem.memo.isNotBlank()) {
                                Text(mem.memo, style = AppTypography.bodyMedium.copy(fontSize = 13.sp, color = AppColors.TextPrimary, lineHeight = 19.sp))
                            }
                        }
                    }
                }
            }
        }

        if (memoFeed.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .fillMaxWidth()
                    .background(AppColors.Card, RoundedCornerShape(18.dp))
                    .border(1.dp, AppColors.BorderCard, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Text(
                    "最近のメモ",
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = AppTypography.titleSmall.copy(fontSize = 13.sp, color = AppColors.TextPrimary),
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    memoFeed.forEach { m ->
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.openDayModal(m.date) },
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                                Text(m.dateLabel, style = AppTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary))
                                Text("${m.weightDisplay}kg", style = AppTypography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary))
                            }
                            Text(m.memo, style = AppTypography.bodyMedium.copy(fontSize = 13.sp, color = AppColors.TextPrimary, lineHeight = 19.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavGlyphButton(glyph: String, color: Color, fontSize: TextUnit, onClick: () -> Unit) {
    Text(
        glyph,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 6.dp),
        style = AppTypography.labelLarge.copy(fontSize = fontSize, color = color),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarDayCell(
    cell: CalendarCell,
    accent: Color,
    accentDarkColor: Color,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!cell.inMonth || cell.date == null) {
        Box(modifier = modifier.aspectRatio(0.78f))
        return
    }
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(13.dp))
            .background(if (cell.hasEntry) AppColors.CardAlt else AppColors.Card)
            .border(1.5.dp, if (cell.isToday) accent else AppColors.BorderLight, RoundedCornerShape(13.dp))
            .clickable { onClick(cell.date) }
            .padding(vertical = 5.dp, horizontal = 2.dp),
    ) {
        if (cell.hasMemo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accentDarkColor),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(cell.dayLabel, style = AppTypography.labelSmall.copy(fontSize = 10.5.sp, color = AppColors.TextSecondary))
            Text(cell.weightDisplay, style = AppTypography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary))
            if (cell.stamps.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 1.dp),
                ) {
                    cell.stamps.forEach { stamp ->
                        StampIcon(stamp, tint = stamp.color, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}
