package com.synology.sylib.syhttp3.relay.models;

/* loaded from: classes2.dex */
public class PunchIdleTimeoutInfo {
    private int mPort;
    private String mServerID;
    private String mServiceID;

    public PunchIdleTimeoutInfo(String str, String str2, int i) {
        this.mServerID = str;
        this.mServiceID = str2;
        this.mPort = i;
    }

    public String getServerID() {
        return this.mServerID;
    }

    public String getServiceID() {
        return this.mServiceID;
    }

    public int getPort() {
        return this.mPort;
    }
}