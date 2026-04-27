package com.synology.sylib.security.recover;

import android.content.Context;
import com.synology.sylib.security.internal.method.CryptMethod;
import com.synology.sylib.security.internal.method.SimpleCryptMethod;
import java.security.KeyStore;
import java.util.HashMap;

/* loaded from: classes.dex */
public class AesKeyRecoverUtil {
    private static final HashMap<String, SimpleCryptMethod> backupCryptMethods = new HashMap<>();

    public static void setupBackupCryptMethod(String str, KeyStore keyStore, Context context) {
        backupCryptMethods.put(str, new SimpleCryptMethod(str, keyStore, context));
    }

    public static CryptMethod getBackupMethod(CryptMethod cryptMethod) {
        return backupCryptMethods.get(cryptMethod.getKeyAlias());
    }
}