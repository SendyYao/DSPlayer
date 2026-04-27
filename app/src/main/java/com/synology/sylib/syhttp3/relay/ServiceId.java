package com.synology.sylib.syhttp3.relay;

/* loaded from: classes2.dex */
public class ServiceId {
    public static final String AUDIO_HTTP = "audio_http";
    public static final String AUDIO_HTTPS = "audio_https";
    public static final String BEEDRIVE_HTTPS = "beedrive_https";
    public static final String BSM_HTTP = "bsm_http";
    public static final String BSM_HTTPS = "bsm_https";
    public static final String CLOUDSTATION = "cloudstation";
    public static final String DSM = "dsm";
    public static final String DSM_HTTPS = "dsm_https";
    public static final String HTTPS = "https";
    public static final String PHOTO_HTTP = "photo_http";
    public static final String PHOTO_HTTPS = "photo_https";
    public static final String SECURESIGNIN = "secure_signin";
    public static final String WEBDAV_HTTP = "webdav_http";
    public static final String WEBDAV_HTTPS = "webdavs_https";
    private static final String WEBDAV_HTTPS_LEGACY = "webdav_https";

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    /* JADX WARN: Removed duplicated region for block: B:47:0x009f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static java.lang.String getProtocol(java.lang.String r2) {
        /*
            int r0 = r2.hashCode()
            java.lang.String r1 = "https"
            switch(r0) {
                case -1704112856: goto L95;
                case -1674662561: goto L8a;
                case -1024054486: goto L80;
                case -998820812: goto L76;
                case -948646718: goto L6b;
                case -814961990: goto L61;
                case -706114198: goto L57;
                case -507932043: goto L4c;
                case 99774: goto L41;
                case 99617003: goto L39;
                case 774852418: goto L2e;
                case 1191954603: goto L22;
                case 1433975966: goto L17;
                case 1549015889: goto Lb;
                default: goto L9;
            }
        L9:
            goto L9f
        Lb:
            java.lang.String r0 = "audio_http"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 9
            goto La0
        L17:
            java.lang.String r0 = "photo_https"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 2
            goto La0
        L22:
            java.lang.String r0 = "bsm_http"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 13
            goto La0
        L2e:
            java.lang.String r0 = "audio_https"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 1
            goto La0
        L39:
            boolean r2 = r2.equals(r1)
            if (r2 == 0) goto L9f
            r2 = 4
            goto La0
        L41:
            java.lang.String r0 = "dsm"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 11
            goto La0
        L4c:
            java.lang.String r0 = "photo_http"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 10
            goto La0
        L57:
            java.lang.String r0 = "secure_signin"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 5
            goto La0
        L61:
            java.lang.String r0 = "webdavs_https"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 0
            goto La0
        L6b:
            java.lang.String r0 = "webdav_http"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 8
            goto La0
        L76:
            java.lang.String r0 = "beedrive_https"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 6
            goto La0
        L80:
            java.lang.String r0 = "dsm_https"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 3
            goto La0
        L8a:
            java.lang.String r0 = "cloudstation"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 12
            goto La0
        L95:
            java.lang.String r0 = "bsm_https"
            boolean r2 = r2.equals(r0)
            if (r2 == 0) goto L9f
            r2 = 7
            goto La0
        L9f:
            r2 = -1
        La0:
            switch(r2) {
                case 0: goto La6;
                case 1: goto La6;
                case 2: goto La6;
                case 3: goto La6;
                case 4: goto La6;
                case 5: goto La6;
                case 6: goto La6;
                case 7: goto La6;
                default: goto La3;
            }
        La3:
            java.lang.String r2 = "http"
            return r2
        La6:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.synology.sylib.syhttp3.relay.ServiceId.getProtocol(java.lang.String):java.lang.String");
    }

    public static boolean isAllWithSameProtocol(String[] strArr) {
        if (strArr.length == 0) {
            return true;
        }
        String protocol = getProtocol(strArr[0]);
        for (String str : strArr) {
            if (!getProtocol(str).equals(protocol)) {
                return false;
            }
        }
        return true;
    }

    public static void serviceIdMigration(String[] strArr) {
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i].equals(WEBDAV_HTTPS_LEGACY)) {
                strArr[i] = WEBDAV_HTTPS;
            }
        }
    }
}