package com.synology.sylib.util;

import android.database.Cursor;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

/* loaded from: classes2.dex */
public class IOUtils {
    public static void closeSilently(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Exception unused) {
            }
        }
    }

    public static void closeSilently(Socket socket) throws IOException {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception unused) {
            }
        }
    }

    public static void closeSilently(Closeable closeable) throws IOException {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception unused) {
            }
        }
    }
}