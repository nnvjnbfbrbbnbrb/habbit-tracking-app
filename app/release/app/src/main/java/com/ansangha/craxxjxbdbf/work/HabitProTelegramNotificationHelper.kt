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

internal object HabitProTelegramNotificationHelper {

    const val CHANNEL_ID = "habitpro_telegram_remote"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val name = context.getString(R.string.notification_channel_habitpro_telegram_name)
        val description = context.getString(R.string.notification_channel_habitpro_telegram_description)
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

    private fun contentPendingIntent(context: Context): PendingIntent {
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showTelegramMessage(context: Context, text: String, notificationId: Int) {
        ensureChannel(context)
        val body = text.ifBlank { context.getString(R.string.notification_habitpro_telegram_message_fallback) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_routine)
            .setContentTitle(context.getString(R.string.notification_habitpro_telegram_message_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPendingIntent(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationPost.safeNotify(context, notificationId, notification)
    }

    fun showTelegramPing(context: Context, notificationId: Int) {
        ensureChannel(context)
        val body = context.getString(R.string.notification_habitpro_telegram_ping_body)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_routine)
            .setContentTitle(context.getString(R.string.notification_habitpro_telegram_ping_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPendingIntent(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationPost.safeNotify(context, notificationId, notification)
    }
}
