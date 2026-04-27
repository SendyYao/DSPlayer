package com.synology.sylib.syhttp3.relay.vos;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/* loaded from: classes2.dex */
public class SmartDnsVo {
    private String external;

    @SerializedName("externalv6")
    private String externalV6;

    @SerializedName("hole_punch")
    private String holePunch;
    private String host;

    @SerializedName("lanv6")
    private List<String> lanV6s;

    @SerializedName("lan")
    private List<String> lans;

    public String getHost() {
        return this.host;
    }

    public String getExternal() {
        return this.external;
    }

    public String getExternalV6() {
        return this.externalV6;
    }

    public List<String> getLans() {
        return this.lans;
    }

    public List<String> getLanV6s() {
        return this.lanV6s;
    }

    public String getHolePunch() {
        return this.holePunch;
    }
}