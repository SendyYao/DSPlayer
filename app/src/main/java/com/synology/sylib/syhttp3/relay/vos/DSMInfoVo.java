package com.synology.sylib.syhttp3.relay.vos;

import java.util.List;

/* loaded from: classes2.dex */
public class DSMInfoVo extends BasicVo {
    private ClientVo client;
    private EnvVo env;
    private ServerVo server;
    private ServiceVo service;
    private List<String> sites;
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

    public List<String> getSites() {
        return this.sites;
    }

    public SmartDnsVo getSmartDnsVo() {
        return this.smartdns;
    }
}