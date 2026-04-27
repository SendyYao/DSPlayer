package com.synology.sylib.syhttp3.exceptions;

import java.io.IOException;

/* loaded from: classes2.dex */
public class UnexpectedJsonException extends IOException {
    private final String rawString;

    public UnexpectedJsonException(String str, Throwable th) {
        super("[" + (th == null ? "Unexpected" : th.getClass().getSimpleName()) + "] " + str, th);
        this.rawString = str;
    }

    public String getString() {
        return this.rawString;
    }
}