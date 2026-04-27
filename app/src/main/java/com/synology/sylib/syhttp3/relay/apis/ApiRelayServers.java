package com.synology.sylib.syhttp3.relay.apis;

import androidx.webkit.internal.AssetHelper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synology.sylib.syhttp3.exceptions.IQcServerSSLException;
import com.synology.sylib.syhttp3.exceptions.QcsSSLHandshakeException;
import com.synology.sylib.syhttp3.exceptions.QcsSSLPeerUnverifiedException;
import com.synology.sylib.syhttp3.factory.OkHttpClientFactory;
import com.synology.sylib.syhttp3.relay.models.DSMInfo;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import java.io.IOException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/* loaded from: classes2.dex */
public class ApiRelayServers {
    private static final String GET_SERVER_INFO = "get_server_info";
    private static final String GET_SITE_LIST = "get_site_list";
    private static final String OKHTTP_CLIENT_TAG = "ApiRelayServers";
    private static final String SZ_CA_FINGERPRINTS = "get_ca_fingerprints";
    private static final String SZ_COMMAND = "command";
    private static final String SZ_SERVER_ID = "serverID";
    private static final String SZ_SERVICE_ID = "id";
    private static final String SZ_VERSION = "version";
    private static final String TAG = "ApiRelayServers";
    private static final int VERSION = 1;
    private final boolean mChina;
    private OkHttpClient mHttpClient = OkHttpClientFactory.newOkHttpClientBuilder("ApiRelayServers", 30, 30).build();
    private final String mServerId;
    private final String mServiceId;

    public ApiRelayServers(String str, String str2, boolean z) {
        this.mChina = z;
        this.mServerId = str;
        this.mServiceId = str2;
    }

    public DSMInfo call() throws IOException {
        try {
            return doCallAction();
        } catch (SSLHandshakeException e) {
            throw new QcsSSLHandshakeException(IQcServerSSLException.Type.GlobalControlServer, e);
        } catch (SSLPeerUnverifiedException e2) {
            throw new QcsSSLPeerUnverifiedException(IQcServerSSLException.Type.GlobalControlServer, e2);
        }
    }

    private DSMInfo doCallAction() throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", (Number) 1);
        jsonObject.addProperty(SZ_COMMAND, GET_SERVER_INFO);
        jsonObject.addProperty(SZ_SERVER_ID, this.mServerId);
        jsonObject.addProperty("id", this.mServiceId);
        jsonObject.addProperty(SZ_CA_FINGERPRINTS, (Boolean) true);
        return DSMInfo.parseServerInfo(this.mServerId, this.mServiceId, this.mHttpClient.newCall(new Request.Builder().url(getGlobalUrl()).post(RequestBody.create(MediaType.parse(AssetHelper.DEFAULT_MIME_TYPE), new Gson().toJson((JsonElement) jsonObject))).build()).execute().body().byteStream());
    }

    public boolean isCallChinaSite() {
        return this.mChina;
    }

    private String getGlobalUrl() {
        return (this.mChina ? RelayUtil.GLOBAL_SERVER_CN : RelayUtil.GLOBAL_SERVER_WW).concat("/Serv.php");
    }
}