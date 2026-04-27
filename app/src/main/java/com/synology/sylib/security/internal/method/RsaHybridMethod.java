package com.synology.sylib.security.internal.method;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.text.TextUtils;
import android.util.Base64;
//import com.google.firebase.crashlytics.internal.metadata.UserMetadata;
import com.synology.sylib.security.internal.KsManager;
import com.synology.sylib.security.util.Logger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/* loaded from: classes.dex */
public class RsaHybridMethod extends CryptMethod {
    private static final int RSA_KEY_BITS = 2048;
    public static final String SZ_ALGORITHM_AES = "AES";
    public static final String SZ_ALGORITHM_RSA = "RSA";
    public static final String SZ_KEY_AES_KEY = "key";
    public static final String SZ_RSA_ECB_PKCS1 = "RSA/ECB/PKCS1Padding";
    private final Context mContext;
    private String mEncryptedAesKey;
    private final KeyRecoverListener mKeyRecoverListener;
    private static final Method METHOD = Method.RSA_HYBRID;
    private static File sRootBackupPath = null;

    @FunctionalInterface
    public interface KeyRecoverListener {
        void onRecoverAesKey(String str, String str2);
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public boolean isNeedValidateAfterLoad() {
        return true;
    }

    public static void removeBackupEncryptedKey(Context context, String str) {
        File backupRoot = getBackupRoot(context);
        if (backupRoot == null) {
            return;
        }
        new File(backupRoot, getBackupFileName(str)).delete();
    }

    public static void clearAllBackups(Context context) {
        File backupRoot = getBackupRoot(context);
        if (backupRoot == null) {
            return;
        }
        for (File file : backupRoot.listFiles()) {
            file.delete();
        }
    }

    public RsaHybridMethod(String str, KeyStore keyStore, Context context, KeyRecoverListener keyRecoverListener) {
        this(str, keyStore, null, context, keyRecoverListener);
    }

    public RsaHybridMethod(String str, KeyStore keyStore, HashMap<String, Object> map, Context context, KeyRecoverListener keyRecoverListener) {
        super(str, keyStore, map);
        this.mKeyRecoverListener = keyRecoverListener;
        this.mContext = context;
        if (this.mSetting != null) {
            String strValueOf = String.valueOf(this.mSetting.get("key"));
            this.mEncryptedAesKey = strValueOf;
            if (TextUtils.isEmpty(strValueOf)) {
                this.mEncryptedAesKey = loadBackupEncryptedKey(context, str);
                this.mSetting.put("key", this.mEncryptedAesKey);
                keyRecoverListener.onRecoverAesKey(getKeyAlias(), this.mEncryptedAesKey);
            }
        }
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    Key getAesKey() {
        return getAesKeyInternal(true);
    }

    private Key getAesKeyInternal(boolean z) {
        String str;
        byte[] bArrCryptRSA;
        try {
            if (getKeyStore() != null && (str = this.mEncryptedAesKey) != null && (bArrCryptRSA = cryptRSA(2, Base64.decode(str, 2))) != null) {
                return new SecretKeySpec(bArrCryptRSA, "AES");
            }
        } catch (Exception e) {
            Logger.e("RsaCrypt", "getKey : " + e.getMessage(), e);
        }
        Logger.w("RsaCrypt", "getKey fail , retry : " + z);
        if (!z) {
            return null;
        }
        this.mEncryptedAesKey = loadBackupEncryptedKey(this.mContext, getKeyAlias());
        this.mKeyRecoverListener.onRecoverAesKey(getKeyAlias(), this.mEncryptedAesKey);
        return getAesKeyInternal(false);
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    boolean onValidate() {
        KeyStore keyStore = getKeyStore();
        String str = this.mEncryptedAesKey;
        if (str != null && str.length() > 0 && keyStore != null) {
            try {
                boolean zEqualsIgnoreCase = SZ_ALGORITHM_RSA.equalsIgnoreCase(keyStore.getKey(getKeyAlias(), null).getAlgorithm());
                Logger.d("RsaCrypt", "validate : algorithm match = " + zEqualsIgnoreCase);
                return zEqualsIgnoreCase;
            } catch (Exception e) {
                Logger.e("RsaCrypt", "validate : " + e.getMessage(), e);
                return false;
            }
        }
        Logger.d("RsaCrypt", "validate : aeskey = " + this.mEncryptedAesKey + " , keyStore = " + keyStore);
        return false;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public Method getMethod() {
        return METHOD;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    public void onDelete() {
        removeBackupEncryptedKey(this.mContext, getKeyAlias());
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    HashMap<String, Object> onCreateSettings() {
        backupEncryptedKey(this.mContext, getKeyAlias(), this.mEncryptedAesKey);
        if (this.mEncryptedAesKey == null) {
            return null;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("key", this.mEncryptedAesKey);
        return map;
    }

    @Override // com.synology.sylib.security.internal.method.CryptMethod
    boolean onCreateKey(String str) {
        KeyStore keyStore = getKeyStore();
        if (keyStore == null) {
            return false;
        }
        try {
            if (keyStore.containsAlias(getKeyAlias())) {
                Logger.w("RsaCrypt", "create : Key alias is already existed.");
                return true;
            }
            boolean zGenerateRsaKeyStore_api23 = generateRsaKeyStore_api23();
            Logger.dev("RsaCrypt", "Gen RAS 23 : " + zGenerateRsaKeyStore_api23);
            if (!zGenerateRsaKeyStore_api23) {
                return false;
            }
            this.mEncryptedAesKey = getEncryptedAesKey(generateAesRandomKey());
            Logger.dev("RsaCrypt", "Gen Random AES : " + this.mEncryptedAesKey);
            return this.mEncryptedAesKey != null;
        } catch (InterruptedException e) {
            Logger.e("RasCrypt", "create interrupted : " + e.getMessage());
            return false;
        } catch (Exception e2) {
            Logger.e("RasCrypt", "create :  " + e2.getMessage(), e2);
            return false;
        }
    }

    private boolean generateRsaKeyStore_api23() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(SZ_ALGORITHM_RSA, KsManager.KEYSTORE_PROVIDER);
        keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(getKeyAlias(), 3).setBlockModes("ECB").setDigests("SHA-256", "SHA-512").setEncryptionPaddings("PKCS1Padding").setKeySize(2048).build());
        keyPairGenerator.generateKeyPair();
        return true;
    }

    private boolean generateRsaKeyStore_api18() throws Exception {
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(1, 100);
        KeyPairGeneratorSpec.Builder endDate = new KeyPairGeneratorSpec.Builder(this.mContext).setAlias(getKeyAlias()).setSubject(new X500Principal("CN=" + getKeyAlias())).setSerialNumber(BigInteger.TEN).setStartDate(calendar.getTime()).setEndDate(calendar2.getTime());
        endDate.setKeySize(2048);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(SZ_ALGORITHM_RSA, KsManager.KEYSTORE_PROVIDER);
        keyPairGenerator.initialize(endDate.build());
        keyPairGenerator.generateKeyPair();
        return true;
    }

    private Key generateAesRandomKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    private String getEncryptedAesKey(Key key) throws Exception {
        return Base64.encodeToString(cryptRSA(1, key.getEncoded()), 2);
    }

    private byte[] cryptRSA(int i, byte[] bArr) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException, IOException, KeyStoreException {
        Key key;
        int iBitLength;
        KeyStore keyStore = getKeyStore();
        if (keyStore == null) {
            return null;
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            if (i == 1) {
                key = keyStore.getCertificate(getKeyAlias()).getPublicKey();
                iBitLength = ((RSAPublicKey) key).getModulus().bitLength();
            } else {
                PublicKey publicKey = keyStore.getCertificate(getKeyAlias()).getPublicKey();
                key = keyStore.getKey(getKeyAlias(), null);
                iBitLength = ((RSAPublicKey) publicKey).getModulus().bitLength();
            }
            Cipher cipher = Cipher.getInstance(SZ_RSA_ECB_PKCS1);
            cipher.init(i, key);
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
            Logger.e("RsaCrypt", "RSA Mode[" + i + "] fail : " + e.getMessage(), e);
            return null;
        }
    }

    private static void backupEncryptedKey(Context context, String str, String str2) {
        if (str2 == null) {
            return;
        }
        File backupRoot = getBackupRoot(context);
        if (backupRoot == null) {
            Logger.d("BackupKey", "Fail : Not found");
            return;
        }
        try {
            PrintStream printStream = new PrintStream(new File(backupRoot, getBackupFileName(str)));
            printStream.print(str2);
            printStream.close();
        } catch (Exception e) {
            Logger.d("BackupKey", "Fail : " + e.getMessage());
        }
    }

    public static String loadBackupEncryptedKey(Context context, String str) {
        File backupRoot = getBackupRoot(context);
        String strNextLine = null;
        if (backupRoot == null) {
            return null;
        }
        try {
            Scanner scanner = new Scanner(new File(backupRoot, getBackupFileName(str)));
            strNextLine = scanner.nextLine();
            scanner.close();
            return strNextLine;
        } catch (Exception e) {
            Logger.d("LoadBackupKey", "Fail : " + e.getMessage());
            return strNextLine;
        }
    }

    public static String getBackupFileName(String str) {
        return String.format("RSA_%X", Integer.valueOf(str.hashCode())) + ".key";
    }

    public static File getBackupRoot(Context context) {
        File file = null;
        if (sRootBackupPath == null) {
            synchronized (RsaHybridMethod.class) {
                if (sRootBackupPath == null) {
                    File file2 = null;
                    try {
                        File noBackupFilesDir = context.getNoBackupFilesDir();
                        try {
                            // UserMetadata.KEYDATA_FILENAME
                            file = new File(noBackupFilesDir, "internal-keys");
                        } catch (Exception e) {
                            file2 = noBackupFilesDir;
                        }
                    } catch (Exception e) {

                    }
                    try {
                        if (!file.exists() && !file.mkdirs()) {
                            Logger.d("GetBackupDir", "Fail : Cannot create root folder");
                            return null;
                        }
                    } catch (Exception e) {
                        file2 = file;
                        Logger.d("GetBackupDir", "Fail : " + e.getMessage());
                        file = file2;
                        sRootBackupPath = file;
                        return sRootBackupPath;
                    }
                    sRootBackupPath = file;
                }
            }
        }
        return sRootBackupPath;
    }
}