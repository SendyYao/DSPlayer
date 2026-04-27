package com.synology.sylib.security.util;

import android.util.Log;
import com.synology.sylib.security.internal.util.Util;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/* loaded from: classes.dex */
public class Logger {
    private static boolean sDevEnabled = false;

    private enum Level {
        V,
        D,
        I,
        W,
        E
    }

    public static void setDevEnabled(boolean z) {
        sDevEnabled = z;
    }

    public static void dev(String str, String str2) {
        dev(str, str2, null);
    }

    public static void dev(String str, String str2, Throwable th) {
        if (sDevEnabled) {
            logCat(Level.D, "DEV:" + str, str2, th);
        }
    }

    public static void v(String str, String str2) {
        v(str, str2, null);
    }

    public static void v(String str, String str2, Throwable th) {
        logCat(Level.V, str, str2, th);
    }

    public static void d(String str, String str2) {
        d(str, str2, null);
    }

    public static void d(String str, String str2, Throwable th) {
        logCat(Level.D, str, str2, th);
    }

    public static void i(String str, String str2) {
        i(str, str2, null);
    }

    public static void i(String str, String str2, Throwable th) {
        logCat(Level.I, str, str2, th);
    }

    public static void w(String str, String str2) {
        w(str, str2, null);
    }

    public static void w(String str, String str2, Throwable th) {
        logCat(Level.W, str, str2, th);
    }

    public static void e(String str, String str2) {
        e(str, str2, null);
    }

    public static void e(String str, String str2, Throwable th) {
        logCat(Level.E, str, str2, th);
    }

    private static void logCat(Level level, String str, String str2, Throwable th) {
        String str3 = String.format(Locale.ENGLISH, "[%s] %s", str, str2);
        if (th != null) {
            str3 = str3 + StringUtils.LF + Log.getStackTraceString(th);
        }
        int i = AnonymousClass1.$SwitchMap$com$synology$sylib$security$util$Logger$Level[level.ordinal()];
        if (i == 1) {
            Log.v(Util.TAG, str3);
            return;
        }
        if (i == 2) {
            Log.d(Util.TAG, str3);
            return;
        }
        if (i == 3) {
            Log.i(Util.TAG, str3);
        } else if (i == 4) {
            Log.w(Util.TAG, str3);
        } else {
            if (i != 5) {
                return;
            }
            Log.e(Util.TAG, str3);
        }
    }

    /* renamed from: com.synology.sylib.security.util.Logger$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$synology$sylib$security$util$Logger$Level;

        static {
            int[] iArr = new int[Level.values().length];
            $SwitchMap$com$synology$sylib$security$util$Logger$Level = iArr;
            try {
                iArr[Level.V.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$synology$sylib$security$util$Logger$Level[Level.D.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$synology$sylib$security$util$Logger$Level[Level.I.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$synology$sylib$security$util$Logger$Level[Level.W.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$com$synology$sylib$security$util$Logger$Level[Level.E.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
        }
    }
}