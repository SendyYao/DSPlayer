package com.synology.sylib.syhttp3.interceptors;

import android.content.Context;
import android.util.Log;
import com.synology.sylib.syhttp3.SyHttpClient;
import com.synology.sylib.syhttp3.TrustManagerProvider;
import com.synology.sylib.syhttp3.VerifyCertsManager;
import com.synology.sylib.syhttp3.relay.RelayManager;
import com.synology.sylib.syhttp3.relay.RelayRecord;
import com.synology.sylib.syhttp3.relay.RelayRecordKey;
import com.synology.sylib.syhttp3.relay.RelayResolveManager;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.conn.ssl.StrictHostnameVerifier;

/* loaded from: classes2.dex */
public class RelayInterceptor implements Interceptor {
    public static final String SYNO_REQUEST_HOST = "SYNO-REQUEST-HOST";
    private static final String TAG = "RelayInterceptor";
    private final SyHttpClient mClient;
    private String mCurrentHost;
    private boolean mVerifyCertificate = true;
    private final RelayManager mRelayManager = RelayManager.getInstance();
    private final RelayResolveManager mRelayResolveManager = RelayResolveManager.getInstance();

    public RelayInterceptor(SyHttpClient syHttpClient) {
        this.mClient = syHttpClient;
    }

    public boolean isVerifyCertificate() {
        return this.mVerifyCertificate;
    }

    public void setVerifyCertificate(boolean z) {
        this.mVerifyCertificate = z;
        try {
            verifyCertificate(z);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

    }

    @Override // okhttp3.Interceptor
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        URL url = request.url().url();
        String host = url.getHost();
        if (RelayUtil.isQuickConnectId(host)) {
            Context context = getContext();
            if (context == null) {
                throw new IllegalStateException("geContext() == null, call SyHttpClient.setContext(Context) first");
            }
            boolean zEqualsIgnoreCase = url.getProtocol().equalsIgnoreCase("https");
            RelayRecord resolvedRecord = null;
            try {
                resolvedRecord = this.mRelayResolveManager.getResolvedRecord(context, host, zEqualsIgnoreCase);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (resolvedRecord.getRealURL() == null) {
                resolvedRecord = this.mRelayManager.fetchRealURL(RelayRecordKey.getInstance(context, host, zEqualsIgnoreCase));
                if (resolvedRecord.getConnectivity() == 7) {
                    try {
                        verifyCertificate(false);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    } catch (KeyManagementException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            URL realURL = resolvedRecord.getRealURL();
            request = request.newBuilder().header(SYNO_REQUEST_HOST, host).url(new URL(realURL.getProtocol(), realURL.getHost(), realURL.getPort(), url.getFile())).build();
        }
        try {
            updateCurrentHost(host);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        return chain.proceed(request);
    }

    private Context getContext() {
        return SyHttpClient.getContext();
    }

    private void updateCurrentHost(String str) throws NoSuchAlgorithmException, KeyStoreException {
        if (str.equals(this.mCurrentHost)) {
            return;
        }
        this.mCurrentHost = str;
        VerifyCertsManager.getInstance(true).setCurrentHost(this.mCurrentHost);
        VerifyCertsManager.getInstance(false).setCurrentHost(this.mCurrentHost);
    }

    private void verifyCertificate(boolean z) throws NoSuchAlgorithmException, KeyManagementException {
        try {
            SSLContext sSLContext = SSLContext.getInstance("TLS");
            VerifyCertsManager verifyCertsManager = VerifyCertsManager.getInstance(z);
            verifyCertsManager.setIsLegalCertificate(true);
            sSLContext.init(null, new TrustManager[]{verifyCertsManager}, new SecureRandom());
            if (z) {
                this.mClient.setHostnameVerifier(new StrictHostnameVerifier());
            } else {
                this.mClient.setHostnameVerifier((str, sSLSession) -> true);
            }
            SSLSocketFactory socketFactory = sSLContext.getSocketFactory();
            this.mClient.setSslSocketFactory(socketFactory, TrustManagerProvider.tryToGetX509TrustManager(socketFactory));
        } catch (KeyManagementException e) {
            String message = e.getMessage();
            Log.e(TAG, "KeyManagementException: " + (message != null ? message : ""));
        } catch (NoSuchAlgorithmException e2) {
            String message2 = e2.getMessage();
            Log.e(TAG, "NoSuchAlgorithmException: " + (message2 != null ? message2 : ""));
        } catch (NoSuchFieldException | IllegalAccessException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

}