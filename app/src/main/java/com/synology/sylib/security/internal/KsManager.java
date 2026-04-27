package com.synology.sylib.security.internal;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.synology.sylib.security.internal.method.AesCryptMethod;
import com.synology.sylib.security.internal.method.CryptMethod;
import com.synology.sylib.security.internal.method.Method;
import com.synology.sylib.security.internal.method.RsaHybridMethod;
import com.synology.sylib.security.internal.method.SimpleCryptMethod;
import com.synology.sylib.security.recover.AesKeyRecoverUtil;
import com.synology.sylib.security.util.Logger;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/* loaded from: classes.dex */
public class KsManager {
    public static final int AES_KEY_BITS = 256;
    public static final int AUTH_TAG_LEN_BITS = 128;
    public static final int IV_LEN_BITS = 96;
    public static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    public static final int MAX_GEN_KEY_TIME_MS = 2000;
    protected static final String PREFS_NAME = "libKsHelperSettings";
    public static final String SZ_AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    public static final String SZ_ALGORITHM_AES = "AES";
    private static volatile KsManager sInstance;
    protected final Context mAppContext;
    private KeyStore mKeyStore;
    private final SharedPreferences mPrefs;

    public static KsManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (KsManager.class) {
                if (sInstance == null) {
                    sInstance = new KsManager(context);
                }
            }
        }
        return sInstance;
    }

    public static void changeInstanceForTest(KsManager ksManager) {
        if (!"com.synology.sylib.security.kshelper.test".equals(ksManager.mAppContext.getPackageName())) {
            throw new RuntimeException(new IllegalAccessException("This method can only be access from AndroidTest"));
        }
        synchronized (KsManager.class) {
            sInstance = ksManager;
            Logger.dev("UnitTest", "==== Instance changed to " + sInstance + " ===");
        }
    }

    public static String getPrefKeyPrefix(String str) {
        return getPrefKey(str, null);
    }

    public static String getPrefKey(String str, String str2) {
        StringBuilder sbAppend = new StringBuilder().append(str).append("_");
        if (str2 == null) {
            str2 = "";
        }
        return sbAppend.append(str2).toString();
    }

    KsManager(Context context) {
        if (context.getApplicationContext() == null) {
            throw new RuntimeException("Create KsManager too early.");
        }
        Context applicationContext = context.getApplicationContext();
        this.mAppContext = applicationContext;
        this.mPrefs = applicationContext.getSharedPreferences(PREFS_NAME, 0);
    }

    public CryptMethod get(String str, boolean z) throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        boolean zLoadKeyStore = loadKeyStore();
        CryptMethod cryptMethodLoadCryptMethod = loadCryptMethod(str, z);
        Logger.dev("KSMGet", "Loaded : " + zLoadKeyStore + ", result = " + cryptMethodLoadCryptMethod);
        if (zLoadKeyStore) {
            if (cryptMethodLoadCryptMethod != null) {
                return cryptMethodLoadCryptMethod;
            }
            Logger.dev("KSMGet", "Ready to gen key : " + str);
            CryptMethod cryptMethodGenerateKey = generateKey(str);
            saveCryptSetting(cryptMethodGenerateKey);
            return cryptMethodGenerateKey;
        }
        if (cryptMethodLoadCryptMethod != null) {
            return cryptMethodLoadCryptMethod;
        }
        Logger.dev("KSMGet", "Create new SimpleMethod");
        SimpleCryptMethod simpleCryptMethod = new SimpleCryptMethod(str, this.mKeyStore, this.mAppContext);
        saveCryptSetting(simpleCryptMethod);
        return simpleCryptMethod;
    }

    public void delete(CryptMethod cryptMethod) throws KeyStoreException {
        String keyAlias = cryptMethod.getKeyAlias();
        try {
            cryptMethod.onDelete();
        } catch (Exception e) {
            Logger.w("KSMDel", "<" + keyAlias + "> onDelete : " + e.getMessage(), e);
        }
        removeKeyStoreEntry(keyAlias);
    }

    public void clearAll() throws KeyStoreException {
        Iterator<? extends Map.Entry<String, ?>> it = this.mPrefs.getAll().entrySet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            if (key.matches("(.+)_method")) {
                removeKeyStoreEntry(key.replaceAll("(.+)_method", "$1"));
            }
        }
        this.mPrefs.edit().clear().apply();
        RsaHybridMethod.clearAllBackups(this.mAppContext);
    }

    protected boolean takeFutureResult(Future<Boolean> future, long j) throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(j, TimeUnit.MILLISECONDS).booleanValue();
    }

    private CryptMethod generateKey(String str) throws KeyStoreException {
        CryptMethod next;
        List<CryptMethod> cryptMethodList = getCryptMethodList(str);
        ExecutorService executorServiceNewSingleThreadExecutor = Executors.newSingleThreadExecutor();
        Iterator<CryptMethod> it = cryptMethodList.iterator();
        long j = 2000;
        while (it.hasNext()) {
            next = it.next();
            Future<Boolean> futureSubmit = executorServiceNewSingleThreadExecutor.submit(next.getCreateKeyTask());
            try {
                try {
                    Logger.d("KSMGenKey", "<" + next.getName() + "> Timeout : " + j + " ms.");
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    boolean zTakeFutureResult = takeFutureResult(futureSubmit, j);
                    long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
                    j -= jCurrentTimeMillis2;
                    Logger.d("KSMGenKey", "<" + next.getName() + "> Cost time : " + jCurrentTimeMillis2 + " ms. Success : " + zTakeFutureResult);
                    if (zTakeFutureResult) {
                        futureSubmit.cancel(true);
                        break;
                    }
                } catch (TimeoutException unused) {
                    Logger.i("KSMGenKey", "<" + next.getName() + "> initKey timeout.");
                    futureSubmit.cancel(true);
                } catch (Exception e) {
                    Logger.w("KSMGenKey", "<" + next.getName() + "> initKey : " + e.getMessage(), e);
                }
                futureSubmit.cancel(true);
                if (j <= 0) {
                    break;
                }
            } catch (Throwable th) {
                futureSubmit.cancel(true);
                throw th;
            }
        }
        next = null;
        if (next != null) {
            return next;
        }
        removeKeyStoreEntry(str);
        return new SimpleCryptMethod(str, this.mKeyStore, this.mAppContext);
    }

    List<CryptMethod> getCryptMethodList(String str) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new AesCryptMethod(str, this.mKeyStore));
        arrayList.add(new RsaHybridMethod(str, this.mKeyStore, this.mAppContext, new KsManager$$ExternalSyntheticLambda0(this)));
        return arrayList;
    }

    public final /* synthetic */ class KsManager$$ExternalSyntheticLambda0 implements RsaHybridMethod.KeyRecoverListener {
        public final /* synthetic */ KsManager f$0;

        public /* synthetic */ KsManager$$ExternalSyntheticLambda0(KsManager ksManager) {
            this.f$0 = ksManager;
        }

        @Override // com.synology.sylib.security.internal.method.RsaHybridMethod.KeyRecoverListener
        public final void onRecoverAesKey(String str, String str2) {
            this.f$0.onRsaKeyRecovered(str, str2);
        }
    }

    protected boolean loadKeyStore() {
        if (this.mKeyStore != null) {
            return true;
        }
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            this.mKeyStore = keyStore;
            keyStore.load(null);
            return true;
        } catch (Exception e) {
            Logger.w("KSMLoadKS", e.getMessage(), e);
            return false;
        }
    }

    private void saveCryptSetting(CryptMethod cryptMethod) {
        clearCryptSetting(cryptMethod.getKeyAlias());
        clearOldBackups(cryptMethod);
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        for (Map.Entry<String, Object> entry : cryptMethod.getSettings().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Integer) {
                editorEdit.putInt(key, ((Integer) value).intValue());
            } else if (value instanceof Long) {
                editorEdit.putLong(key, ((Long) value).longValue());
            } else if (value instanceof Boolean) {
                editorEdit.putBoolean(key, ((Boolean) value).booleanValue());
            } else if (value instanceof Float) {
                editorEdit.putFloat(key, ((Float) value).floatValue());
            } else if (value instanceof String) {
                editorEdit.putString(key, value.toString());
            } else {
                throw new RuntimeException("Unsupported setting data type " + value.getClass().getSimpleName());
            }
        }
        editorEdit.apply();
        Logger.d("KSMSave", cryptMethod.getName());
    }

    private CryptMethod loadCryptMethod(String str, boolean z) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        HashMap<String, Object> map = new HashMap<>();
        for (Map.Entry<String, ?> entry : this.mPrefs.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith(getPrefKeyPrefix(str))) {
                map.put(key, value);
            }
        }
        CryptMethod simpleCryptMethod = null;
        if (map.isEmpty()) {
            return null;
        }
        Object obj = map.get(getPrefKey(str, "method"));
        Method method = Method.get(obj instanceof Integer ? ((Integer) obj).intValue() : Method.NOT_SPECIFIC.getValue());
        if (method == Method.SIMPLE_ENCODE && z && (simpleCryptMethod = createCryptMethod(Method.AES_KEYSTORE, str, map)) != null) {
            saveCryptSetting(simpleCryptMethod);
        }
        if (simpleCryptMethod == null) {
            simpleCryptMethod = createCryptMethod(method, str, map);
        }
        if (simpleCryptMethod instanceof AesCryptMethod) {
            AesKeyRecoverUtil.setupBackupCryptMethod(str, this.mKeyStore, this.mAppContext);
        } else if (simpleCryptMethod == null) {
            simpleCryptMethod = new SimpleCryptMethod(str, this.mKeyStore, this.mAppContext);
            saveCryptSetting(simpleCryptMethod);
        }
        Logger.d("KSMLoad", simpleCryptMethod.getName());
        return simpleCryptMethod;
    }

    /* renamed from: com.synology.sylib.security.internal.KsManager$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$synology$sylib$security$internal$method$Method;

        static {
            int[] iArr = new int[Method.values().length];
            $SwitchMap$com$synology$sylib$security$internal$method$Method = iArr;
            try {
                iArr[Method.RSA_HYBRID.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$synology$sylib$security$internal$method$Method[Method.AES_KEYSTORE.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$synology$sylib$security$internal$method$Method[Method.SIMPLE_ENCODE.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
        }
    }

    private CryptMethod createCryptMethod(Method method, String str, HashMap<String, Object> map) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        CryptMethod rsaHybridMethod = getCryptMethod(method, str, map);
        if (rsaHybridMethod != null && rsaHybridMethod.isNeedValidateAfterLoad()) {
            boolean zValidateKey = rsaHybridMethod.validateKey();
            boolean z = !zValidateKey;
            Logger.d("KSMLoad", "Check " + rsaHybridMethod.getName() + " validate : " + zValidateKey);
            if (z) {
                return null;
            }
        }
        return rsaHybridMethod;
    }

    @Nullable
    private CryptMethod getCryptMethod(Method method, String str, HashMap<String, Object> map) {
        CryptMethod rsaHybridMethod;
        int i = AnonymousClass1.$SwitchMap$com$synology$sylib$security$internal$method$Method[method.ordinal()];
        if (i == 1) {
            rsaHybridMethod = new RsaHybridMethod(str, this.mKeyStore, map, this.mAppContext, new KsManager$$ExternalSyntheticLambda0(this));
        } else if (i == 2) {
            rsaHybridMethod = new AesCryptMethod(str, this.mKeyStore, map);
        } else {
            rsaHybridMethod = i != 3 ? null : new SimpleCryptMethod(str, this.mKeyStore, map, this.mAppContext);
        }
        return rsaHybridMethod;
    }

    private void clearOldBackups(CryptMethod cryptMethod) {
        if (cryptMethod instanceof RsaHybridMethod) {
            return;
        }
        RsaHybridMethod.removeBackupEncryptedKey(this.mAppContext, cryptMethod.getKeyAlias());
    }

    private void clearCryptSetting(String str) {
        Map<String, ?> all = this.mPrefs.getAll();
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        Iterator<? extends Map.Entry<String, ?>> it = all.entrySet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            if (key.startsWith(getPrefKeyPrefix(str))) {
                editorEdit.remove(key);
            }
        }
        editorEdit.apply();
    }

    private void removeKeyStoreEntry(String str) throws KeyStoreException {
        try {
            KeyStore keyStore = this.mKeyStore;
            if (keyStore != null) {
                keyStore.deleteEntry(str);
                Logger.d("KSMDel", "<" + str + "> entry removed");
            }
        } catch (KeyStoreException e) {
            Logger.w("KSMDel", "<" + str + "> remove entry : " + e.getMessage(), e);
        }
    }

    public void onRsaKeyRecovered(String str, String str2) {
        if (str2 != null) {
            this.mPrefs.edit().putString(getPrefKey(str, "key"), str2).apply();
        }
    }
}