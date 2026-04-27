package com.synology.synoholepunch;

public class PunchInfo {
    public static final int GLOBAL_SERVER_CN = 1;
    public static final int GLOBAL_SERVER_WW = 0;
    private static final String LIB_NAME = "synoholepunch";
    public static final int SERVICE_TYPE_BEEDRIVE_HTTPS = 9;
    public static final int SERVICE_TYPE_BSM_HTTP = 10;
    public static final int SERVICE_TYPE_BSM_HTTPS = 11;
    public static final int SERVICE_TYPE_CLOUDSTATION = 7;
    public static final int SERVICE_TYPE_DSM = 1;
    public static final int SERVICE_TYPE_DSM_HTTPS = 2;
    public static final int SERVICE_TYPE_HTTP = 3;
    public static final int SERVICE_TYPE_HTTPS = 4;
    public static final int SERVICE_TYPE_NO_OP = 0;
    public static final int SERVICE_TYPE_VPNPLUS = 8;
    public static final int SERVICE_TYPE_WEBDAV = 5;
    public static final int SERVICE_TYPE_WEBDAV_HTTPS = 6;
    private int connectedPort = -1;
    private int mGlobalServerSite;
    private Long urdObject;

    private native void synopunchDelete(long j);

    private native long synopunchNew(int i, int i2);

    private native void synopunchStart(long j, String str, String str2, int i, int i2, int i3, String str3, SynoPunchCallback synoPunchCallback);

    private native void synopunchStop(long j);

    private native int synopunchTcpPort(long j);

    static {
        System.loadLibrary(LIB_NAME);
    }

    public PunchInfo(int i, int i2, int i3) {
        this.mGlobalServerSite = 0;
        this.urdObject = Long.valueOf(synopunchNew(i, i2));
        this.mGlobalServerSite = i3;
    }

    public void start(String str, String str2, int i, int i2, SynoPunchCallback synoPunchCallback) {
        synopunchStart(this.urdObject.longValue(), str, str2, i, i2, this.mGlobalServerSite, CertFileTask.getCertFilePath(), getWrapperCallback(synoPunchCallback));
        synoPunchCallback.URDClosed();
    }

    public void stop() {
        synopunchStop(this.urdObject.longValue());
    }

    public int getTcpPort() {
        return this.connectedPort;
    }

    public void delete() {
        synopunchDelete(this.urdObject.longValue());
    }

    private SynoPunchCallback getWrapperCallback(final SynoPunchCallback synoPunchCallback) {
        return new SynoPunchCallback() { // from class: com.synology.synoholepunch.PunchInfo.1
            @Override // com.synology.synoholepunch.SynoPunchCallback
            public void URDConnected(int i) {
                PunchInfo.this.connectedPort = i;
                synoPunchCallback.URDConnected(i);
            }

            @Override // com.synology.synoholepunch.SynoPunchCallback
            public void URDClosed() {
                synoPunchCallback.URDClosed();
            }

            @Override // com.synology.synoholepunch.SynoPunchCallback
            public void URDIdleTimeout(int i, int i2) {
                synoPunchCallback.URDIdleTimeout(i, i2);
            }

            @Override // com.synology.synoholepunch.SynoPunchCallback
            public void URDLog(String str) {
                synoPunchCallback.URDLog(str);
            }
        };
    }
}