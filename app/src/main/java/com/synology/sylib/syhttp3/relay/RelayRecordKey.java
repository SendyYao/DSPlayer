package com.synology.sylib.syhttp3.relay;

import android.content.Context;
import android.util.Log;
import com.synology.sylib.history.SynoApplication;
import com.synology.sylib.syhttp3.relay.utils.RelayCommon;
import java.util.Locale;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/* loaded from: classes2.dex */
public class RelayRecordKey {
    private static final String KEY_DIVIDER = "#";
    private static final String LOG_TAG = "RelayRecordKey";
    private boolean mIsHttps;
    private String mServerId;

    private RelayRecordKey(String str, boolean z) {
        this.mServerId = str.toLowerCase(Locale.US);
        this.mIsHttps = z;
    }

    public static RelayRecordKey convertToRelayRecordKey(String str) {
        String strSubstring;
        int iIndexOf = str.indexOf(KEY_DIVIDER);
        if (iIndexOf == -1) {
            Log.w(LOG_TAG, "convertToRelayRecordKey: keyString without divider #");
            strSubstring = "http";
        } else {
            String strSubstring2 = str.substring(0, iIndexOf);
            strSubstring = str.substring(iIndexOf + 1);
            str = strSubstring2;
        }
        return new RelayRecordKey(str, RelayCommon.isHttps(strSubstring));
    }

    public static RelayRecordKey getInstance(String str, String str2) {
        return new RelayRecordKey(str, RelayCommon.isHttps(ServiceId.getProtocol(str2)));
    }

    public static RelayRecordKey getInstance(Context context, String str, boolean z) {
        if (SynoApplication.DSCLOUD.equals(context.getPackageName())) {
            z = false;
        }
        return new RelayRecordKey(str, z);
    }

    public static RelayRecordKey getInstanceWithCorrectHttps(String str, boolean z) {
        return new RelayRecordKey(str, z);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof RelayRecordKey)) {
            return false;
        }
        RelayRecordKey relayRecordKey = (RelayRecordKey) obj;
        return new EqualsBuilder().append(this.mServerId, relayRecordKey.mServerId).append(this.mIsHttps, relayRecordKey.mIsHttps).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(this.mServerId).append(this.mIsHttps).hashCode();
    }

    public String toString() {
        return this.mServerId + KEY_DIVIDER + RelayCommon.getProtocol(this.mIsHttps);
    }

    public String getServerId() {
        return this.mServerId;
    }
}