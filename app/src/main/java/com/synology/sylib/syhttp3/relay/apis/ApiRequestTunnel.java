package com.synology.sylib.syhttp3.relay.apis;

import android.os.Build;
import android.text.TextUtils;
import androidx.webkit.internal.AssetHelper;
import com.google.gson.JsonObject;
import com.synology.sylib.syhttp3.exceptions.IQcServerSSLException;
import com.synology.sylib.syhttp3.exceptions.QcsSSLHandshakeException;
import com.synology.sylib.syhttp3.exceptions.QcsSSLPeerUnverifiedException;
import com.synology.sylib.syhttp3.exceptions.UnexpectedJsonException;
import com.synology.sylib.syhttp3.factory.OkHttpClientFactory;
import com.synology.sylib.syhttp3.relay.RelayException;
import com.synology.sylib.syhttp3.relay.RelayResult;
import com.synology.sylib.syhttp3.relay.ServiceId;
import com.synology.sylib.syhttp3.relay.models.DSMInfo;
import com.synology.sylib.syhttp3.relay.models.ServiceInfo;
import com.synology.sylib.syhttp3.relay.util.SafeGson;
import com.synology.sylib.syhttp3.relay.vos.ServerVo;
import com.synology.sylib.syhttp3.relay.vos.ServiceVo;
import com.synology.sylib.syhttp3.relay.vos.TunnelVo;
import com.synology.sylib.util.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/* loaded from: classes2.dex */
public class ApiRequestTunnel {
    private static final String OKHTTP_CLIENT_TAG = "ApiRequestTunnel";
    private static final String REQUEST_TUNNEL = "request_tunnel";
    private static final String SZ_COMMAND = "command";
    private static final String SZ_LOCATION = "location";
    private static final String SZ_PLATFORM = "platform";
    private static final String SZ_SERVER_ID = "serverID";
    private static final String SZ_SERVICE_ID = "id";
    private static final String SZ_VERSION = "version";
    private static final String TAG = "ApiRequestTunnel";
    private static final int VERSION = 1;
    private final String mLocation;
    private final String mRelayServer;
    private final String mServerId;
    private final String mServiceId;
    private final boolean mUseSmartDns;
    private final SafeGson mSafeGson = new SafeGson();
    private OkHttpClient mHttpClient = OkHttpClientFactory.newOkHttpClientBuilder("ApiRequestTunnel", 30, 30).build();

    public ApiRequestTunnel(String str, String str2, String str3, String str4, boolean z) {
        this.mRelayServer = str;
        this.mServerId = str2;
        this.mServiceId = str3;
        this.mLocation = str4;
        this.mUseSmartDns = z;
    }

    public RelayResult call() throws IOException {
        try {
            return doCallAction();
        } catch (SSLHandshakeException e) {
            throw new QcsSSLHandshakeException(IQcServerSSLException.Type.RelayServer, e);
        } catch (SSLPeerUnverifiedException e2) {
            throw new QcsSSLPeerUnverifiedException(IQcServerSSLException.Type.RelayServer, e2);
        }
    }

    private RelayResult doCallAction() throws IOException {
        if (TextUtils.isEmpty(this.mRelayServer)) {
            throw new IllegalArgumentException("relayServer is empty");
        }
        if (TextUtils.isEmpty(this.mServerId)) {
            throw new IllegalArgumentException("serverId is empty");
        }
        if (TextUtils.isEmpty(this.mServiceId)) {
            throw new IllegalArgumentException("serviceId is empty");
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", (Number) 1);
        jsonObject.addProperty(SZ_COMMAND, REQUEST_TUNNEL);
        jsonObject.addProperty(SZ_SERVER_ID, this.mServerId);
        jsonObject.addProperty("id", this.mServiceId);
        jsonObject.addProperty("location", this.mLocation);
        jsonObject.addProperty(SZ_PLATFORM, "Android " + Build.VERSION.RELEASE);
        InputStream inputStreamByteStream = this.mHttpClient.newCall(new Request.Builder().url("https://" + this.mRelayServer + "/Serv.php").post(RequestBody.create(MediaType.parse(AssetHelper.DEFAULT_MIME_TYPE), this.mSafeGson.toJson(jsonObject))).build()).execute().body().byteStream();
        try {
            try {
                ServiceInfo serviceInfo = parse((TunnelVo) this.mSafeGson.fromJson(inputStreamByteStream, TunnelVo.class, StandardCharsets.UTF_8), this.mServiceId).getServiceInfo();
                if (serviceInfo == null) {
                    throw new IOException("serviceInfo == null");
                }
                if (TextUtils.isEmpty(serviceInfo.getRelayAddress(this.mUseSmartDns))) {
                    throw new IOException("relayAddress is empty");
                }
                if (serviceInfo.getRelayPort() <= 0) {
                    throw new IOException("relay port <= 0");
                }
                return new RelayResult(new URL(ServiceId.getProtocol(this.mServiceId), serviceInfo.getRelayAddress(this.mUseSmartDns), serviceInfo.getRelayPort(), ""), 7);
            } catch (UnexpectedJsonException e) {
                throw new RelayException(101, e);
            }
        } finally {
            IOUtils.closeSilently(inputStreamByteStream);
        }
    }

    private DSMInfo parse(TunnelVo tunnelVo, String str) throws IOException {
        if (tunnelVo == null) {
            throw new IOException("empty value object(TunnelVo)");
        }
        if (tunnelVo.getErrno() != 0) {
            throw new RelayException(tunnelVo.getErrno());
        }
        ServerVo server = tunnelVo.getServer();
        if (server == null) {
            throw new IOException("empty value object(ServerVo)");
        }
        ServiceVo service = tunnelVo.getService();
        if (service == null) {
            throw new IOException("empty value object(ServiceVo)");
        }
        DSMInfo dSMInfo = new DSMInfo();
        String str2 = this.mServerId;
        DSMInfo.parseServerInfo(str2, str2, server, tunnelVo.getSmartDnsVo(), dSMInfo);
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setId(service.getId());
        serviceInfo.setPort(service.getPort());
        serviceInfo.setExtPort(service.getExtPort());
        serviceInfo.setRelayIP(service.getRelayIP());
        serviceInfo.setRelayIPV6(service.getRelayIPV6());
        serviceInfo.setRelayPort(service.getRelayPort());
        serviceInfo.setRelayDualStack(service.getRelayDualStack());
        serviceInfo.setRelayDn(service.getRelayDn());
        dSMInfo.setServiceInfo(serviceInfo);
        return dSMInfo;
    }
}