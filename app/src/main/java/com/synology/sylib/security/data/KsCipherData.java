package com.synology.sylib.security.data;

import android.text.TextUtils;
import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.lang.ref.WeakReference;
import java.util.Objects;

/* loaded from: classes.dex */
public class KsCipherData {

    @SerializedName(alternate = {"cipher", "long"}, value = "a")
    private String cipher;

    @SerializedName(alternate = {"iv", "short"}, value = "b")
    private String iv;
    private transient WeakReference<Gson> mGsonHolder;

    public static KsCipherData fromJson(String str) {
        if (str == null) {
            return null;
        }
        try {
            return (KsCipherData) new Gson().fromJson(str, KsCipherData.class);
        } catch (Exception unused) {
            return null;
        }
    }

    public static KsCipherData fromEncoded(String str) {
        if (str == null) {
            return null;
        }
        try {
            return fromJson(new String(Base64.decode(str, 2)));
        } catch (Exception unused) {
            return null;
        }
    }

    public KsCipherData setCipher(byte[] bArr) {
        this.cipher = Base64.encodeToString(bArr, 2);
        return this;
    }

    public KsCipherData setIV(byte[] bArr) {
        this.iv = Base64.encodeToString(bArr, 2);
        return this;
    }

    public byte[] getCipherBytes() {
        return Base64.decode(this.cipher, 2);
    }

    public byte[] getIVBytes() {
        return Base64.decode(this.iv, 2);
    }

    public boolean isCipherEmpty() {
        return TextUtils.isEmpty(this.cipher);
    }

    public String toString() {
        return getGson().toJson(this);
    }

    public String getEncoded() {
        return Base64.encodeToString(toString().getBytes(), 2);
    }

    public int hashCode() {
        String str = this.cipher;
        int iHashCode = (str == null ? 0 : str.hashCode()) * 31;
        String str2 = this.iv;
        return iHashCode + (str2 != null ? str2.hashCode() : 0);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof KsCipherData)) {
            return false;
        }
        KsCipherData ksCipherData = (KsCipherData) obj;
        return Objects.equals(this.cipher, ksCipherData.cipher) && Objects.equals(this.iv, ksCipherData.iv);
    }

    private Gson getGson() {
        WeakReference<Gson> weakReference = this.mGsonHolder;
        Gson gson = weakReference != null ? weakReference.get() : null;
        if (gson != null) {
            return gson;
        }
        Gson gson2 = new Gson();
        this.mGsonHolder = new WeakReference<>(gson2);
        return gson2;
    }
}