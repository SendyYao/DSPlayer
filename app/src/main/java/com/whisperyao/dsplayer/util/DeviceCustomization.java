package com.whisperyao.dsplayer.util;

import com.whisperyao.dsplayer.Common;

public class DeviceCustomization {
    private static final String DEVICE_A0001 = "A0001";
    private static final String DEVICE_LG_G2 = "LG-D802";
    private static final String DEVICE_LG_G3 = "LG-D855";
    private static final String DEVICE_NEXUS_10 = "Nexus 10";
    private static final String DEVICE_NEXUS_5 = "Nexus 5";
    private static final String DEVICE_SAMSUNG_S6_EDGE = "SM-G9250";

    public static boolean supportRemoveVolume() {
        return true;
    }

    public static boolean supportAudioEffect() {
        return !Common.getDeviceName().equals(DEVICE_A0001);
    }

    public static boolean supportForceNext() {
        return Common.getDeviceName().equals(DEVICE_NEXUS_5);
    }
}
