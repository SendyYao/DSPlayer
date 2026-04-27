package com.synology.sylib.syhttp3.relay;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.synology.sylib.history.SynoApplication;
import com.synology.sylib.syhttp3.factory.OkHttpClientFactory;
import com.synology.sylib.syhttp3.relay.models.PunchIdleTimeoutInfo;
import com.synology.sylib.syhttp3.relay.ping.PingPongHandler;
import com.synology.sylib.syhttp3.relay.util.SafeGson;
import com.synology.sylib.syhttp3.relay.utils.RelayExecutors;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import com.synology.sylib.syhttp3.util.OkHttpClientUtil;
import com.synology.sylib.util.IOUtils;
import io.reactivex.rxjava3.functions.Consumer;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/* loaded from: classes2.dex */
public class RelayResolveManager {
    private static final String OKHTTP_CLIENT_TAG = "RelayResolveManager";
    private static final String TAG = "RelayResolveManager";
    private static RelayResolveManager sInstance;
    private Map<RelayRecordKey, RelayRecord> mResolvedHostMap = new HashMap();
    private List<Callback> mCallbaks = new ArrayList();
    private RelayManager mRelayManager = RelayManager.getInstance();
    private OkHttpClient mHttpClient = OkHttpClientFactory.newOkHttpClientBuilder(OKHTTP_CLIENT_TAG).build();
    private SafeGson mSafeGson = new SafeGson();

    enum CacheType {
        DISK,
        MEMORY
    }

    interface Callback {
        void onConnectFail(IOException iOException, RelayRecordKey relayRecordKey, RelayRecord relayRecord);

        void onConnectivityChanged(RelayRecordKey relayRecordKey, RelayRecord relayRecord, RelayRecord relayRecord2);

        void onPunchTimeout(String str, String str2, int i);
    }

    public static RelayResolveManager getInstance() {
        if (sInstance == null) {
            synchronized (RelayResolveManager.class) {
                if (sInstance == null) {
                    sInstance = new RelayResolveManager();
                }
            }
        }
        return sInstance;
    }

    private RelayResolveManager() {
        this.mRelayManager.getObservablePunchIdleTimeout().onTerminateDetach().subscribe(new Consumer<PunchIdleTimeoutInfo>() { // from class: com.synology.sylib.syhttp3.relay.RelayResolveManager.1

            @Override // io.reactivex.functions.Consumer
            public void accept(PunchIdleTimeoutInfo punchIdleTimeoutInfo) throws Exception {
                RelayResolveManager.this.onPunchTimeout(punchIdleTimeoutInfo);
            }
        }, new Consumer<Throwable>() { // from class: com.synology.sylib.syhttp3.relay.RelayResolveManager.2

            @Override // io.reactivex.functions.Consumer
            public void accept(Throwable th) throws Exception {
                th.printStackTrace();
            }
        });
    }

    /* renamed from: com.synology.sylib.syhttp3.relay.RelayResolveManager$1 */
    class AnonymousClass1 implements Consumer<PunchIdleTimeoutInfo> {

        @Override // io.reactivex.functions.Consumer
        public void accept(PunchIdleTimeoutInfo punchIdleTimeoutInfo) throws Exception {
            RelayResolveManager.this.onPunchTimeout(punchIdleTimeoutInfo);
        }
    }

    /* renamed from: com.synology.sylib.syhttp3.relay.RelayResolveManager$2 */
    class AnonymousClass2 implements Consumer<Throwable> {

        @Override // io.reactivex.functions.Consumer
        public void accept(Throwable th) throws Exception {
            th.printStackTrace();
        }
    }

    public void registerCallback(Callback callback) {
        this.mCallbaks.add(callback);
    }

    public void unregisterCallback(Callback callback) {
        this.mCallbaks.remove(callback);
    }

    private Map<RelayRecordKey, RelayRecord> getRelayRecordCache(CacheType cacheType) {
        if (cacheType == CacheType.DISK) {
            return RelayUtil.getAllRelayRecords();
        }
        if (cacheType == CacheType.MEMORY) {
            return this.mResolvedHostMap;
        }
        return Collections.emptyMap();
    }

    synchronized void checkQuickConnectDiskCache(boolean z) {
        checkQuickConnectCache(z, CacheType.DISK);
    }

    public synchronized void checkQuickConnectMemoryCache() {
        checkQuickConnectCache(true, CacheType.MEMORY);
    }


    synchronized void checkQuickConnectCache(boolean z, CacheType cacheType) {
        ExecutorService executorServiceNewAsyncTaskExecutor = RelayExecutors.newAsyncTaskExecutor(TAG);
        try {
            ExecutorCompletionService executorCompletionService = new ExecutorCompletionService(executorServiceNewAsyncTaskExecutor);
            int i = 0;
            for (Map.Entry<RelayRecordKey, RelayRecord> entry : getRelayRecordCache(cacheType).entrySet()) {
                RelayRecordKey key = entry.getKey();
                RelayRecord value = entry.getValue();
                if (!TextUtils.isEmpty(key.getServerId()) && value != null && value.getServiceIds() != null && value.getPingPongPaths() != null) {
//                    executorCompletionService.submit(newCallable(key, value, z));
                    i++;
                }
            }
            for (int i2 = 0; i2 < i; i2++) {
                try {
                    try {
                        RelayRecord relayRecord = (RelayRecord) executorCompletionService.take().get();
                        if (relayRecord != null) {
                            Log.d(TAG, "resolved serviceId: " + relayRecord.getServerId() + ", realURL: " + relayRecord.getRealURL());
                        }
                    } catch (InterruptedException e) {
                        String message = e.getMessage();
                        String str = TAG;
                        StringBuilder sbAppend = new StringBuilder().append("InterruptedException: ");
                        if (message == null) {
                            message = "";
                        }
                        Log.e(str, sbAppend.append(message).toString());
                    }
                } catch (ExecutionException e2) {
                    Log.e(TAG, "ExecutionException: ", e2);
                }
            }
        } finally {
            executorServiceNewAsyncTaskExecutor.shutdownNow();
        }
    }

    public boolean pingConnection(String str, RelayRecord relayRecord) throws IOException, NoSuchAlgorithmException {
        String serverId2 = "";
        InputStream inputStreamByteStream = null;
        URL realURL = relayRecord.getRealURL();
        if (realURL == null) {
            return false;
        }
        String realPingPongPath = relayRecord.getRealPingPongPath();
        if (TextUtils.isEmpty(realPingPongPath)) {
            return false;
        }
        OkHttpClient.Builder timeout = this.mHttpClient.newBuilder().connectTimeout(5L, TimeUnit.SECONDS).readTimeout(5L, TimeUnit.SECONDS);
        OkHttpClientUtil.bypassVerifyCertificate(timeout);
        this.mHttpClient = timeout.build();
        PingPongHandler pingPongHandler = this.mRelayManager.getPingPongHandler();
        for (String str2 : relayRecord.getServiceIds()) {
            try {
                serverId2 = relayRecord.getServerId2();
                inputStreamByteStream = this.mHttpClient.newCall(new Request.Builder().url(new URL(realURL.getProtocol(), realURL.getHost(), realURL.getPort(), realPingPongPath)).build()).execute().body().byteStream();
            } catch (MalformedURLException e) {
                String message = e.getMessage();
                String str3 = TAG;
                StringBuilder sb = new StringBuilder("MalformedURLException: ");
                if (message == null) {
                    message = "";
                }
                Log.e(str3, sb.append(message).toString());
            } catch (IOException e2) {
                String message2 = e2.getMessage();
                String str4 = TAG;
                StringBuilder sb2 = new StringBuilder("pingConnection failed: ");
                if (message2 == null) {
                    message2 = "";
                }
                Log.e(str4, sb2.append(message2).toString());
            }
            try {
                if (pingPongHandler.checkIfWanted(str2, serverId2, inputStreamByteStream)) {
                    IOUtils.closeSilently(inputStreamByteStream);
                    return true;
                }
                IOUtils.closeSilently(inputStreamByteStream);
            } catch (Throwable th) {
                IOUtils.closeSilently(inputStreamByteStream);
                throw th;
            }
        }
        return false;
    }

    public RelayRecord quickConnect(RelayRecordKey relayRecordKey, RelayRecord relayRecord) {
        try {
            RelayRecord relayRecordClone = RelayRecord.clone(relayRecord);
            RelayRecord relayRecordFetchRealURL = this.mRelayManager.fetchRealURL(relayRecordKey);
            String str = TAG;
            Log.i(str, "old address: " + relayRecordClone.getRealURL());
            Log.i(str, "new address: " + relayRecordFetchRealURL.getRealURL());
            RelayUtil.setRelayRecord(relayRecordFetchRealURL);
            onConnectivityChanged(relayRecordKey, relayRecordClone, relayRecordFetchRealURL);
            return relayRecordFetchRealURL;
        } catch (IOException e) {
            String message = e.getMessage();
            String str2 = TAG;
            StringBuilder sb = new StringBuilder("quickConnect failed: ");
            if (message == null) {
                message = "";
            }
            Log.e(str2, sb.append(message).toString());
            onConnectFail(e, relayRecordKey, relayRecord);
            return null;
        }
    }

    @Deprecated
    private RelayRecord checkRecordProtocol(Context context, String str, RelayRecord relayRecord, boolean z) throws IOException, ClassNotFoundException {
        URL realURL;
        return (!SynoApplication.DSCLOUD.equals(context.getPackageName()) || (realURL = relayRecord.getRealURL()) == null || realURL.getProtocol().equalsIgnoreCase("https") == z) ? relayRecord : RelayUtil.newRelayRecord(str, z);
    }

    public RelayRecord getResolvedRecord(Context context, String str, boolean z) throws IOException, ClassNotFoundException {
        RelayRecord relayRecordCheckRecordProtocol;
        RelayRecordKey relayRecordKey = RelayRecordKey.getInstance(context, str, z);
        synchronized (this) {
            if (!this.mResolvedHostMap.containsKey(relayRecordKey)) {
                checkQuickConnectDiskCache(true);
                RelayRecord relayRecord = RelayUtil.getRelayRecord(relayRecordKey);
                if (relayRecord == null) {
                    relayRecordCheckRecordProtocol = RelayUtil.newRelayRecord(str, z);
                } else {
                    relayRecordCheckRecordProtocol = checkRecordProtocol(context, str, relayRecord, z);
                }
                this.mResolvedHostMap.put(relayRecordKey, relayRecordCheckRecordProtocol);
            } else {
                this.mResolvedHostMap.put(relayRecordKey, checkRecordProtocol(context, str, this.mResolvedHostMap.get(relayRecordKey), z));
            }
        }
        return this.mResolvedHostMap.get(relayRecordKey);
    }

    public void clearResolvedResult(RelayRecordKey relayRecordKey) {
        RelayRecord relayRecordRemove = this.mResolvedHostMap.remove(relayRecordKey);
        if (relayRecordRemove != null) {
            stopPunchDaemonIfNeeded(relayRecordRemove);
        }
    }

    public void clearAllResolvedResult() {
        this.mResolvedHostMap.clear();
        stopAllPunchDaemon();
    }

    private void stopPunchDaemonIfNeeded(RelayRecord relayRecord) {
        if (relayRecord.getConnectivity() == 6) {
            String serverId = relayRecord.getServerId();
            for (String str : relayRecord.getServiceIds()) {
                PunchInfoManager.getInstance().stop(serverId, str);
            }
        }
    }

    private void stopAllPunchDaemon() {
        PunchInfoManager.getInstance().stopAll();
    }

    private void onConnectivityChanged(RelayRecordKey relayRecordKey, RelayRecord relayRecord, RelayRecord relayRecord2) {
        this.mResolvedHostMap.put(relayRecordKey, relayRecord2);
        Iterator<Callback> it = this.mCallbaks.iterator();
        while (it.hasNext()) {
            it.next().onConnectivityChanged(relayRecordKey, relayRecord, relayRecord2);
        }
    }

    private void onConnectFail(IOException iOException, RelayRecordKey relayRecordKey, RelayRecord relayRecord) {
        RelayRecord relayRecord2;
        if (relayRecord != null && (relayRecord2 = this.mResolvedHostMap.get(relayRecordKey)) != null && relayRecord2.getRealURL().equals(relayRecord.getRealURL())) {
            this.mResolvedHostMap.remove(relayRecordKey);
            stopPunchDaemonIfNeeded(relayRecord2);
        }
        Iterator<Callback> it = this.mCallbaks.iterator();
        while (it.hasNext()) {
            it.next().onConnectFail(iOException, relayRecordKey, relayRecord);
        }
    }

    public void onPunchTimeout(PunchIdleTimeoutInfo punchIdleTimeoutInfo) {
        onPunchTimeout(punchIdleTimeoutInfo.getServerID(), punchIdleTimeoutInfo.getServiceID(), punchIdleTimeoutInfo.getPort());
    }

    private void onPunchTimeout(String str, String str2, int i) {
        Iterator<Callback> it = this.mCallbaks.iterator();
        while (it.hasNext()) {
            it.next().onPunchTimeout(str, str2, i);
        }
    }
}