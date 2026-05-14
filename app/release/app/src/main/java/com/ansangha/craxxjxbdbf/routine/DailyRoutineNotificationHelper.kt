package com.ansangha.craxxjxbdbf.routine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ansangha.craxxjxbdbf.MainActivity
import com.ansangha.craxxjxbdbf.R
import com.ansangha.craxxjxbdbf.work.NotificationPost

internal object DailyRoutineNotificationHelper {

    const val CHANNEL_ID = "daily_routine_reminder"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val name = context.getString(R.string.notification_channel_daily_routine_name)
        val description = context.getString(R.string.notification_channel_daily_routine_description)
        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            this.description = description
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showReminder(
        context: Context,
        taskId: Long,
        taskName: String,
        isFollowUp: Boolean,
    ) {
        ensureChannel(context)
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ROUTINE_COMPLETE, true)
            putExtra(MainActivity.EXTRA_ROUTINE_TASK_ID, taskId)
        }
        val pending = PendingIntent.getActivity(
            context,
            (0x4_000_000 xor taskId.toInt() xor if (isFollowUp) 1 else 0),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = context.getString(
            if (isFollowUp) R.string.notification_daily_routine_followup_title
            else R.string.notification_daily_routine_title,
        )
        val body = if (isFollowUp) {
            context.getString(R.string.notification_daily_routine_followup_body, taskName)
        } else {
            context.getString(R.string.notification_daily_routine_body, taskName)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_routine)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        val nid = (0x4_100_000 xor taskId.toInt() xor if (isFollowUp) 1 else 0)
        NotificationPost.safeNotify(context, nid, notification)
    }
}
