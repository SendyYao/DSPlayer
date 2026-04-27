package com.synology.sylib.security;

import android.content.Context;
import com.synology.sylib.security.data.CipherData;
import com.synology.sylib.security.data.KsCipherData;
import com.synology.sylib.security.internal.KsManager;
import com.synology.sylib.security.migrate.data.KsRef;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.crypto.NoSuchPaddingException;

/* loaded from: classes.dex */
public class KsMigrator {
    private static final boolean DEBUG = false;
    private static final String TAG = "KsMigrator";
    private static Boolean sHasLegacyAlias;
    private final Context mAppContext;
    private final Future<KsHelper> mHelper;
    private KeyStore mKeyStore;
    private final KeyStoreHelper mLegacyHelper;

    static void log(String str, Throwable th) {
    }

    static KsMigrator newInstance(Context context, KeyStoreHelper keyStoreHelper) {
        if (context.getApplicationContext() == null) {
            throw new IllegalStateException("Call KsMigrator too early.");
        }
        return new KsMigrator(context.getApplicationContext(), keyStoreHelper);
    }

    public static KsCipherData convert(CipherData cipherData) {
        if (cipherData == null) {
            return null;
        }
        KsCipherData ksCipherData = new KsCipherData();
        ksCipherData.setCipher(cipherData.getDateBytes());
        ksCipherData.setIV(cipherData.getIVBytes());
        return ksCipherData;
    }

    public static CipherData convert(KsCipherData ksCipherData) {
        if (ksCipherData == null) {
            return null;
        }
        CipherData cipherData = new CipherData();
        cipherData.setData(ksCipherData.getCipherBytes());
        cipherData.setIV(ksCipherData.getIVBytes());
        return cipherData;
    }

    static void log(String str) {
        log(str, null);
    }

    private KsMigrator(Context context, KeyStoreHelper keyStoreHelper) {
        String str;
        this.mAppContext = context;
        this.mLegacyHelper = keyStoreHelper;
        String aliasName = keyStoreHelper.getAliasName();
        if ("SYNOKEY".equalsIgnoreCase(aliasName)) {
            str = "SYNOKEYSTORE";
        } else {
            str = aliasName + "_KsMigrate";
        }
        log("KsHelper with alias : " + str);
        this.mHelper = KsHelper.init(context, str);
    }

    CipherData encrypt(byte[] bArr) {
        try {
            log("Delegate encrypt to KsMigrate");
            return convert(this.mHelper.get().encrypt(bArr));
        } catch (Exception e) {
            log("KsMigrate.encrypt fail", e);
            return null;
        }
    }

    byte[] decrypt(CipherData cipherData) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InvalidAlgorithmParameterException, NoSuchPaddingException, ExecutionException, NoSuchProviderException, InvalidKeyException, InterruptedException {
        return decrypt(cipherData, null);
    }

    <T> byte[] decrypt(CipherData cipherData, KsRef<T> ksRef) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InvalidAlgorithmParameterException, NoSuchPaddingException, ExecutionException, NoSuchProviderException, InvalidKeyException, InterruptedException {
        byte[] bArrDecryptInternal;
        log("Delegate decrypt to KsMigrate");
        initKsRefValue(ksRef);
        if (cipherData == null || cipherData.isDataEmpty()) {
            return null;
        }
        try {
            bArrDecryptInternal = this.mHelper.get().decrypt(convert(cipherData));
        } catch (Exception e) {
            log("KsMigrate.decrypt fail : " + e.getMessage(), e);
            bArrDecryptInternal = null;
        }
        if (bArrDecryptInternal == null) {
            if (isLegacyKeyExist()) {
                log("KsHelper decrypt result null, call legacy method.");
                bArrDecryptInternal = this.mLegacyHelper.decryptInternal(cipherData);
                if (bArrDecryptInternal != null && ksRef != null) {
                    if (String.class.equals(ksRef.getTypeClass())) {
                        CipherData cipherDataEncrypt = encrypt(bArrDecryptInternal);
                        setKsRef(ksRef, cipherDataEncrypt != null ? cipherDataEncrypt.getEncoded() : null);
                    } else if (Boolean.class.equals(ksRef.getTypeClass()) || Boolean.TYPE.equals(ksRef.getTypeClass())) {
                        setKsRef(ksRef, Boolean.TRUE);
                    }
                }
            } else {
                log("KsHelper decrypt result null, no legacy instance, skip.");
            }
        }
        return bArrDecryptInternal;
    }

    private boolean isLegacyKeyExist() throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException {
        Boolean bool = sHasLegacyAlias;
        if (bool != null) {
            return bool.booleanValue();
        }
        try {
            if (this.mKeyStore == null) {
                KeyStore keyStore = KeyStore.getInstance(KsManager.KEYSTORE_PROVIDER);
                this.mKeyStore = keyStore;
                keyStore.load(null);
            }
            Boolean boolValueOf = Boolean.valueOf(this.mKeyStore.containsAlias(this.mLegacyHelper.getAliasName()));
            sHasLegacyAlias = boolValueOf;
            if (!boolValueOf.booleanValue()) {
                sHasLegacyAlias = Boolean.valueOf(isLegacyPrefsExist());
                log("Legacy KeyStore prefs exist : " + sHasLegacyAlias);
            } else {
                log("Legacy KeyStore alias exist.");
            }
            return sHasLegacyAlias.booleanValue();
        } catch (Exception unused) {
            sHasLegacyAlias = Boolean.valueOf(isLegacyPrefsExist());
            log("Legacy KeyStore prefs exist : " + sHasLegacyAlias);
            return sHasLegacyAlias.booleanValue();
        }
    }

    private boolean isLegacyPrefsExist() {
        String str = "libHelper_" + this.mLegacyHelper.getAliasName();
        if (new File(new File(this.mAppContext.getFilesDir().getParent(), "shared_prefs"), str + ".xml").isFile()) {
            return this.mAppContext.getSharedPreferences(str, 0).contains("method");
        }
        return false;
    }

    private <T> void initKsRefValue(KsRef<T> ksRef) {
        if (ksRef == null) {
            return;
        }
        if (Boolean.class.equals(ksRef.getTypeClass()) || Boolean.TYPE.equals(ksRef.getTypeClass())) {
            setKsRef(ksRef, Boolean.FALSE);
        } else {
            setKsRef(ksRef, null);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private <T> void setKsRef(KsRef<T> ksRef, Object obj) {
        if (ksRef != null) {
            ksRef.set((T) obj);
        }
    }
}