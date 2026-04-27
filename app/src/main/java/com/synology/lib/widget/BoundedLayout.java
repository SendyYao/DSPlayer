package com.synology.lib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.whisperyao.dsplayer.R;


public class BoundedLayout extends FrameLayout {
    private final int mMaxHeight;
    private final int mMaxWidth;

    public BoundedLayout(Context context) {
        this(context, null);
    }

    public BoundedLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoundedLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attrs, new int[]{R.attr.maxHeight});
        this.mMaxWidth = typedArrayObtainStyledAttributes.getLayoutDimension(1, 0);
        this.mMaxHeight = typedArrayObtainStyledAttributes.getLayoutDimension(0, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = View.MeasureSpec.getSize(widthMeasureSpec);
        int i = this.mMaxWidth;
        if (i > 0 && i < size) {
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mMaxWidth, View.MeasureSpec.getMode(widthMeasureSpec));
        }
        int size2 = View.MeasureSpec.getSize(heightMeasureSpec);
        int i2 = this.mMaxHeight;
        if (i2 > 0 && i2 < size2) {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mMaxHeight, View.MeasureSpec.getMode(heightMeasureSpec));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
