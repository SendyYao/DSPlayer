package com.synology.sylib.security.data;

import android.text.TextUtils;
import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.lang.ref.WeakReference;

@Deprecated
/* loaded from: classes.dex */
public class CipherData {

    @SerializedName(alternate = {"cipher", "a"}, value = "long")
    private String data;

    @SerializedName(alternate = {"iv", "b"}, value = "short")
    private String iv;
    private transient WeakReference<Gson> mGson;

    public static CipherData fromJson(String str) {
        if (str == null) {
            return null;
        }
        try {
            return (CipherData) new Gson().fromJson(str, CipherData.class);
        } catch (Exception unused) {
            return null;
        }
    }

    public static CipherData fromEncoded(String str) {
        if (str == null) {
            return null;
        }
        try {
            return fromJson(new String(Base64.decode(str, 2)));
        } catch (Exception unused) {
            return null;
        }
    }

    public CipherData setData(byte[] bArr) {
        this.data = Base64.encodeToString(bArr, 2);
        return this;
    }

    public CipherData setIV(byte[] bArr) {
        this.iv = Base64.encodeToString(bArr, 2);
        return this;
    }

    public byte[] getDateBytes() {
        return Base64.decode(this.data, 2);
    }

    public byte[] getIVBytes() {
        return Base64.decode(this.iv, 2);
    }

    public boolean isDataEmpty() {
        return TextUtils.isEmpty(this.data);
    }

    public String toString() {
        return initGson().toJson(this);
    }

    public String getEncoded() {
        return Base64.encodeToString(toString().getBytes(), 2);
    }

    public int hashCode() {
        String str = this.data;
        int iHashCode = (str == null ? 0 : str.hashCode()) * 31;
        String str2 = this.iv;
        return iHashCode + (str2 != null ? str2.hashCode() : 0);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CipherData)) {
            return false;
        }
        CipherData cipherData = (CipherData) obj;
        String str = this.data;
        boolean zEquals = str == null ? cipherData.data == null : str.equals(cipherData.data);
        String str2 = this.iv;
        String str3 = cipherData.iv;
        return zEquals && (str2 == null ? str3 == null : str2.equals(str3));
    }

    private Gson initGson() {
        WeakReference<Gson> weakReference = this.mGson;
        if (weakReference == null || weakReference.get() == null) {
            this.mGson = new WeakReference<>(new Gson());
        }
        return this.mGson.get();
    }
}