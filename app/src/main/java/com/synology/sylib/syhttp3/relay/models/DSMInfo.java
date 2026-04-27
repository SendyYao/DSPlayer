package com.synology.sylib.syhttp3.relay.models;

import android.util.Log;
import com.synology.sylib.syhttp3.exceptions.UnexpectedJsonException;
import com.synology.sylib.syhttp3.relay.RelayException;
import com.synology.sylib.syhttp3.relay.ServiceId;
import com.synology.sylib.syhttp3.relay.util.SafeGson;
import com.synology.sylib.syhttp3.relay.vos.DSMInfoVo;
import com.synology.sylib.syhttp3.relay.vos.EnvVo;
import com.synology.sylib.syhttp3.relay.vos.ExternalVo;
import com.synology.sylib.syhttp3.relay.vos.IPv6Vo;
import com.synology.sylib.syhttp3.relay.vos.ServerVo;
import com.synology.sylib.syhttp3.relay.vos.ServiceVo;
import com.synology.sylib.syhttp3.relay.vos.SmartDnsVo;
import com.synology.sylib.util.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;

/* loaded from: classes2.dex */
public class DSMInfo {
    private static final String TAG = "DSMInfo";
    private EnvInfo mEnvInfo;
    private List<String> mRelayServers;
    private ServerInfo mServerInfo;
    private ServiceInfo mServiceInfo;

    public EnvInfo getEnvInfo() {
        return this.mEnvInfo;
    }

    public void setEnvInfo(EnvInfo envInfo) {
        this.mEnvInfo = envInfo;
    }

    public ServerInfo getServerInfo() {
        return this.mServerInfo;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.mServerInfo = serverInfo;
    }

    public ServiceInfo getServiceInfo() {
        return this.mServiceInfo;
    }

    public void setServiceInfo(ServiceInfo serviceInfo) {
        this.mServiceInfo = serviceInfo;
    }

    public List<String> getRelayServers() {
        return this.mRelayServers;
    }

    public void setRelayServers(List<String> list) {
        this.mRelayServers = list;
    }

    public static DSMInfo parseServerInfo(String str, String str2, InputStream inputStream) throws IOException {
        SafeGson safeGson = new SafeGson();
        DSMInfo dSMInfo = new DSMInfo();
        try {
            try {
                DSMInfoVo dSMInfoVo = (DSMInfoVo) safeGson.fromJson(inputStream, DSMInfoVo.class, StandardCharsets.UTF_8);
                if (dSMInfoVo == null) {
                    throw new IOException("empty value object(DSMInfoVo)");
                }
                int errno = dSMInfoVo.getErrno();
                if (errno != 0) {
                    List<String> sites = dSMInfoVo.getSites();
                    if (sites != null && sites.size() > 0) {
                        dSMInfo.setRelayServers(sites);
                    } else {
                        throw new RelayException(errno);
                    }
                } else {
                    ServerVo server = dSMInfoVo.getServer();
                    if (server == null) {
                        throw new IOException("empty value object(ServerVo)");
                    }
                    ServiceVo service = dSMInfoVo.getService();
                    if (service == null) {
                        throw new IOException("empty value object(ServiceVo)");
                    }
                    EnvVo env = dSMInfoVo.getEnv();
                    if (env != null) {
                        EnvInfo envInfo = new EnvInfo();
                        envInfo.setRelayRegion(env.getRelayRegion());
                        envInfo.setControlHost(env.getControlHost());
                        dSMInfo.setEnvInfo(envInfo);
                    }
                    parseServerInfo(str, str2, server, dSMInfoVo.getSmartDnsVo(), dSMInfo);
                    ServiceInfo serviceInfo = new ServiceInfo();
                    serviceInfo.setId(service.getId());
                    serviceInfo.setPort(service.getPort());
                    serviceInfo.setExtPort(service.getExtPort());
                    serviceInfo.setRelayIP(service.getRelayIP());
                    serviceInfo.setRelayIPV6(service.getRelayIPV6());
                    serviceInfo.setRelayPort(service.getRelayPort());
                    serviceInfo.setRelayDualStack(service.getRelayDualStack());
                    serviceInfo.setRelayDn(service.getRelayDn());
                    dSMInfo.setServiceInfo(serviceInfo);
                }
                return dSMInfo;
            } catch (UnexpectedJsonException e) {
                throw new RelayException(100, e);
            }
        } finally {
            IOUtils.closeSilently(inputStream);
        }
    }

    public static void parseServerInfo(String str, String str2, ServerVo serverVo, SmartDnsVo smartDnsVo, DSMInfo dSMInfo) {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setServerId(str);
        serverInfo.setServerId2(serverVo.getServerID());
        serverInfo.setGateway(serverVo.getGateway());
        serverInfo.setUDPPunchPort(serverVo.getUDPPunchPort());
        serverInfo.setTCPPunchPort(serverVo.getTCPPunchPort());
        boolean zEquals = ServiceId.getProtocol(str2).equals("https");
        if (smartDnsVo != null && zEquals) {
            parseSmartDnsAddress(serverInfo, smartDnsVo);
        }
        parseServerAddress(serverInfo, serverVo);
        List<IPv6Vo> iPv6Tunnel = serverVo.getIPv6Tunnel();
        if (iPv6Tunnel != null) {
            ArrayList arrayList = new ArrayList();
            Iterator<IPv6Vo> it = iPv6Tunnel.iterator();
            while (it.hasNext()) {
                try {
                    InetAddress byName = Inet6Address.getByName(it.next().getAddress());
                    if (byName instanceof Inet6Address) {
                        arrayList.add((Inet6Address) byName);
                    }
                } catch (UnknownHostException unused) {
                    Log.e(TAG, "parse ipv6Tunnel fail");
                }
            }
            serverInfo.setIPv6Tunnels(arrayList);
        }
        List<String> dSExpectedFingerPrints = serverVo.getDSExpectedFingerPrints();
        if (dSExpectedFingerPrints != null) {
            ArrayList arrayList2 = new ArrayList();
            Iterator<String> it2 = dSExpectedFingerPrints.iterator();
            while (it2.hasNext()) {
                arrayList2.add(it2.next().toLowerCase());
            }
            serverInfo.setDSExpectedFingerPrints(arrayList2);
        }
        dSMInfo.setServerInfo(serverInfo);
    }

    private static void parseSmartDnsAddress(ServerInfo serverInfo, SmartDnsVo smartDnsVo) {
        ServerInfo.ConnectAddress connectAddress = new ServerInfo.ConnectAddress();
        if (smartDnsVo.getHost() != null) {
            connectAddress.setDDNS(smartDnsVo.getHost());
            connectAddress.setFQDN(smartDnsVo.getHost());
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        if (smartDnsVo.getLans() != null) {
            for (String str : smartDnsVo.getLans()) {
                ServerInfo.InterfaceInfo interfaceInfo = new ServerInfo.InterfaceInfo();
                interfaceInfo.setIP(str);
                arrayList.add(interfaceInfo);
            }
        }
        if (smartDnsVo.getLanV6s() != null) {
            Iterator<String> it = smartDnsVo.getLanV6s().iterator();
            while (it.hasNext()) {
                try {
                    InetAddress byName = Inet6Address.getByName(it.next());
                    if (byName instanceof Inet6Address) {
                        arrayList2.add((Inet6Address) byName);
                    }
                } catch (UnknownHostException unused) {
                    Log.e(TAG, "resolve internal ipv6 fail");
                }
            }
            if (arrayList2.size() > 0) {
                ServerInfo.InterfaceInfo interfaceInfo2 = new ServerInfo.InterfaceInfo();
                interfaceInfo2.setIPv6s(arrayList2);
                arrayList.add(interfaceInfo2);
            }
        }
        connectAddress.setInterfaceInfos(arrayList);
        if (smartDnsVo.getExternal() != null) {
            connectAddress.setExternalIP(smartDnsVo.getExternal());
        }
        if (smartDnsVo.getExternalV6() != null) {
            try {
                InetAddress byName2 = Inet6Address.getByName(smartDnsVo.getExternalV6());
                if (byName2 instanceof Inet6Address) {
                    connectAddress.setExternalV6IP((Inet6Address) byName2);
                }
            } catch (UnknownHostException unused2) {
                Log.e(TAG, "resolve external ipv6 fail");
            }
        }
        if (smartDnsVo.getHolePunch() != null) {
            connectAddress.setHolePunchAddress(smartDnsVo.getHolePunch());
        }
        serverInfo.setSmartDnsAddress(connectAddress);
    }

    private static void parseServerAddress(ServerInfo serverInfo, ServerVo serverVo) {
        ServerInfo.ConnectAddress connectAddress = new ServerInfo.ConnectAddress();
        if (serverVo.getDDNS() != null) {
            connectAddress.setDDNS(serverVo.getDDNS());
        }
        if (serverVo.getFQDN() != null) {
            connectAddress.setFQDN(serverVo.getFQDN());
        }
        List<ServerVo.InterfaceVo> list = serverVo.getInterface();
        if (list != null) {
            ArrayList arrayList = new ArrayList();
            for (ServerVo.InterfaceVo interfaceVo : list) {
                ServerInfo.InterfaceInfo interfaceInfo = new ServerInfo.InterfaceInfo();
                interfaceInfo.setIP(interfaceVo.getIP());
                interfaceInfo.setMask(interfaceVo.getMask());
                interfaceInfo.setName(interfaceVo.getName());
                List<IPv6Vo> iPv6 = interfaceVo.getIPv6();
                if (iPv6 != null) {
                    ArrayList arrayList2 = new ArrayList();
                    Iterator<IPv6Vo> it = iPv6.iterator();
                    while (it.hasNext()) {
                        try {
                            InetAddress byName = Inet6Address.getByName(it.next().getAddress());
                            if (byName instanceof Inet6Address) {
                                arrayList2.add((Inet6Address) byName);
                            }
                        } catch (UnknownHostException unused) {
                            Log.e(TAG, "parse internal ipv6 fail");
                        }
                    }
                    interfaceInfo.setIPv6s(arrayList2);
                }
                arrayList.add(interfaceInfo);
            }
            connectAddress.setInterfaceInfos(arrayList);
        }
        ExternalVo external = serverVo.getExternal();
        if (external != null) {
            connectAddress.setExternalIP(external.getIP());
        }
        connectAddress.setHolePunchAddress("127.0.0.1");
        serverInfo.setServerAddress(connectAddress);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DSMInfo)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        DSMInfo dSMInfo = (DSMInfo) obj;
        return new EqualsBuilder().append(this.mEnvInfo, dSMInfo.mEnvInfo).append(this.mServerInfo, dSMInfo.mServerInfo).append(this.mServiceInfo, dSMInfo.mServiceInfo).append(this.mRelayServers, dSMInfo.mRelayServers).isEquals();
    }
}