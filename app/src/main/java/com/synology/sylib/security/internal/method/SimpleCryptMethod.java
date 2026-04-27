package com.synology.sylib.security.internal.method;

import android.content.Context;
import android.provider.Settings;
import com.synology.sylib.security.internal.util.Util;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import javax.crypto.spec.SecretKeySpec;

/* loaded from: classes.dex */
public class SimpleCryptMethod extends CryptMethod {
    private static final Method METHOD = Method.SIMPLE_ENCODE;
    private final Context mContext;

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public boolean isNeedValidateAfterLoad() {
        return false;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    boolean onCreateKey(String str) {
        return true;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public HashMap<String, Object> onCreateSettings() {
        return null;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public void onDelete() {
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    boolean onValidate() {
        return true;
    }

    public SimpleCryptMethod(String str, KeyStore keyStore, Context context) {
        this(str, keyStore, null, context);
    }

    public SimpleCryptMethod(String str, KeyStore keyStore, HashMap<String, Object> map, Context context) {
        super(str, keyStore, map);
        this.mContext = context;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    Key getAesKey() throws NoSuchAlgorithmException {
        return new SecretKeySpec(Util.getHash(getAndroidId().getBytes(), getKeyAlias().getBytes()), "AES");
    }

    protected String getAndroidId() {
        return Settings.Secure.getString(this.mContext.getContentResolver(), "android_id");
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public Method getMethod() {
        return METHOD;
    }
}