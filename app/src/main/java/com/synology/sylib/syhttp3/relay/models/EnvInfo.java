package com.synology.sylib.syhttp3.relay.models;

import org.apache.commons.lang3.builder.EqualsBuilder;

/* loaded from: classes2.dex */
public class EnvInfo {
    private String mControlHost;
    private String mRelayRegion;

    public String getRelayRegion() {
        return this.mRelayRegion;
    }

    public void setRelayRegion(String str) {
        this.mRelayRegion = str;
    }

    public String getControlHost() {
        return this.mControlHost;
    }

    public void setControlHost(String str) {
        this.mControlHost = str;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof EnvInfo)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        EnvInfo envInfo = (EnvInfo) obj;
        return new EqualsBuilder().append(this.mRelayRegion, envInfo.mRelayRegion).append(this.mControlHost, envInfo.mControlHost).isEquals();
    }
}