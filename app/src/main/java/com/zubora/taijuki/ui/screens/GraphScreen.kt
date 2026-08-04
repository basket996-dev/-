package com.zubora.taijuki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.AppViewModel
import com.zubora.taijuki.UiState
import com.zubora.taijuki.domain.buildGraphData
import com.zubora.taijuki.domain.buildWeeklyStampCounts
import com.zubora.taijuki.domain.computeBmi
import com.zubora.taijuki.domain.currentWeight
import com.zubora.taijuki.domain.dayDiff
import com.zubora.taijuki.domain.dayDiffText
import com.zubora.taijuki.domain.diffColor
import com.zubora.taijuki.domain.filterByPeriod
import com.zubora.taijuki.domain.round1
import com.zubora.taijuki.domain.toFixed1
import com.zubora.taijuki.ui.components.WeightLineChart
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography

@Composable
fun GraphScreen(uiState: UiState, viewModel: AppViewModel) {
    val accent = uiState.accent
    val settings = uiState.settings
    val entries = uiState.entries

    val current = remember(entries, settings.startWeight) { currentWeight(entries, settings.startWeight) }
    val lostSoFarDisplay = remember(settings.startWeight, current) { "-" + round1(settings.startWeight - current).toFixed1() }
    val remainingDisplay = remember(current, settings.targetWeight) { round1(current - settings.targetWeight).toFixed1() }
    val diff = remember(entries) { dayDiff(entries) }
    val periodEntries = remember(entries, settings.graphPeriod, uiState.today) {
        filterByPeriod(entries, settings.graphPeriod, uiState.today)
    }
    val graphData = remember(periodEntries, settings.graphStampMode) { buildGraphData(periodEntries, settings.graphStampMode) }
    val bmi = remember(current, settings.heightCm) { computeBmi(current, settings.heightCm) }
    val weeklyCounts = remember(entries, uiState.today) { buildWeeklyStampCounts(entries, uiState.today) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "グラフ",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp),
            style = AppTypography.titleMedium.copy(fontSize = 19.sp, color = AppColors.TextPrimary),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatChip("スタート時から", lostSoFarDisplay, accent, modifier = Modifier.weight(1f))
            StatChip("目標まであと", remainingDisplay, AppColors.TextPrimary, modifier = Modifier.weight(1f))
            StatChip("前日比", dayDiffText(diff), diffColor(diff), modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 10.dp)
                .fillMaxWidth()
                .background(AppColors.Card, RoundedCornerShape(18.dp))
                .border(1.dp, AppColors.BorderCard, RoundedCornerShape(18.dp))
                .padding(top = 18.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
        ) {
            WeightLineChart(graphData = graphData, accent = accent, modifier = Modifier.fillMaxWidth())
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .background(AppColors.Card, RoundedCornerShape(18.dp))
                .border(1.dp, AppColors.BorderCard, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("BMI", style = AppTypography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary))
                Text(bmi, style = AppTypography.titleMedium.copy(fontSize = 20.sp, color = AppColors.TextPrimary))
            }
            Text(
                "今週の記録",
                modifier = Modifier.padding(bottom = 8.dp),
                style = AppTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary),
            )
            if (weeklyCounts.isNotEmpty()) {
                WeeklyStampChips(weeklyCounts.map { Triple(it.stamp.color, it.stamp.label, it.count) })
            } else {
                Text("まだ記録がありません", style = AppTypography.bodySmall.copy(fontSize = 12.sp, color = AppColors.TextPlaceholder))
            }
            Text(
                "スタンプの詳細はカレンダーで確認できます",
                modifier = Modifier.padding(top = 10.dp),
                style = AppTypography.labelSmall.copy(fontSize = 10.5.sp, color = AppColors.TextPlaceholder),
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AppColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.BorderCard, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            textAlign = TextAlign.Center,
            style = AppTypography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary),
        )
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text(value, style = AppTypography.titleMedium.copy(fontSize = 20.sp, color = valueColor))
            Text("kg", style = AppTypography.labelSmall.copy(fontSize = 11.5.sp, color = valueColor))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyStampChips(items: List<Triple<Color, String, Int>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (color, label, count) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .background(AppColors.CardAlt, RoundedCornerShape(100))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                Text("$label ×$count", style = AppTypography.bodySmall.copy(fontSize = 11.5.sp, color = AppColors.TextPrimary))
            }
        }
    }
}
