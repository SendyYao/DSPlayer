package com.synology.sylib.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.Locale;

public class PackageUtils {
    private static final String SYNO_APP_NAME = "synoAppName";

    public static String getPackageLastName(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        String packageName = context.getPackageName();
        int iLastIndexOf = packageName.lastIndexOf(".");
        return iLastIndexOf > 0 ? packageName.substring(iLastIndexOf + 1) : packageName;
    }

    public static String getVersionName(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return String.format(Locale.ENGLISH, "%s-%03d", packageInfo.versionName, packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bundle getMetaDataBundle(Context context, String str) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(str, 128);
            return applicationInfo.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getSynoAppName(Context context) {
        return getSynoAppName(context, context.getPackageName());
    }

    public static String getSynoAppName(Context context, String str) {
        Bundle metaDataBundle = getMetaDataBundle(context, str);
        if (metaDataBundle != null) {
            return metaDataBundle.getString(SYNO_APP_NAME);
        }
        return null;
    }
}
