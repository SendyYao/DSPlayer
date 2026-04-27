package com.synology.sylib.syhttp3.factory;

import com.synology.sylib.syhttp3.SyHttpClient;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

/* loaded from: classes2.dex */
public class OkHttpClientFactory {
    private static OkHttpCallBack okHttpCallback;
    private static SyHttpCreatedCallBack syHttpCreatedCallback;
    private static final Object lock = new Object();
    private static final Object syLock = new Object();

    public interface OkHttpCallBack {
        void onCreate(String str, OkHttpClient.Builder builder);
    }

    public interface OkHttpCallBackFactory {
        OkHttpCallBack createOkHttpCallback(OkHttpCallBack okHttpCallBack);
    }

    public interface SyHttpCreatedCallBack {
        void onCreated(SyHttpClient syHttpClient);
    }

    public static class TimeoutInfo {
        public static final TimeoutInfo DEFAULT = new TimeoutInfo(0, TimeUnit.NANOSECONDS);
        public final long timeout;
        public final TimeUnit unit;

        public TimeoutInfo(long j, TimeUnit timeUnit) {
            this.timeout = j;
            this.unit = timeUnit;
        }
    }

    public static void setOkHttpCallback(OkHttpCallBackFactory okHttpCallBackFactory) {
        synchronized (lock) {
            okHttpCallback = okHttpCallBackFactory.createOkHttpCallback(okHttpCallback);
        }
    }

    public static void setSyHttpCreatedCallback(SyHttpCreatedCallBack syHttpCreatedCallBack) {
        synchronized (syLock) {
            syHttpCreatedCallback = syHttpCreatedCallBack;
        }
    }

    public static void onSyHttpCreated(SyHttpClient syHttpClient) {
        SyHttpCreatedCallBack syHttpCreatedCallBack;
        synchronized (syLock) {
            syHttpCreatedCallBack = syHttpCreatedCallback;
        }
        if (syHttpCreatedCallBack != null) {
            syHttpCreatedCallBack.onCreated(syHttpClient);
        }
    }

    public static OkHttpClient.Builder newOkHttpClientBuilder(String str) {
        return internalNewOkHttpClientBuilder(str, TimeoutInfo.DEFAULT, TimeoutInfo.DEFAULT);
    }

    public static OkHttpClient.Builder newOkHttpClientBuilder(String str, long j, long j2) {
        return internalNewOkHttpClientBuilder(str, new TimeoutInfo(j, TimeUnit.SECONDS), new TimeoutInfo(j2, TimeUnit.SECONDS));
    }

    public static OkHttpClient.Builder newOkHttpClientBuilder(String str, TimeoutInfo timeoutInfo, TimeoutInfo timeoutInfo2) {
        return internalNewOkHttpClientBuilder(str, timeoutInfo, timeoutInfo2);
    }

    private static OkHttpClient.Builder internalNewOkHttpClientBuilder(String str, TimeoutInfo timeoutInfo, TimeoutInfo timeoutInfo2) {
        OkHttpCallBack okHttpCallBack;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (timeoutInfo2 != TimeoutInfo.DEFAULT) {
            builder.readTimeout(timeoutInfo2.timeout, timeoutInfo2.unit);
        }
        if (timeoutInfo != TimeoutInfo.DEFAULT) {
            builder.connectTimeout(timeoutInfo.timeout, timeoutInfo.unit);
        }
        synchronized (lock) {
            okHttpCallBack = okHttpCallback;
        }
        if (okHttpCallBack != null) {
            okHttpCallBack.onCreate(str, builder);
        }
        return builder;
    }
}