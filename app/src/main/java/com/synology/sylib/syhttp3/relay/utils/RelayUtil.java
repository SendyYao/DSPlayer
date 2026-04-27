package com.synology.sylib.syhttp3.relay.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.synology.sylib.syhttp3.SyHttpClient;
import com.synology.sylib.syhttp3.relay.RelayInfo;
import com.synology.sylib.syhttp3.relay.RelayManager;
import com.synology.sylib.syhttp3.relay.RelayRecord;
import com.synology.sylib.syhttp3.relay.RelayRecordKey;
import com.synology.sylib.syhttp3.relay.RelayResolveManager;
import com.synology.sylib.syhttp3.relay.ServiceId;
import com.synology.sylib.syhttp3.relay.ping.PingPongPolicy;
import com.synology.sylib.syhttp3.relay.vos.PingPongVo;
import com.synology.sylib.syhttp3.util.QuickConnectUtil;
import com.synology.sylib.util.HashUtils;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.CharUtils;

/* loaded from: classes2.dex */
public class RelayUtil {
    public static final String GLOBAL_SERVER_CN = "https://global.quickconnect.cn";
    public static final String GLOBAL_SERVER_WW = "https://global.quickconnect.to";
    public static final int HOLE_PUNCH_TIMEOUT_DISABLED = 0;
    private static final String KEY_CUSTOM_PUNCH_TIMEOUT = "custom_punch_timeout";
    private static final String KEY_USE_HOLE_PUNCH = "use_hole_punch";
    private static final String PREF_HOLE_PUNCH_RECORD = "hole_punch_record";

    @Deprecated
    private static final String REEF_RELAY_RECORDS2 = "pref_relay_records2";
    private static final String REEF_RELAY_RECORDS3 = "pref_relay_records3";
    private static final String TAG = "RelayUtil";
    private static Map<RelayRecordKey, RelayRecord> sRecordMap;

    public static boolean isQuickConnectId(String str) {
        return !TextUtils.isEmpty(str) && str.indexOf(46) < 0 && str.indexOf(58) < 0;
    }

    public static URL composeValidURL(String str, String str2, int i) {
        try {
            return new URL(str, str2, i, "");
        } catch (MalformedURLException unused) {
            return null;
        }
    }

    public static int getConnectivity(RelayRecordKey relayRecordKey) {
        if (!isQuickConnectId(relayRecordKey.getServerId())) {
            return 1;
        }
        RelayRecord relayRecord = getRelayRecord(relayRecordKey);
        if (relayRecord == null) {
            return 0;
        }
        return relayRecord.getConnectivity();
    }

    public static synchronized void setGlobalPingPongPolicy(PingPongPolicy pingPongPolicy) {
        RelayManager.getInstance().setPingPongPolicy(pingPongPolicy);
    }

    public static synchronized void addRelayRecord(String str, String[] strArr, String[] strArr2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("serverId is empty");
        }
        if (strArr == null) {
            throw new IllegalArgumentException("serviceIds == null");
        }
        if (strArr2 == null) {
            throw new IllegalArgumentException("pingPongPaths == null");
        }
        RelayRecord relayRecord = new RelayRecord(str, strArr, strArr2);
        if (sRecordMap == null) {
            sRecordMap = new HashMap();
        }
        RelayRecordKey relayRecordKey = RelayRecordKey.getInstance(str, strArr[0]);
        sRecordMap.put(relayRecordKey, relayRecord);
        saveToSharedPreferences(getContext(), relayRecordKey, relayRecord);
    }

    public static synchronized void setRelayRecord(RelayRecord relayRecord) {
        try {
            if (relayRecord == null) {
                throw new IllegalArgumentException("record == null");
            }
            String serverId = relayRecord.getServerId();
            if (TextUtils.isEmpty(serverId)) {
                throw new IllegalArgumentException("serverId is empty");
            }
            if (sRecordMap == null) {
                sRecordMap = restoreFromSharedPreferences(getContext());
            }
            RelayRecordKey relayRecordKey = RelayRecordKey.getInstance(serverId, relayRecord.getServiceIds()[0]);
            sRecordMap.put(relayRecordKey, relayRecord);
            Log.d(TAG, "[save relay record] serverId: " + serverId + ", url: " + relayRecord.getRealURL());
            saveToSharedPreferences(getContext(), relayRecordKey, relayRecord);
        } catch (Throwable th) {
            throw th;
        }
    }

    public static synchronized RelayRecord getRelayRecord(RelayRecordKey relayRecordKey) {
        if (sRecordMap == null) {
            sRecordMap = restoreFromSharedPreferences(getContext());
        }
        return sRecordMap.get(relayRecordKey);
    }

    public static synchronized Map<RelayRecordKey, RelayRecord> getAllRelayRecords() {
        if (sRecordMap == null) {
            sRecordMap = restoreFromSharedPreferences(getContext());
        }
        return Collections.unmodifiableMap(new HashMap(sRecordMap));
    }

    @Deprecated
    public static synchronized void clearRelayRecord(String str) {
        RelayRecordKey relayRecordKey = RelayRecordKey.getInstance(str, ServiceId.DSM);
        RelayRecordKey relayRecordKey2 = RelayRecordKey.getInstance(str, ServiceId.DSM_HTTPS);
        clearRelayRecord(relayRecordKey);
        clearRelayRecord(relayRecordKey2);
        RelayResolveManager relayResolveManager = RelayResolveManager.getInstance();
        relayResolveManager.clearResolvedResult(relayRecordKey);
        relayResolveManager.clearResolvedResult(relayRecordKey2);
    }

    public static synchronized void clearRelayRecord(RelayRecordKey relayRecordKey) {
        if (sRecordMap == null) {
            sRecordMap = restoreFromSharedPreferences(getContext());
        }
        sRecordMap.remove(relayRecordKey);
        RelayResolveManager.getInstance().clearResolvedResult(relayRecordKey);
    }

    public static synchronized void clearAllRelayRecords() {
        Map<RelayRecordKey, RelayRecord> map = sRecordMap;
        if (map != null) {
            map.clear();
        }
        clearSharedPreferences(getContext());
        RelayResolveManager.getInstance().clearAllResolvedResult();
    }

    private static Context getContext() {
        Context context = SyHttpClient.getContext();
        if (context != null) {
            return context;
        }
        throw new IllegalStateException("mContext is null, call SyHttpClient.setContext() first");
    }

    private static void saveToSharedPreferences(Context context, RelayRecordKey relayRecordKey, RelayRecord relayRecord) {
        context.getSharedPreferences(REEF_RELAY_RECORDS3, 0).edit().putString(relayRecordKey.toString(), new Gson().toJson(relayRecord)).apply();
    }

    private static Map<RelayRecordKey, RelayRecord> restoreFromSharedPreferences(Context context) {
        HashMap map = new HashMap();
        Gson gson = new Gson();
        if (hasPrefsFile(context, REEF_RELAY_RECORDS2)) {
            for (Map.Entry<String, ?> entry : context.getSharedPreferences(REEF_RELAY_RECORDS2, 0).getAll().entrySet()) {
                String key = entry.getKey();
                RelayRecord relayRecord = (RelayRecord) gson.fromJson((String) entry.getValue(), RelayRecord.class);
                String[] serviceIds = relayRecord.getServiceIds();
                if (serviceIds != null && serviceIds.length > 0) {
                    map.put(RelayRecordKey.getInstance(key, serviceIds[0]), relayRecord);
                }
            }
            deletePrefsFile(context, REEF_RELAY_RECORDS2);
        } else {
            for (Map.Entry<String, ?> entry2 : context.getSharedPreferences(REEF_RELAY_RECORDS3, 0).getAll().entrySet()) {
                map.put(RelayRecordKey.convertToRelayRecordKey(entry2.getKey()), gson.fromJson((String) entry2.getValue(), RelayRecord.class));
            }
        }
        return map;
    }

    private static void clearSharedPreferences(Context context) {
        deletePrefsFile(context, REEF_RELAY_RECORDS2);
        context.getSharedPreferences(REEF_RELAY_RECORDS3, 0).edit().clear().apply();
    }

    public static RelayRecord newRelayRecord(String str, boolean z) throws IOException, ClassNotFoundException {
        RelayInfo relayInfo = RelayInfoUtil.getRelayInfo(z);
        if (relayInfo == null) {
            throw new IllegalArgumentException("no valid relay record generated");
        }
        addRelayRecord(str, relayInfo.getServiceIds(), relayInfo.getPingPongPaths());
        return getRelayRecord(RelayRecordKey.getInstance(str, relayInfo.getServiceIds()[0]));
    }

    public static void addRelayInfo(String str, String str2, String[] strArr, String[] strArr2) throws IOException, ClassNotFoundException {
        RelayInfoUtil.addRelayInfo(str, str2, strArr, strArr2);
    }

    public static void saveUseHolePunch(boolean z) {
        getContext().getSharedPreferences(PREF_HOLE_PUNCH_RECORD, 0).edit().putBoolean(KEY_USE_HOLE_PUNCH, z).apply();
    }

    public static boolean getUseHolePunch() {
        return getContext().getSharedPreferences(PREF_HOLE_PUNCH_RECORD, 0).getBoolean(KEY_USE_HOLE_PUNCH, false);
    }

    public static void saveHolePunchTimeout(int i) {
        getContext().getSharedPreferences(PREF_HOLE_PUNCH_RECORD, 0).edit().putInt(KEY_CUSTOM_PUNCH_TIMEOUT, i).apply();
    }

    public static int getHolePunchTimeout() {
        return getContext().getSharedPreferences(PREF_HOLE_PUNCH_RECORD, 0).getInt(KEY_CUSTOM_PUNCH_TIMEOUT, 0);
    }

    public static String getRealURL(String str, boolean z) {
        try {
            return getRealURL(new URL(str), z).toExternalForm();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return str;
        }
    }

    public static URL getRealURL(URL url, boolean z) {
        RelayRecord relayRecord;
        String host = url.getHost();
        if (isQuickConnectId(host) && (relayRecord = getRelayRecord(RelayRecordKey.getInstanceWithCorrectHttps(host, z))) != null && relayRecord.getRealURL() != null) {
            URL realURL = relayRecord.getRealURL();
            try {
                return new URL(realURL.getProtocol(), realURL.getHost(), realURL.getPort(), url.getFile());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    public static String getExternalUrl(String str, boolean z) {
        try {
            return getExternalUrl(new URL(str).getHost(), str, z);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return str;
        }
    }

    public static String getExternalUrl(String str, boolean z, boolean z2, int i) {
        try {
            return getExternalUrl(new URL(str).getHost(), str, z, z2, i);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return str;
        }
    }

    public static String getExternalUrl(String str, String str2, boolean z) {
        return getExternalUrl(str, str2, z, false, 80);
    }

    public static String getExternalUrl(String str, String str2, boolean z, boolean z2, int i) {
        RelayRecordKey instanceWithCorrectHttps = RelayRecordKey.getInstanceWithCorrectHttps(str, z);
        String realURL = getRealURL(str2, z);
        try {
            if (getConnectivity(instanceWithCorrectHttps) == 6) {
                URL url = new URL(str2);
                String str3 = String.format(Locale.US, "%s.%s.%s", str, getRelayRecord(instanceWithCorrectHttps).getRelayRegion(), getControlHostDomain(instanceWithCorrectHttps));
                realURL = new URL(url.getProtocol(), str3, "direct" + url.getFile()).toExternalForm();
                QuickConnectUtil.log("Compose external url with hole punch : " + str3);
            } else if (getConnectivity(instanceWithCorrectHttps) != 7) {
                URL url2 = new URL(realURL);
                boolean z3 = url2.getProtocol().equals("https") && z2;
                String protocol = url2.getProtocol();
                int port = url2.getPort();
                if (z3) {
                    protocol = "http";
                } else {
                    i = port;
                }
                realURL = new URL(protocol, url2.getHost(), i, url2.getFile()).toExternalForm();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return realURL;
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    public static int getServiceTypeFromID(String str) {
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case -1704112856:
                if (str.equals(ServiceId.BSM_HTTPS)) {
                    c = 0;
                    break;
                }
                break;
            case -1674662561:
                if (str.equals(ServiceId.CLOUDSTATION)) {
                    c = 1;
                    break;
                }
                break;
            case -1024054486:
                if (str.equals(ServiceId.DSM_HTTPS)) {
                    c = 2;
                    break;
                }
                break;
            case -998820812:
                if (str.equals(ServiceId.BEEDRIVE_HTTPS)) {
                    c = 3;
                    break;
                }
                break;
            case -948646718:
                if (str.equals(ServiceId.WEBDAV_HTTP)) {
                    c = 4;
                    break;
                }
                break;
            case -814961990:
                if (str.equals(ServiceId.WEBDAV_HTTPS)) {
                    c = 5;
                    break;
                }
                break;
            case -706114198:
                if (str.equals(ServiceId.SECURESIGNIN)) {
                    c = 6;
                    break;
                }
                break;
            case -507932043:
                if (str.equals(ServiceId.PHOTO_HTTP)) {
                    c = 7;
                    break;
                }
                break;
            case 99774:
                if (str.equals(ServiceId.DSM)) {
                    c = '\b';
                    break;
                }
                break;
            case 99617003:
                if (str.equals("https")) {
                    c = '\t';
                    break;
                }
                break;
            case 774852418:
                if (str.equals(ServiceId.AUDIO_HTTPS)) {
                    c = '\n';
                    break;
                }
                break;
            case 1191954603:
                if (str.equals(ServiceId.BSM_HTTP)) {
                    c = 11;
                    break;
                }
                break;
            case 1433975966:
                if (str.equals(ServiceId.PHOTO_HTTPS)) {
                    c = '\f';
                    break;
                }
                break;
            case 1549015889:
                if (str.equals(ServiceId.AUDIO_HTTP)) {
                    c = CharUtils.CR;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return 11;
            case 1:
                return 7;
            case 2:
            case 6:
            case '\t':
            case '\n':
                return 2;
            case 3:
                return 9;
            case 4:
                return 5;
            case 5:
                return 6;
            case 7:
                return 3;
            case '\b':
            case '\r':
                return 1;
            case 11:
                return 10;
            case '\f':
                return 4;
            default:
                return 0;
        }
    }

    public static boolean isValidPingPong(String str, PingPongVo pingPongVo) throws NoSuchAlgorithmException {
        if (pingPongVo == null || !pingPongVo.isSuccess()) {
            return false;
        }
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "serverId2 is empty");
            return true;
        }
        String mD5Hash = HashUtils.getMD5Hash(str);
        String ezId = pingPongVo.getEzId();
        String str2 = TAG;
        Log.d(str2, "serverId2: " + str);
        Log.d(str2, "hash: " + mD5Hash + ", ezid: " + ezId);
        return TextUtils.equals(mD5Hash, ezId);
    }

    public static String getControlHostDomain(RelayRecordKey relayRecordKey) {
        RelayRecord relayRecord;
        if (!isQuickConnectId(relayRecordKey.getServerId()) || (relayRecord = getRelayRecord(relayRecordKey)) == null) {
            return "QuickConnect.to";
        }
        String lowerCase = relayRecord.getControlHost().toLowerCase(Locale.ENGLISH);
        if (lowerCase.matches(".+\\.(quickconnect\\..+)")) {
            return lowerCase.replaceAll(".+\\.(quickconnect\\..+)", "$1");
        }
        return isRegionCn(relayRecord.getRelayRegion()) ? "Quickconnect.cn" : "Quickconnect.to";
    }

    public static boolean isRegionCn(String str) {
        return !TextUtils.isEmpty(str) && str.startsWith("cn") && "cn".equalsIgnoreCase(str.replaceAll("\\d", ""));
    }

    private static boolean hasPrefsFile(Context context, String str) {
        return new File(new File(context.getFilesDir().getParent(), "shared_prefs"), str + ".xml").isFile();
    }

    private static synchronized boolean deletePrefsFile(Context context, String str) {
        context.getSharedPreferences(str, 0).edit().clear().commit();
        return context.deleteSharedPreferences(str);
    }
}