package com.synology.sylib.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtils {

    public static boolean isLANAddress(String str) {
        if (str == null) {
            throw new IllegalArgumentException("address == null");
        }
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        try {
            InetAddress byName = InetAddress.getByName(str);
            if (!byName.isLinkLocalAddress()) {
                if (!byName.isSiteLocalAddress()) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isNetworkConnected(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        return isNetworkConnected((ConnectivityManager) context.getSystemService("connectivity"));
    }

    public static boolean isNetworkConnected(ConnectivityManager connectivityManager) {
        if (connectivityManager == null) {
            throw new IllegalArgumentException("connMgr == null");
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isLANConnected(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        return isLANConnected((ConnectivityManager) context.getSystemService("connectivity"));
    }

    public static boolean isWifiConnected(ConnectivityManager connectivityManager) {
        NetworkInfo activeNetworkInfo;
        return isNetworkConnected(connectivityManager) && (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) != null && 1 == activeNetworkInfo.getType();
    }

    public static boolean isLANConnected(ConnectivityManager connectivityManager) {
        if (isWifiConnected(connectivityManager)) {
            return true;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && 9 == activeNetworkInfo.getType();
    }

}
