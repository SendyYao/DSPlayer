package com.synology.sylib.syhttp3.relay.vos;

/* loaded from: classes2.dex */
public class IPv6Vo {
    private int addr_type;
    private String address;
    private int prefix_length;
    private String scope;

    public int getAddrType() {
        return this.addr_type;
    }

    public String getAddress() {
        return this.address;
    }

    public int getPrefixLength() {
        return this.prefix_length;
    }

    public String getScope() {
        return this.scope;
    }
}