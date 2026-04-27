package com.synology.sylib.syhttp3.relay.vos;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/* loaded from: classes2.dex */
public class ServerVo {

    @SerializedName("ca_fingerprints")
    private List<String> DSExpectedFingerPrints;
    private boolean behind_nat;
    private String ddns;
    private String ds_state;
    private ExternalVo external;
    private String fqdn;
    private String gateway;

    @SerializedName("interface")
    private List<InterfaceVo> interface_;
    private List<IPv6Vo> ipv6_tunnel;
    private String serverID;
    private int tcp_punch_port;
    private int udp_punch_port;

    public String getServerID() {
        return this.serverID;
    }

    public String getDDNS() {
        return this.ddns;
    }

    public String getFQDN() {
        return this.fqdn;
    }

    public List<IPv6Vo> getIPv6Tunnel() {
        return this.ipv6_tunnel;
    }

    public String getGateway() {
        return this.gateway;
    }

    public List<InterfaceVo> getInterface() {
        return this.interface_;
    }

    public class InterfaceVo {
        private String ip;
        private List<IPv6Vo> ipv6;
        private String mask;
        private String name;

        public InterfaceVo() {
        }

        public String getIP() {
            return this.ip;
        }

        public List<IPv6Vo> getIPv6() {
            return this.ipv6;
        }

        public String getMask() {
            return this.mask;
        }

        public String getName() {
            return this.name;
        }
    }

    public ExternalVo getExternal() {
        return this.external;
    }

    public boolean isBehindNAT() {
        return this.behind_nat;
    }

    public int getUDPPunchPort() {
        return this.udp_punch_port;
    }

    public int getTCPPunchPort() {
        return this.tcp_punch_port;
    }

    public String getDSState() {
        return this.ds_state;
    }

    public List<String> getDSExpectedFingerPrints() {
        return this.DSExpectedFingerPrints;
    }
}