package com.whisperyao.dsplayer.adapters;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/* loaded from: classes.dex */
public class RecyclerDecoration extends RecyclerView.ItemDecoration {
    public static final int[] ATTRS = {R.attr.listDivider};
    private Drawable mDivider;
    private boolean mDrawDivider;
    private boolean mShowOffset;
    private int mStartDrawingPosition = 0;
    private int mSpanCount = 2;
    private int mSpacing = 8;
    private boolean mIncludeEdge = true;

    public RecyclerDecoration(Context context) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(ATTRS);
        this.mDivider = typedArrayObtainStyledAttributes.getDrawable(0);
        typedArrayObtainStyledAttributes.recycle();
    }

    public RecyclerDecoration(Drawable divider) {
        this.mDivider = divider;
    }

    public void setStartDrawingPosition(int p) {
        this.mStartDrawingPosition = p;
    }

    public void setSpan(int span) {
        this.mSpanCount = span;
    }

    public void setSpacing(int mSpacing) {
        this.mSpacing = mSpacing;
    }

    public void setIncludeEdge(boolean b) {
        this.mIncludeEdge = b;
    }

    public void showOffset(boolean b) {
        this.mShowOffset = b;
    }

    public void drawDivider(boolean b) {
        this.mDrawDivider = b;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.ItemDecoration
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        drawHorizontalLine(c, parent);
    }

    private void drawHorizontalLine(Canvas c, RecyclerView parent) {
        if (this.mDrawDivider) {
            int paddingLeft = parent.getPaddingLeft();
            int width = parent.getWidth() - parent.getPaddingRight();
            int childCount = parent.getChildCount();
            int i = 0;
            for (int i2 = 0; i2 <= this.mStartDrawingPosition && parent.findViewHolderForAdapterPosition(i2) != null; i2++) {
                i = i2;
            }
            while (i < childCount) {
                View childAt = parent.getChildAt(i);
                int bottom = childAt.getBottom() + ((RecyclerView.LayoutParams) childAt.getLayoutParams()).bottomMargin;
                this.mDivider.setBounds(paddingLeft, bottom, width, this.mDivider.getIntrinsicHeight() + bottom);
                this.mDivider.draw(c);
                i++;
            }
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.ItemDecoration
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (this.mShowOffset && (parent.getLayoutManager() instanceof GridLayoutManager)) {
            int childAdapterPosition = parent.getChildAdapterPosition(view);
            int i = this.mStartDrawingPosition;
            if (childAdapterPosition >= i) {
                int i2 = i == 0 ? childAdapterPosition + i : childAdapterPosition + (this.mSpanCount - i);
                int i3 = this.mSpanCount;
                int i4 = i2 % i3;
                if (this.mIncludeEdge) {
                    int i5 = this.mSpacing;
                    outRect.left = i5 - ((i4 * i5) / i3);
                    outRect.right = ((i4 + 1) * this.mSpacing) / this.mSpanCount;
                    if (i2 < this.mSpanCount) {
                        outRect.top = this.mSpacing;
                    }
                    outRect.bottom = this.mSpacing;
                    return;
                }
                outRect.left = (this.mSpacing * i4) / i3;
                int i6 = this.mSpacing;
                outRect.right = i6 - (((i4 + 1) * i6) / this.mSpanCount);
                if (i2 >= this.mSpanCount) {
                    outRect.top = this.mSpacing;
                    return;
                }
                return;
            }
            super.getItemOffsets(outRect, view, parent, state);
            return;
        }
        super.getItemOffsets(outRect, view, parent, state);
    }
}
