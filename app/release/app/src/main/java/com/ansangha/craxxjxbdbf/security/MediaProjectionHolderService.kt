package com.ansangha.craxxjxbdbf.security

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service placeholder with [android:foregroundServiceType]="mediaProjection".
 * Start this only when you have an active [android.media.projection.MediaProjection] token to hold.
 */
class MediaProjectionHolderService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null
}
