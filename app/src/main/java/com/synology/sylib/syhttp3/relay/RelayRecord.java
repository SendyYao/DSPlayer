package com.synology.sylib.syhttp3.relay;

import android.text.TextUtils;
import android.util.Log;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import com.synology.sylib.util.HashUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.builder.EqualsBuilder;

/* loaded from: classes2.dex */
public class RelayRecord implements Serializable, Comparable<String> {
    private static final String TAG = "RelayRecord";
    private int mConnectivity;
    private String mControlHost;
    private List<String> mDSExpectedFingerPrints;
    private String[] mPingPongPaths;
    private String mRealPingPongPath;
    private URL mRealURL;
    private String mRelayRegion;
    private String mServerId;
    private String mServerId2;
    private String[] mServiceIds;

    public RelayRecord(String str, String[] strArr, String[] strArr2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("quickConnectId is empty");
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
        this.mServerId = str.toLowerCase(Locale.US);
        this.mServiceIds = strArr;
        this.mPingPongPaths = strArr2;
        this.mConnectivity = 0;
    }

    public String getServerId() {
        return this.mServerId;
    }

    public String[] getServiceIds() {
        return this.mServiceIds;
    }

    public String[] getPingPongPaths() {
        return this.mPingPongPaths;
    }

    public URL getRealURL() {
        return this.mRealURL;
    }

    public void setRealURL(URL url) {
        this.mRealURL = url;
        Log.d(TAG, "real url: " + this.mRealURL);
    }

    public String getServerId2() {
        return this.mServerId2;
    }

    public void setServerId2(String str) {
        this.mServerId2 = str;
    }

    public String getRealPingPongPath() {
        return this.mRealPingPongPath;
    }

    public void setRealPingPongPath(String str) {
        this.mRealPingPongPath = str;
    }

    public int getConnectivity() {
        return this.mConnectivity;
    }

    public void setConnectivity(int i) {
        this.mConnectivity = i;
    }

    public String getRelayRegion() {
        return this.mRelayRegion;
    }

    public void setRelayRegion(String str) {
        this.mRelayRegion = str;
    }

    public String getControlHost() {
        String str;
        String str2 = this.mControlHost;
        if (str2 != null && str2.length() > 0) {
            return this.mControlHost;
        }
        String str3 = this.mRelayRegion;
        if (str3 != null && RelayUtil.isRegionCn(str3)) {
            str = RelayUtil.GLOBAL_SERVER_CN;
        } else {
            str = RelayUtil.GLOBAL_SERVER_WW;
        }
        this.mControlHost = str;
        return str;
    }

    public void setControlHost(String str) {
        this.mControlHost = str;
    }

    public void setDSExpectedFingerPrints(List<String> list) {
        this.mDSExpectedFingerPrints = list;
    }

    public List<String> getDSExpectedFingerPrints() {
        return this.mDSExpectedFingerPrints;
    }

    public static RelayRecord clone(RelayRecord relayRecord) {
        if (relayRecord == null) {
            throw new IllegalArgumentException("oldRecord == null");
        }
        RelayRecord relayRecord2 = new RelayRecord(relayRecord.getServerId(), relayRecord.getServiceIds(), relayRecord.getPingPongPaths());
        relayRecord2.setRealURL(relayRecord.getRealURL());
        relayRecord2.setRealPingPongPath(relayRecord.getRealPingPongPath());
        relayRecord2.setConnectivity(relayRecord.getConnectivity());
        relayRecord2.setControlHost(relayRecord.getControlHost());
        relayRecord2.setDSExpectedFingerPrints(relayRecord.getDSExpectedFingerPrints());
        return relayRecord2;
    }

    @Override // java.lang.Comparable
    public int compareTo(String str) {
        return this.mServerId.compareTo(str);
    }

    @Deprecated
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

    @Deprecated
    public static RelayRecord decode(String str) throws ClassNotFoundException, IOException {
        try {
            Object object = new ObjectInputStream(new ByteArrayInputStream(HashUtils.hexStringToByteArray(str))).readObject();
            if (object instanceof com.synology.sylib.syhttp.relay.RelayRecord) {
                object = changePackage((com.synology.sylib.syhttp.relay.RelayRecord) object);
            }
            return (RelayRecord) object;
        } catch (IOException e) {
            Log.d(TAG, "IOException in decode", e);
            return null;
        } catch (ClassNotFoundException e2) {
            Log.d(TAG, "ClassNotFoundException in decode", e2);
            return null;
        }
    }

    @Deprecated
    private static RelayRecord changePackage(com.synology.sylib.syhttp.relay.RelayRecord relayRecord) {
        RelayRecord relayRecord2 = new RelayRecord(relayRecord.getServerId(), relayRecord.getServiceIds(), relayRecord.getPingPongPaths());
        relayRecord2.mRealURL = relayRecord.getRealURL();
        relayRecord2.mConnectivity = relayRecord.getConnectivity();
        relayRecord2.mRelayRegion = relayRecord.getRelayRegion();
        return relayRecord2;
    }

    @Deprecated
    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(this.mServerId);
        objectOutputStream.writeObject(this.mServiceIds);
        objectOutputStream.writeObject(this.mPingPongPaths);
        objectOutputStream.writeObject(this.mRealURL);
        objectOutputStream.writeInt(this.mConnectivity);
        objectOutputStream.writeObject(this.mRelayRegion);
        objectOutputStream.writeObject(this.mControlHost);
        objectOutputStream.writeObject(this.mDSExpectedFingerPrints);
    }

    @Deprecated
    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.mServerId = (String) objectInputStream.readObject();
        this.mServiceIds = (String[]) objectInputStream.readObject();
        this.mPingPongPaths = (String[]) objectInputStream.readObject();
        this.mRealURL = (URL) objectInputStream.readObject();
        this.mConnectivity = objectInputStream.readInt();
        this.mRelayRegion = (String) objectInputStream.readObject();
        try {
            this.mControlHost = (String) objectInputStream.readObject();
        } catch (OptionalDataException unused) {
            this.mControlHost = null;
        }
        this.mDSExpectedFingerPrints = (List) objectInputStream.readObject();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof RelayRecord)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        RelayRecord relayRecord = (RelayRecord) obj;
        return new EqualsBuilder().append(this.mServerId, relayRecord.mServerId).append((Object[]) this.mServiceIds, (Object[]) relayRecord.mServiceIds).append((Object[]) this.mPingPongPaths, (Object[]) relayRecord.mPingPongPaths).isEquals();
    }
}