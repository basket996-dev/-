package com.zubora.taijuki.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.zubora.taijuki.MainActivity
import com.zubora.taijuki.ZuboraApplication
import com.zubora.taijuki.domain.WeekProgress
import com.zubora.taijuki.domain.WidgetState
import com.zubora.taijuki.domain.weekStart
import com.zubora.taijuki.domain.widgetState
import com.zubora.taijuki.ui.theme.AppColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Home-screen widget: whether today is recorded, the previous weight, this
 * week's ●●○, and a 記録する button that opens [QuickRecordActivity] — a
 * keypad over the home screen, since widgets can't hold a text field.
 */
class WeightWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ZuboraApplication
        val content = contentFlow(app)
        val initial = content.first()
        provideContent {
            // Follows the database while the widget session is alive, so a save shows up straight away.
            val current by content.collectAsState(initial)
            WidgetContent(state = current.first, accent = current.second)
        }
    }

    private fun contentFlow(app: ZuboraApplication): Flow<Pair<WidgetState, Color>> =
        combine(app.database.entryDao().observeAll(), app.settingsRepository.settings) { rows, settings ->
            val today = LocalDate.now()
            val todayKey = today.toString()
            val weekFrom = weekStart(today).toString()
            val previous = rows.filter { it.date < todayKey }.maxByOrNull { it.date }
            val state = widgetState(
                todayWeight = rows.firstOrNull { it.date == todayKey }?.weight,
                previousDate = previous?.date?.let(LocalDate::parse),
                previousWeight = previous?.weight,
                today = today,
                weekCount = rows.count { it.date in weekFrom..todayKey },
            )
            state to AppColors.AccentOptions.getOrElse(settings.accentIndex) { AppColors.DefaultAccent }
        }

    companion object {
        /** Redraws placed widgets — after a save, a delete, or an accent change. Also rolls "today" over. */
        suspend fun refresh(context: Context) = WeightWidget().updateAll(context)
    }
}

class WeightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeightWidget()
}

private fun text(color: Color, size: Float, bold: Boolean = false) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
)

@Composable
private fun WidgetContent(state: WidgetState, accent: Color) {
    // Already recorded → open the app to look; not yet → straight to the keypad.
    val tapAction: Action =
        if (state.recordedToday) actionStartActivity<MainActivity>() else actionStartActivity<QuickRecordActivity>()
    val compact = LocalSize.current.height < 100.dp
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(AppColors.Card)
            .cornerRadius(20.dp)
            .clickable(tapAction)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (compact) CompactLayout(state, accent, tapAction) else FullLayout(state, accent, tapAction)
    }
}

@Composable
private fun FullLayout(state: WidgetState, accent: Color, tapAction: Action) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ズボラ体重記", style = text(AppColors.TextSecondary, 11f), modifier = GlanceModifier.defaultWeight())
            WeekDots(state.week, accent)
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(state.headline, style = text(AppColors.TextPrimary, 19f, bold = true), maxLines = 1)
        Text(state.detail, style = text(AppColors.TextSecondary, 12f), maxLines = 1)
        Spacer(GlanceModifier.defaultWeight())
        if (state.recordedToday) {
            Text("記録ずみ・タップでアプリを開く", style = text(accent, 11.5f))
        } else {
            RecordButton(accent, tapAction, GlanceModifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CompactLayout(state: WidgetState, accent: Color, tapAction: Action) {
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(state.headline, style = text(AppColors.TextPrimary, 16f, bold = true), maxLines = 1)
            Text(state.detail, style = text(AppColors.TextSecondary, 11f), maxLines = 1)
        }
        if (!state.recordedToday) {
            Spacer(GlanceModifier.width(8.dp))
            RecordButton(accent, tapAction)
        }
    }
}

@Composable
private fun RecordButton(accent: Color, tapAction: Action, modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier
            .background(accent)
            .cornerRadius(100.dp)
            .clickable(tapAction)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("記録する", style = text(AppColors.Card, 14f, bold = true))
    }
}

/** This week against the 週3回 target, as in the app. */
@Composable
private fun WeekDots(week: WeekProgress, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(week.target) { i ->
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(if (i < week.count) accent else AppColors.BorderLight),
            ) {}
            Spacer(GlanceModifier.width(3.dp))
        }
        Text(
            if (week.count <= week.target) "${week.count}/${week.target}" else "${week.count}回",
            style = text(AppColors.TextSecondary, 11f),
        )
    }
}
