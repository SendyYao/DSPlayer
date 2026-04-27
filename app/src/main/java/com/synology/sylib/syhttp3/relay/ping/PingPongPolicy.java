package com.synology.sylib.syhttp3.relay.ping;

import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: PingPongPolicy.kt */
@Metadata(d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u0000 \u00072\u00020\u0001:\u0003\u0006\u0007\bB\u000f\b\u0004\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004J\u0006\u0010\u0005\u001a\u00020\u0003R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000\u0082\u0001\u0002\t\n¨\u0006\u000b"}, d2 = {"Lcom/synology/sylib/syhttp3/relay/ping/PingPongPolicy;", "", "handler", "Lcom/synology/sylib/syhttp3/relay/ping/PingPongHandler;", "(Lcom/synology/sylib/syhttp3/relay/ping/PingPongHandler;)V", "getPingPongHandler", "Classical", "Companion", "Customized", "Lcom/synology/sylib/syhttp3/relay/ping/PingPongPolicy$Classical;", "Lcom/synology/sylib/syhttp3/relay/ping/PingPongPolicy$Customized;", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
/* loaded from: classes2.dex */
public abstract class PingPongPolicy {
    public static PingPongPolicy defaultPolicy = Classical.INSTANCE;
    private final PingPongHandler handler;

    public /* synthetic */ PingPongPolicy(PingPongHandler pingPongHandler, DefaultConstructorMarker defaultConstructorMarker) {
        this(pingPongHandler);
    }

    private PingPongPolicy(PingPongHandler pingPongHandler) {
        this.handler = pingPongHandler;
    }

    /* compiled from: PingPongPolicy.kt */
    @Metadata(d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bÆ\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002¨\u0006\u0003"}, d2 = {"Lcom/synology/sylib/syhttp3/relay/ping/PingPongPolicy$Classical;", "Lcom/synology/sylib/syhttp3/relay/ping/PingPongPolicy;", "()V", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
    public static final class Classical extends PingPongPolicy {
        public static final Classical INSTANCE = new Classical();

        private Classical() {
            super(new ClassicalPingPongHandler(), null);
        }
    }

    /* compiled from: PingPongPolicy.kt */
    @Metadata(d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004¨\u0006\u0005"}, d2 = {"Lcom/synology/sylib/syhttp3/relay/ping/PingPongPolicy$Customized;", "Lcom/synology/sylib/syhttp3/relay/ping/PingPongPolicy;", "handler", "Lcom/synology/sylib/syhttp3/relay/ping/PingPongHandler;", "(Lcom/synology/sylib/syhttp3/relay/ping/PingPongHandler;)V", "syhttp3_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
    public static final class Customized extends PingPongPolicy {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public Customized(PingPongHandler handler) {
            super(handler, null);
            Intrinsics.checkNotNullParameter(handler, "handler");
        }
    }

    /* renamed from: getPingPongHandler, reason: from getter */
    public final PingPongHandler getHandler() {
        return this.handler;
    }
}