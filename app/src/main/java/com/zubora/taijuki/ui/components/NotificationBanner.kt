package com.zubora.taijuki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography

/** The mock OS-notification banner, reused for both the save confirmation and the reminder preview. */
@Composable
fun NotificationBanner(visible: Boolean, text: String, accent: Color, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = AppColors.TextPrimary, spotColor = AppColors.TextPrimary)
                .background(AppColors.Card, RoundedCornerShape(14.dp))
                .border(1.dp, AppColors.BorderCard, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                CheckGlyph(color = AppColors.Card, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(
                    "ズボラ体重記",
                    style = AppTypography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary),
                )
                Text(
                    text,
                    style = AppTypography.labelMedium.copy(fontSize = 12.sp, color = AppColors.TextSecondary),
                )
            }
        }
    }
}
