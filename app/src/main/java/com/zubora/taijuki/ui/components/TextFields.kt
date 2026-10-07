package com.zubora.taijuki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppFontFamily
import androidx.compose.foundation.text.BasicTextField

/** The notebook-style dashed-underline number field used on onboarding and the day-edit sheet. */
@Composable
fun DashedNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    fontSize: TextUnit = 24.sp,
    textAlign: TextAlign = TextAlign.Start,
) {
    val textStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        color = AppColors.TextPrimary,
        textAlign = textAlign,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(AppColors.TextPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .dashedBottomBorder(AppColors.BorderDashed)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle.copy(color = AppColors.TextPlaceholder))
                }
                innerTextField()
            }
        },
    )
}

/** The boxed, solid-border number field used in Settings > 目標設定. */
@Composable
fun BoxNumberField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val textStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = AppColors.TextPrimary,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(AppColors.TextPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .background(AppColors.InputBackground, RoundedCornerShape(10.dp))
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

/** [BoxNumberField]'s look for free text — the stamp name and emoji boxes. */
@Composable
fun BoxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(fontFamily = AppFontFamily, fontSize = 14.sp, color = AppColors.TextPrimary)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(AppColors.TextPrimary),
        modifier = modifier
            .background(AppColors.InputBackground, RoundedCornerShape(10.dp))
            .border(1.dp, AppColors.BorderLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle.copy(color = AppColors.TextPlaceholder))
                }
                inner()
            }
        },
    )
}

/** The card-background memo textarea shared by the input tab and the day-edit sheet. */
@Composable
fun MemoTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "ひとことメモ（任意）",
) {
    val textStyle = TextStyle(fontFamily = AppFontFamily, fontSize = 13.sp, color = AppColors.TextPrimary)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        cursorBrush = SolidColor(AppColors.TextPrimary),
        minLines = 2,
        modifier = modifier
            .background(AppColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.BorderCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle.copy(color = AppColors.TextPlaceholder))
                }
                inner()
            }
        },
    )
}
