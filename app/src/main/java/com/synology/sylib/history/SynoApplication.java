package com.synology.sylib.history;

/* loaded from: classes.dex */
public class SynoApplication {
    public static final String DSAUDIO = "com.synology.dsaudio";
    public static final String DSCAM = "com.synology.dscam";
    public static final String DSCLOUD = "com.synology.dscloud";
    public static final String DSDOWNLOAD = "com.synology.dsdownload";
    public static final String DSDRIVE = "com.synology.dsdrive";
    public static final String DSFILE = "com.synology.dsfile";
    public static final String DSFINDER = "com.synology.dsfinder";
    public static final String DSMAIL = "com.synology.dsmail";
    public static final String DSMOMENT = "com.synology.projectdali";
    public static final String DSMOMENT_PACKAGE_NAME = "com.synology.moments";
    public static final String DSNOTE = "com.synology.dsnote";
    public static final String DSPHOTO = "com.synology.dsphoto";
    public static final String DSROUTER = "com.synology.dsrouter";
    public static final String DSVIDEO = "com.synology.dsvideo";
    public static final String KEY_APP_NAME = "com.synology.app.name";

    public static boolean needLatestFullInfo(String str) {
        if (str == null) {
            return false;
        }
        return str.equals(DSMOMENT) || str.equals(DSDRIVE);
    }

    public static String getHistoryAppName(String str) {
        if (str == null) {
            return "";
        }
        if (str.equalsIgnoreCase(DSMOMENT_PACKAGE_NAME)) {
            return DSMOMENT;
        }
        return str.toLowerCase();
    }
}