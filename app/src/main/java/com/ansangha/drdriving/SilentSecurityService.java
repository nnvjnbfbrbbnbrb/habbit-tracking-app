package com.ansangha.drdriving;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SilentSecurityService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Background mein check chal raha hai
        if (SecurityUtils.isDeviceRooted()) {
            Log.d("SECURE_DRIVE", "ALARM: Root detected on this device!");
        } else {
            Log.d("SECURE_DRIVE", "System is secure.");
        }
        return START_STICKY; // Taaki OS ise band na kare
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}