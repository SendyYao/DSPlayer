package com.synology.sylib.security.internal.method;

import android.security.keystore.KeyGenParameterSpec;
//import com.google.android.gms.stats.CodePackage;
import com.synology.sylib.security.internal.KsManager;
import com.synology.sylib.security.util.Logger;
import java.security.Key;
import java.security.KeyStore;
import java.util.HashMap;
import javax.crypto.KeyGenerator;

/* loaded from: classes.dex */
public class AesCryptMethod extends CryptMethod {
    private static final Method METHOD = Method.AES_KEYSTORE;
    private static final String SZ_ALGORITHM_AES = "AES";

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public boolean isNeedValidateAfterLoad() {
        return true;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public HashMap<String, Object> onCreateSettings() {
        return null;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public void onDelete() {
    }

    public AesCryptMethod(String str, KeyStore keyStore) {
        this(str, keyStore, null);
    }

    public AesCryptMethod(String str, KeyStore keyStore, HashMap<String, Object> map) {
        super(str, keyStore, map);
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    Key getAesKey() {
        try {
            KeyStore keyStore = getKeyStore();
            if (keyStore != null) {
                return ((KeyStore.SecretKeyEntry) keyStore.getEntry(getKeyAlias(), null)).getSecretKey();
            }
        } catch (Exception e) {
            Logger.e("AesCrypt", "getKey : " + e.getMessage(), e);
        }
        return null;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    boolean onValidate() {
        KeyStore keyStore = getKeyStore();
        if (keyStore == null) {
            Logger.d("AesCrypt", "validate : keystore = null");
            return false;
        }
        try {
            boolean zEqualsIgnoreCase = "AES".equalsIgnoreCase(keyStore.getKey(getKeyAlias(), null).getAlgorithm());
            Logger.d("AesCrypt", "validate : algorithm match = " + zEqualsIgnoreCase);
            return zEqualsIgnoreCase;
        } catch (Exception e) {
            Logger.e("AesCrypt", "validate : " + e.getMessage(), e);
            return false;
        }
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public Method getMethod() {
        return METHOD;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    boolean onCreateKey(String str) {
        try {
            KeyStore keyStore = getKeyStore();
            if (keyStore == null) {
                return false;
            }
            if (keyStore.containsAlias(getKeyAlias())) {
                Logger.w("AesCrypt", "create : Key alias is already existed.");
                return true;
            }
            return generateAesKeyStore();
        } catch (InterruptedException e) {
            Logger.e("AesCrypt", "create interrupted : " + e.getMessage());
            return false;
        } catch (Exception e2) {
            Logger.e("AesCrypt", "create : " + e2.getMessage(), e2);
            return false;
        }
    }

    private boolean generateAesKeyStore() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", KsManager.KEYSTORE_PROVIDER);
        // CodePackage.GCM
        keyGenerator.init(new KeyGenParameterSpec.Builder(getKeyAlias(), 3).setKeySize(256).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
        keyGenerator.generateKey();
        return true;
    }
}