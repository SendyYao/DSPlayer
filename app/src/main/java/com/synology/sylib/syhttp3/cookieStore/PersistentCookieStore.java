package com.synology.sylib.syhttp3.cookieStore;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
//import com.google.firebase.sessions.settings.RemoteSettings;
import com.synology.sylib.security.migrate.data.KsRef;

import java.io.IOException;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.crypto.NoSuchPaddingException;

/* loaded from: classes2.dex */
public abstract class PersistentCookieStore implements CookieStore {
    private static final String SP_KEY_DELIMITER = "|";
    private static final String SP_KEY_DELIMITER_REGEX = "\\|";
    private static final String TAG = "PersistentCookieStore";
    private Map<URI, Set<HttpCookie>> allCookies;
    private SharedPreferences sharedPreferences;

    abstract String convertCookieToString(HttpCookie httpCookie) throws IOException;

    abstract HttpCookie convertStringToCookie(String str, KsRef<String> ksRef) throws IOException, NoSuchFieldException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException, CertificateException, NoSuchAlgorithmException, KeyStoreException, ExecutionException, NoSuchProviderException, InvalidKeyException, InterruptedException;

    protected abstract String getPersistentPrefsName();

    protected abstract String getTag();

    abstract void onBeforeInit(Context context, String str) throws PackageManager.NameNotFoundException;

    public PersistentCookieStore(Context context) throws PackageManager.NameNotFoundException {
        initCookieStore(context, getPersistentPrefsName());
    }

    public PersistentCookieStore(Context context, String str) throws PackageManager.NameNotFoundException {
        initCookieStore(context, str);
    }

    private void initCookieStore(Context context, String str) throws PackageManager.NameNotFoundException {
        onBeforeInit(context, str);
        this.sharedPreferences = context.getSharedPreferences(str, 0);
        loadAllFromPersistence();
    }

    private void loadAllFromPersistence() {
        this.allCookies = new HashMap();
        Map<String, ?> all = this.sharedPreferences.getAll();
        HashMap map = new HashMap();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            try {
                URI uri = new URI(entry.getKey().split(SP_KEY_DELIMITER_REGEX, 2)[0]);
                String str = (String) entry.getValue();
                KsRef<String> ksRefNewString = KsRef.newString();
                HttpCookie httpCookieConvertStringToCookie = convertStringToCookie(str, ksRefNewString);
                if (httpCookieConvertStringToCookie != null) {
                    if (!ksRefNewString.isNull()) {
                        map.put(entry.getKey(), ksRefNewString.get());
                    }
                    Set<HttpCookie> hashSet = this.allCookies.get(uri);
                    if (hashSet == null) {
                        hashSet = new HashSet<>();
                        this.allCookies.put(uri, hashSet);
                    }
                    hashSet.add(httpCookieConvertStringToCookie);
                }
            } catch (URISyntaxException e) {
                Log.w(getTag(), e);
            } catch (IOException | NoSuchFieldException | ClassNotFoundException |
                     InvalidAlgorithmParameterException | NoSuchPaddingException |
                     CertificateException | NoSuchAlgorithmException | KeyStoreException |
                     ExecutionException | NoSuchProviderException | InvalidKeyException |
                     InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (!map.isEmpty()) {
            SharedPreferences.Editor editorEdit = this.sharedPreferences.edit();
//            for (Map.Entry entry2 : map.entrySet()) {
//                editorEdit.putString((String) entry2.getKey(), (String) entry2.getValue());
//            }
            editorEdit.apply();
        }
    }

    @Override // java.net.CookieStore
    public synchronized void add(URI uri, HttpCookie httpCookie) {
        URI uriCookieUri = cookieUri(uri, httpCookie);
        Set<HttpCookie> hashSet = this.allCookies.get(uriCookieUri);
        if (hashSet == null) {
            hashSet = new HashSet<>();
            this.allCookies.put(uriCookieUri, hashSet);
        }
        hashSet.remove(httpCookie);
        hashSet.add(httpCookie);
        try {
            saveToPersistence(uriCookieUri, httpCookie);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static URI cookieUri(URI uri, HttpCookie httpCookie) {
        if (httpCookie.getDomain() == null) {
            return uri;
        }
        try {
            // RemoteSettings.FORWARD_SLASH_STRING
            return new URI(uri.getScheme() == null ? "http" : uri.getScheme(), httpCookie.getDomain(), httpCookie.getPath() == null ? "/" : httpCookie.getPath(), null);
        } catch (URISyntaxException e) {
            Log.w(TAG, "cookieUri", e);
            return uri;
        }
    }

    private static String getFixedHost(URI uri) {
        if (uri.getHost() != null) {
            return uri.getHost();
        }
        try {
            String host = Uri.parse(uri.toString()).getHost();
            if (host != null) {
                return host.contains(".") ? host : "";
            }
            return "";
        } catch (Exception unused) {
            return "";
        }
    }

    private void saveToPersistence(URI uri, HttpCookie httpCookie) throws IOException {
        SharedPreferences.Editor editorEdit = this.sharedPreferences.edit();
        String strConvertCookieToString = convertCookieToString(httpCookie);
        if (strConvertCookieToString != null) {
            editorEdit.putString(uri.toString() + SP_KEY_DELIMITER + httpCookie.getName(), strConvertCookieToString);
        }
        editorEdit.apply();
    }

    @Override // java.net.CookieStore
    public synchronized List<HttpCookie> get(URI uri) {
        return getValidCookies(uri);
    }

    @Override // java.net.CookieStore
    public synchronized List<HttpCookie> getCookies() {
        ArrayList arrayList;
        arrayList = new ArrayList();
        Iterator<Map.Entry<URI, Set<HttpCookie>>> it = this.allCookies.entrySet().iterator();
        while (it.hasNext()) {
            arrayList.addAll(getValidCookies(it.next().getKey()));
        }
        return arrayList;
    }

    private List<HttpCookie> getValidCookies(URI uri) {
        HashSet hashSet = new HashSet();
        Iterator<Map.Entry<URI, Set<HttpCookie>>> it = this.allCookies.entrySet().iterator();
        while (it.hasNext()) {
            URI key = it.next().getKey();
            String fixedHost = getFixedHost(key);
            String fixedHost2 = getFixedHost(uri);
            if (!TextUtils.isEmpty(key.getScheme()) && !TextUtils.isEmpty(uri.getScheme()) && !TextUtils.isEmpty(fixedHost) && !TextUtils.isEmpty(fixedHost2) && key.getScheme().equalsIgnoreCase(uri.getScheme()) && fixedHost.equalsIgnoreCase(fixedHost2)) {
                hashSet.addAll(this.allCookies.get(key));
            }
        }
        return new ArrayList(hashSet);
    }

    @Override // java.net.CookieStore
    public synchronized List<URI> getURIs() {
        return new ArrayList(this.allCookies.keySet());
    }

    @Override // java.net.CookieStore
    public synchronized boolean remove(URI uri, HttpCookie httpCookie) {
        boolean z;
        Set<HttpCookie> set = this.allCookies.get(uri);
        z = set != null && set.remove(httpCookie);
        if (z) {
            removeFromPersistence(uri, httpCookie);
        }
        return z;
    }

    private void removeFromPersistence(URI uri, HttpCookie httpCookie) {
        SharedPreferences.Editor editorEdit = this.sharedPreferences.edit();
        editorEdit.remove(uri.toString() + SP_KEY_DELIMITER + httpCookie.getName());
        editorEdit.apply();
    }

    @Override // java.net.CookieStore
    public synchronized boolean removeAll() {
        this.allCookies.clear();
        removeAllFromPersistence();
        return true;
    }

    private void removeAllFromPersistence() {
        this.sharedPreferences.edit().clear().apply();
    }
}