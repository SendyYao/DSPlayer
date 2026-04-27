package com.synology.synoholepunch;

/* loaded from: classes.dex */
public interface SynoPunchCallback {
    void URDClosed();

    void URDConnected(int i);

    void URDIdleTimeout(int i, int i2);

    void URDLog(String str);
}