package com.whisperyao.dsplayer.util;

import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.datasource.network.ConnectionManager;
import com.synology.sylib.syhttp3.SyHttpClient;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.Intrinsics;

public final class SyhttpInitializer {
    public static final SyhttpInitializer INSTANCE = new SyhttpInitializer();

    private SyhttpInitializer() {
    }

    @JvmStatic
    public static SyHttpClient generateClient(ConnectionManager connectionManager) {
        Intrinsics.checkNotNullParameter(connectionManager, "connectionManager");
        Common.setCookieStore(connectionManager.getCookieStore());
        SyHttpClient syHttpClient = new SyHttpClient();
        syHttpClient.setConnectTimeout(120000L, TimeUnit.MILLISECONDS);
        syHttpClient.setReadTimeout(120000L, TimeUnit.MILLISECONDS);
        connectionManager.getPreferenceManager().isVerifyCertification();
        syHttpClient.setVerifyCertificate(true);
        syHttpClient.setCookieHandler(new CookieManager(connectionManager.getCookieStore(), CookiePolicy.ACCEPT_ALL));
        FlipperUtils.INSTANCE.addDebugInterceptors(syHttpClient);
        return syHttpClient;
    }
}