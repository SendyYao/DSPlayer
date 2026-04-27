package com.synology.sylib.syhttp3.interceptors;

import android.text.TextUtils;
import com.synology.sylib.syhttp3.VerifyCertsManager;
import com.synology.sylib.syhttp3.exceptions.CertificateFingerprintException;
import com.synology.sylib.syhttp3.util.CertificateUtil;
import com.synology.sylib.syhttp3.util.StringUtil;
import com.synology.sylib.util.IOUtils;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/* loaded from: classes2.dex */
public class CertificateInterceptor implements Interceptor {
    private static final String TAG = "CertificateInterceptor";
    private boolean mVerifyFingerprint = true;

    public boolean isVerifyFingerprint() {
        return this.mVerifyFingerprint;
    }

    public void setVerifyFingerprint(boolean z) {
        this.mVerifyFingerprint = z;
    }

    @Override // okhttp3.Interceptor
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Connection connection;
        Request request = chain.request();
        if (!this.mVerifyFingerprint || !request.isHttps()) {
            return chain.proceed(request);
        }
        String strHeader = request.header(RelayInterceptor.SYNO_REQUEST_HOST);
        if (TextUtils.isEmpty(strHeader)) {
            strHeader = request.url().url().getHost();
        }
        if (!TextUtils.isEmpty(strHeader) && (connection = chain.connection()) != null) {
            List<Certificate> listPeerCertificates = connection.handshake().peerCertificates();
            if (listPeerCertificates.size() > 0) {
                String fingerprint = CertificateUtil.getFingerprint(strHeader);
                X509Certificate x509Certificate = (X509Certificate) listPeerCertificates.get(0);
                String certificateSHA1String = StringUtil.getCertificateSHA1String(x509Certificate);
                if (TextUtils.isEmpty(fingerprint)) {
                    CertificateUtil.putFingerprint(strHeader, certificateSHA1String);
                } else if (!TextUtils.equals(fingerprint, certificateSHA1String)) {
                    if (VerifyCertsManager.isLegalCertFingerPrint(strHeader, x509Certificate)) {
                        CertificateUtil.putFingerprint(strHeader, certificateSHA1String);
                    } else {
                        IOUtils.closeSilently(connection.socket());
                        throw new CertificateFingerprintException(strHeader, fingerprint, certificateSHA1String);
                    }
                }
            }
        }
        return chain.proceed(request);
    }
}