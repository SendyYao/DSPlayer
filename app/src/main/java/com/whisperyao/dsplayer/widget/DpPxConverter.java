package com.whisperyao.dsplayer.widget;

import android.content.Context;

public class DpPxConverter {
    public static float dp2px(float dp, Context context) {
        return dp * getDensity(context);
    }

    public static float px2dp(float px, Context context) {
        return px / getDensity(context);
    }

    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }
}