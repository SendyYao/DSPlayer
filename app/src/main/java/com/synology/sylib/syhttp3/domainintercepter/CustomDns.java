package com.synology.sylib.syhttp3.domainintercepter;

import android.content.Context;
import android.text.TextUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import okhttp3.Dns;

/* compiled from: CustomDns.kt */
@Metadata(d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\u0006\u0010\b\u001a\u00020\tH\u0002J\u0016\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\u0006\u0010\b\u001a\u00020\tH\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\u000b"}, d2 = {"Lcom/synology/sylib/syhttp3/domainintercepter/CustomDns;", "Lokhttp3/Dns;", "applicationContext", "Landroid/content/Context;", "(Landroid/content/Context;)V", "getInetAddressList", "", "Ljava/net/InetAddress;", "hostname", "", "lookup", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
/* loaded from: classes2.dex */
public final class CustomDns implements Dns {
    private final Context applicationContext;

    public CustomDns(Context applicationContext) {
        Intrinsics.checkNotNullParameter(applicationContext, "applicationContext");
        this.applicationContext = applicationContext;
    }

    @Override // okhttp3.Dns
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        Intrinsics.checkNotNullParameter(hostname, "hostname");
        if (TextUtils.isEmpty(hostname)) {
            return Dns.SYSTEM.lookup(hostname);
        }
        try {
            return getInetAddressList(hostname);
        } catch (Exception unused) {
            return Dns.SYSTEM.lookup(hostname);
        }
    }

    private final List<InetAddress> getInetAddressList(String hostname) throws UnknownHostException {
        InetAddress[] inetAddresses = InetAddress.getAllByName(hostname);
        ArrayList arrayList = new ArrayList();
        if (ResolveCache.INSTANCE.getLastUsedIPAddressType(this.applicationContext, hostname) == ResolveCache.IPAddressType.IPV4) {
            Intrinsics.checkNotNullExpressionValue(inetAddresses, "inetAddresses");
            for (InetAddress inetAddress : inetAddresses) {
                if (inetAddress instanceof Inet4Address) {
                    arrayList.add(0, inetAddress);
                } else {
                    Intrinsics.checkNotNullExpressionValue(inetAddress, "inetAddress");
                    arrayList.add(inetAddress);
                }
            }
        } else {
            Intrinsics.checkNotNullExpressionValue(inetAddresses, "inetAddresses");
            for (InetAddress inetAddress2 : inetAddresses) {
                if (inetAddress2 instanceof Inet6Address) {
                    arrayList.add(0, inetAddress2);
                } else {
                    Intrinsics.checkNotNullExpressionValue(inetAddress2, "inetAddress");
                    arrayList.add(inetAddress2);
                }
            }
        }
        return arrayList;
    }
}