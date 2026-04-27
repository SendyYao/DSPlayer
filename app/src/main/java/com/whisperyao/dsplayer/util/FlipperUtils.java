package com.whisperyao.dsplayer.util;

import android.content.Context;

import com.synology.sylib.syhttp3.SyHttpClient;

import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.Intrinsics;

public final class FlipperUtils {
    public static final FlipperUtils INSTANCE = new FlipperUtils();

    @JvmStatic
    public static final void init(Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
    }

    public final void addDebugInterceptors(SyHttpClient syHttpClient) {
        Intrinsics.checkNotNullParameter(syHttpClient, "syHttpClient");
    }

    private FlipperUtils() {
    }
}
