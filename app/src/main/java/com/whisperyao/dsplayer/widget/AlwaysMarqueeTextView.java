package com.whisperyao.dsplayer.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;


public class AlwaysMarqueeTextView extends androidx.appcompat.widget.AppCompatTextView {
    @Override
    public boolean isFocused() {
        return true;
    }

    public AlwaysMarqueeTextView(Context context) {
        super(context);
    }

    public AlwaysMarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlwaysMarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) {
            super.onFocusChanged(true, direction, previouslyFocusedRect);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        if (focused) {
            super.onWindowFocusChanged(true);
        }
    }
}
