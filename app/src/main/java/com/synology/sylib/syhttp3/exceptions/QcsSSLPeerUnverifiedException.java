package com.synology.sylib.syhttp3.exceptions;

import javax.net.ssl.SSLPeerUnverifiedException;

/* loaded from: classes2.dex */
public class QcsSSLPeerUnverifiedException extends SSLPeerUnverifiedException implements IQcServerSSLException {
    private final SSLPeerUnverifiedException mOriginalException;
    private final IQcServerSSLException.Type mType;

    public QcsSSLPeerUnverifiedException(IQcServerSSLException.Type type, SSLPeerUnverifiedException sSLPeerUnverifiedException) {
        super("[" + type + "]" + sSLPeerUnverifiedException.getMessage());
        this.mOriginalException = sSLPeerUnverifiedException;
        this.mType = type;
    }

    public SSLPeerUnverifiedException getOriginalException() {
        return this.mOriginalException;
    }

    @Override // com.synology.sylib.syhttp3.exceptions.IQcServerSSLException
    public IQcServerSSLException.Type getType() {
        return this.mType;
    }
}