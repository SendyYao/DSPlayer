package com.synology.sylib.syhttp3.exceptions;

/* loaded from: classes2.dex */
public interface IQcServerSSLException {

    public enum Type {
        GlobalControlServer,
        RegionalControlServer,
        RelayServer
    }

    Type getType();
}