package com.synology.sylib.syhttp3.cookieStore;

import android.content.Context;
import android.content.pm.PackageManager;

import com.synology.sylib.security.migrate.data.KsRef;

import java.io.IOException;
import java.net.HttpCookie;

/* loaded from: classes2.dex */
public class PlainPersistentCookieStore extends PersistentCookieStore {
    public static final String DEFAULT_PREFS_NAME = "cookieStore";
    private static final String TAG = "PlainPersistentCookieStore";

    @Override // com.synology.sylib.syhttp3.cookieStore.PersistentCookieStore
    void onBeforeInit(Context context, String str) {
    }

    public PlainPersistentCookieStore(Context context) throws PackageManager.NameNotFoundException {
        super(context);
    }

    public PlainPersistentCookieStore(Context context, String str) throws PackageManager.NameNotFoundException {
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
    String convertCookieToString(HttpCookie httpCookie) throws IOException {
        return new SerializableHttpCookie().encode(httpCookie);
    }

    @Override // com.synology.sylib.syhttp3.cookieStore.PersistentCookieStore
    HttpCookie convertStringToCookie(String str, KsRef<String> ksRef) throws IOException, NoSuchFieldException, ClassNotFoundException {
        return new SerializableHttpCookie().decode(str);
    }
}