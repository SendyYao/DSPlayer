package com.synology.sylib.syhttp3.relay;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.whisperyao.dsplayer.Common;
import com.synology.sylib.syhttp3.SyHttpClient;
import com.synology.sylib.syhttp3.relay.apis.ApiDSM;
import com.synology.sylib.syhttp3.relay.apis.ApiPingDSM;
import com.synology.sylib.syhttp3.relay.apis.ApiRelayServers;
import com.synology.sylib.syhttp3.relay.apis.ApiRequestTunnel;
import com.synology.sylib.syhttp3.relay.models.DSMInfo;
import com.synology.sylib.syhttp3.relay.models.PunchIdleTimeoutInfo;
import com.synology.sylib.syhttp3.relay.models.ServerInfo;
import com.synology.sylib.syhttp3.relay.models.ServiceInfo;
import com.synology.sylib.syhttp3.relay.ping.DefaultServicePingBuilder;
import com.synology.sylib.syhttp3.relay.ping.PingPongHandler;
import com.synology.sylib.syhttp3.relay.ping.PingPongPolicy;
import com.synology.sylib.syhttp3.relay.ping.ServicePingBuilder;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import com.synology.sylib.syhttp3.util.QuickConnectUtil;
import com.synology.sylib.util.NetworkUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* loaded from: classes2.dex */
public class RelayManager {
    public static final String CONNECTIVITY_ACTION = "com.synology.sylib.syhttp3.CONNECTIVITY_ACTION";
    private static final String TAG = "RelayManager";
    public static final String UPDATE_RELAY_ACTION = "com.synology.sylib.syhttp3.UPDATE_RELAY";
    private static RelayManager sInstance;
    private Boolean mIgnoreConnectionPriority = false;
    private Boolean mUseHolePunch = null;
    private PublishSubject<PunchIdleTimeoutInfo> mSubjectPunchIdleTimeout = PublishSubject.create();
    private Integer mHolePunchTimeout = null;
    private PingPongPolicy pingPongPolicy = PingPongPolicy.defaultPolicy;

    public static RelayManager getInstance() {
        if (sInstance == null) {
            synchronized (RelayManager.class) {
                if (sInstance == null) {
                    sInstance = new RelayManager();
                }
            }
        }
        return sInstance;
    }

    private PingPongPolicy getPingPongPolicy() {
        return this.pingPongPolicy;
    }

    public void setPingPongPolicy(PingPongPolicy pingPongPolicy) {
        this.pingPongPolicy = pingPongPolicy;
    }

    public PingPongHandler getPingPongHandler() {
        return getPingPongPolicy().getHandler();
    }

    Observable<PunchIdleTimeoutInfo> getObservablePunchIdleTimeout() {
        return this.mSubjectPunchIdleTimeout;
    }

    private RelayRecord modifyRecord(RelayRecord relayRecord, RelayResult relayResult, String str, String str2, String str3, String str4, List<String> list) {
        relayRecord.setServerId2(str);
        relayRecord.setRealPingPongPath(str2);
        relayRecord.setRelayRegion(str3);
        relayRecord.setControlHost(str4);
        relayRecord.setRealURL(relayResult.getURL());
        relayRecord.setConnectivity(relayResult.getConnectivity());
        relayRecord.setDSExpectedFingerPrints(list);
        RelayUtil.setRelayRecord(relayRecord);
        return relayRecord;
    }

    public RelayRecord fetchRealURL(RelayRecordKey relayRecordKey) throws IOException {
        return fetchRealURL(relayRecordKey, new DefaultServicePingBuilder());
    }

    /* JADX WARN: Removed duplicated region for block: B:146:0x0077  */
    /* JADX WARN: Removed duplicated region for block: B:147:0x008a  */
    /* JADX WARN: Removed duplicated region for block: B:152:0x00c9  */
    /* JADX WARN: Removed duplicated region for block: B:168:0x009a A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public RelayRecord fetchRealURL(RelayRecordKey r17, ServicePingBuilder r18) throws IOException {
        /*
            Method dump skipped, instructions count: 256
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.synology.sylib.syhttp3.relay.RelayManager.fetchRealURL(com.synology.sylib.syhttp3.relay.RelayRecordKey, com.synology.sylib.syhttp3.relay.ping.ServicePingBuilder):com.synology.sylib.syhttp3.relay.RelayRecord");
    }

    public RelayRecord updateRecordFingerPrint(RelayRecordKey relayRecordKey) throws IOException {
        DSMInfo dSMInfoFetchRelayOrEnv = null;
        Pair<String, DSMInfo> pairFetchDSMInfo;
        String serverId = relayRecordKey.getServerId();
        if (!RelayUtil.isQuickConnectId(serverId)) {
            throw new IllegalArgumentException("invalid serverId: " + serverId);
        }
        RelayRecord relayRecord = RelayUtil.getRelayRecord(relayRecordKey);
        if (relayRecord == null) {
            throw new IllegalArgumentException("record == null");
        }
        for (String str : relayRecord.getServiceIds()) {
            try {
                dSMInfoFetchRelayOrEnv = fetchRelayOrEnv(serverId, str);
                List<String> relayServers = dSMInfoFetchRelayOrEnv.getRelayServers();
                if (relayServers != null && relayServers.size() > 0 && (pairFetchDSMInfo = fetchDSMInfo(relayServers, serverId, str)) != null && pairFetchDSMInfo.second != null) {
                    dSMInfoFetchRelayOrEnv = (DSMInfo) pairFetchDSMInfo.second;
                }
            } catch (RelayException unused) {
            }
            if (dSMInfoFetchRelayOrEnv.getServerInfo() != null && dSMInfoFetchRelayOrEnv.getServerInfo().getDSExpectedFingerPrints() != null) {
                relayRecord.setDSExpectedFingerPrints(dSMInfoFetchRelayOrEnv.getServerInfo().getDSExpectedFingerPrints());
                return relayRecord;
            }
        }
        return relayRecord;
    }

    public void setUseHolePunch(boolean z) {
        this.mUseHolePunch = Boolean.valueOf(z);
        RelayUtil.saveUseHolePunch(z);
    }

    public boolean getUseHolePunch() {
        if (this.mUseHolePunch == null) {
            this.mUseHolePunch = Boolean.valueOf(RelayUtil.getUseHolePunch());
        }
        return this.mUseHolePunch.booleanValue();
    }

    public void setHolePunchTimeout(int i) {
        if (i < 0) {
            i = 0;
        }
        this.mHolePunchTimeout = Integer.valueOf(i);
        RelayUtil.saveHolePunchTimeout(i);
    }

    public int getHolePunchTimeout() {
        if (this.mHolePunchTimeout == null) {
            this.mHolePunchTimeout = Integer.valueOf(RelayUtil.getHolePunchTimeout());
        }
        return this.mHolePunchTimeout.intValue();
    }

    private DSMInfo fetchRelayOrEnv(String str, String str2) throws IOException {
        DSMInfo dSMInfo;
        boolean z;
        ExecutorService executorServiceNewSingleThreadExecutor = Executors.newSingleThreadExecutor();
        ExecutorCompletionService executorCompletionService = new ExecutorCompletionService(executorServiceNewSingleThreadExecutor);
        ArrayList arrayList = new ArrayList();
        CallRelayServersTask callRelayServersTask = new CallRelayServersTask(new ApiRelayServers(str, str2, false));
        if (QuickConnectUtil.isDeviceInChina(getContext())) {
            arrayList.add(new CallRelayServersTask(new ApiRelayServers(str, str2, true)));
        }
        arrayList.add(callRelayServersTask);
        Iterator it = arrayList.iterator();
        Throwable th = null;
        while (true) {
            if (!it.hasNext()) {
                dSMInfo = null;
                break;
            }
            CallRelayServersTask callRelayServersTask2 = (CallRelayServersTask) it.next();
            QuickConnectUtil.log("Send query to " + (callRelayServersTask2.api.isCallChinaSite() ? "CN" : "WW") + " site");
            executorCompletionService.submit(callRelayServersTask2);
            try {
                ApiTaskResult apiTaskResult = (ApiTaskResult) executorCompletionService.take().get();
                if (apiTaskResult.throwable != null) {
                    String message = apiTaskResult.throwable.getMessage();
                    if (apiTaskResult.throwable instanceof RelayException) {
                        message = "RelayException [" + ((RelayException) apiTaskResult.throwable).getErrno() + "] with (" + str + Common.SZ_DATABASE_SEPARATOR + str2 + ")";
                        z = true;
                    } else {
                        if (message == null) {
                            message = apiTaskResult.throwable.toString();
                        }
                        z = false;
                    }
                    Log.d(TAG, "Exception for " + (apiTaskResult.isChina ? "CN" : "WW") + " site : " + message);
                    QuickConnectUtil.log("Fail with exception : " + message);
                    if (z) {
                        throw ((RelayException) apiTaskResult.throwable);
                    }
                    th = apiTaskResult.throwable;
                } else if (apiTaskResult.dsmInfo != null) {
                    dSMInfo = apiTaskResult.dsmInfo;
                    QuickConnectUtil.setUseChinaSite(getContext(), apiTaskResult.isChina);
                    QuickConnectUtil.log("Success with " + (apiTaskResult.isChina ? "CN" : "WW") + " site");
                }
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Get ApiTask result fail", e);
                QuickConnectUtil.log("Request fail : " + e.getMessage());
            }
        }
        executorServiceNewSingleThreadExecutor.shutdownNow();
        if (dSMInfo != null) {
            return dSMInfo;
        }
        if (th instanceof RuntimeException) {
            throw ((RuntimeException) th);
        }
        if (th instanceof IOException) {
            throw ((IOException) th);
        }
        throw new IOException("response DSMInfo == null");
    }

    private Pair<String, DSMInfo> fetchDSMInfo(List<String> list, String str, String str2) throws IOException {
        if (list == null || list.size() <= 0) {
            throw new IllegalArgumentException("relayServers is empty");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("serverId is empty");
        }
        if (TextUtils.isEmpty(str2)) {
            throw new IllegalArgumentException("serviceId is empty");
        }
        ApiDSM apiDSM = new ApiDSM();
        apiDSM.setRelayServers(list);
        apiDSM.setServerId(str);
        apiDSM.setServiceId(str2);
        return apiDSM.call();
    }

    private RelayResult ping(ServicePingBuilder servicePingBuilder, ServerInfo serverInfo, ServiceInfo serviceInfo, String str, String str2, String str3) throws IOException {
        String serverId = serverInfo.getServerId();
        RelayResult relayResultPingDSM = pingDSM(servicePingBuilder, serverInfo, serviceInfo, str, str2);
        if (relayResultPingDSM != null) {
            Log.d(TAG, "[success] ping: " + relayResultPingDSM.getRealURL() + ", connectivity: " + relayResultPingDSM.getConnectivityInfo());
            return relayResultPingDSM;
        }
        RelayResult relayResultRequestTunnel = requestTunnel(str3, serverId, str, true);
        if (relayResultRequestTunnel != null) {
            Log.d(TAG, "[success] tunnel request: " + relayResultRequestTunnel.getRealURL() + ", connectivity: " + relayResultRequestTunnel.getConnectivityInfo());
            return relayResultRequestTunnel;
        }
        RelayResult relayResultRequestTunnel2 = requestTunnel(str3, serverId, str, false);
        if (relayResultRequestTunnel2 == null) {
            return null;
        }
        Log.d(TAG, "[success] tunnel request: " + relayResultRequestTunnel2.getRealURL() + ", connectivity: " + relayResultRequestTunnel2.getConnectivityInfo());
        return relayResultRequestTunnel2;
    }

    private RelayResult pingDSM(ServicePingBuilder servicePingBuilder, ServerInfo serverInfo, ServiceInfo serviceInfo, String str, String str2) {
        int i;
        int i2;
        int holePunchTimeout = getHolePunchTimeout();
        if (getContext() != null) {
            boolean zIsDeviceInChina = QuickConnectUtil.isDeviceInChina(getContext());
            // NetworkUtils.isWifiConnected(getContext())
            if (true) {
                i2 = zIsDeviceInChina ? 1 : 0;
                i = 0;
            } else {
                i = holePunchTimeout;
                i2 = zIsDeviceInChina ? 1 : 0;
            }
        } else {
            i = holePunchTimeout;
            i2 = 0;
        }
       // this.mSubjectPunchIdleTimeout
        return new ApiPingDSM(serverInfo, serviceInfo, str, str2, getUseHolePunch(), i, i2, null).call(servicePingBuilder, getIgnoreConnectionPriority().booleanValue());
    }

    private RelayResult requestTunnel(String str, String str2, String str3, boolean z) throws IOException {
        return new ApiRequestTunnel(str, str2, str3, ((TelephonyManager) getContext().getSystemService("phone")).getNetworkCountryIso(), z).call();
    }

    public Boolean getIgnoreConnectionPriority() {
        return this.mIgnoreConnectionPriority;
    }

    public void setIgnoreConnectionPriority(Boolean bool) {
        this.mIgnoreConnectionPriority = bool;
    }

    private Context getContext() {
        return SyHttpClient.getContext();
    }

    private class ApiTaskResult {
        final DSMInfo dsmInfo;
        final boolean isChina;
        final Throwable throwable;

        ApiTaskResult(DSMInfo dSMInfo, Throwable th, boolean z) {
            this.dsmInfo = dSMInfo;
            this.throwable = th;
            this.isChina = z;
        }
    }

    private class CallRelayServersTask implements Callable<ApiTaskResult> {
        private final ApiRelayServers api;

        CallRelayServersTask(ApiRelayServers apiRelayServers) {
            this.api = apiRelayServers;
        }

        @Override // java.util.concurrent.Callable
        public ApiTaskResult call() throws IOException {
            DSMInfo dSMInfoCall = null;
            Throwable th;
            try {
                th = null;
                dSMInfoCall = this.api.call();
            } catch (Throwable throwable) {
                th = throwable;
            }
            return RelayManager.this.new ApiTaskResult(dSMInfoCall, th, this.api.isCallChinaSite());
        }
    }
}