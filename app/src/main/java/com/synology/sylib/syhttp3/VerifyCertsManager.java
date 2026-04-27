package com.synology.sylib.syhttp3;

import android.util.Log;
import com.synology.sylib.syhttp3.util.StringUtil;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/* loaded from: classes2.dex */
public class VerifyCertsManager implements X509TrustManager {
    private static final String TAG = "VerifyCertsManager";
    private static VerifyCertsManager sInstanceStrict;
    private static VerifyCertsManager sInstanceUnSafe;
    private static HashSet<String> sLegalCertFingerPrintSet = new HashSet<>();
    private String mCurrentHost;
    private boolean mIsLegalCertificate = true;
    private boolean mNeedToVerifyCertificate;
    private X509TrustManager mTrustManager;

    public static VerifyCertsManager getInstance(boolean z) throws NoSuchAlgorithmException, KeyStoreException {
        return z ? getStrictInstance() : getUnSafeInstance();
    }

    public static VerifyCertsManager getStrictInstance() throws NoSuchAlgorithmException, KeyStoreException {
        if (sInstanceStrict == null) {
            synchronized (VerifyCertsManager.class) {
                if (sInstanceStrict == null) {
                    sInstanceStrict = new VerifyCertsManager(true);
                }
            }
        }
        return sInstanceStrict;
    }

    public static VerifyCertsManager getUnSafeInstance() throws NoSuchAlgorithmException, KeyStoreException {
        if (sInstanceUnSafe == null) {
            synchronized (VerifyCertsManager.class) {
                if (sInstanceUnSafe == null) {
                    sInstanceUnSafe = new VerifyCertsManager(false);
                }
            }
        }
        return sInstanceUnSafe;
    }

    /* JADX WARN: Removed duplicated region for block: B:9:0x0013  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static synchronized boolean isLegalCertFingerPrint(java.lang.String r1, java.security.cert.X509Certificate r2) {
        /*
            java.lang.Class<com.synology.sylib.syhttp3.VerifyCertsManager> r0 = com.synology.sylib.syhttp3.VerifyCertsManager.class
            monitor-enter(r0)
            java.lang.String r1 = getLegalCertFingerprintKey(r1, r2)     // Catch: java.lang.Throwable -> L16
            if (r1 == 0) goto L13
            java.util.HashSet<java.lang.String> r2 = com.synology.sylib.syhttp3.VerifyCertsManager.sLegalCertFingerPrintSet     // Catch: java.lang.Throwable -> L16
            boolean r1 = r2.contains(r1)     // Catch: java.lang.Throwable -> L16
            if (r1 == 0) goto L13
            r1 = 1
            goto L14
        L13:
            r1 = 0
        L14:
            monitor-exit(r0)
            return r1
        L16:
            r1 = move-exception
            monitor-exit(r0)
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.synology.sylib.syhttp3.VerifyCertsManager.isLegalCertFingerPrint(java.lang.String, java.security.cert.X509Certificate):boolean");
    }

    private static synchronized void addLegalCertFingerPrint(String str, X509Certificate x509Certificate) {
        String legalCertFingerprintKey = getLegalCertFingerprintKey(str, x509Certificate);
        if (legalCertFingerprintKey != null) {
            sLegalCertFingerPrintSet.add(legalCertFingerprintKey);
        }
    }

    private static String getLegalCertFingerprintKey(String str, X509Certificate x509Certificate) {
        try {
            return str + " - " + StringUtil.getCertificateSHA1String(x509Certificate);
        } catch (IOException unused) {
            return null;
        }
    }

    private VerifyCertsManager(boolean z) throws NoSuchAlgorithmException, KeyStoreException {
        this.mTrustManager = null;
        this.mNeedToVerifyCertificate = z;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            this.mTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
        } catch (KeyStoreException e) {
            String message = e.getMessage();
            Log.e(TAG, "KeyStoreException: " + (message != null ? message : ""));
        } catch (NoSuchAlgorithmException e2) {
            String message2 = e2.getMessage();
            Log.e(TAG, "NoSuchAlgorithmException: " + (message2 != null ? message2 : ""));
        }
    }

    public void setIsLegalCertificate(Boolean bool) {
        this.mIsLegalCertificate = bool.booleanValue();
    }

    public boolean isLegalCertificate() {
        return this.mIsLegalCertificate;
    }

    @Override // javax.net.ssl.X509TrustManager
    public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
        X509TrustManager x509TrustManager = this.mTrustManager;
        if (x509TrustManager != null) {
            try {
                x509TrustManager.checkClientTrusted(x509CertificateArr, str);
            } catch (CertificateException e) {
                if (this.mNeedToVerifyCertificate) {
                    throw e;
                }
            }
        }
    }

    @Override // javax.net.ssl.X509TrustManager
    public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
        X509TrustManager x509TrustManager = this.mTrustManager;
        if (x509TrustManager != null) {
            try {
                x509TrustManager.checkServerTrusted(x509CertificateArr, str);
                for (X509Certificate x509Certificate : x509CertificateArr) {
                    addLegalCertFingerPrint(getCurrentHost(), x509Certificate);
                }
            } catch (CertificateException e) {
                this.mIsLegalCertificate = false;
                if (this.mNeedToVerifyCertificate) {
                    throw e;
                }
            }
        }
    }

    public synchronized void setCurrentHost(String str) {
        this.mCurrentHost = str;
    }

    private synchronized String getCurrentHost() {
        return this.mCurrentHost;
    }

    @Override // javax.net.ssl.X509TrustManager
    public X509Certificate[] getAcceptedIssuers() {
        X509TrustManager x509TrustManager = this.mTrustManager;
        return x509TrustManager != null ? x509TrustManager.getAcceptedIssuers() : new X509Certificate[0];
    }
}