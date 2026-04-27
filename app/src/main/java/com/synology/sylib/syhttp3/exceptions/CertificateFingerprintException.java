package com.synology.sylib.syhttp3.exceptions;

import java.security.cert.CertificateException;
import javax.net.ssl.SSLHandshakeException;

/* loaded from: classes2.dex */
public class CertificateFingerprintException extends SSLHandshakeException {
    private final String mExpectedFingerprint;
    private final String mHostname;
    private final String mReceivedFingerprint;

    public CertificateFingerprintException(String str, String str2, String str3) {
        super("Fingerprint not match");
        initCause(new CertificateException());
        this.mHostname = str;
        this.mExpectedFingerprint = str2;
        this.mReceivedFingerprint = str3;
    }

    public String getHostname() {
        return this.mHostname;
    }

    public String getExpectedFingerprint() {
        return this.mExpectedFingerprint;
    }

    public String getReceivedFingerprint() {
        return this.mReceivedFingerprint;
    }
}