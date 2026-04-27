package com.synology.sylib.syhttp3.util;

import android.content.Context;
import com.synology.sylib.syhttp3.SyHttpClient;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes2.dex */
public class CertificateUtil {
    private static final String PREF_REQUEST_FINGERPRINTS = "pref_request_fingerprints";
    private static Map<String, String> sFingerprintMap = new HashMap();

    public static void putFingerprint(String str, String str2) {
        Map<String, String> map = sFingerprintMap;
        if (map == null || map.isEmpty()) {
            sFingerprintMap = restoreFromSharedPreferences();
        }
        sFingerprintMap.put(str, str2);
        saveToSharedPreferences(str, str2);
    }

    public static String getFingerprint(String str) {
        Map<String, String> map = sFingerprintMap;
        if (map == null || map.isEmpty()) {
            sFingerprintMap = restoreFromSharedPreferences();
        }
        return sFingerprintMap.get(str);
    }

    private static Context getContext() {
        Context context = SyHttpClient.getContext();
        if (context != null) {
            return context;
        }
        throw new IllegalStateException("mContext == null, call SyHttpClient.setContext(Context) first");
    }

    private static void saveToSharedPreferences(String str, String str2) {
        getContext().getSharedPreferences(PREF_REQUEST_FINGERPRINTS, 0).edit().putString(str, str2).apply();
    }

    private static Map<String, String> restoreFromSharedPreferences() {
        Map<String, ?> all = getContext().getSharedPreferences(PREF_REQUEST_FINGERPRINTS, 0).getAll();
        sFingerprintMap.clear();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            sFingerprintMap.put(entry.getKey(), (String) entry.getValue());
        }
        return sFingerprintMap;
    }
}