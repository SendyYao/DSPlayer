package com.synology.sylib.syhttp3.relay;

import java.net.URL;

/* loaded from: classes2.dex */
public class RelayResult {
    private int mConnectivity;
    private URL mUrl;

    public RelayResult(URL url, int i) {
        this.mUrl = url;
        this.mConnectivity = i;
    }

    public URL getURL() {
        return this.mUrl;
    }

    public URL getRealURL() {
        return getURL();
    }

    public int getConnectivity() {
        return this.mConnectivity;
    }

    String getConnectivityInfo() {
        String str;
        switch (this.mConnectivity) {
            case 0:
                str = "NOT_CONNECTED";
                break;
            case 1:
                str = "IP_DDNS";
                break;
            case 2:
                str = "QC_LAN_IP";
                break;
            case 3:
                str = "QC_DDNS";
                break;
            case 4:
                str = "QC_FQDN";
                break;
            case 5:
                str = "QC_WAN_IP";
                break;
            case 6:
                str = "QC_HOLEPUNCH";
                break;
            case 7:
                str = "QC_TUNNEL";
                break;
            default:
                str = "UNKNOWN";
                break;
        }
        return str + " [" + this.mConnectivity + "]";
    }
}