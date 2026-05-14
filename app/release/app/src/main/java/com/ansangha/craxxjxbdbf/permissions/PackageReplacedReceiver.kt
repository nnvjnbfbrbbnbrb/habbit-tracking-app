package com.ansangha.craxxjxbdbf.permissions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        PermissionGateRefreshBus.notifyRefresh()
    }
}
