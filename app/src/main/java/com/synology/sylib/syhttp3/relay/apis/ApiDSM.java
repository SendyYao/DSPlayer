package com.synology.sylib.syhttp3.relay.apis;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.webkit.internal.AssetHelper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synology.sylib.syhttp3.exceptions.IQcServerSSLException;
import com.synology.sylib.syhttp3.exceptions.QcsSSLHandshakeException;
import com.synology.sylib.syhttp3.exceptions.QcsSSLPeerUnverifiedException;
import com.synology.sylib.syhttp3.factory.OkHttpClientFactory;
import com.synology.sylib.syhttp3.relay.RelayException;
import com.synology.sylib.syhttp3.relay.models.DSMInfo;
import com.synology.sylib.syhttp3.relay.utils.RelayExecutors;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/* loaded from: classes2.dex */
public class ApiDSM {
    private static final String GET_SERVER_INFO = "get_server_info";
    private static final String OKHTTP_CLIENT_TAG = "ApiDSM";
    private static final String SZ_CA_FINGERPRINTS = "get_ca_fingerprints";
    private static final String SZ_COMMAND = "command";
    private static final String SZ_CONNECT_ID = "id";
    private static final String SZ_SERVER_ID = "serverID";
    private static final String SZ_VERSION = "version";
    private static final String TAG = "ApiDSM";
    private static final int VERSION = 1;
    private Gson mGson = new Gson();
    private OkHttpClient mHttpClient = OkHttpClientFactory.newOkHttpClientBuilder(OKHTTP_CLIENT_TAG, 30, 30).build();
    private List<String> mRelayServers;
    private String mServerId;
    private String mServiceId;

    public void setRelayServers(List<String> list) {
        this.mRelayServers = list;
    }

    public void setServerId(String str) {
        this.mServerId = str;
    }

    public void setServiceId(String str) {
        this.mServiceId = str;
    }

    public Pair<String, DSMInfo> call() throws IOException {
        try {
            return doCallAction();
        } catch (SSLHandshakeException e) {
            throw new QcsSSLHandshakeException(IQcServerSSLException.Type.RegionalControlServer, e);
        } catch (SSLPeerUnverifiedException e2) {
            throw new QcsSSLPeerUnverifiedException(IQcServerSSLException.Type.RegionalControlServer, e2);
        }
    }

    private Pair<String, DSMInfo> doCallAction() throws IOException {
        Pair<String, DSMInfo> pair = null;
        List<String> list = this.mRelayServers;
        if (list == null || list.size() <= 0) {
            throw new IllegalArgumentException("relayServers is empty");
        }
        if (TextUtils.isEmpty(this.mServerId)) {
            throw new IllegalArgumentException("serverId is empty");
        }
        if (TextUtils.isEmpty(this.mServiceId)) {
            throw new IllegalArgumentException("serviceId is empty");
        }
        ExecutorService executorServiceNewFixedThreadPool = RelayExecutors.newFixedThreadPool(TAG, this.mRelayServers.size());
        try {
            ExecutorCompletionService executorCompletionService = getExecutorCompletionService(executorServiceNewFixedThreadPool);
            for (int i = 0; i < this.mRelayServers.size(); i++) {
                try {
                    try {
                        pair = (Pair) executorCompletionService.take().get();
                    } catch (InterruptedException e) {
                        String message = e.getMessage();
                        String str2 = TAG;
                        StringBuilder sbAppend = new StringBuilder().append("InterruptedException: ");
                        if (message == null) {
                            message = "";
                        }
                        Log.e(str2, sbAppend.append(message).toString());
                    }
                } catch (ExecutionException e2) {
                    String message2 = e2.getMessage();
                    String str3 = TAG;
                    StringBuilder sbAppend2 = new StringBuilder().append("ExecutionException: ");
                    if (message2 == null) {
                        message2 = "";
                    }
                    Log.e(str3, sbAppend2.append(message2).toString());
                }
                if (pair != null && pair.second != null) {
                    executorServiceNewFixedThreadPool.shutdownNow();
                    return pair;
                }
            }
            executorServiceNewFixedThreadPool.shutdownNow();
            return null;
        } finally {
            executorServiceNewFixedThreadPool.shutdownNow();
        }
    }

    @NonNull
    private ExecutorCompletionService getExecutorCompletionService(ExecutorService executorServiceNewFixedThreadPool) {
        ExecutorCompletionService executorCompletionService = new ExecutorCompletionService(executorServiceNewFixedThreadPool);
        for (final String str : this.mRelayServers) {
            executorCompletionService.submit(new Callable<Pair<String, DSMInfo>>() { // from class: com.synology.sylib.syhttp3.relay.apis.ApiDSM.1
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // java.util.concurrent.Callable
                public Pair<String, DSMInfo> call() throws Exception {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("version", (Number) 1);
                    jsonObject.addProperty(ApiDSM.SZ_COMMAND, ApiDSM.GET_SERVER_INFO);
                    jsonObject.addProperty(ApiDSM.SZ_SERVER_ID, ApiDSM.this.mServerId);
                    jsonObject.addProperty("id", ApiDSM.this.mServiceId);
                    jsonObject.addProperty(ApiDSM.SZ_CA_FINGERPRINTS, (Boolean) true);
                    try {
                        DSMInfo serverInfo = DSMInfo.parseServerInfo(ApiDSM.this.mServerId, ApiDSM.this.mServiceId, ApiDSM.this.mHttpClient.newCall(new Request.Builder().url("https://" + str + "/Serv.php").post(RequestBody.create(MediaType.parse(AssetHelper.DEFAULT_MIME_TYPE), ApiDSM.this.mGson.toJson((JsonElement) jsonObject))).build()).execute().body().byteStream());
                        if (serverInfo == null || serverInfo.getServerInfo() == null) {
                            return null;
                        }
                        return new Pair<>(str, serverInfo);
                    } catch (RelayException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            });
        }
        return executorCompletionService;
    }
}