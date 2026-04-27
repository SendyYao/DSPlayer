package com.synology.sylib.syhttp3.relay.vos;

/* loaded from: classes2.dex */
public class ServiceVo {
    private int ext_port;
    private String id;
    private String pingpong;
    private int port;
    private String relay_dn;
    private String relay_dualstack;
    private String relay_ip;
    private String relay_ipv6;
    private int relay_port;

    public String getId() {
        return this.id;
    }

    public int getPort() {
        return this.port;
    }

    public int getExtPort() {
        return this.ext_port;
    }

    public String getPongpong() {
        return this.pingpong;
    }

    public String getRelayIP() {
        return this.relay_ip;
    }

    public String getRelayIPV6() {
        return this.relay_ipv6;
    }

    public int getRelayPort() {
        return this.relay_port;
    }

    public String getRelayDualStack() {
        return this.relay_dualstack;
    }

    public String getRelayDn() {
        return this.relay_dn;
    }
}