package com.synology.sylib.syhttp3.relay.models;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;

/* loaded from: classes2.dex */
public class ServiceInfo {
    private int mExtPort;
    private String mId;
    private int mPort;
    private String mRelayDn;
    private String mRelayDualStack;
    private String mRelayIP;
    private String mRelayIPV6;
    private int mRelayPort;

    public String getId() {
        return this.mId;
    }

    public void setId(String str) {
        this.mId = str;
    }

    public int getPort() {
        return this.mPort;
    }

    public void setPort(int i) {
        this.mPort = i;
    }

    public int getExtPort() {
        return this.mExtPort;
    }

    public void setExtPort(int i) {
        this.mExtPort = i;
    }

    public String getRelayAddress(boolean z) {
        ArrayList arrayList;
        if (z) {
            arrayList = new ArrayList(Arrays.asList(this.mRelayDn, this.mRelayDualStack, this.mRelayIP, this.mRelayIPV6));
        } else {
            arrayList = new ArrayList(Arrays.asList(this.mRelayIP, this.mRelayIPV6, this.mRelayDn, this.mRelayDualStack));
        }
        return getFirstNonEmptyAddress(arrayList);
    }

    private String getFirstNonEmptyAddress(List<String> list) {
        for (String str : list) {
            if (!TextUtils.isEmpty(str)) {
                return str;
            }
        }
        return "";
    }

    public String getRelayIP() {
        return this.mRelayIP;
    }

    public void setRelayIP(String str) {
        this.mRelayIP = str;
    }

    public String getRelayIPV6() {
        return this.mRelayIPV6;
    }

    public void setRelayIPV6(String str) {
        this.mRelayIPV6 = str;
    }

    public int getRelayPort() {
        return this.mRelayPort;
    }

    public void setRelayPort(int i) {
        this.mRelayPort = i;
    }

    public String getRelayDualStack() {
        return this.mRelayDualStack;
    }

    public void setRelayDualStack(String str) {
        this.mRelayDualStack = str;
    }

    public String getRelayDn() {
        return this.mRelayDn;
    }

    public void setRelayDn(String str) {
        this.mRelayDn = str;
    }

    public boolean hasRelayInfo() {
        return !TextUtils.isEmpty(this.mRelayIP) && this.mRelayPort > 0;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceInfo)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        ServiceInfo serviceInfo = (ServiceInfo) obj;
        return new EqualsBuilder().append(this.mId, serviceInfo.mId).append(this.mPort, serviceInfo.mPort).append(this.mExtPort, serviceInfo.mExtPort).append(this.mRelayIP, serviceInfo.mRelayIP).append(this.mRelayPort, serviceInfo.mRelayPort).isEquals();
    }
}