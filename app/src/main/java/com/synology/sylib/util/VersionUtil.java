package com.synology.sylib.util;

import android.os.Build;

public abstract class VersionUtil {
    public static boolean isKitkat() {
        return false;
    }

    public static boolean isAndroid5() {
        return isAtLeastVersion(21);
    }

    public static boolean isAtLeastVersion(int i) {
        return Build.VERSION.SDK_INT >= i;
    }
}
