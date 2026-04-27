package com.synology.sylib.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
//import com.google.firebase.sessions.settings.RemoteSettings;
import com.synology.sylib.security.data.CipherData;
import com.synology.sylib.security.migrate.data.KsRef;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

@Deprecated
/* loaded from: classes.dex */
public class KeyStoreHelper {
    private static final int AES_IV_BITS = 128;
    private static final int AES_KEY_BITS = 256;
    private static final String CIPHER_ALGORITHM_RSA = "RSA/ECB/PKCS1Padding";
    private static final boolean DEBUG = false;
    private static final String KEYSTORE_ALGORITHM_AES = "AES";
    private static final String KEYSTORE_ALGORITHM_RSA = "RSA";
    static final String KEYSTORE_ALIAS = "SYNOKEY";
    static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_GEN_ALGORITHM_AES = "AES";
    private static final byte MAGIC_BYTE = -85;
    public static final String METADATA_DEFAULT_ALIAS = "keystoreHelper.default.alias";
    public static final String METADATA_RSA_STRONG = "keystoreHelper.rsa.strong.key";
    private static final String PREF_KEY_DATA = "data";
    static final String PREF_KEY_METHOD = "method";
    private static final int RSA_KEY_BITS_NORMAL = 2048;
    private static final int RSA_KEY_BITS_STRONG = 4096;
    static final String SZ_PREFS_PREFIX = "libHelper_";
    private static final String TAG = "KeystoreHelper";
    private Method mCachedMethod;
    private String mCachedPrefAesKey;
    private Context mContext;
    private FutureTask<Boolean> mFutureTaskLock;
    private String mKeyAliasName;
    private KeyStore mKeyStore;
    private KsMigrator mMigrateHelper;
    private boolean mNeedInit;
    private SharedPreferences mPrefs;
    private int mRSABitLength;
    private boolean mUseStrongRSA;
    private static final String AES_BLOCK_MODE = "CTR";
    private static final String AES_PADDING_MODE = "NoPadding";
    // RemoteSettings.FORWARD_SLASH_STRING
    private static final String CIPHER_ALGORITHM_AES = "AES/CTR" + "/" + "NoPadding";
    private static KeyStoreHelper sDefaultInstance = null;

    private enum Method {
        NOT_SPECIFIC(-1, false),
        SIMPLE_ENCODE(0, false),
        RSA_HYBRID(1, true),
        AES_KEYSTORE(2, false);

        private final boolean isHybrid;
        private final int value;

        Method(int i, boolean z) {
            this.value = i;
            this.isHybrid = z;
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

    public static KeyStoreHelper initDefaultSingleton(Context context) throws PackageManager.NameNotFoundException {
        return get(context);
    }

    public static KeyStoreHelper get(Context context) throws PackageManager.NameNotFoundException {
        if (sDefaultInstance == null) {
            synchronized (KeyStoreHelper.class) {
                if (sDefaultInstance == null) {
                    sDefaultInstance = new KeyStoreHelper(context);
                }
            }
        }
        return sDefaultInstance;
    }

    @Deprecated
    public static KeyStoreHelper get() throws NullPointerException {
        KeyStoreHelper keyStoreHelper = sDefaultInstance;
        if (keyStoreHelper != null) {
            return keyStoreHelper;
        }
        throw new NullPointerException("Please call KeyStoreHelper.initDefaultSingleton(context) before calling KeyStoreHelper.get()");
    }

    private KeyStoreHelper(Context context) throws PackageManager.NameNotFoundException {
        this(context, null, true);
    }

    public KeyStoreHelper(Context context, String str) throws PackageManager.NameNotFoundException {
        this(context, str, false);
    }

    private KeyStoreHelper(Context context, String str, boolean z) throws PackageManager.NameNotFoundException {
        this.mNeedInit = true;
        this.mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        this.mUseStrongRSA = false;
        Bundle metaData = getMetaData();
        String str2 = KEYSTORE_ALIAS;
        String string = metaData.getString(METADATA_DEFAULT_ALIAS, KEYSTORE_ALIAS);
        str = str == null ? string : str;
        this.mUseStrongRSA = metaData.getBoolean(METADATA_RSA_STRONG, false);
        this.mKeyAliasName = TextUtils.isEmpty(str) ? str2 : str;
        this.mPrefs = this.mContext.getSharedPreferences(SZ_PREFS_PREFIX + this.mKeyAliasName, 0);
        this.mRSABitLength = this.mUseStrongRSA ? 4096 : 2048;
        if (!z && string.equalsIgnoreCase(this.mKeyAliasName)) {
            throw new IllegalArgumentException("Default KeyStore alias can only be create by KeyStoreHelper.initDefaultSingleton(context) or KeyStoreHelper.get(context)");
        }
        this.mMigrateHelper = KsMigrator.newInstance(this.mContext, this);
    }

    private Bundle getMetaData() throws PackageManager.NameNotFoundException {
        Bundle bundle = new Bundle();
        try {
            ApplicationInfo applicationInfo = this.mContext.getPackageManager().getApplicationInfo(this.mContext.getPackageName(), 128);
            return applicationInfo.metaData != null ? applicationInfo.metaData : bundle;
        } catch (Exception e) {
            Log.e(TAG, "Can not get meta-data, use default setting.", e);
            return bundle;
        }
    }

    @Deprecated
    public void reInit() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        if (this.mFutureTaskLock == null || this.mNeedInit || needCreateKey()) {
            this.mFutureTaskLock = null;
            initWithFutureTask();
        }
    }

    public CipherData encrypt(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return encrypt(str.getBytes());
    }

    public CipherData encrypt(byte[] bArr) {
        return this.mMigrateHelper.encrypt(bArr);
    }

    public String encryptAndEncode(String str) {
        CipherData cipherDataEncrypt;
        if (TextUtils.isEmpty(str) || (cipherDataEncrypt = encrypt(str.getBytes())) == null) {
            return null;
        }
        return cipherDataEncrypt.getEncoded();
    }

    public <T> String decryptAsString(CipherData cipherData, String str) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InvalidAlgorithmParameterException, NoSuchPaddingException, ExecutionException, NoSuchProviderException, InvalidKeyException, InterruptedException {
        KsRef<?> ksRef = null;
        byte[] bArrDecrypt = decrypt(cipherData, ksRef);
        return bArrDecrypt == null ? str : new String(bArrDecrypt);
    }

    public byte[] decrypt(CipherData cipherData) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InvalidAlgorithmParameterException, NoSuchPaddingException, ExecutionException, NoSuchProviderException, InvalidKeyException, InterruptedException {
        return decrypt(cipherData, null);
    }

    public <T> byte[] decrypt(CipherData cipherData, KsRef<T> ksRef) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InvalidAlgorithmParameterException, NoSuchPaddingException, ExecutionException, NoSuchProviderException, InvalidKeyException, InterruptedException {
        return this.mMigrateHelper.decrypt(cipherData, ksRef);
    }

    byte[] decryptInternal(CipherData cipherData) throws ExecutionException, InterruptedException, InvalidAlgorithmParameterException, CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException {
        initWithFutureTask();
        waitInitDone();
        synchronized (this) {
            checkNeedInit();
            if (cipherData != null && !cipherData.isDataEmpty()) {
                byte[] bArrDecryptDataWithAES = decryptDataWithAES(getAesKey(), cipherData);
                if (bArrDecryptDataWithAES == null) {
                    return null;
                }
                return bArrDecryptDataWithAES;
            }
            return null;
        }
    }

    @Deprecated
    public boolean validateKey(String name) {
        return validateKey(KeyStoreHelper.class.getName());
    }


    @Deprecated
    public synchronized void deleteKey() throws KeyStoreException {
        KeyStore keyStore = null;
        this.mPrefs.edit().clear().apply();
        try {
            keyStore = this.mKeyStore;
        } catch (Exception e2) {
            Log.e(TAG, "Other exception", e2);
        }
        if (keyStore != null && keyStore.containsAlias(this.mKeyAliasName)) {
            this.mKeyStore.deleteEntry(this.mKeyAliasName);
            Log.d(TAG, "Key entry removed");
            this.mCachedPrefAesKey = null;
            this.mCachedMethod = null;
            this.mFutureTaskLock = null;
            markNeedInit(true);
        } else {
            this.mCachedPrefAesKey = null;
            this.mCachedMethod = null;
            this.mFutureTaskLock = null;
            markNeedInit(true);
        }
    }

    public String getAliasName() {
        return this.mKeyAliasName;
    }

    private CipherData encryptDataWithAES(Key key, byte[] bArr) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        CipherData cipherData = null;
        try {
            if (key == null) {
                Log.e(TAG, "Key is null with method " + getMethod().value + " when encrypt");
                return null;
            }
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_AES);
            byte[] bArrCreateInitialVector = createInitialVector(128);
            try {
                cipher.init(1, key, new IvParameterSpec(bArrCreateInitialVector));
            } catch (InvalidAlgorithmParameterException unused) {
                cipher.init(1, key);
                bArrCreateInitialVector = ((IvParameterSpec) cipher.getParameters().getParameterSpec(IvParameterSpec.class)).getIV();
            }
            CipherData cipherData2 = new CipherData();
            try {
                cipherData2.setData(cipher.doFinal(bArr)).setIV(bArrCreateInitialVector);
                return cipherData2;
            } catch (Exception e) {
                cipherData = cipherData2;
                Log.e(TAG, "Encrypt With AES fail : " + e.getMessage(), e);
                return cipherData;
            }
        } catch (Exception e) {
            Log.e(TAG, "Encrypt With AES fail : " + e.getMessage(), e);
            return cipherData;
        }
    }

    private byte[] decryptDataWithAES(Key key, CipherData cipherData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        try {
            if (key == null) {
                Log.e(TAG, "Key is null with method " + getMethod().value + " when decrypt");
                return null;
            }
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_AES);
            cipher.init(2, key, new IvParameterSpec(cipherData.getIVBytes()));
            return cipher.doFinal(cipherData.getDateBytes());
        } catch (Exception e) {
            Log.e(TAG, "Decrypt With AES fail : " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] cryptRSA(int i, byte[] bArr) {
        int iBitLength;
        Key publicKey;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            if (i == 1) {
                publicKey = this.mKeyStore.getCertificate(this.mKeyAliasName).getPublicKey();
                iBitLength = ((RSAPublicKey) publicKey).getModulus().bitLength();
            } else {
                PublicKey publicKey2 = this.mKeyStore.getCertificate(this.mKeyAliasName).getPublicKey();
                Key key = this.mKeyStore.getKey(this.mKeyAliasName, null);
                iBitLength = ((RSAPublicKey) publicKey2).getModulus().bitLength();
                publicKey = key;
            }
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(i, publicKey);
            byte[] bArrCopyOfRange = new byte[(iBitLength / 8) - (i == 1 ? 11 : 0)];
            while (true) {
                int i2 = byteArrayInputStream.read(bArrCopyOfRange, 0, bArrCopyOfRange.length);
                if (i2 != -1) {
                    if (i2 != bArrCopyOfRange.length) {
                        bArrCopyOfRange = Arrays.copyOfRange(bArrCopyOfRange, 0, i2);
                    }
                    byte[] bArrDoFinal = cipher.doFinal(bArrCopyOfRange);
                    byteArrayOutputStream.write(bArrDoFinal, 0, bArrDoFinal.length);
                } else {
                    byteArrayInputStream.close();
                    byteArrayOutputStream.close();
                    return byteArrayOutputStream.toByteArray();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "crypt with RSA fail, mode : " + i, e);
            return null;
        }
    }

    /* renamed from: com.synology.sylib.security.KeyStoreHelper$2, reason: invalid class name */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$synology$sylib$security$KeyStoreHelper$Method;

        static {
            int[] iArr = new int[Method.values().length];
            $SwitchMap$com$synology$sylib$security$KeyStoreHelper$Method = iArr;
            try {
                iArr[Method.RSA_HYBRID.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$synology$sylib$security$KeyStoreHelper$Method[Method.AES_KEYSTORE.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
        }
    }

    private Key getAesKey() {
        byte[] bArrCryptRSA;
        int i = AnonymousClass2.$SwitchMap$com$synology$sylib$security$KeyStoreHelper$Method[getMethod().ordinal()];
        if (i == 1) {
            String prefAESKey = getPrefAESKey();
            if (prefAESKey == null || (bArrCryptRSA = cryptRSA(2, Base64.decode(prefAESKey, 2))) == null || bArrCryptRSA.length == 0) {
                return null;
            }
            return new SecretKeySpec(bArrCryptRSA, "AES");
        }
        if (i == 2) {
            try {
                return ((KeyStore.SecretKeyEntry) this.mKeyStore.getEntry(this.mKeyAliasName, null)).getSecretKey();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        byte[] bArr = new byte[32];
        byte[] bArrHexStringToBytes = hexStringToBytes(Settings.Secure.getString(this.mContext.getContentResolver(), "android_id"));
        for (int i2 = 0; i2 < 32; i2++) {
            int length = i2 % bArrHexStringToBytes.length;
            int length2 = i2 / bArrHexStringToBytes.length;
            if (length2 == 1) {
                bArr[i2] = (byte) (bArrHexStringToBytes[length] ^ MAGIC_BYTE);
            } else if (length2 == 2) {
                bArr[i2] = (byte) (bArrHexStringToBytes[length] & MAGIC_BYTE);
            } else if (length2 == 3) {
                bArr[i2] = (byte) (bArrHexStringToBytes[length] | MAGIC_BYTE);
            } else {
                bArr[i2] = bArrHexStringToBytes[length];
            }
        }
        return new SecretKeySpec(bArr, "AES");
    }

    private boolean isUsingSimpleEncode() {
        return getMethod().value == Method.SIMPLE_ENCODE.value;
    }

    private Method getMethod() {
        if (this.mCachedMethod == null) {
            this.mCachedMethod = Method.get(this.mPrefs.getInt("method", Method.NOT_SPECIFIC.value));
        }
        return this.mCachedMethod;
    }

    private String getPrefAESKey() {
        if (this.mCachedPrefAesKey == null) {
            this.mCachedPrefAesKey = this.mPrefs.getString("data", null);
        }
        return this.mCachedPrefAesKey;
    }

    private boolean isAlgorithmMatch() {
        String keyStoreAlgorithm = getKeyStoreAlgorithm();
        int i = AnonymousClass2.$SwitchMap$com$synology$sylib$security$KeyStoreHelper$Method[getMethod().ordinal()];
        if (i == 1) {
            return "RSA".equals(keyStoreAlgorithm);
        }
        if (i != 2) {
            return keyStoreAlgorithm == null;
        }
        return "AES".equals(keyStoreAlgorithm);
    }

    private String getKeyStoreAlgorithm() {
        try {
            try {
                return ((KeyStore.SecretKeyEntry) this.mKeyStore.getEntry(this.mKeyAliasName, null)).getSecretKey().getAlgorithm();
            } catch (Exception unused) {
                return this.mKeyStore.getCertificate(this.mKeyAliasName).getPublicKey().getAlgorithm();
            }
        } catch (Exception unused2) {
            return null;
        }
    }

    private void initWithFutureTask() {
        if (this.mFutureTaskLock == null) {
            this.mFutureTaskLock = new FutureTask<>(new Callable<Boolean>() { // from class: com.synology.sylib.security.KeyStoreHelper.1
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // java.util.concurrent.Callable
                public Boolean call() throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException, NoSuchProviderException, InvalidAlgorithmParameterException {
                    System.currentTimeMillis();
                    KeyStoreHelper.this.initKeyStore();
                    return true;
                }
            });
            new Thread(this.mFutureTaskLock).start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:24:0x004c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void initKeyStore() throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException, NoSuchProviderException, InvalidAlgorithmParameterException {
        /*
            r5 = this;
            java.lang.String r0 = "KeystoreHelper"
            r1 = 0
            r2 = 0
            java.security.KeyStore r3 = r5.mKeyStore     // Catch: java.lang.Exception -> L2d
            if (r3 != 0) goto L13
            java.lang.String r3 = "AndroidKeyStore"
            java.security.KeyStore r3 = java.security.KeyStore.getInstance(r3)     // Catch: java.lang.Exception -> L2d
            r5.mKeyStore = r3     // Catch: java.lang.Exception -> L2d
            r3.load(r1)     // Catch: java.lang.Exception -> L2d
        L13:
            boolean r3 = r5.needCreateKey()     // Catch: java.lang.Exception -> L2d
            if (r3 != 0) goto L2b
            boolean r3 = r5.isUsingSimpleEncode()     // Catch: java.lang.Exception -> L2d
            if (r3 == 0) goto L25
            java.lang.String r3 = "Using Simple encoding"
            android.util.Log.d(r0, r3)     // Catch: java.lang.Exception -> L2d
            goto L2a
        L25:
            java.lang.String r3 = "Key alias found"
            android.util.Log.d(r0, r3)     // Catch: java.lang.Exception -> L2d
        L2a:
            return
        L2b:
            r0 = 1
            goto L34
        L2d:
            r3 = move-exception
            java.lang.String r4 = "Can not access Android key store"
            android.util.Log.e(r0, r4, r3)
            r0 = r2
        L34:
            r5.deleteKey()
            if (r0 == 0) goto L4c
            boolean r0 = r5.internal_genAESKey23()
            if (r0 != 0) goto L44
            boolean r0 = r5.internal_genRSAKey23()
            goto L4a
        L44:
            if (r0 != 0) goto L4a
            boolean r0 = r5.internal_genRSAKey18()
        L4a:
            if (r0 != 0) goto L51
        L4c:
            com.synology.sylib.security.KeyStoreHelper$Method r0 = com.synology.sylib.security.KeyStoreHelper.Method.SIMPLE_ENCODE
            r5.savePreference(r0, r1)
        L51:
            r5.markNeedInit(r2)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.synology.sylib.security.KeyStoreHelper.initKeyStore():void");
    }

    private boolean internal_genAESKey23() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, KeyStoreException {
        Log.d(TAG, "Ready to generate AES key in KeyStore for 23+");
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            keyGenerator.init(new KeyGenParameterSpec.Builder(this.mKeyAliasName, 3).setKeySize(256).setBlockModes(AES_BLOCK_MODE).setEncryptionPaddings(AES_PADDING_MODE).build());
            keyGenerator.generateKey();
            Log.d(TAG, "Init Key store AES 23+");
            savePreference(Method.AES_KEYSTORE, null);
            if (internal_testCrypt()) {
                return true;
            }
            throw new RuntimeException("Test crypt fail");
        } catch (Exception e) {
            deleteKey();
            Log.d(TAG, "Can not create AES key for 23+ : " + e.getMessage());
            return false;
        }
    }

    private boolean internal_genRSAKey23() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, KeyStoreException {
        Log.d(TAG, "Ready to generate RSA key in KeyStore for 23+");
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(this.mKeyAliasName, 3).setBlockModes("ECB").setDigests("SHA-256", "SHA-512").setEncryptionPaddings("PKCS1Padding").setKeySize(this.mRSABitLength).build());
            keyPairGenerator.generateKeyPair();
            Log.d(TAG, "Init Key store RSA 23+ - with " + this.mRSABitLength + "-bits");
            internal_generateAesKey();
            if (internal_testCrypt()) {
                return true;
            }
            throw new RuntimeException("Test crypt fail");
        } catch (Exception e) {
            deleteKey();
            Log.d(TAG, "Can not create RSA key for 23+ : " + e.getMessage());
            return false;
        }
    }

    private boolean internal_genRSAKey18() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, KeyStoreException {
        Log.d(TAG, "Ready to generate RSA key in KeyStore for 18+");
        try {
            Calendar calendar = Calendar.getInstance();
            Calendar calendar2 = Calendar.getInstance();
            calendar2.add(1, 100);
            KeyPairGeneratorSpec.Builder endDate = new KeyPairGeneratorSpec.Builder(this.mContext).setAlias(this.mKeyAliasName).setSubject(new X500Principal("CN=" + this.mKeyAliasName)).setSerialNumber(BigInteger.TEN).setStartDate(calendar.getTime()).setEndDate(calendar2.getTime());
            endDate.setKeySize(this.mRSABitLength);
            String str = " - with " + this.mRSABitLength + "-bits";
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            keyPairGenerator.initialize(endDate.build());
            keyPairGenerator.generateKeyPair();
            Log.d(TAG, "Init Key store RSA 18+ " + str);
            internal_generateAesKey();
            if (internal_testCrypt()) {
                return true;
            }
            throw new RuntimeException("Test crypt fail");
        } catch (Exception e) {
            deleteKey();
            Log.d(TAG, "Can not create RSA key for 18+ : " + e.getMessage());
            return false;
        }
    }

    private void internal_generateAesKey() throws Exception {
        Log.d(TAG, " ==> Generating AES key...");
        savePreference(Method.RSA_HYBRID, Base64.encodeToString(cryptRSA(1, createAESKey(256).getEncoded()), 2));
        Log.d(TAG, " ==> Generate AES key -- DONE");
    }

    private boolean internal_testCrypt() throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        CipherData cipherDataEncryptDataWithAES;
        byte[] bArrDecryptDataWithAES;
        Key aesKey = getAesKey();
        if (aesKey == null || (cipherDataEncryptDataWithAES = encryptDataWithAES(aesKey, TAG.getBytes())) == null || (bArrDecryptDataWithAES = decryptDataWithAES(aesKey, cipherDataEncryptDataWithAES)) == null) {
            return false;
        }
        return TAG.equals(new String(bArrDecryptDataWithAES));
    }

    private void waitInitDone() throws ExecutionException, InterruptedException {
        FutureTask<Boolean> futureTask = this.mFutureTaskLock;
        if (futureTask == null || futureTask.isDone()) {
            return;
        }
        try {
            this.mFutureTaskLock.get();
        } catch (Exception e) {
            Log.e(TAG, "Fail when waiting finish : " + e.getMessage(), e);
        }
    }

    private void checkNeedInit() throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException, NoSuchProviderException, InvalidAlgorithmParameterException {
        if (this.mNeedInit || needCreateKey()) {
            initKeyStore();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:11:0x0022  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean needCreateKey() throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException {
        /*
            r3 = this;
            java.security.KeyStore r0 = r3.mKeyStore
            if (r0 != 0) goto L10
            java.lang.String r0 = "AndroidKeyStore"
            java.security.KeyStore r0 = java.security.KeyStore.getInstance(r0)     // Catch: java.lang.Exception -> L10
            r3.mKeyStore = r0     // Catch: java.lang.Exception -> L10
            r1 = 0
            r0.load(r1)     // Catch: java.lang.Exception -> L10
        L10:
            r0 = 1
            boolean r1 = r3.isUsingSimpleEncode()     // Catch: java.lang.Exception -> L23
            if (r1 != 0) goto L22
            java.security.KeyStore r1 = r3.mKeyStore     // Catch: java.lang.Exception -> L23
            java.lang.String r2 = r3.mKeyAliasName     // Catch: java.lang.Exception -> L23
            boolean r1 = r1.containsAlias(r2)     // Catch: java.lang.Exception -> L23
            if (r1 != 0) goto L22
            goto L23
        L22:
            r0 = 0
        L23:
            r3.markNeedInit(r0)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.synology.sylib.security.KeyStoreHelper.needCreateKey():boolean");
    }

    private void markNeedInit(boolean z) {
        this.mNeedInit = z;
    }

    private void savePreference(Method method, String str) {
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        editorEdit.putInt("method", method.value);
        if (str == null) {
            editorEdit.remove("data");
        } else {
            editorEdit.putString("data", str);
        }
        editorEdit.apply();
    }

    private static byte[] createInitialVector(int i) {
        return new SecureRandom().generateSeed(i / 8);
    }

    private static SecretKey createAESKey(int i) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(i);
        return keyGenerator.generateKey();
    }

    private static byte[] hexStringToBytes(String str) {
        int length = str.length();
        if (length % 2 != 0) {
            length++;
            str = "0" + str;
        }
        byte[] bArr = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }
}