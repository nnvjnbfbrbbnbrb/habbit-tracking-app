package com.ansangha.craxxjxbdbf.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ansangha.craxxjxbdbf.MainActivity
import com.ansangha.craxxjxbdbf.R

internal object RoutineNotificationHelper {

    const val CHANNEL_ID = "routine_integrity"
    private const val NOTIFICATION_ID = 71042

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val name = context.getString(R.string.notification_channel_routine_name)
        val description = context.getString(R.string.notification_channel_routine_description)
        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            this.description = description
            setShowBadge(true)
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    fun showOpenHabitsReminder(
        context: Context,
        openCount: Int,
        previewNames: List<String>,
    ) {
        ensureChannel(context)
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val preview = previewNames.joinToString(separator = " · ").trim()
        val body = if (preview.isNotEmpty()) {
            context.getString(R.string.notification_routine_body_with_names, openCount, preview)
        } else {
            context.getString(R.string.notification_routine_body_generic, openCount)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_routine)
            .setContentTitle(context.getString(R.string.notification_routine_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationPost.safeNotify(context, NOTIFICATION_ID, notification)
    }
}
