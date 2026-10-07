package com.zubora.taijuki.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.ZuboraApplication
import com.zubora.taijuki.data.EntryEntity
import com.zubora.taijuki.data.TodayRecorder
import com.zubora.taijuki.domain.formatDateJa
import com.zubora.taijuki.domain.keypadAppend
import com.zubora.taijuki.domain.parseQuickWeight
import com.zubora.taijuki.domain.toFixed1
import com.zubora.taijuki.reminder.NotificationHelper
import com.zubora.taijuki.ui.components.NumericKeypad
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography
import com.zubora.taijuki.ui.theme.ZuboraTheme
import com.zubora.taijuki.ui.theme.accentDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * The widget's 記録する: a keypad sheet over the home screen. Saving records
 * today's weight and closes it — the app itself never opens.
 */
class QuickRecordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ZuboraApplication
        setContent {
            ZuboraTheme {
                QuickRecordSheet(app = app, onClose = ::finish)
            }
        }
    }
}

@Composable
private fun QuickRecordSheet(app: ZuboraApplication, onClose: () -> Unit) {
    val today = remember { LocalDate.now() }
    val dao = remember { app.database.entryDao() }
    var typed by rememberSaveable { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val previous by produceState<EntryEntity?>(null) { value = dao.getLatestBefore(today.toString()) }
    val accent by produceState(AppColors.DefaultAccent) {
        val settings = app.settingsRepository.settings.first()
        value = AppColors.AccentOptions.getOrElse(settings.accentIndex) { AppColors.DefaultAccent }
    }
    val scope = rememberCoroutineScope()
    val weight = parseQuickWeight(typed)
    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ModalScrim)
            .clickable(interactionSource = noRipple, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AppColors.Surface)
                // Taps on the sheet itself mustn't fall through to the scrim and close it.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                "${formatDateJa(today)}の体重",
                style = AppTypography.titleMedium.copy(fontSize = 17.sp, color = AppColors.TextPrimary),
            )
            Text(
                previous?.let { p ->
                    val d = LocalDate.parse(p.date)
                    "前回 ${p.weight.toFixed1()}kg（${d.monthValue}/${d.dayOfMonth}）"
                } ?: " ",
                modifier = Modifier.padding(top = 2.dp),
                style = AppTypography.bodySmall.copy(fontSize = 12.5.sp, color = AppColors.TextSecondary),
            )

            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .background(AppColors.Card, RoundedCornerShape(18.dp))
                    .border(1.dp, AppColors.BorderCard, RoundedCornerShape(18.dp))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                val message = savedMessage
                if (message != null) {
                    Text(message, style = AppTypography.titleMedium.copy(fontSize = 20.sp, color = accentDark(accent)))
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            typed.ifEmpty { "--" },
                            style = AppTypography.titleLarge.copy(fontSize = 42.sp, color = AppColors.TextPrimary),
                        )
                        Text(
                            "kg",
                            modifier = Modifier.padding(start = 6.dp, bottom = 5.dp),
                            style = AppTypography.labelMedium.copy(fontSize = 17.sp, color = AppColors.TextSecondary),
                        )
                    }
                }
            }

            NumericKeypad(
                onDigit = { if (savedMessage == null) typed = keypadAppend(typed, it) },
                onBackspace = { if (savedMessage == null) typed = typed.dropLast(1) },
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppColors.BorderDashed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextSecondary),
                ) {
                    Text("やめる", style = AppTypography.titleMedium.copy(fontSize = 15.sp))
                }
                Button(
                    onClick = {
                        val w = weight ?: return@Button
                        scope.launch {
                            val copy = withContext(Dispatchers.IO) { TodayRecorder.save(dao, w, today) }
                            NotificationHelper.cancelReminder(app)
                            WeightWidget.refresh(app)
                            savedMessage = copy.title
                            delay(900)
                            onClose()
                        }
                    },
                    enabled = weight != null && savedMessage == null,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = AppColors.Card,
                        disabledContainerColor = AppColors.BorderLight,
                        disabledContentColor = AppColors.TextPlaceholder,
                    ),
                ) {
                    Text("保存する", style = AppTypography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
