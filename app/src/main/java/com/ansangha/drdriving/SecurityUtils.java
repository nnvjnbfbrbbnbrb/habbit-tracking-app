package com.ansangha.drdriving;

import java.io.File;

public class SecurityUtils {
    // Check karega ki phone rooted hai ya nahi
    public static boolean isDeviceRooted() {
        String[] paths = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
}