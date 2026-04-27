package com.synology.sylib.syhttp3.relay.ping;

import com.synology.sylib.syhttp3.exceptions.UnexpectedJsonException;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import kotlin.Metadata;

/* compiled from: PingPongHandler.kt */
@Metadata(d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J \u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\bH&¨\u0006\t"}, d2 = {"Lcom/synology/sylib/syhttp3/relay/ping/PingPongHandler;", "", "checkIfWanted", "", "serviceId", "", "serverId2", "inputStream", "Ljava/io/InputStream;", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
/* loaded from: classes2.dex */
public interface PingPongHandler {
    boolean checkIfWanted(String serviceId, String serverId2, InputStream inputStream) throws NoSuchAlgorithmException, UnexpectedJsonException;
}