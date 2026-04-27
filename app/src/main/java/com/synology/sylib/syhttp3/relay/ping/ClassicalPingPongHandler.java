package com.synology.sylib.syhttp3.relay.ping;

import com.synology.sylib.syhttp3.exceptions.UnexpectedJsonException;
import com.synology.sylib.syhttp3.relay.ServiceId;
import com.synology.sylib.syhttp3.relay.util.SafeGson;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import com.synology.sylib.syhttp3.relay.vos.PingPongVo;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ClassicalPingPongHandler.kt */
@Metadata(d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002J \u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u000bH\u0016R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\f"}, d2 = {"Lcom/synology/sylib/syhttp3/relay/ping/ClassicalPingPongHandler;", "Lcom/synology/sylib/syhttp3/relay/ping/PingPongHandler;", "()V", "mSafeGson", "Lcom/synology/sylib/syhttp3/relay/util/SafeGson;", "checkIfWanted", "", "serviceId", "", "serverId2", "inputStream", "Ljava/io/InputStream;", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
/* loaded from: classes2.dex */
public final class ClassicalPingPongHandler implements PingPongHandler {
    private final SafeGson mSafeGson = new SafeGson();

    @Override // com.synology.sylib.syhttp3.relay.ping.PingPongHandler
    public boolean checkIfWanted(String serviceId, String serverId2, InputStream inputStream) throws NoSuchAlgorithmException, UnexpectedJsonException {
        Intrinsics.checkNotNullParameter(serviceId, "serviceId");
        Intrinsics.checkNotNullParameter(serverId2, "serverId2");
        Intrinsics.checkNotNullParameter(inputStream, "inputStream");
        if (Intrinsics.areEqual(serviceId, ServiceId.WEBDAV_HTTP) || Intrinsics.areEqual(serviceId, ServiceId.WEBDAV_HTTPS)) {
            return true;
        }
        Object objFromJson = this.mSafeGson.fromJson(inputStream, PingPongVo.class, StandardCharsets.UTF_8);
        Intrinsics.checkNotNullExpressionValue(objFromJson, "mSafeGson.fromJson(input…, StandardCharsets.UTF_8)");
        return RelayUtil.isValidPingPong(serverId2, (PingPongVo) objFromJson);
    }
}