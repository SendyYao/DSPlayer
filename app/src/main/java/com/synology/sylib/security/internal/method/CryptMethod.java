package com.synology.sylib.security.internal.method;

import com.synology.sylib.security.KsHelper;
import com.synology.sylib.security.data.KsCipherData;
import com.synology.sylib.security.internal.KsManager;
import com.synology.sylib.security.internal.exception.CryptFailException;
import com.synology.sylib.security.internal.util.Util;
import com.synology.sylib.security.util.Logger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

/* loaded from: classes.dex */
public abstract class CryptMethod {
    public static final String KEY_METHOD = "method";
    private static final String TAG = "CryptMethod";
    private GenerateKeyTask mGenKeyTask;
    private final String mKeyAlias;
    private final KeyStore mKeyStore;
    final HashMap<String, Object> mSetting;

    abstract Key getAesKey() throws NoSuchAlgorithmException;

    public abstract Method getMethod();

    public abstract boolean isNeedValidateAfterLoad();

    abstract boolean onCreateKey(String str);

    abstract HashMap<String, Object> onCreateSettings();

    public abstract void onDelete();

    abstract boolean onValidate();

    CryptMethod(String str, KeyStore keyStore) {
        this(str, keyStore, null);
    }

    CryptMethod(String str, KeyStore keyStore, HashMap<String, Object> map) {
        if (str.length() == 0) {
            throw new IllegalArgumentException("Argument alias can not be empty string");
        }
        this.mKeyAlias = str;
        this.mKeyStore = keyStore;
        this.mSetting = parseSetting(map);
    }

    public final Callable<Boolean> getCreateKeyTask() {
        if (this.mGenKeyTask == null) {
            this.mGenKeyTask = new GenerateKeyTask();
        }
        return this.mGenKeyTask;
    }

    public final String getName() {
        return getMethod().name();
    }

    public final String getKeyAlias() {
        return this.mKeyAlias;
    }

    final KeyStore getKeyStore() {
        return this.mKeyStore;
    }

    private String getKeyPrefix() {
        return KsManager.getPrefKeyPrefix(getKeyAlias());
    }

    private String getPrefKeyName(String str) {
        return KsManager.getPrefKey(getKeyAlias(), str);
    }

    public final boolean validateKey() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (!onValidate()) {
            Logger.w("Validate", "Validate fail : " + this);
            return false;
        }
        if (getAesKey() == null) {
            Logger.w("Validate", "Key is null : " + this);
            return false;
        }
        byte[] bArrDecrypt = decrypt(encrypt(TAG.getBytes()));
        if (bArrDecrypt != null) {
            boolean zEquals = TAG.equals(new String(bArrDecrypt));
            Logger.d("Validate", "Decrypt success : " + zEquals);
            return zEquals;
        }
        Logger.d("Validate", "Decrypt fail");
        return false;
    }

    public final HashMap<String, Object> getSettings() {
        HashMap<String, Object> map = new HashMap<>();
        HashMap<String, Object> mapOnCreateSettings = this.mSetting;
        if (mapOnCreateSettings == null) {
            mapOnCreateSettings = onCreateSettings();
        }
        if (mapOnCreateSettings != null) {
            for (Map.Entry<String, Object> entry : mapOnCreateSettings.entrySet()) {
                String key = entry.getKey();
                map.put(getPrefKeyName(key), entry.getValue());
            }
        }
        map.put(getPrefKeyName("method"), Integer.valueOf(getMethod().getValue()));
        return map;
    }

    public final KsCipherData encrypt(byte[] bArr) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        Key aesKey = getAesKey();
        if (aesKey == null) {
            Logger.e("Encrypt", "Can not get key : " + getKeyAlias());
            return null;
        }
        try {
            byte[] secureRandomBytes = Util.getSecureRandomBytes(12);
            Cipher cipher = Cipher.getInstance(KsManager.SZ_AES_GCM_NO_PADDING);
            try {
                cipher.init(1, aesKey, new GCMParameterSpec(128, secureRandomBytes));
            } catch (InvalidAlgorithmParameterException unused) {
                cipher.init(1, aesKey);
                secureRandomBytes = ((GCMParameterSpec) cipher.getParameters().getParameterSpec(GCMParameterSpec.class)).getIV();
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bArr.length);
            byte[] bArr2 = new byte[KsHelper.getSegmentSize()];
            while (true) {
                int i = byteArrayInputStream.read(bArr2);
                if (i == -1) {
                    break;
                }
                byte[] bArrUpdate = cipher.update(bArr2, 0, i);
                if (bArrUpdate != null) {
                    byteArrayOutputStream.write(bArrUpdate);
                }
            }
            byte[] bArrDoFinal = cipher.doFinal();
            if (bArrDoFinal != null) {
                byteArrayOutputStream.write(bArrDoFinal);
            }
            return new KsCipherData().setCipher(byteArrayOutputStream.toByteArray()).setIV(secureRandomBytes);
        } catch (Exception e) {
            Logger.e("Encrypt", "Fail : " + e.getMessage(), e);
            return null;
        }
    }

    public final byte[] decrypt(KsCipherData ksCipherData) {
        try {
            return decryptOrThrow(ksCipherData);
        } catch (Exception e) {
            Logger.e("Decrypt", "Fail : " + e.getMessage(), e);
            return null;
        }
    }

    public final byte[] decryptOrThrow(KsCipherData ksCipherData) throws Exception {
        if (ksCipherData == null) {
            throw new CryptFailException(this, CryptFailException.Type.Decrypt, CryptFailException.Reason.SourceIsNull, (Throwable) null);
        }
        if (ksCipherData.isCipherEmpty()) {
            throw new CryptFailException(this, CryptFailException.Type.Decrypt, CryptFailException.Reason.SourceCipherEmpty, (Throwable) null);
        }
        Key aesKey = getAesKey();
        if (aesKey == null) {
            Logger.e("Decrypt", "Can not get key : " + getKeyAlias());
            throw new CryptFailException(this, CryptFailException.Type.Decrypt, CryptFailException.Reason.CanNotGetAesKey, (Throwable) null);
        }
        Cipher cipher = Cipher.getInstance(KsManager.SZ_AES_GCM_NO_PADDING);
        cipher.init(2, aesKey, new GCMParameterSpec(128, ksCipherData.getIVBytes()));
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(ksCipherData.getCipherBytes());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[KsHelper.getSegmentSize()];
        while (true) {
            int i = byteArrayInputStream.read(bArr);
            if (i == -1) {
                break;
            }
            byte[] bArrUpdate = cipher.update(bArr, 0, i);
            if (bArrUpdate != null) {
                byteArrayOutputStream.write(bArrUpdate);
            }
        }
        byte[] bArrDoFinal = cipher.doFinal();
        if (bArrDoFinal != null) {
            byteArrayOutputStream.write(bArrDoFinal);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private HashMap<String, Object> parseSetting(HashMap<String, Object> map) {
        if (map == null) {
            return null;
        }
        HashMap<String, Object> map2 = new HashMap<>();
        String str = "^" + getKeyPrefix() + "(.+)";
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.matches(str)) {
                map2.put(key.replaceAll(str, "$1"), value);
            }
        }
        return map2;
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "[%s@%x] %s %s", getName(), Integer.valueOf(hashCode()), this.mKeyAlias, this.mSetting);
    }

    public final class GenerateKeyTask implements Callable<Boolean> {
        public GenerateKeyTask() {
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.util.concurrent.Callable
        public Boolean call() {
            try {
                CryptMethod cryptMethod = CryptMethod.this;
                return Boolean.valueOf(cryptMethod.onCreateKey(cryptMethod.mKeyAlias) && CryptMethod.this.validateKey());
            } catch (Exception unused) {
                return false;
            }
        }
    }
}