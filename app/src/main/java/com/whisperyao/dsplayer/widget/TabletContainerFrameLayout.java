package com.whisperyao.dsplayer.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class TabletContainerFrameLayout extends LinearLayout {
    public TabletContainerFrameLayout(Context context) {
        this(context, null);
    }

    public TabletContainerFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabletContainerFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        setupChildWeights(getResources().getConfiguration());
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupChildWeights(newConfig);
    }

    private void setupChildWeights(Configuration config) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) getChildAt(0).getLayoutParams();
        LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) getChildAt(1).getLayoutParams();
        if (config.orientation == 2) {
            layoutParams.weight = 2.0f;
            layoutParams2.weight = 4.0f;
        } else {
            layoutParams.weight = 1.0f;
            layoutParams2.weight = 1.0f;
        }
    }
}