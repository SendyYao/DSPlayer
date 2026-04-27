package com.synology.sylib.syhttp3.relay.utils;

import android.content.Context;
import android.util.Log;
import com.whisperyao.dsplayer.BuildConfig;
import com.synology.sylib.history.SynoApplication;
import com.synology.sylib.syhttp3.SyHttpClient;
import com.synology.sylib.syhttp3.relay.RelayInfo;
import com.synology.sylib.syhttp3.relay.ServiceId;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes2.dex */
class RelayInfoUtil {
    private static final String PREF_RELAY_INFOS = "pref_relay_infos";
    private static final String TAG = "RelayInfoUtil";
    private static Map<String, RelayInfo> sRelayInfoMap;

    RelayInfoUtil() {
    }

    private static String getMapKey(String str, String str2) {
        return str + ":" + str2;
    }

    static void addRelayInfo(String str, String str2, String[] strArr, String[] strArr2) throws IOException, ClassNotFoundException {
        RelayInfo relayInfo = new RelayInfo(str, str2, strArr, strArr2);
        if (sRelayInfoMap == null) {
            sRelayInfoMap = restoreFromSharedPreferences(getContext());
        }
        String mapKey = getMapKey(str, str2);
        sRelayInfoMap.put(mapKey, relayInfo);
        saveToSharedPreferences(getContext(), mapKey, relayInfo);
    }

    static RelayInfo getRelayInfo(boolean z) throws IOException, ClassNotFoundException {
        String packageName = getContext().getPackageName();
        Log.d(TAG, "create a template relay record from " + packageName);
        return getRelayInfo(getMapKey(packageName, RelayCommon.getProtocol(z)));
    }

    private static RelayInfo getRelayInfo(String str) throws IOException, ClassNotFoundException {
        if (sRelayInfoMap == null) {
            sRelayInfoMap = restoreFromSharedPreferences(getContext());
        }
        return sRelayInfoMap.get(str);
    }

    private static Context getContext() {
        Context context = SyHttpClient.getContext();
        if (context != null) {
            return context;
        }
        throw new IllegalStateException("mContext is null, call SyHttpClient.setContext() first");
    }

    private static void saveToSharedPreferences(Context context, String str, RelayInfo relayInfo) throws IOException {
        context.getSharedPreferences(PREF_RELAY_INFOS, 0).edit().putString(str, relayInfo.encode()).apply();
    }

    private static Map<String, RelayInfo> restoreFromSharedPreferences(Context context) throws IOException, ClassNotFoundException {
        Map<String, RelayInfo> defaultRelayInfo = getDefaultRelayInfo();
        for (Map.Entry<String, ?> entry : context.getSharedPreferences(PREF_RELAY_INFOS, 0).getAll().entrySet()) {
            defaultRelayInfo.put(entry.getKey(), RelayInfo.decode((String) entry.getValue()));
        }
        return defaultRelayInfo;
    }

    private static Map<String, RelayInfo> getDefaultRelayInfo() {
        HashMap map = new HashMap();
        String[] strArr = {"com.synology.DScam", "com.synology.DSdownload", "com.synology.DSfinder", SynoApplication.DSNOTE, SynoApplication.DSROUTER, SynoApplication.DSVIDEO};
        for (int i = 0; i < 6; i++) {
            String str = strArr[i];
            map.put(getMapKey(str, "http"), new RelayInfo(str, "http", new String[]{ServiceId.DSM}, new String[]{"/webman/pingpong.cgi"}));
            map.put(getMapKey(str, "https"), new RelayInfo(str, "https", new String[]{ServiceId.DSM_HTTPS}, new String[]{"/webman/pingpong.cgi"}));
        }
        map.put(getMapKey(BuildConfig.APPLICATION_ID, "http"), new RelayInfo(BuildConfig.APPLICATION_ID, "http", new String[]{ServiceId.AUDIO_HTTP}, new String[]{"/webman/pingpong.php"}));
        map.put(getMapKey(BuildConfig.APPLICATION_ID, "https"), new RelayInfo(BuildConfig.APPLICATION_ID, "https", new String[]{ServiceId.AUDIO_HTTPS}, new String[]{"/webman/pingpong.php"}));
        map.put(getMapKey("com.synology.DSfile", "http"), new RelayInfo("com.synology.DSfile", "http", new String[]{ServiceId.DSM, ServiceId.WEBDAV_HTTP}, new String[]{"/webman/pingpong.cgi", "/~DSFile/queryWebdav.cgi"}));
        map.put(getMapKey("com.synology.DSfile", "https"), new RelayInfo("com.synology.DSfile", "https", new String[]{ServiceId.DSM_HTTPS, ServiceId.WEBDAV_HTTPS}, new String[]{"/webman/pingpong.cgi", "/~DSFile/queryWebdav.cgi"}));
        map.put(getMapKey(SynoApplication.DSPHOTO, "http"), new RelayInfo(SynoApplication.DSPHOTO, "http", new String[]{ServiceId.PHOTO_HTTP}, new String[]{"/webman/pingpong.php"}));
        map.put(getMapKey(SynoApplication.DSPHOTO, "https"), new RelayInfo(SynoApplication.DSPHOTO, "https", new String[]{ServiceId.PHOTO_HTTPS}, new String[]{"/webman/pingpong.php"}));
        return map;
    }
}