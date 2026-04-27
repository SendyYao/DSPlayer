package com.synology.sylib.syhttp3.domainintercepter;

import android.content.Context;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import kotlin.Metadata;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/* compiled from: ResolveCache.kt */
@Metadata(d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\bÆ\u0002\u0018\u00002\u00020\u0001:\u0001\u0010B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004J\u0018\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\f\u001a\u00020\rH\u0007J \u0010\u000e\u001a\u00020\u000b2\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u0006H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T¢\u0006\u0002\n\u0000¨\u0006\u0011"}, d2 = {"Lcom/synology/sylib/syhttp3/domainintercepter/ResolveCache;", "", "()V", "PREF_RESOLVE_CACHE", "", "getLastUsedIPAddressType", "Lcom/synology/sylib/syhttp3/domainintercepter/ResolveCache$IPAddressType;", "context", "Landroid/content/Context;", "hostname", "saveNetworkDetailWhenConnectEnd", "", "inetSocketAddress", "Ljava/net/InetSocketAddress;", "setLastUsedIPAddressType", "ipAddressType", "IPAddressType", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
/* loaded from: classes2.dex */
public final class ResolveCache {
    public static final ResolveCache INSTANCE = new ResolveCache();
    private static final String PREF_RESOLVE_CACHE = "pref_resolve_cache";

    private ResolveCache() {
    }

    /* compiled from: ResolveCache.kt */
    @Metadata(d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0001\u0018\u0000 \u00052\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\u0005B\u0007\b\u0002¢\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004¨\u0006\u0006"}, d2 = {"Lcom/synology/sylib/syhttp3/domainintercepter/ResolveCache$IPAddressType;", "", "(Ljava/lang/String;I)V", "IPV4", "IPV6", "Companion", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
    public enum IPAddressType {
        IPV4,
        IPV6;


        /* renamed from: Companion, reason: from kotlin metadata */
        public static final Companion INSTANCE = new Companion(null);

        /* compiled from: ResolveCache.kt */
        @Metadata(d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006¨\u0006\u0007"}, d2 = {"Lcom/synology/sylib/syhttp3/domainintercepter/ResolveCache$IPAddressType$Companion;", "", "()V", "getIPAddressType", "Lcom/synology/sylib/syhttp3/domainintercepter/ResolveCache$IPAddressType;", "str", "", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
        public static final class Companion {
            public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
                this();
            }

            private Companion() {
            }

            public final IPAddressType getIPAddressType(String str) {
                for (IPAddressType iPAddressType : IPAddressType.values()) {
                    if (StringsKt.equals(iPAddressType.name(), str, true)) {
                        return iPAddressType;
                    }
                }
                return IPAddressType.IPV4;
            }
        }
    }

    public final IPAddressType getLastUsedIPAddressType(Context context, String hostname) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(hostname, "hostname");
        return IPAddressType.INSTANCE.getIPAddressType(context.getSharedPreferences(PREF_RESOLVE_CACHE, 0).getString(hostname, "IPV4"));
    }

    private final void setLastUsedIPAddressType(Context context, String hostname, IPAddressType ipAddressType) {
        context.getSharedPreferences(PREF_RESOLVE_CACHE, 0).edit().putString(hostname, ipAddressType.name()).apply();
    }

    @JvmStatic
    public static final void saveNetworkDetailWhenConnectEnd(Context context, InetSocketAddress inetSocketAddress) {
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(inetSocketAddress, "inetSocketAddress");
        String hostname = inetSocketAddress.getHostName();
        IPAddressType iPAddressType = inetSocketAddress.getAddress() instanceof Inet4Address ? IPAddressType.IPV4 : IPAddressType.IPV6;
        ResolveCache resolveCache = INSTANCE;
        Intrinsics.checkNotNullExpressionValue(hostname, "hostname");
        resolveCache.setLastUsedIPAddressType(context, hostname, iPAddressType);
    }
}