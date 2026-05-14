package com.ansangha.craxxjxbdbf.security

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Observes posted/dismissed notifications when the user enables the listener in system settings.
 */
class MySecurityNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
