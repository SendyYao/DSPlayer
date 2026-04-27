package com.synology.sylib.syhttp3.cookieStore;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.synology.sylib.security.KeyStoreHelper;
import com.synology.sylib.security.data.CipherData;
import com.synology.sylib.security.migrate.data.KsRef;
import java.io.IOException;
import java.net.HttpCookie;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;

import javax.crypto.NoSuchPaddingException;

/* loaded from: classes2.dex */
public class CipherPersistentCookieStore extends PersistentCookieStore {
    public static final String DEFAULT_PREFS_NAME = "synoCookieStore";
    private static final String TAG = "CipherPersistentCookieStore";
    private KeyStoreHelper mKeyStoreHelper;

    public CipherPersistentCookieStore(Context context) throws PackageManager.NameNotFoundException {
        super(context);
    }

    public CipherPersistentCookieStore(Context context, String str) throws PackageManager.NameNotFoundException {
        super(context, str);
    }

    @Override // com.synology.sylib.syhttp3.cookieStore.PersistentCookieStore
    protected String getTag() {
        return TAG;
    }

    @Override // com.synology.sylib.syhttp3.cookieStore.PersistentCookieStore
    protected String getPersistentPrefsName() {
        return DEFAULT_PREFS_NAME;
    }

    @Override // com.synology.sylib.syhttp3.cookieStore.PersistentCookieStore
    void onBeforeInit(Context context, String str) throws PackageManager.NameNotFoundException {
        this.mKeyStoreHelper = KeyStoreHelper.get(context);
    }

    @Override // com.synology.sylib.syhttp3.cookieStore.PersistentCookieStore
    String convertCookieToString(HttpCookie httpCookie) throws IOException {
        CipherData cipherDataEncrypt = this.mKeyStoreHelper.encrypt(new SerializableHttpCookie().encode(httpCookie));
        if (cipherDataEncrypt == null) {
            Log.w(getTag(), "save cookie fail");
            return null;
        }
        return cipherDataEncrypt.getEncoded();
    }

    @Override // com.synology.sylib.syhttp3.cookieStore.PersistentCookieStore
    HttpCookie convertStringToCookie(String str, KsRef<String> ksRef) throws IOException, NoSuchFieldException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException, CertificateException, NoSuchAlgorithmException, KeyStoreException, ExecutionException, NoSuchProviderException, InvalidKeyException, InterruptedException {
        String strDecryptAsString = this.mKeyStoreHelper.decryptAsString(CipherData.fromEncoded(str), ksRef.toString());
        if (strDecryptAsString == null) {
            Log.w(getTag(), "Decode cookie fail");
            return null;
        }
        return new SerializableHttpCookie().decode(strDecryptAsString);
    }
}