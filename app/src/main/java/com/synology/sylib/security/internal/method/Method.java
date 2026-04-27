package com.synology.sylib.security.internal.method;

/* loaded from: classes.dex */
public enum Method {
    NOT_SPECIFIC(-1),
    SIMPLE_ENCODE(0),
    RSA_HYBRID(1),
    AES_KEYSTORE(2);

    private final int value;

    Method(int i) {
        this.value = i;
    }

    public int getValue() {
        return this.value;
    }

    public static Method get(int i) {
        for (Method method : values()) {
            if (method.value == i) {
                return method;
            }
        }
        return NOT_SPECIFIC;
    }
}