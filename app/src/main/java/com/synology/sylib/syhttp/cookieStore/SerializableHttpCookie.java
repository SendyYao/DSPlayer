package com.synology.sylib.syhttp.cookieStore;

import android.util.Log;
import com.synology.sylib.util.HashUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpCookie;

/* loaded from: classes2.dex */
public class SerializableHttpCookie implements Serializable {
    private static final String TAG = "SerializableHttpCookie";
    private static final long serialVersionUID = 6374381323722046732L;
    private transient HttpCookie cookie;

    public String encode(HttpCookie httpCookie) throws IOException {
        this.cookie = httpCookie;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(byteArrayOutputStream).writeObject(this);
            return HashUtils.byteArrayToHexString(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            Log.d(TAG, "IOException in encodeCookie", e);
            return null;
        }
    }

    public HttpCookie decode(String str) {
        try {
            return ((SerializableHttpCookie) new ObjectInputStream(new ByteArrayInputStream(HashUtils.hexStringToByteArray(str))).readObject()).cookie;
        } catch (IOException e) {
            Log.d(TAG, "IOException in decodeCookie", e);
            return null;
        } catch (ClassNotFoundException e2) {
            Log.d(TAG, "ClassNotFoundException in decodeCookie", e2);
            return null;
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(this.cookie.getName());
        objectOutputStream.writeObject(this.cookie.getValue());
        objectOutputStream.writeObject(this.cookie.getComment());
        objectOutputStream.writeObject(this.cookie.getCommentURL());
        objectOutputStream.writeObject(this.cookie.getDomain());
        objectOutputStream.writeLong(this.cookie.getMaxAge());
        objectOutputStream.writeObject(this.cookie.getPath());
        objectOutputStream.writeObject(this.cookie.getPortlist());
        objectOutputStream.writeInt(this.cookie.getVersion());
        objectOutputStream.writeBoolean(this.cookie.getSecure());
        objectOutputStream.writeBoolean(this.cookie.getDiscard());
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        HttpCookie httpCookie = new HttpCookie((String) objectInputStream.readObject(), (String) objectInputStream.readObject());
        this.cookie = httpCookie;
        httpCookie.setComment((String) objectInputStream.readObject());
        this.cookie.setCommentURL((String) objectInputStream.readObject());
        this.cookie.setDomain((String) objectInputStream.readObject());
        this.cookie.setMaxAge(objectInputStream.readLong());
        this.cookie.setPath((String) objectInputStream.readObject());
        this.cookie.setPortlist((String) objectInputStream.readObject());
        this.cookie.setVersion(objectInputStream.readInt());
        this.cookie.setSecure(objectInputStream.readBoolean());
        this.cookie.setDiscard(objectInputStream.readBoolean());
    }
}