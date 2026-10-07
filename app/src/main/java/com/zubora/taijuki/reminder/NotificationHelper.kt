package com.zubora.taijuki.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.zubora.taijuki.MainActivity
import com.zubora.taijuki.R
import com.zubora.taijuki.domain.ReminderCopy

object NotificationHelper {
    const val CHANNEL_ID = "reminder"
    const val NOTIFICATION_ID = 1001

    /** RemoteInput key for the weight typed straight into the reminder. */
    const val KEY_QUICK_WEIGHT = "quick_weight"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(context: Context): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * The reminder, with a 記録する action that takes the weight right in the
     * notification — no need to open the app. [error] replaces the body text
     * when the last thing typed couldn't be read as a weight.
     */
    fun showReminder(context: Context, copy: ReminderCopy, error: String? = null) {
        if (!hasPermission(context)) return

        val remoteInput = RemoteInput.Builder(KEY_QUICK_WEIGHT)
            .setLabel("体重（例 77.5）")
            .build()
        // Mutable so the system can attach what was typed; explicit, so nothing else can receive it.
        val recordIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, QuickRecordReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val recordAction = NotificationCompat.Action.Builder(R.drawable.ic_notification, "記録する", recordIntent)
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .build()

        val text = error ?: copy.text
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(copy.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true) // re-posting after a typo shouldn't buzz again
            .setContentIntent(openAppIntent(context))
            .addAction(recordAction)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Today got recorded in the app, so a "please record" reminder still in the shade is out of date. */
    fun cancelReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Replaces the reminder once the typed weight is saved (which also clears the reply spinner). */
    fun showRecorded(context: Context, copy: ReminderCopy) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(copy.title)
            .setContentText(copy.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.text))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(10_000)
            .setContentIntent(openAppIntent(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
