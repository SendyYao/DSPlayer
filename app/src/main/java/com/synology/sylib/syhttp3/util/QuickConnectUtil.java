package com.synology.sylib.syhttp3.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

/* loaded from: classes2.dex */
public class QuickConnectUtil {
    private static final String KEY_IS_CN_REGION = "cn_region";
    private static final String PREFS_NAME = "lib_qct_info";
    private static final String TAG = "QuickConnectUtil";
    private static boolean sEnableLog = false;

    @Deprecated
    public static void setUseChinaSite(Context context, boolean z) {
        context.getSharedPreferences(PREFS_NAME, 0).edit().putBoolean(KEY_IS_CN_REGION, z).apply();
    }

    @Deprecated
    public static boolean isUseChinaSite(Context context) {
        return context.getSharedPreferences(PREFS_NAME, 0).getBoolean(KEY_IS_CN_REGION, false);
    }

    public static boolean isDeviceInChina(Context context) {
        Context applicationContext = context.getApplicationContext();
        if (applicationContext == null) {
            logIsDeviceInChina("Context", false);
            return false;
        }
        TelephonyManager telephonyManager = (TelephonyManager) applicationContext.getSystemService("phone");
        if (telephonyManager == null) {
            logIsDeviceInChina("Manager", false);
            return false;
        }
        String networkCountryIso = telephonyManager.getNetworkCountryIso();
        if (!TextUtils.isEmpty(networkCountryIso)) {
            boolean zEqualsIgnoreCase = "cn".equalsIgnoreCase(networkCountryIso);
            logIsDeviceInChina("MCC", zEqualsIgnoreCase);
            return zEqualsIgnoreCase;
        }
        try {
            boolean zEqualsIgnoreCase2 = "CHN".equalsIgnoreCase(applicationContext.getResources().getConfiguration().getLocales().get(0).getISO3Country());
            logIsDeviceInChina("Locale", zEqualsIgnoreCase2);
            return zEqualsIgnoreCase2;
        } catch (Exception unused) {
            logIsDeviceInChina("Other", false);
            return false;
        }
    }

    public static void setEnableLog(boolean z) {
        sEnableLog = z;
    }

    public static void log(String str) {
        if (sEnableLog) {
            Log.v(TAG, str);
        }
    }

    private static void logIsDeviceInChina(String str, boolean z) {
        log("Device" + (z ? "" : " NOT") + " in CN - " + str);
    }
}