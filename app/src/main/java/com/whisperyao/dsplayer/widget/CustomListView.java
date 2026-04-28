package com.whisperyao.dsplayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.AbsListView;
import android.widget.ListView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CustomListView extends ListView {
    private static final int DEFAULT_INT = -1;
    private static final String TAG = "CustomListView";
    private static Method sOnInterceptTouchEventMethod;
    private static Method sReportScrollStateChangeMethod;
    private static Method sSetStateMethod;
    private int STATE_VISIBLE;
    private Object mFastScroller;
    private int mState;
    private Field stateField;

    public CustomListView(Context context, AttributeSet attrs) throws SecurityException, IllegalArgumentException {
        super(context, attrs);
        this.mState = -1;
        this.stateField = null;
        this.STATE_VISIBLE = -1;
        try {
            Field declaredField = AbsListView.class.getDeclaredField("mFastScroll");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(this);
            this.mFastScroller = obj;
            Field declaredField2 = obj.getClass().getDeclaredField("STATE_VISIBLE");
            declaredField2.setAccessible(true);
            this.STATE_VISIBLE = declaredField2.getInt(this.mFastScroller);
            Field declaredField3 = this.mFastScroller.getClass().getDeclaredField("mState");
            this.stateField = declaredField3;
            declaredField3.setAccessible(true);
            this.mState = this.stateField.getInt(this.mFastScroller);
            if (sOnInterceptTouchEventMethod == null) {
                sOnInterceptTouchEventMethod = this.mFastScroller.getClass().getMethod("onInterceptTouchEvent", MotionEvent.class);
            }
            if (sSetStateMethod == null) {
                Method declaredMethod = this.mFastScroller.getClass().getDeclaredMethod("setState", Integer.TYPE);
                sSetStateMethod = declaredMethod;
                declaredMethod.setAccessible(true);
            }
            if (sReportScrollStateChangeMethod == null) {
                Method declaredMethod2 = AbsListView.class.getDeclaredMethod("reportScrollStateChange", Integer.TYPE);
                sReportScrollStateChangeMethod = declaredMethod2;
                declaredMethod2.setAccessible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // android.widget.AbsListView, android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent ev) throws IllegalArgumentException {
        if (ev.getAction() == 0) {
            try {
                Field field = this.stateField;
                if (field != null) {
                    this.mState = field.getInt(this.mFastScroller);
                }
                Log.d(TAG, " onInterceptTouchEvent ACTION_DOWN,  mState =  " + this.mState);
            } catch (Exception e) {
                e.printStackTrace();
            }
            boolean zOnInterceptTouchEvent = super.onInterceptTouchEvent(ev);
            int i = this.mState;
            if (i != -1 && i != this.STATE_VISIBLE) {
                try {
                    if (!zOnInterceptTouchEvent) {
                        if (sOnInterceptTouchEventMethod != null) {
                            MotionEvent motionEventObtain = MotionEvent.obtain(0L, 0L, 3, 0.0f, 0.0f, 0);
                            sOnInterceptTouchEventMethod.invoke(this.mFastScroller, motionEventObtain);
                            motionEventObtain.recycle();
                        }
                    } else if (sReportScrollStateChangeMethod != null && sSetStateMethod != null) {
                        requestDisallowInterceptTouchEvent(false);
                        sReportScrollStateChangeMethod.invoke(this, 0);
                        sSetStateMethod.invoke(this.mFastScroller, 0);
                        return false;
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            return zOnInterceptTouchEvent;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
