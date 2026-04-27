package com.synology.sylib.syhttp3;

import android.content.Context;
import android.util.Log;
import com.synology.sylib.syhttp3.domainintercepter.CustomDns;
import com.synology.sylib.syhttp3.domainintercepter.ResolveCache;
import com.synology.sylib.syhttp3.factory.OkHttpClientFactory;
import com.synology.sylib.syhttp3.interceptors.CertificateInterceptor;
import com.synology.sylib.syhttp3.interceptors.RelayInterceptor;
import com.synology.sylib.syhttp3.interceptors.UserAgentInterceptor;
import com.synology.sylib.syhttp3.relay.RelayManager;
import com.synology.synoholepunch.CertFileTask;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.CookieJar;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;

/* loaded from: classes2.dex */
public class SyHttpClient {
    private static final String OKHTTP_CLIENT_TAG = "SyHttpClient";
    private static final String TAG = "SyHttpClient";
    private static Context mContext;
    private static AtomicInteger mLastGetTrustManagerMethod = new AtomicInteger(0);
    private CertificateInterceptor mCertificateInterceptor;
    private OkHttpClient mClient;
    private CookieHandler mCookieHandler;
    private RelayInterceptor mRelayInterceptor;

    public static void setContext(Context context) {
        mContext = context.getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }

    private static OkHttpClient.Builder getOkHttpClientBuilder() {
        return OkHttpClientFactory.newOkHttpClientBuilder(OKHTTP_CLIENT_TAG);
    }

    public SyHttpClient() {
        this(getOkHttpClientBuilder().build(), null);
    }

    public SyHttpClient(String str) {
        this(getOkHttpClientBuilder().build(), str);
    }

    public SyHttpClient(boolean z) {
        this(getOkHttpClientBuilder().followRedirects(z).followSslRedirects(z).build());
    }

    public SyHttpClient(OkHttpClient okHttpClient) {
        this(okHttpClient, null);
    }

    public SyHttpClient(OkHttpClient okHttpClient, String str) {
        this.mClient = addCustomDnsTo(okHttpClient);
        RelayInterceptor relayInterceptor = new RelayInterceptor(this);
        this.mRelayInterceptor = relayInterceptor;
        addInterceptor(relayInterceptor);
        CertificateInterceptor certificateInterceptor = new CertificateInterceptor();
        this.mCertificateInterceptor = certificateInterceptor;
        addNetworkInterceptor(certificateInterceptor);
        addNetworkInterceptor(new UserAgentInterceptor(str));
        OkHttpClientFactory.onSyHttpCreated(this);
    }

    public static void setUseHolePunch(Context context, boolean z) {
        RelayManager.getInstance().setUseHolePunch(z);
        if (z) {
            CertFileTask.setContext(context);
        }
    }

    public static void setHolePunchTimeout(int i) {
        RelayManager.getInstance().setHolePunchTimeout(i);
    }

    public static void setIgnoreConnectionPriority(Boolean bool) {
        RelayManager.getInstance().setIgnoreConnectionPriority(bool);
    }

    public boolean isVerifyCertificate() {
        return this.mRelayInterceptor.isVerifyCertificate();
    }

    public void setVerifyCertificate(boolean z) {
        this.mRelayInterceptor.setVerifyCertificate(z);
    }

    public boolean isVerifyCertificateFingerprint() {
        return this.mCertificateInterceptor.isVerifyFingerprint();
    }

    public void setVerifyCertificateFingerprint(boolean z) {
        if (z) {
            if (this.mClient.networkInterceptors().contains(this.mCertificateInterceptor)) {
                return;
            }
            addNetworkInterceptor(this.mCertificateInterceptor);
        } else if (this.mClient.networkInterceptors().contains(this.mCertificateInterceptor)) {
            removeNetworkInterceptor(this.mCertificateInterceptor);
        }
    }

    private OkHttpClient addCustomDnsTo(OkHttpClient okHttpClient) {
        OkHttpClient.Builder builderNewBuilder = okHttpClient.newBuilder();
        builderNewBuilder.dns(new CustomDns(mContext.getApplicationContext()));
        builderNewBuilder.eventListener(new EventListener() {
            @Override // okhttp3.EventListener
            public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
                super.connectEnd(call, inetSocketAddress, proxy, protocol);
                SyHttpClient.this.asyncSaveNetworkDetailWhenConnectEnd(inetSocketAddress);
            }
        });
        return builderNewBuilder.build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void asyncSaveNetworkDetailWhenConnectEnd(final InetSocketAddress inetSocketAddress) {
        new Thread(new Runnable() { // from class: com.synology.sylib.syhttp3.SyHttpClient.2
            @Override // java.lang.Runnable
            public void run() {
                ResolveCache.saveNetworkDetailWhenConnectEnd(SyHttpClient.mContext.getApplicationContext(), inetSocketAddress);
            }
        }).start();
    }

    public void connectionSpecs(ConnectionSpec... connectionSpecArr) {
        this.mClient = this.mClient.newBuilder().connectionSpecs(Arrays.asList(connectionSpecArr)).build();
    }

    @Deprecated
    public void setSslSocketFactory(SSLSocketFactory sSLSocketFactory) throws NoSuchAlgorithmException, KeyStoreException, NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
        this.mClient = setSslSocketFactoryLegacy(this.mClient.newBuilder(), sSLSocketFactory).build();
    }

    private OkHttpClient.Builder setSslSocketFactoryLegacy(OkHttpClient.Builder builder, SSLSocketFactory sSLSocketFactory) throws IllegalStateException, IllegalAccessException, NoSuchFieldException, NoSuchAlgorithmException, ClassNotFoundException, KeyStoreException, IllegalArgumentException {
        int i = mLastGetTrustManagerMethod.get();
        while (true) {
            if (i >= 3) {
                break;
            }
            if (i == 0) {
                try {
                    builder.sslSocketFactory(sSLSocketFactory);
                    mLastGetTrustManagerMethod.set(i);
                    Log.d(TAG, "Set sslSocketFactory with deprecated method success");
                    break;
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Set sslSocketFactory with deprecated method fail", e);
                }
            } else {
                if (i != 1) {
                    if (i != 2) {
                        i++;
                    } else {
                        X509TrustManager x509TrustManagerByReflection = TrustManagerProvider.getX509TrustManagerByReflection(sSLSocketFactory);
                        if (x509TrustManagerByReflection != null) {
                            builder.sslSocketFactory(sSLSocketFactory, x509TrustManagerByReflection);
                            mLastGetTrustManagerMethod.set(i);
                            Log.d(TAG, "Set sslSocketFactory with reflection success");
                        } else {
                            Log.w(TAG, "Can not retrieve trust manager by reflection");
                            throw new IllegalStateException("Unable to extract the trust manager when setting sslSocketFactory");
                        }
                    }
                }
                X509TrustManager x509TrustManagerByFactory = TrustManagerProvider.getX509TrustManagerByFactory();
                if (x509TrustManagerByFactory != null) {
                    builder.sslSocketFactory(sSLSocketFactory, x509TrustManagerByFactory);
                    mLastGetTrustManagerMethod.set(i);
                    Log.d(TAG, "Set sslSocketFactory with factory and trustManager");
                    break;
                }
                Log.w(TAG, "Can not retrieve trust manager by TrustManagerFactory");
                i++;
            }
        }
        return builder;
    }

    public void setSslSocketFactory(SSLSocketFactory sSLSocketFactory, X509TrustManager x509TrustManager) {
        this.mClient = this.mClient.newBuilder().sslSocketFactory(sSLSocketFactory, x509TrustManager).build();
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.mClient = this.mClient.newBuilder().hostnameVerifier(hostnameVerifier).build();
    }

    public void setConnectTimeout(long j, TimeUnit timeUnit) {
        this.mClient = this.mClient.newBuilder().connectTimeout(j, timeUnit).build();
    }

    public void setCookieHandler(CookieHandler cookieHandler) {
        setCookieJar(new JavaNetCookieJar(cookieHandler));
        this.mCookieHandler = cookieHandler;
    }

    public void setCookieJar(CookieJar cookieJar) {
        this.mCookieHandler = null;
        this.mClient = this.mClient.newBuilder().cookieJar(cookieJar).build();
    }

    public CookieHandler getCookieHandler() {
        return this.mCookieHandler;
    }

    public CookieJar getCookieJar() {
        return this.mClient.cookieJar();
    }

    public void addInterceptor(Interceptor interceptor) {
        OkHttpClient.Builder builderNewBuilder = this.mClient.newBuilder();
        builderNewBuilder.addInterceptor(interceptor);
        this.mClient = builderNewBuilder.build();
    }

    public void addInterceptor(int i, Interceptor interceptor) {
        OkHttpClient.Builder builderNewBuilder = this.mClient.newBuilder();
        builderNewBuilder.interceptors().add(i, interceptor);
        this.mClient = builderNewBuilder.build();
    }

    public void removeInterceptor(Interceptor interceptor) {
        OkHttpClient.Builder builderNewBuilder = this.mClient.newBuilder();
        builderNewBuilder.interceptors().remove(interceptor);
        this.mClient = builderNewBuilder.build();
    }

    public void addNetworkInterceptor(Interceptor interceptor) {
        OkHttpClient.Builder builderNewBuilder = this.mClient.newBuilder();
        builderNewBuilder.addNetworkInterceptor(interceptor);
        this.mClient = builderNewBuilder.build();
    }

    public void addNetworkInterceptor(int i, Interceptor interceptor) {
        OkHttpClient.Builder builderNewBuilder = this.mClient.newBuilder();
        builderNewBuilder.networkInterceptors().add(i, interceptor);
        this.mClient = builderNewBuilder.build();
    }

    public void removeNetworkInterceptor(Interceptor interceptor) {
        OkHttpClient.Builder builderNewBuilder = this.mClient.newBuilder();
        builderNewBuilder.networkInterceptors().remove(interceptor);
        this.mClient = builderNewBuilder.build();
    }

    public void setReadTimeout(long j, TimeUnit timeUnit) {
        this.mClient = this.mClient.newBuilder().readTimeout(j, timeUnit).build();
    }

    public Call newCall(Request request) {
        return this.mClient.newCall(request);
    }

    public void cancel(Object obj) {
        for (Call call : this.mClient.dispatcher().queuedCalls()) {
            if (obj.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call2 : this.mClient.dispatcher().runningCalls()) {
            if (obj.equals(call2.request().tag())) {
                call2.cancel();
            }
        }
    }

    public OkHttpClient getClient() {
        return this.mClient;
    }
}