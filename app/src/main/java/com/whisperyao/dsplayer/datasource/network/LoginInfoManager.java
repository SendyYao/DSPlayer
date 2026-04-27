package com.whisperyao.dsplayer.datasource.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.whisperyao.dsplayer.BuildConfig;
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext;
import com.whisperyao.dsplayer.util.DataKeyStoreHelper;
import com.synology.sylib.syhttp3.relay.RelayRecord;
import com.synology.sylib.syhttp3.relay.RelayRecordKey;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Named;
import kotlin.jvm.internal.Intrinsics;
import okhttp3.HttpUrl;

public final class LoginInfoManager {
    public static final String PREF_ACCOUNT = "account";
    private static final String PREF_EXT_HOST_QUICONNECT = "ext_host_quickconnect";
    private static final String PREF_HOST = "host";
    public static final String PREF_IS_HTTPS = "isHttps";
    private static final String PREF_IS_LINKED = "isLinked";
    public static final String PREF_NAME = "cm_pref";
    private static final String PREF_PASSWORD = "password";
    private static final String PREF_PORT = "port";
    public static final String PREF_USER_INPUT_ADDRESS = "user_input_address";
    private String account;
    private final Context context;
    private final DataKeyStoreHelper dataKeyStoreHelper;
    private String externalHost;
    private HttpUrl httpUrl;
    private boolean isLinked;
    private boolean isPortalPort;
    private String otpCode;
    private String password;
    private final SharedPreferences sharedPreferences;
    private boolean trustDevice;
    private String userInputAddress;

    @Inject
    public LoginInfoManager(@ApplicationContext Context context, @Named(PREF_NAME) SharedPreferences sharedPreferences, DataKeyStoreHelper dataKeyStoreHelper) {
        HttpUrl.Builder builderNewBuilder;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(sharedPreferences, "sharedPreferences");
        Intrinsics.checkNotNullParameter(dataKeyStoreHelper, "dataKeyStoreHelper");
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.dataKeyStoreHelper = dataKeyStoreHelper;
        String string = sharedPreferences.getString(PREF_USER_INPUT_ADDRESS, "");
        this.userInputAddress = string == null ? BuildConfig.NAS_ADDRESS : string;
        String string2 = sharedPreferences.getString("account", "");
        this.account = string2 == null ? BuildConfig.NAS_ACCOUNT : string2;
        String string3 = sharedPreferences.getString(PREF_EXT_HOST_QUICONNECT, "");
        this.externalHost = string3 == null ? "" : string3;
        this.isLinked = sharedPreferences.getBoolean(PREF_IS_LINKED, false);
        String string4 = sharedPreferences.getString("password", "");
        string4 = string4 == null ? "" : string4;
        String strDecodeData = dataKeyStoreHelper.decodeData(string4);
        this.password = strDecodeData != null ? strDecodeData : string4;
        String string5 = sharedPreferences.getString(PREF_HOST, "");
        String str = string5 != null ? string5 : "";
        String str2 = sharedPreferences.getBoolean("isHttps", false) ? "https" : "http";
        int i = sharedPreferences.getInt(PREF_PORT, 5000);
        if (TextUtils.isEmpty(str)) {
            return;
        }
        HttpUrl httpUrl = HttpUrl.parse(str);
        this.httpUrl = ((httpUrl == null || (builderNewBuilder = httpUrl.newBuilder()) == null) ? new HttpUrl.Builder().host(str) : builderNewBuilder).scheme(str2).port(i).build();
    }

    public final Context getContext() {
        return this.context;
    }

    public final SharedPreferences getSharedPreferences() {
        return this.sharedPreferences;
    }

    /* renamed from: isLinked, reason: from getter */
    public final boolean getIsLinked() {
        return this.isLinked;
    }

    public final void setLinked(boolean z) {
        this.isLinked = z;
    }

    public final void setHttpUrl(HttpUrl httpUrl) {
        this.httpUrl = httpUrl;
    }

    public final HttpUrl getHttpUrl() {
        HttpUrl httpUrl = this.httpUrl;
        return httpUrl == null ? HttpUrl.parse("") : httpUrl;
    }

    public final String getUserInputAddress() {
        return this.userInputAddress;
    }

    public final void setUserInputAddress(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.userInputAddress = str;
    }

    public final String getAccount() {
        return this.account;
    }

    public final void setAccount(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.account = str;
    }

    public final String getPassword() {
        return this.password;
    }

    public final void setPassword(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.password = str;
    }

    public final String getOtpCode() {
        return this.otpCode;
    }

    public final void setOtpCode(String str) {
        this.otpCode = str;
    }

    public final boolean getTrustDevice() {
        return this.trustDevice;
    }

    public final void setTrustDevice(boolean z) {
        this.trustDevice = z;
    }

    /* renamed from: isPortalPort$app_globalRelease, reason: from getter */
    public final boolean getIsPortalPort() {
        return this.isPortalPort;
    }

    public final void setPortalPort$app_globalRelease(boolean z) {
        this.isPortalPort = z;
    }

    public final String getExternalHost() {
        return this.externalHost;
    }

    public final void setExternalHost(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.externalHost = str;
    }

    public final void storeData$app_globalRelease() {
        this.sharedPreferences.edit().putBoolean(PREF_IS_LINKED, this.isLinked).commit();
        SharedPreferences.Editor editorPutString = this.sharedPreferences.edit().putString(PREF_HOST, String.valueOf(getHttpUrl()));
        HttpUrl httpUrl = getHttpUrl();
        SharedPreferences.Editor editorPutInt = editorPutString.putInt(PREF_PORT, httpUrl != null ? httpUrl.port() : 5000);
        HttpUrl httpUrl2 = getHttpUrl();
        editorPutInt.putBoolean("isHttps", httpUrl2 != null ? httpUrl2.isHttps() : false).putString(PREF_USER_INPUT_ADDRESS, this.userInputAddress).putString("account", this.account).putString("password", this.dataKeyStoreHelper.encodeData(this.password)).putString(PREF_EXT_HOST_QUICONNECT, this.externalHost).apply();
    }

    public final String getPrefix() {
        URI uri;
        HttpUrl httpUrl = getHttpUrl();
        String string = (httpUrl == null || (uri = httpUrl.uri()) == null) ? null : uri.toString();
        return string == null ? "" : string;
    }

    public final boolean isHttps() {
        HttpUrl httpUrl = getHttpUrl();
        if (httpUrl != null) {
            return httpUrl.isHttps();
        }
        return false;
    }

    public final boolean isTunnel() {
        return relayConnectivity() == 7;
    }

    public final boolean isHolePunch() {
        return relayConnectivity() == 6;
    }

    private final int relayConnectivity() {
        RelayRecord relayRecord;
        HttpUrl httpUrl = getHttpUrl();
        if (httpUrl == null || (relayRecord = RelayUtil.getRelayRecord(RelayRecordKey.getInstanceWithCorrectHttps(httpUrl.host(), isHttps()))) == null) {
            return 0;
        }
        return relayRecord.getConnectivity();
    }

    public final void clearData() {
        this.isLinked = false;
        this.sharedPreferences.edit().remove(PREF_IS_LINKED).remove("password").commit();
    }
}