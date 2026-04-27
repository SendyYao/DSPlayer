package com.synology.sylib.syhttp3.relay.ping;

import com.google.gson.Gson;
import com.synology.sylib.syhttp3.factory.OkHttpClientFactory;
import com.synology.sylib.syhttp3.util.OkHttpClientUtil;
import okhttp3.OkHttpClient;

/* loaded from: classes2.dex */
public class DefaultServicePingBuilder implements ServicePingBuilder {
    private static final int CONNECT_TIMEOUT = 5;
    private static final int HOLE_PUNCH_IDLE_TIMEOUT = 600;
    private static final int HOLE_PUNCH_TIMEOUT = 5000;
    private static final String OKHTTP_CLIENT_TAG = "DefaultServicePingBuilder";
    private static final int READ_TIMEOUT = 5;
    private static final String TAG = "DefaultServicePingBuilder";
    private Gson mGson;
    private OkHttpClient mHttpClient;

    public DefaultServicePingBuilder() {
        OkHttpClient.Builder builderNewOkHttpClientBuilder = OkHttpClientFactory.newOkHttpClientBuilder("DefaultServicePingBuilder", 5L, 5L);
        OkHttpClientUtil.bypassVerifyCertificate(builderNewOkHttpClientBuilder);
        this.mHttpClient = builderNewOkHttpClientBuilder.build();
        this.mGson = new Gson();
    }

    @Override // com.synology.sylib.syhttp3.relay.ping.ServicePingBuilder
    public ServicePing generateServicePinger(String str, String str2, String str3) {
        return new PingPongServicePing(this.mHttpClient, this.mGson, str, str2, str3);
    }
}