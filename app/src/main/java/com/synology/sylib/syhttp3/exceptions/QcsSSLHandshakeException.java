package com.synology.sylib.syhttp3.exceptions;

import javax.net.ssl.SSLHandshakeException;

/* loaded from: classes2.dex */
public class QcsSSLHandshakeException extends SSLHandshakeException implements IQcServerSSLException {
    private final SSLHandshakeException mOriginalException;
    private final IQcServerSSLException.Type mType;

    public QcsSSLHandshakeException(IQcServerSSLException.Type type, SSLHandshakeException sSLHandshakeException) {
        super("[" + type + "]" + sSLHandshakeException.getMessage());
        this.mOriginalException = sSLHandshakeException;
        this.mType = type;
    }

    public SSLHandshakeException getOriginalException() {
        return this.mOriginalException;
    }

    @Override // com.synology.sylib.syhttp3.exceptions.IQcServerSSLException
    public IQcServerSSLException.Type getType() {
        return this.mType;
    }
}