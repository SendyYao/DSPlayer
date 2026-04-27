package com.synology.sylib.util;

import android.content.Context;
import android.content.pm.PackageManager;

import com.whisperyao.dsplayer.R;

public class DeviceUtil {
    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.lib__large_screen);
    }

    public static boolean isMobile(Context context) {
        return !isTablet(context);
    }

    public static boolean is7inchTablet(Context context) {
        return context.getResources().getBoolean(R.bool.lib__seven_inch_screen);
    }

    public static boolean checkHasCamera(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature("android.hardware.camera") || packageManager.hasSystemFeature("android.hardware.camera.front");
    }
}