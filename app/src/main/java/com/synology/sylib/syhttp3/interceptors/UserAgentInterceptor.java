package com.synology.sylib.syhttp3.interceptors;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import com.google.common.net.HttpHeaders;
import com.synology.sylib.syhttp3.SyHttpClient;
import java.io.IOException;
import java.util.Locale;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

/* loaded from: classes2.dex */
public class UserAgentInterceptor implements Interceptor {
    public static final String METADATA_USER_AGENT_NAME = "userAgentName";
    private static final String TAG = "UserAgentInterceptor";
    private String customUserAgent;

    public UserAgentInterceptor() {
        this(null);
    }

    public UserAgentInterceptor(String str) {
        this.customUserAgent = str;
    }

    @Override // okhttp3.Interceptor
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        try {
            Context context = SyHttpClient.getContext();
            if (context == null) {
                throw new IllegalStateException("geContext() == null, call SyHttpClient.setContext(Context) first");
            }
            String humanReadableAscii = this.customUserAgent;
            if (humanReadableAscii == null) {
                humanReadableAscii = toHumanReadableAscii(String.format("Synology-%s_%s_%s_%s_(%s)", getAppName(context), getAppVersion(context), getModel(), getOSVersion(), System.getProperty("http.agent")));
            }
            return chain.proceed(request.newBuilder().removeHeader(HttpHeaders.USER_AGENT).addHeader(HttpHeaders.USER_AGENT, humanReadableAscii).build());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return chain.proceed(request);
        }
    }

    private String getAppName(Context context) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        return packageManager.getApplicationInfo(packageName, 128).metaData.getString(METADATA_USER_AGENT_NAME, packageName);
    }

    private String getAppVersion(Context context) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return String.format(Locale.ENGLISH, "%s_rv:%03d", packageInfo.versionName, Integer.valueOf(packageInfo.versionCode));
    }

    private String getModel() {
        return Build.MODEL;
    }

    private String getOSVersion() {
        return "Android_" + Build.VERSION.SDK_INT;
    }

    public static String toHumanReadableAscii(String str) {
        int length = str.length();
        int iCharCount = 0;
        while (iCharCount < length) {
            int iCodePointAt = str.codePointAt(iCharCount);
            if (iCodePointAt > 31 && iCodePointAt < 127) {
                iCharCount += Character.charCount(iCodePointAt);
            } else {
                Buffer buffer = new Buffer();
                buffer.writeUtf8(str, 0, iCharCount);
                while (iCharCount < length) {
                    int iCodePointAt2 = str.codePointAt(iCharCount);
                    buffer.writeUtf8CodePoint((iCodePointAt2 <= 31 || iCodePointAt2 >= 127) ? 63 : iCodePointAt2);
                    iCharCount += Character.charCount(iCodePointAt2);
                }
                return buffer.readUtf8();
            }
        }
        return str;
    }
}