package com.synology.sylib.syhttp3.util;

import android.util.Log;
import com.synology.sylib.syhttp3.TrustAllCertsManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import okhttp3.OkHttpClient;

/* loaded from: classes2.dex */
public class OkHttpClientUtil {
    private static final String TAG = "OkHttpClientUtil";
    private static HostnameVerifier sByPassHostnameVerifier = new HostnameVerifier() { // from class: com.synology.sylib.syhttp3.util.OkHttpClientUtil.1
        @Override // javax.net.ssl.HostnameVerifier
        public boolean verify(String str, SSLSession sSLSession) {
            return true;
        }
    };

    public static void bypassVerifyCertificate(OkHttpClient.Builder builder) {
        try {
            TrustAllCertsManager trustAllCertsManager = TrustAllCertsManager.getInstance();
            SSLContext sSLContext = SSLContext.getInstance("TLS");
            sSLContext.init(null, new TrustManager[]{trustAllCertsManager}, new SecureRandom());
            SSLSocketFactory socketFactory = sSLContext.getSocketFactory();
            builder.hostnameVerifier(sByPassHostnameVerifier);
            builder.sslSocketFactory(socketFactory, trustAllCertsManager);
        } catch (KeyManagementException e) {
            Log.e(TAG, "KeyManagementException: ", e);
        } catch (NoSuchAlgorithmException e2) {
            Log.e(TAG, "NoSuchAlgorithmException: ", e2);
        }
    }
}