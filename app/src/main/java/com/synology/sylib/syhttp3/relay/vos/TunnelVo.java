package com.synology.sylib.syhttp3.relay.vos;

/* loaded from: classes2.dex */
public class TunnelVo extends BasicVo {
    private ClientVo client;
    private EnvVo env;
    private ServerVo server;
    private ServiceVo service;
    private SmartDnsVo smartdns;

    public ServerVo getServer() {
        return this.server;
    }

    public ClientVo getClient() {
        return this.client;
    }

    public EnvVo getEnv() {
        return this.env;
    }

    public ServiceVo getService() {
        return this.service;
    }

    public SmartDnsVo getSmartDnsVo() {
        return this.smartdns;
    }
}