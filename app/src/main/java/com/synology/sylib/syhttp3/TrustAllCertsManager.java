package com.synology.sylib.syhttp3;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/* loaded from: classes2.dex */
public class TrustAllCertsManager implements X509TrustManager {
    private static TrustAllCertsManager sInstance;

    @Override // javax.net.ssl.X509TrustManager
    public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
    }

    @Override // javax.net.ssl.X509TrustManager
    public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
    }

    public static TrustAllCertsManager getInstance() {
        if (sInstance == null) {
            synchronized (TrustAllCertsManager.class) {
                if (sInstance == null) {
                    sInstance = new TrustAllCertsManager();
                }
            }
        }
        return sInstance;
    }

    private TrustAllCertsManager() {
    }

    @Override // javax.net.ssl.X509TrustManager
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}