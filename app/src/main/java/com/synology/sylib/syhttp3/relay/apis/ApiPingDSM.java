package com.synology.sylib.syhttp3.relay.apis;

import android.text.TextUtils;
import android.util.Log;
import com.synology.sylib.syhttp3.relay.PunchInfoManager;
import com.synology.sylib.syhttp3.relay.RelayResult;
import com.synology.sylib.syhttp3.relay.ServiceId;
import com.synology.sylib.syhttp3.relay.models.PunchIdleTimeoutInfo;
import com.synology.sylib.syhttp3.relay.models.ServerInfo;
import com.synology.sylib.syhttp3.relay.models.ServiceInfo;
import com.synology.sylib.syhttp3.relay.ping.ServicePing;
import com.synology.sylib.syhttp3.relay.ping.ServicePingBuilder;
import com.synology.sylib.syhttp3.relay.utils.RelayExecutors;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import com.synology.sylib.syhttp3.util.QuickConnectUtil;
import com.synology.synoholepunch.PunchInfo;
import com.synology.synoholepunch.SynoPunchCallback;
import io.reactivex.rxjava3.core.Observer;
import java.io.IOException;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/* loaded from: classes2.dex */
public class ApiPingDSM {
    private static final int HOLE_PUNCH_IDLE_TIMEOUT = 300;
    private static final int HOLE_PUNCH_TIMEOUT = 5000;
    private static final String TAG = "ApiPingDSM";
    private Observer<PunchIdleTimeoutInfo> mObserverPunchIdleTimeout;
    private final String mPingPath;
    private int mPunchSiteRegion;
    private int mPunchTimeout;
    private final ServerInfo mServerInfo;
    private final String mServiceId;
    private final ServiceInfo mServiceInfo;
    private boolean mUseHolePunch;
    private static final ConcurrentHashMap<Integer, HolePunchDaemonThread> HOLE_PUNCH_DAEMON_CACHE = new ConcurrentHashMap<>();
    private static final CONNECTION_TYPE[][] MULTI_PRIORITY_QUEUE = {new CONNECTION_TYPE[]{CONNECTION_TYPE.LAN_SMART_DNS}, new CONNECTION_TYPE[]{CONNECTION_TYPE.LAN}, new CONNECTION_TYPE[]{CONNECTION_TYPE.WAN_DDNS_SMART_DNS}, new CONNECTION_TYPE[]{CONNECTION_TYPE.WAN_DDNS}, new CONNECTION_TYPE[]{CONNECTION_TYPE.WAN_IP_SMART_DNS}, new CONNECTION_TYPE[]{CONNECTION_TYPE.WAN_IP}, new CONNECTION_TYPE[]{CONNECTION_TYPE.WAN_PUNCH_SMART_DNS}, new CONNECTION_TYPE[]{CONNECTION_TYPE.WAN_PUNCH}, new CONNECTION_TYPE[]{CONNECTION_TYPE.TUNNEL_SMART_DNS}, new CONNECTION_TYPE[]{CONNECTION_TYPE.TUNNEL}};
    private static final CONNECTION_TYPE[][] SINGLE_PRIORITY_QUEUE = {new CONNECTION_TYPE[]{CONNECTION_TYPE.LAN_SMART_DNS, CONNECTION_TYPE.WAN_DDNS_SMART_DNS, CONNECTION_TYPE.WAN_IP_SMART_DNS, CONNECTION_TYPE.WAN_PUNCH_SMART_DNS, CONNECTION_TYPE.TUNNEL_SMART_DNS}, new CONNECTION_TYPE[]{CONNECTION_TYPE.LAN, CONNECTION_TYPE.WAN_DDNS, CONNECTION_TYPE.WAN_IP, CONNECTION_TYPE.WAN_PUNCH, CONNECTION_TYPE.TUNNEL}};

    private enum CONNECTION_TYPE {
        LAN_SMART_DNS(true),
        LAN(false),
        WAN_DDNS_SMART_DNS(true),
        WAN_DDNS(false),
        WAN_IP_SMART_DNS(true),
        WAN_IP(false),
        WAN_PUNCH_SMART_DNS(true),
        WAN_PUNCH(false),
        TUNNEL_SMART_DNS(true),
        TUNNEL(false);

        private boolean mUseSmartDns;

        CONNECTION_TYPE(boolean z) {
            this.mUseSmartDns = z;
        }

        public boolean useSmartDns() {
            return this.mUseSmartDns;
        }
    }

    public ApiPingDSM(ServerInfo serverInfo, ServiceInfo serviceInfo, String str, String str2, boolean z, int i, Observer<PunchIdleTimeoutInfo> observer) {
        this(serverInfo, serviceInfo, str, str2, z, 5000, i, observer);
    }

    public ApiPingDSM(ServerInfo serverInfo, ServiceInfo serviceInfo, String str, String str2, boolean z, int i, int i2, Observer<PunchIdleTimeoutInfo> observer) {
        this.mServerInfo = serverInfo;
        this.mServiceInfo = serviceInfo;
        this.mServiceId = str;
        this.mPingPath = str2;
        this.mUseHolePunch = z;
        this.mPunchTimeout = i;
        if (i == 0) {
            this.mPunchTimeout = 5000;
        }
        this.mObserverPunchIdleTimeout = observer;
        this.mPunchSiteRegion = i2;
    }

    public RelayResult call(ServicePingBuilder servicePingBuilder, boolean z) {
        if (this.mServerInfo == null) {
            throw new IllegalArgumentException("ServerInfo == null");
        }
        if (this.mServiceInfo == null) {
            throw new IllegalArgumentException("ServiceInfo == null");
        }
        if (TextUtils.isEmpty(this.mPingPath)) {
            throw new IllegalArgumentException("pingPongPath is empty");
        }
        if (!this.mUseHolePunch) {
            PunchInfoManager.getInstance().stopAll();
        }
        ServicePing servicePingGenerateServicePinger = servicePingBuilder.generateServicePinger(this.mServiceId, this.mServerInfo.getServerId2(), this.mPingPath);
        ExecutorService executorServiceNewAsyncTaskExecutor = RelayExecutors.newAsyncTaskExecutor(TAG);
        ArrayList arrayList = new ArrayList();
        try {
            try {
                for (CONNECTION_TYPE[] connection_typeArr : z ? SINGLE_PRIORITY_QUEUE : MULTI_PRIORITY_QUEUE) {
                    PingCompletionService pingCompletionService = new PingCompletionService(executorServiceNewAsyncTaskExecutor);
                    for (CONNECTION_TYPE connection_type : connection_typeArr) {
                        composeService(connection_type, servicePingGenerateServicePinger, pingCompletionService);
                    }
                    if (pingCompletionService.hasTask()) {
                        arrayList.add(pingCompletionService);
                    }
                }
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    RelayResult relayResultTakeFirstOrNull = ((PingCompletionService) it.next()).takeFirstOrNull();
                    if (relayResultTakeFirstOrNull != null) {
                        if (6 != relayResultTakeFirstOrNull.getConnectivity()) {
                            PunchInfoManager.getInstance().stop(this.mServerInfo.getServerId(), this.mServiceId);
                        }
                        return relayResultTakeFirstOrNull;
                    }
                }
                if (this.mServiceInfo.hasRelayInfo()) {
                    throw new IOException("no successful internal/hostname/external/tunnel ping");
                }
                throw new IOException("no successful internal/hostname/external ping");
            } catch (IOException e) {
                String message = e.getMessage();
                String str = TAG;
                StringBuilder sbAppend = new StringBuilder().append("IOException: ");
                if (message == null) {
                    message = "";
                }
                Log.e(str, sbAppend.append(message).toString());
                executorServiceNewAsyncTaskExecutor.shutdownNow();
                PunchInfoManager.getInstance().stop(this.mServerInfo.getServerId(), this.mServiceId);
                return null;
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } finally {
            executorServiceNewAsyncTaskExecutor.shutdownNow();
        }
    }

    private void composeInternalService(ServicePing servicePing, PingCompletionService pingCompletionService, boolean z) {
        List<ServerInfo.InterfaceInfo> interfaceInfos;
        String protocol = ServiceId.getProtocol(this.mServiceId);
        int port = this.mServiceInfo.getPort();
        ServerInfo.ConnectAddress connectAddress = this.mServerInfo.getConnectAddress(z);
        if (connectAddress == null || (interfaceInfos = connectAddress.getInterfaceInfos()) == null) {
            return;
        }
        for (ServerInfo.InterfaceInfo interfaceInfo : interfaceInfos) {
            pingCompletionService.submit(servicePing.ping(protocol, interfaceInfo.getIP(), port, 2));
            if (interfaceInfo.getIPv6s() != null) {
                for (Inet6Address inet6Address : interfaceInfo.getIPv6s()) {
                    pingCompletionService.submit(servicePing.ping(protocol, inet6Address.getHostName(), port, (inet6Address.isAnyLocalAddress() || inet6Address.isLinkLocalAddress() || inet6Address.isSiteLocalAddress()) ? 2 : 5));
                }
            }
        }
    }

    private void composeHostnameService(ServicePing servicePing, PingCompletionService pingCompletionService, boolean z) {
        String protocol = ServiceId.getProtocol(this.mServiceId);
        int port = this.mServiceInfo.getPort();
        int extPort = this.mServiceInfo.getExtPort();
        if (extPort != 0) {
            port = extPort;
        }
        ServerInfo.ConnectAddress connectAddress = this.mServerInfo.getConnectAddress(z);
        if (connectAddress == null) {
            return;
        }
        String ddns = connectAddress.getDDNS();
        if (!TextUtils.isEmpty(ddns)) {
            Log.d(TAG, "ddns: " + ddns);
            pingCompletionService.submit(servicePing.ping(protocol, ddns, port, 3));
        }
        String fqdn = connectAddress.getFQDN();
        if (TextUtils.isEmpty(fqdn)) {
            return;
        }
        Log.d(TAG, "fqdn: " + fqdn);
        pingCompletionService.submit(servicePing.ping(protocol, fqdn, port, 4));
    }

    private void composeExternalService(ServicePing servicePing, PingCompletionService pingCompletionService, boolean z) {
        String protocol = ServiceId.getProtocol(this.mServiceId);
        int port = this.mServiceInfo.getPort();
        ServerInfo.ConnectAddress connectAddress = this.mServerInfo.getConnectAddress(z);
        if (connectAddress == null) {
            return;
        }
        String externalIP = connectAddress.getExternalIP();
        int extPort = this.mServiceInfo.getExtPort();
        if (extPort == 0) {
            extPort = port;
        }
        pingCompletionService.submit(servicePing.ping(protocol, externalIP, extPort, 5));
        Inet6Address externalV6IP = connectAddress.getExternalV6IP();
        if (externalV6IP != null) {
            pingCompletionService.submit(servicePing.ping(protocol, externalV6IP.getHostName(), extPort, 5));
        }
        if (this.mServerInfo.getIPv6Tunnels() != null) {
            Iterator<Inet6Address> it = this.mServerInfo.getIPv6Tunnels().iterator();
            while (it.hasNext()) {
                pingCompletionService.submit(servicePing.ping(protocol, it.next().getHostName(), port, 5));
            }
        }
    }

    private void composeHolePunchService(ServicePing servicePing, PingCompletionService pingCompletionService, boolean z) {
        if (this.mUseHolePunch) {
            String protocol = ServiceId.getProtocol(this.mServiceId);
            PunchInfo punchInfo = getPunchInfo();
            if (punchInfo != null) {
                Log.d(TAG, this + "/ composeHolePunch port not -1: " + punchInfo.getTcpPort());
                pingCompletionService.submit(holePunchCallable(servicePing, protocol, z, null));
            } else {
                int holePunchDaemonID = getHolePunchDaemonID(this.mServerInfo.getServerId(), this.mServiceId, z);
                CountDownLatch countDownLatch = new CountDownLatch(1);
                pingCompletionService.submit(holePunchCallable(servicePing, protocol, z, countDownLatch));
                HOLE_PUNCH_DAEMON_CACHE.computeIfAbsent(Integer.valueOf(holePunchDaemonID), new Function<Integer, HolePunchDaemonThread>() { // from class: com.synology.sylib.syhttp3.relay.apis.ApiPingDSM.1
                    @Override // java.util.function.Function
                    public HolePunchDaemonThread apply(Integer num) {
                        HolePunchDaemonThread holePunchDaemonThread = ApiPingDSM.this.new HolePunchDaemonThread(num.intValue());
                        holePunchDaemonThread.start();
                        return holePunchDaemonThread;
                    }
                }).addLatchIfNeeded(countDownLatch);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PunchInfo getPunchInfo() {
        return PunchInfoManager.getInstance().getLastestPunchInfo(this.mServerInfo.getServerId(), this.mServiceId);
    }

    private int getHolePunchDaemonID(String str, String str2, boolean z) {
        return new HashCodeBuilder(17, 31).append(str).append(str2).append(z).hashCode();
    }

    private void composeTunnelService(ServicePing servicePing, PingCompletionService pingCompletionService, boolean z) {
        if (this.mServiceInfo.hasRelayInfo()) {
            pingCompletionService.submit(servicePing.ping(ServiceId.getProtocol(this.mServiceId), this.mServiceInfo.getRelayAddress(z), this.mServiceInfo.getRelayPort(), 7));
        }
    }

    private Callable<RelayResult> holePunchCallable(final ServicePing servicePing, final String str, final boolean z, final CountDownLatch countDownLatch) {
        return new Callable<RelayResult>() { // from class: com.synology.sylib.syhttp3.relay.apis.ApiPingDSM.2
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // java.util.concurrent.Callable
            public RelayResult call() throws Exception {
                ServerInfo.ConnectAddress connectAddress;
                CountDownLatch countDownLatch2 = countDownLatch;
                if (countDownLatch2 != null) {
                    countDownLatch2.await();
                }
                PunchInfo punchInfo = ApiPingDSM.this.getPunchInfo();
                int tcpPort = punchInfo != null ? punchInfo.getTcpPort() : -1;
                if (tcpPort == -1 || (connectAddress = ApiPingDSM.this.mServerInfo.getConnectAddress(z)) == null) {
                    return null;
                }
                return servicePing.ping(str, connectAddress.getHolePunchAddress(), tcpPort, 6).call();
            }
        };
    }

    private class HolePunchDaemonThread extends Thread {
        private static final String PREFIX = "HolePunch";
        private final List<CountDownLatch> countDownLatchList = new ArrayList();
        private final int daemonId;

        HolePunchDaemonThread(int i) {
            setName(PREFIX + (i % 100000));
            this.daemonId = i;
            setDaemon(true);
        }

        public void addLatchIfNeeded(CountDownLatch countDownLatch) {
            synchronized (this.countDownLatchList) {
                if (ApiPingDSM.this.getPunchInfo() == null) {
                    this.countDownLatchList.add(countDownLatch);
                } else {
                    countDownLatch.countDown();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void countDownAll() {
            synchronized (this.countDownLatchList) {
                Iterator<CountDownLatch> it = this.countDownLatchList.iterator();
                while (it.hasNext()) {
                    it.next().countDown();
                }
                this.countDownLatchList.clear();
            }
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            PunchInfo punchInfo = new PunchInfo(ApiPingDSM.this.mPunchTimeout, 300, ApiPingDSM.this.mPunchSiteRegion);
            try {
                startHolePunch(punchInfo);
            } finally {
                PunchInfoManager.getInstance().remove(ApiPingDSM.this.mServerInfo.getServerId(), ApiPingDSM.this.mServiceId, punchInfo.getTcpPort());
                countDownAll();
                ApiPingDSM.HOLE_PUNCH_DAEMON_CACHE.remove(Integer.valueOf(this.daemonId));
                Log.d("SynoPunch", "Daemon closed");
            }
        }

        private void startHolePunch(final PunchInfo punchInfo) {
            Log.d("SynoPunch", "HolePunch timeout: " + ApiPingDSM.this.mPunchTimeout);
            final String serverId = ApiPingDSM.this.mServerInfo.getServerId();
            punchInfo.start(serverId, ApiPingDSM.this.mServerInfo.getServerAddress().getExternalIP(), ApiPingDSM.this.mServerInfo.getUDPPunchPort(), RelayUtil.getServiceTypeFromID(ApiPingDSM.this.mServiceId), new SynoPunchCallback() { // from class: com.synology.sylib.syhttp3.relay.apis.ApiPingDSM.HolePunchDaemonThread.1
                @Override // com.synology.synoholepunch.SynoPunchCallback
                public void URDConnected(int i) {
                    Log.d("SynoPunch", String.format(Locale.US, "URD callback: %d", Integer.valueOf(i)));
                    PunchInfoManager.getInstance().add(serverId, ApiPingDSM.this.mServiceId, punchInfo);
                    HolePunchDaemonThread.this.countDownAll();
                }

                @Override // com.synology.synoholepunch.SynoPunchCallback
                public void URDClosed() {
                    Log.d("SynoPunch", "URD Closed");
                }

                @Override // com.synology.synoholepunch.SynoPunchCallback
                public void URDIdleTimeout(int i, int i2) {
                    Log.d("SynoPunch", "URD idleTimeout");
                    PunchInfoManager.DaemonInfo daemonInfo = PunchInfoManager.getInstance().getDaemonInfo(i);
                    if (daemonInfo == null || ApiPingDSM.this.mObserverPunchIdleTimeout == null) {
                        return;
                    }
                    ApiPingDSM.this.mObserverPunchIdleTimeout.onNext(new PunchIdleTimeoutInfo(daemonInfo.getServerID(), daemonInfo.getServiceID(), i));
                }

                @Override // com.synology.synoholepunch.SynoPunchCallback
                public void URDLog(String str) {
                    QuickConnectUtil.log(str);
                }
            });
        }
    }

    private static class PingCompletionService {
        private int mCount = 0;
        private CompletionService<RelayResult> mService;

        PingCompletionService(Executor executor) {
            this.mService = new ExecutorCompletionService(executor);
        }

        public boolean hasTask() {
            return this.mCount > 0;
        }

        public void submit(Callable<RelayResult> callable) {
            this.mService.submit(callable);
            this.mCount++;
        }

        public RelayResult takeFirstOrNull() throws InterruptedException, ExecutionException {
            Future<RelayResult> futureTake = null;
            RelayResult relayResult;
            while (true) {
                int i = this.mCount;
                this.mCount = i - 1;
                if (i <= 0) {
                    return null;
                }
                try {
                    futureTake = this.mService.take();
                } catch (InterruptedException e) {
                    String message = e.getMessage();
                    String str = ApiPingDSM.TAG;
                    StringBuilder sb = new StringBuilder("InterruptedException: ");
                    if (message == null) {
                        message = "";
                    }
                    Log.e(str, sb.append(message).toString());
                }
                if (futureTake != null && (relayResult = futureTake.get()) != null) {
                    return relayResult;
                }
            }
        }
    }

    /* renamed from: com.synology.sylib.syhttp3.relay.apis.ApiPingDSM$3, reason: invalid class name */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE;

        static {
            int[] iArr = new int[CONNECTION_TYPE.values().length];
            $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE = iArr;
            try {
                iArr[CONNECTION_TYPE.LAN_SMART_DNS.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.LAN.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.WAN_DDNS_SMART_DNS.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.WAN_DDNS.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.WAN_IP_SMART_DNS.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.WAN_IP.ordinal()] = 6;
            } catch (NoSuchFieldError unused6) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.WAN_PUNCH_SMART_DNS.ordinal()] = 7;
            } catch (NoSuchFieldError unused7) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.WAN_PUNCH.ordinal()] = 8;
            } catch (NoSuchFieldError unused8) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.TUNNEL_SMART_DNS.ordinal()] = 9;
            } catch (NoSuchFieldError unused9) {
            }
            try {
                $SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[CONNECTION_TYPE.TUNNEL.ordinal()] = 10;
            } catch (NoSuchFieldError unused10) {
            }
        }
    }

    private void composeService(CONNECTION_TYPE connection_type, ServicePing servicePing, PingCompletionService pingCompletionService) {
        switch (AnonymousClass3.$SwitchMap$com$synology$sylib$syhttp3$relay$apis$ApiPingDSM$CONNECTION_TYPE[connection_type.ordinal()]) {
            case 1:
            case 2:
                composeInternalService(servicePing, pingCompletionService, connection_type.useSmartDns());
                return;
            case 3:
            case 4:
                composeHostnameService(servicePing, pingCompletionService, connection_type.useSmartDns());
                return;
            case 5:
            case 6:
                composeExternalService(servicePing, pingCompletionService, connection_type.useSmartDns());
                return;
            case 7:
            case 8:
                composeHolePunchService(servicePing, pingCompletionService, connection_type.useSmartDns());
                return;
            case 9:
            case 10:
                composeTunnelService(servicePing, pingCompletionService, connection_type.useSmartDns());
                return;
            default:
                throw new IllegalArgumentException("Unknown connect type");
        }
    }
}