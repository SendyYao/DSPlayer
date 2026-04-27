package com.synology.sylib.syhttp3.relay;

import java.io.IOException;

/* loaded from: classes2.dex */
public class RelayException extends IOException {
    private final Throwable cause;
    private int errno;

    public RelayException(int i, Throwable th) {
        this.errno = i;
        this.cause = th;
    }

    public RelayException(int i) {
        this(i, null);
    }

    public int getErrno() {
        return this.errno;
    }

    @Override // java.lang.Throwable
    public Throwable getCause() {
        return this.cause;
    }
}