package com.whisperyao.dsplayer.util;

import android.util.Log;

/* loaded from: classes2.dex */
public class SynoLog {
    private static final boolean DEBUG_DS_AUDIO = false;
    private static final String LOG_TAG = "Synology";

    public static void d(String self, String msg) {
        Log.d(LOG_TAG, "[" + self + "(" + getLineNumber() + ")] " + msg);
    }

    public static void i(String self, String msg) {
        Log.i(LOG_TAG, "[" + self + "(" + getLineNumber() + ")] " + msg);
    }

    private static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[4].getLineNumber();
    }

    public static void w(String self, String msg) {
        Log.w(LOG_TAG, "[" + self + "(" + getLineNumber() + ")] " + msg);
    }

    public static void e(String self, String msg) {
        Log.e(LOG_TAG, "[" + self + "(" + getLineNumber() + ")] " + msg);
    }

    public static void e(String self, String msg, Throwable tr) {
        Log.e(LOG_TAG, "[" + self + "(" + getLineNumber() + ")] " + msg, tr);
    }
}