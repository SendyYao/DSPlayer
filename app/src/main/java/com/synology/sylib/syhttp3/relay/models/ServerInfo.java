package com.synology.sylib.syhttp3.relay.models;

import android.text.TextUtils;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;

/* loaded from: classes2.dex */
public class ServerInfo {
    private String mGateway;
    private ConnectAddress mServerAddress;
    private String mServerId;
    private String mServerId2;
    private ConnectAddress mSmartDnsAddress;
    private int mUDPPunchPort = 0;
    private int mTCPPunchPort = 0;
    private List<Inet6Address> mIPv6Tunnels = new ArrayList();
    private List<String> mDSExpectedFingerPrints = new ArrayList();

    public String getServerId() {
        return this.mServerId;
    }

    public void setServerId(String str) {
        this.mServerId = str;
    }

    public String getServerId2() {
        return this.mServerId2;
    }

    public void setServerId2(String str) {
        this.mServerId2 = str;
    }

    public String getGateway() {
        return this.mGateway;
    }

    public void setGateway(String str) {
        this.mGateway = str;
    }

    public int getUDPPunchPort() {
        return this.mUDPPunchPort;
    }

    public void setUDPPunchPort(int i) {
        this.mUDPPunchPort = i;
    }

    public int getTCPPunchPort() {
        return this.mTCPPunchPort;
    }

    public void setTCPPunchPort(int i) {
        this.mTCPPunchPort = i;
    }

    public List<Inet6Address> getIPv6Tunnels() {
        return this.mIPv6Tunnels;
    }

    public void setIPv6Tunnels(List<Inet6Address> list) {
        this.mIPv6Tunnels = list;
    }

    public List<String> getDSExpectedFingerPrints() {
        return this.mDSExpectedFingerPrints;
    }

    public void setDSExpectedFingerPrints(List<String> list) {
        this.mDSExpectedFingerPrints = list;
    }

    public ConnectAddress getServerAddress() {
        return this.mServerAddress;
    }

    public void setServerAddress(ConnectAddress connectAddress) {
        this.mServerAddress = connectAddress;
    }

    public ConnectAddress getSmartDnsAddress() {
        return this.mSmartDnsAddress;
    }

    public void setSmartDnsAddress(ConnectAddress connectAddress) {
        this.mSmartDnsAddress = connectAddress;
    }

    public ConnectAddress getConnectAddress(boolean z) {
        return z ? this.mSmartDnsAddress : this.mServerAddress;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ServerInfo)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        ServerInfo serverInfo = (ServerInfo) obj;
        return new EqualsBuilder().append(this.mServerId, serverInfo.mServerId).append(this.mGateway, serverInfo.mGateway).append(this.mUDPPunchPort, serverInfo.mUDPPunchPort).append(this.mTCPPunchPort, serverInfo.mTCPPunchPort).append(this.mIPv6Tunnels, serverInfo.mIPv6Tunnels).append(this.mServerAddress, serverInfo.mServerAddress).append(this.mSmartDnsAddress, serverInfo.mSmartDnsAddress).isEquals();
    }

    public static class ConnectAddress {
        private String mDDNS;
        private String mExternalIP;
        private Inet6Address mExternalV6IP;
        private String mFQDN;
        private String mHolePunchAddress;
        private List<InterfaceInfo> mInterfaceInfos = new ArrayList();

        public List<InterfaceInfo> getInterfaceInfos() {
            return this.mInterfaceInfos;
        }

        public void setInterfaceInfos(List<InterfaceInfo> list) {
            this.mInterfaceInfos = list;
        }

        public String getDDNS() {
            return this.mDDNS;
        }

        public void setDDNS(String str) {
            if (TextUtils.isEmpty(str) || str.equals("NULL")) {
                str = null;
            }
            this.mDDNS = str;
        }

        public String getFQDN() {
            return this.mFQDN;
        }

        public void setFQDN(String str) {
            if (TextUtils.isEmpty(str) || str.equals("NULL")) {
                str = null;
            }
            this.mFQDN = str;
        }

        public String getExternalIP() {
            return this.mExternalIP;
        }

        public void setExternalIP(String str) {
            this.mExternalIP = str;
        }

        public Inet6Address getExternalV6IP() {
            return this.mExternalV6IP;
        }

        public void setExternalV6IP(Inet6Address inet6Address) {
            this.mExternalV6IP = inet6Address;
        }

        public String getHolePunchAddress() {
            return this.mHolePunchAddress;
        }

        public void setHolePunchAddress(String str) {
            this.mHolePunchAddress = str;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ConnectAddress)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            ConnectAddress connectAddress = (ConnectAddress) obj;
            return new EqualsBuilder().append(this.mDDNS, connectAddress.mDDNS).append(this.mFQDN, connectAddress.mFQDN).append(this.mExternalIP, connectAddress.mExternalIP).append(this.mExternalV6IP, connectAddress.mExternalV6IP).append(this.mHolePunchAddress, connectAddress.mHolePunchAddress).append(this.mInterfaceInfos, connectAddress.mInterfaceInfos).isEquals();
        }
    }

    public static class InterfaceInfo {
        private String mIP;
        private List<Inet6Address> mIPv6s = new ArrayList();
        private String mMask;
        private String mName;

        public String getIP() {
            return this.mIP;
        }

        public void setIP(String str) {
            this.mIP = str;
        }

        public String getMask() {
            return this.mMask;
        }

        public void setMask(String str) {
            this.mMask = str;
        }

        public String getName() {
            return this.mName;
        }

        public void setName(String str) {
            this.mName = str;
        }

        public List<Inet6Address> getIPv6s() {
            return this.mIPv6s;
        }

        public void setIPv6s(List<Inet6Address> list) {
            this.mIPv6s = list;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof InterfaceInfo)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            InterfaceInfo interfaceInfo = (InterfaceInfo) obj;
            return new EqualsBuilder().append(this.mIP, interfaceInfo.mIP).append(this.mMask, interfaceInfo.mMask).append(this.mName, interfaceInfo.mName).append(this.mIPv6s, interfaceInfo.mIPv6s).isEquals();
        }
    }
}