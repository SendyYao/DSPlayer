package com.synology.sylib.syhttp3.relay.utils;

/* loaded from: classes2.dex */
public class RelayCommon {
    public static final String PROTOCOL_HTTP = "http";
    public static final String PROTOCOL_HTTPS = "https";

    public static String getProtocol(boolean z) {
        return z ? "https" : "http";
    }

    public static boolean isHttps(String str) {
        return str.equalsIgnoreCase("https");
    }
}