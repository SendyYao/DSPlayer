package com.synology.sylib.syhttp3.relay.ping;

import com.google.gson.Gson;
import com.synology.sylib.syhttp3.relay.RelayManager;
import com.synology.sylib.syhttp3.relay.RelayResult;
import com.synology.sylib.syhttp3.relay.util.SafeGson;
import com.synology.sylib.util.IOUtils;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/* loaded from: classes2.dex */
public class PingPongServicePing implements ServicePing {
    private OkHttpClient mHttpClient;
    private String mPingPath;
    private SafeGson mSafeGson;
    private String mServerId2;
    private String mServiceId;

    public PingPongServicePing(OkHttpClient okHttpClient, Gson gson, String str, String str2, String str3) {
        this.mHttpClient = okHttpClient;
        this.mSafeGson = new SafeGson(gson);
        this.mPingPath = str3;
        this.mServiceId = str;
        this.mServerId2 = str2;
    }

    @Override // com.synology.sylib.syhttp3.relay.ping.ServicePing
    public Callable<RelayResult> ping(String str, String str2, int i, int i2) {
        try {
            return newCallable(new URL(str, str2, i, this.mPingPath), i2);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Callable<RelayResult> newCallable(final URL url, final int i) {
        return new Callable<RelayResult>() { // from class: com.synology.sylib.syhttp3.relay.ping.PingPongServicePing.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // java.util.concurrent.Callable
            public RelayResult call() throws Exception {
                return PingPongServicePing.this.getRealURL(url, i);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public RelayResult getRealURL(URL url, int i) throws Exception {
        PingPongHandler pingPongHandler = RelayManager.getInstance().getPingPongHandler();
        InputStream inputStreamByteStream = this.mHttpClient.newCall(new Request.Builder().url(url).tag(Integer.valueOf(i)).build()).execute().body().byteStream();
        try {
            if (pingPongHandler.checkIfWanted(this.mServiceId, this.mServerId2, inputStreamByteStream)) {
                return new RelayResult(new URL(url.getProtocol(), url.getHost(), url.getPort(), ""), i);
            }
            IOUtils.closeSilently(inputStreamByteStream);
            return null;
        } finally {
            IOUtils.closeSilently(inputStreamByteStream);
        }
    }
}