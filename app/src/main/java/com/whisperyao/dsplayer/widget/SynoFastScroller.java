package com.whisperyao.dsplayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import eu.davidea.fastscroller.FastScroller;

/* loaded from: classes2.dex */
public class SynoFastScroller extends FastScroller {
    public SynoFastScroller(Context context) {
        super(context);
    }

    public SynoFastScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SynoFastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override // eu.davidea.fastscroller.FastScroller, android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        if (this.bar.isShown()) {
            return super.onTouchEvent(event);
        }
        return false;
    }
}