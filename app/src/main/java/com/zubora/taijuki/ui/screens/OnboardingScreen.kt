package com.zubora.taijuki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.AppViewModel
import com.zubora.taijuki.UiState
import com.zubora.taijuki.ui.components.AvatarPicker
import com.zubora.taijuki.ui.components.DashedNumberField
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography

@Composable
fun OnboardingScreen(uiState: UiState, viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 32.dp),
    ) {
        AvatarPicker(bitmap = uiState.avatarBitmap, onPick = viewModel::pickAvatar, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "はじめまして",
            style = AppTypography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "ズボラ体重記へ\nようこそ",
            style = AppTypography.titleLarge.copy(fontSize = 26.sp, color = AppColors.TextPrimary, lineHeight = 36.sp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "最初にちょっとだけ教えてください。\nあとは毎日ワンタップで大丈夫です。",
            style = AppTypography.bodyMedium.copy(fontSize = 13.sp, color = AppColors.TextSecondary, lineHeight = 22.sp),
        )
        Spacer(Modifier.height(32.dp))

        LabeledOnboardingField("身長 (cm)", uiState.obHeight, viewModel::setObHeight, "160")
        Spacer(Modifier.height(22.dp))
        LabeledOnboardingField("現在の体重 (kg)", uiState.obWeight, viewModel::setObWeight, "65.0")
        Spacer(Modifier.height(22.dp))
        LabeledOnboardingField("目標体重 (kg)", uiState.obTarget, viewModel::setObTarget, "60.0")

        Spacer(Modifier.height(32.dp))

        if (uiState.obError) {
            Text(
                "3つとも入力してください",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                style = AppTypography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = AppColors.Negative),
            )
        }
        Button(
            onClick = viewModel::completeOnboarding,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = uiState.accent, spotColor = uiState.accent),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = uiState.accent, contentColor = AppColors.Card),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                "きろくをはじめる",
                style = AppTypography.titleMedium.copy(fontSize = 16.sp),
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun LabeledOnboardingField(label: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    Column {
        Text(
            label,
            style = AppTypography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary),
        )
        Spacer(Modifier.height(6.dp))
        DashedNumberField(value = value, onValueChange = onChange, placeholder = placeholder, modifier = Modifier.fillMaxWidth())
    }
}
