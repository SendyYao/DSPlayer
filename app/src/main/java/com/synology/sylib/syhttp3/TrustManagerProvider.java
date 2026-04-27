package com.synology.sylib.syhttp3;

import android.util.Log;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/* loaded from: classes2.dex */
public class TrustManagerProvider {
    private static final String TAG = "TrustManagerProvider";
    private static final AtomicBoolean mHasTriedInitX509TrustManager = new AtomicBoolean(false);
    private static X509TrustManager mX509TrustManager;

    public static X509TrustManager getX509TrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        return getX509TrustManagerByFactory();
    }

    public static X509TrustManager tryToGetX509TrustManager(SSLSocketFactory sSLSocketFactory) throws NoSuchAlgorithmException, KeyStoreException, NoSuchFieldException, IllegalAccessException {
        X509TrustManager x509TrustManagerByFactory = getX509TrustManagerByFactory();
        return x509TrustManagerByFactory == null ? getX509TrustManagerByReflection(sSLSocketFactory) : x509TrustManagerByFactory;
    }

    protected static X509TrustManager getX509TrustManagerByFactory() throws NoSuchAlgorithmException, KeyStoreException {
        if (mHasTriedInitX509TrustManager.getAndSet(true)) {
            return mX509TrustManager;
        }
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length > 0) {
                TrustManager trustManager = trustManagers[0];
                if (trustManager instanceof X509TrustManager) {
                    mX509TrustManager = (X509TrustManager) trustManager;
                }
            }
        } catch (KeyStoreException e) {
            Log.e(TAG, "KeyStoreException: " + e.getMessage());
        } catch (NoSuchAlgorithmException e2) {
            Log.e(TAG, "NoSuchAlgorithmException: " + e2.getMessage());
        }
        return mX509TrustManager;
    }

    protected static X509TrustManager getX509TrustManagerByReflection(SSLSocketFactory sSLSocketFactory) throws IllegalAccessException, NoSuchFieldException, IllegalArgumentException {
        try {
            Object fieldOrNull = readFieldOrNull(sSLSocketFactory, Class.forName("sun.security.ssl.SSLContextImpl"), "context");
            if (fieldOrNull == null) {
                return null;
            }
            return (X509TrustManager) readFieldOrNull(fieldOrNull, X509TrustManager.class, "trustManager");
        } catch (ClassNotFoundException unused) {
            return null;
        }
    }

    private static <T> T readFieldOrNull(Object obj, Class<T> cls, String str) throws IllegalAccessException, NoSuchFieldException, IllegalArgumentException {
        Object fieldOrNull;
        for (Class<?> superclass = obj.getClass(); superclass != Object.class; superclass = superclass.getSuperclass()) {
            try {
                Field declaredField = superclass.getDeclaredField(str);
                declaredField.setAccessible(true);
                Object obj2 = declaredField.get(obj);
                if (cls.isInstance(obj2)) {
                    return cls.cast(obj2);
                }
                return null;
            } catch (IllegalAccessException unused) {
                throw new AssertionError();
            } catch (NoSuchFieldException unused2) {
            }
        }
        if (str.equals("delegate") || (fieldOrNull = readFieldOrNull(obj, Object.class, "delegate")) == null) {
            return null;
        }
        return (T) readFieldOrNull(fieldOrNull, cls, str);
    }
}
