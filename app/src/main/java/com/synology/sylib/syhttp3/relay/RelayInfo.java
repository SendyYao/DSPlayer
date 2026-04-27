package com.synology.sylib.syhttp3.relay;

import android.text.TextUtils;
import android.util.Log;
import com.synology.sylib.util.HashUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

/* loaded from: classes2.dex */
public class RelayInfo implements Serializable, Comparable<String> {
    private static final String TAG = "RelayInfo";
    private String mPackageName;
    private String[] mPingPongPaths;
    private String mProtocol;
    private String[] mServiceIds;

    public RelayInfo(String str, String str2, String[] strArr, String[] strArr2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("packageName name is empty");
        }
        if (TextUtils.isEmpty(str2)) {
            throw new IllegalArgumentException("protocol name is empty");
        }
        if (strArr == null || strArr.length <= 0) {
            throw new IllegalArgumentException("serviceIds is empty");
        }
//        ServiceId.serviceIdMigration(strArr);
//        if (!ServiceId.isAllWithSameProtocol(strArr)) {
//            throw new IllegalArgumentException("serviceIds with different protocol " + Arrays.toString(strArr));
//        }
        if (strArr2 == null || strArr2.length <= 0) {
            throw new IllegalArgumentException("pingPongPath is empty");
        }
        this.mPackageName = str;
        this.mProtocol = str2;
        this.mServiceIds = strArr;
        for (int i = 0; i < strArr2.length; i++) {
            String str3 = strArr2[i];
            if (str3.contains("pingpong.cgi?")) {
                str3 = str3 + "&quickconnect=true";
            } else if (str3.contains("pingpong.cgi")) {
                str3 = str3 + "?quickconnect=true";
            }
            strArr2[i] = str3;
        }
        this.mPingPongPaths = strArr2;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getProtocol() {
        return this.mProtocol;
    }

    public String[] getServiceIds() {
        return this.mServiceIds;
    }

    public String[] getPingPongPaths() {
        return this.mPingPongPaths;
    }

    @Override // java.lang.Comparable
    public int compareTo(String str) {
        return this.mPackageName.compareTo(str);
    }

    public String encode() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(byteArrayOutputStream).writeObject(this);
            return HashUtils.byteArrayToHexString(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            Log.d(TAG, "IOException in encode", e);
            return null;
        }
    }

    public static RelayInfo decode(String str) throws ClassNotFoundException, IOException {
        try {
            Object object = new ObjectInputStream(new ByteArrayInputStream(HashUtils.hexStringToByteArray(str))).readObject();
            if (object instanceof com.synology.sylib.syhttp.relay.RelayInfo) {
                object = changePackage((com.synology.sylib.syhttp.relay.RelayInfo) object);
            }
            return (RelayInfo) object;
        } catch (IOException e) {
            Log.d(TAG, "IOException in decode", e);
            return null;
        } catch (ClassNotFoundException e2) {
            Log.d(TAG, "ClassNotFoundException in decode", e2);
            return null;
        }
    }

    private static RelayInfo changePackage(com.synology.sylib.syhttp.relay.RelayInfo relayInfo) {
        return new RelayInfo(relayInfo.getPackageName(), relayInfo.getProtocol(), relayInfo.getServiceIds(), relayInfo.getPingPongPaths());
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(this.mPackageName);
        objectOutputStream.writeObject(this.mProtocol);
        objectOutputStream.writeObject(this.mServiceIds);
        objectOutputStream.writeObject(this.mPingPongPaths);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.mPackageName = (String) objectInputStream.readObject();
        this.mProtocol = (String) objectInputStream.readObject();
        this.mServiceIds = (String[]) objectInputStream.readObject();
        this.mPingPongPaths = (String[]) objectInputStream.readObject();
    }
}