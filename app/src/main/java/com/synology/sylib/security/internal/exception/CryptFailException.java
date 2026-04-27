package com.synology.sylib.security.internal.exception;

import com.whisperyao.dsplayer.Common;
import com.synology.sylib.security.KsHelper;
import com.synology.sylib.security.internal.method.CryptMethod;

/* loaded from: classes.dex */
public class CryptFailException extends Exception {
    public final Throwable cause;
    public final String cryptMethodName;
    public final Reason reason;
    public final Type type;

    public enum Reason {
        SourceIsNull,
        SourceCipherEmpty,
        CanNotGetAesKey,
        CryptMethodIsNull,
        CryptWithException,
        ResultIsNull
    }

    public enum Type {
        Encrypt,
        Decrypt
    }

    public CryptFailException(CryptMethod cryptMethod, Type type, Reason reason, Throwable th) {
        this(cryptMethod.getName(), type, reason, th);
    }

    public CryptFailException(KsHelper ksHelper, Type type, Reason reason, Throwable th) {
        this(ksHelper.getCryptMethodName(), type, reason, th);
    }

    private CryptFailException(String str, Type type, Reason reason, Throwable th) {
        super((th == null ? "CryptFailException" : th.getMessage()) + " (" + str + Common.SZ_DATABASE_SEPARATOR + type + Common.SZ_DATABASE_SEPARATOR + reason + ")", th);
        this.cryptMethodName = str;
        this.type = type;
        this.reason = reason;
        this.cause = th;
    }
}