package com.zubora.taijuki.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules the daily reminder. Uses setAndAllowWhileIdle (inexact-while-idle)
 * rather than an exact alarm: a habit reminder doesn't need to-the-minute
 * precision, and staying inexact avoids requesting SCHEDULE_EXACT_ALARM — a
 * permission Play policy expects apps to justify, and a personal weight log
 * has no real justification for it.
 */
object AlarmScheduler {

    fun schedule(context: Context, time: LocalTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerMillis(time)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    fun parseReminderTime(value: String): LocalTime = try {
        LocalTime.parse(value)
    } catch (e: Exception) {
        LocalTime.of(21, 0)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextTriggerMillis(time: LocalTime): Long {
        val now = LocalDateTime.now()
        var trigger = LocalDateTime.of(now.toLocalDate(), time)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        return trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
