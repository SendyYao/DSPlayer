package com.synology.sylib.security;

import android.content.Context;
import android.content.ContextWrapper;
import android.text.TextUtils;
import com.google.android.gms.cast.MediaError;
import com.synology.sylib.security.data.KsCipherData;
import com.synology.sylib.security.internal.KsManager;
import com.synology.sylib.security.internal.exception.CryptFailException;
import com.synology.sylib.security.internal.method.CryptMethod;
import com.synology.sylib.security.recover.AesKeyRecoverUtil;
import com.synology.sylib.security.util.Logger;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/* loaded from: classes.dex */
public class KsHelper {
    private static final String DEFAULT_KEYSTORE_ALIAS = "SYNOKEYSTORE";
    private static final int DEFAULT_SEGMENT_SIZE_4K = 4096;
    private static final Object mInstanceMapLock = new Object();
    private static final ConcurrentHashMap<String, Future<KsHelper>> sInstanceMap = new ConcurrentHashMap<>();
    private static int sSegmentSize = 4096;
    private CryptMethod mCryptMethod;
    private String mDeletedAlias = null;

    public static void setSegmentSize(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Require size > 0");
        }
        sSegmentSize = i;
    }

    public static int getSegmentSize() {
        return sSegmentSize;
    }

    public static Future<KsHelper> init(Context context) {
        return init(context, DEFAULT_KEYSTORE_ALIAS, false);
    }

    public static Future<KsHelper> init(Context context, String str) {
        return init(context, str, false);
    }

    public static Future<KsHelper> init(Context context, String str, boolean z) {
        Future<KsHelper> future;
        if (((context instanceof ContextWrapper) && ((ContextWrapper) context).getBaseContext() == null) || context.getApplicationContext() == null) {
            throw new RuntimeException("Call KsHelper too early. Call KsHelper after app.onCreate().");
        }
        synchronized (mInstanceMapLock) {
            ConcurrentHashMap<String, Future<KsHelper>> concurrentHashMap = sInstanceMap;
            if (!concurrentHashMap.containsKey(str)) {
                concurrentHashMap.put(str, createInstanceBackground(context, str, z));
            }
            future = concurrentHashMap.get(str);
        }
        return future;
    }

    public static KsHelper get(Context context) {
        return get(context, DEFAULT_KEYSTORE_ALIAS);
    }

    public static KsHelper get(Context context, String str) {
        try {
            return init(context, str, false).get();
        } catch (Exception e) {
            Logger.e("KSHGet", "Fail when get future result : " + e.getMessage(), e);
            return null;
        }
    }

    public static KsHelper getOrThrow(Context context, String str) throws Exception {
        return init(context, str, true).get();
    }

    public static KsHelper getOrThrow(Context context) throws Exception {
        return getOrThrow(context, DEFAULT_KEYSTORE_ALIAS);
    }


    public static void clearAll(Context context) throws KeyStoreException {
        KsManager.getInstance(context).clearAll();
        synchronized (mInstanceMapLock) {
            sInstanceMap.clear();
        }
    }

    public static void removeInstanceFromMapForTest(Context context, String str) {
        if (!"com.synology.sylib.security.kshelper.test".equals(context.getPackageName())) {
            throw new RuntimeException(new IllegalAccessException("This method can only be access from AndroidTest"));
        }
        synchronized (mInstanceMapLock) {
            sInstanceMap.remove(str);
        }
        Logger.dev("UnitTest", "==== " + str + " removed from map ===");
    }

    private static Future<KsHelper> createInstanceBackground(Context context, String str, boolean z) {
        FutureTask futureTask = new FutureTask(new InitTask(context, str, z));
        new Thread(futureTask).start();
        return futureTask;
    }

    private static class InitTask implements Callable<KsHelper> {
        private final String mAlias;
        private final Context mContext;
        private final boolean tryRecoverAes;

        private InitTask(Context context, String str, boolean z) {
            this.mAlias = str;
            this.mContext = context;
            this.tryRecoverAes = z;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.util.concurrent.Callable
        public KsHelper call() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, BadPaddingException, InvalidKeyException {
            Logger.dev("KSHCall", "Start InitTask: " + this.mAlias);
            return new KsHelper(KsManager.getInstance(this.mContext).get(this.mAlias, this.tryRecoverAes));
        }
    }

    KsHelper(CryptMethod cryptMethod) {
        this.mCryptMethod = cryptMethod;
    }

    public String getCryptMethodName() {
        checkDeleted();
        CryptMethod cryptMethod = this.mCryptMethod;
        return cryptMethod == null ? MediaError.ERROR_TYPE_ERROR : cryptMethod.getName();
    }

    public KsCipherData encrypt(String str) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        checkDeleted();
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return encrypt(str.getBytes());
    }

    public KsCipherData encrypt(byte[] bArr) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        checkDeleted();
        CryptMethod cryptMethod = this.mCryptMethod;
        if (cryptMethod == null) {
            return null;
        }
        return cryptMethod.encrypt(bArr);
    }

    public byte[] decrypt(KsCipherData ksCipherData) {
        checkDeleted();
        CryptMethod cryptMethod = this.mCryptMethod;
        if (cryptMethod == null) {
            return null;
        }
        return cryptMethod.decrypt(ksCipherData);
    }

    public byte[] decryptOrThrow(KsCipherData ksCipherData) throws Throwable {
        return decryptOrThrow(ksCipherData, null);
    }

    public byte[] decryptOrThrow(KsCipherData ksCipherData, AtomicBoolean atomicBoolean) throws Throwable {
        boolean z = false;
        if (atomicBoolean != null) {
            try {
                atomicBoolean.set(false);
            } catch (Throwable th) {
                if (!(th instanceof CryptFailException)) {
                    throw new CryptFailException(this, CryptFailException.Type.Decrypt, CryptFailException.Reason.CryptWithException, th);
                }
                throw th;
            }
        }
        checkDeleted();
        Throwable th2 = null;
        if (this.mCryptMethod == null) {
            throw new CryptFailException(this, CryptFailException.Type.Decrypt, CryptFailException.Reason.CryptMethodIsNull, (Throwable) null);
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.mCryptMethod);
        CryptMethod backupMethod = AesKeyRecoverUtil.getBackupMethod(this.mCryptMethod);
        if (backupMethod != null) {
            arrayList.add(backupMethod);
        }
        int i = 0;
        byte[] bArrDecryptOrThrow = null;
        while (true) {
            if (i >= arrayList.size()) {
                break;
            }
            CryptMethod cryptMethod = (CryptMethod) arrayList.get(i);
            boolean z2 = i == 0;
            try {
                bArrDecryptOrThrow = cryptMethod.decryptOrThrow(ksCipherData);
            } catch (Throwable th3) {
                if (z2) {
                    th2 = th3;
                }
            }
            if (bArrDecryptOrThrow != null) {
                z = !z2;
                break;
            }
            i++;
        }
        if (bArrDecryptOrThrow != null && z && atomicBoolean != null) {
            atomicBoolean.set(true);
        }
        if (bArrDecryptOrThrow == null && th2 != null) {
            throw th2;
        }
        return bArrDecryptOrThrow;
    }

    public String decryptAsString(String str) {
        return decryptAsString(KsCipherData.fromEncoded(str), (String) null);
    }

    public String decryptAsStringOrThrow(String str) throws Throwable {
        return decryptAsStringOrThrow(str, null);
    }

    public String decryptAsStringOrThrow(String str, AtomicBoolean atomicBoolean) throws Throwable {
        if (atomicBoolean != null) {
            atomicBoolean.set(false);
        }
        byte[] bArrDecryptOrThrow = decryptOrThrow(KsCipherData.fromEncoded(str), atomicBoolean);
        if (bArrDecryptOrThrow == null) {
            throw new CryptFailException(this, CryptFailException.Type.Decrypt, CryptFailException.Reason.ResultIsNull, (Throwable) null);
        }
        return new String(bArrDecryptOrThrow);
    }

    public String decryptAsString(String str, String str2) {
        return decryptAsString(KsCipherData.fromEncoded(str), str2);
    }

    public String decryptAsString(KsCipherData ksCipherData) {
        return decryptAsString(ksCipherData, (String) null);
    }

    public String decryptAsString(KsCipherData ksCipherData, String str) {
        byte[] bArrDecrypt = decrypt(ksCipherData);
        return bArrDecrypt == null ? str : new String(bArrDecrypt);
    }

    public String toString() {
        if (this.mCryptMethod == null) {
            return String.format(Locale.ENGLISH, "{KsHelper@%x %s DELETED}", Integer.valueOf(hashCode()), this.mDeletedAlias);
        }
        return String.format(Locale.ENGLISH, "{KsHelper@%x %s}", Integer.valueOf(hashCode()), this.mCryptMethod.toString());
    }

    private void checkDeleted() {
        if (this.mCryptMethod == null) {
            throw new IllegalStateException("Alias \"" + this.mDeletedAlias + "\" is deleted, use KsHelper.get() or KsHelper.init() to re-create.");
        }
    }
}