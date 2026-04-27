package com.synology.sylib.syhttp3.relay.vos;

/* loaded from: classes2.dex */
public class BasicVo {
    private String command;
    private String errinfo;
    private int errno;
    private int version;

    public int getVersion() {
        return this.version;
    }

    public String getCommand() {
        return this.command;
    }

    public int getErrno() {
        return this.errno;
    }

    public String getErrinfo() {
        return this.errinfo;
    }
}