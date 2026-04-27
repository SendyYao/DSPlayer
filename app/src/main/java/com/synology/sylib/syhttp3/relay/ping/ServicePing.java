package com.synology.sylib.syhttp3.relay.ping;

import com.synology.sylib.syhttp3.relay.RelayResult;
import java.util.concurrent.Callable;

/* loaded from: classes2.dex */
public interface ServicePing {
    Callable<RelayResult> ping(String str, String str2, int i, int i2);
}