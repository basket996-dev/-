package com.zubora.taijuki.domain

import androidx.compose.ui.graphics.Color
import com.zubora.taijuki.ui.theme.AppColors
import java.time.LocalDate
import java.util.Locale
import kotlin.math.round

fun round1(value: Double): Double = round(value * 10) / 10.0

fun Double.toFixed1(): String = String.format(Locale.US, "%.1f", this)

/** Seeds an editable numeric field from a stored value, JS `String(number)`-style (no forced trailing .0). */
fun keypadSeed(weight: Double): String {
    val r = round1(weight)
    val whole = r.toLong()
    return if (r == whole.toDouble()) whole.toString() else String.format(Locale.US, "%.1f", r)
}

private val weekdaysJa = listOf("日", "月", "火", "水", "木", "金", "土")

/** java.time.DayOfWeek is Monday=1..Sunday=7; this remaps to the Sunday-first index used throughout the UI. */
fun weekdayJa(date: LocalDate): String = weekdaysJa[date.dayOfWeek.value % 7]

fun formatDateJa(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日(${weekdayJa(date)})"

fun diffColor(diff: Double?): Color = when {
    diff == null -> AppColors.TextSecondary
    diff > 0 -> AppColors.Negative
    diff < 0 -> AppColors.Positive
    else -> AppColors.TextSecondary
}

/** For the graph's 前日比 stat chip — no unit suffix (the "kg" is a separate, smaller label). */
fun dayDiffText(diff: Double?): String {
    if (diff == null) return "–"
    val r = round1(diff)
    return if (r > 0) "+${r.toFixed1()}" else r.toFixed1()
}

/** For 思い出 memory cards — unit suffix baked into the string. */
fun memoryDiffText(diff: Double): String {
    val r = round1(diff)
    return if (r > 0) "+${r.toFixed1()}kg" else "${r.toFixed1()}kg"
}
