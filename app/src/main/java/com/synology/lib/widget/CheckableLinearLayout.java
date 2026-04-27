package com.synology.lib.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import com.whisperyao.dsplayer.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private List<Integer> mCheckableIdList;
    private List<Checkable> mCheckableList;
    private boolean mChecked;
    private Drawable mDrawable;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeWidgetListener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(CheckableLinearLayout var1, boolean var2);
    }

    public CheckableLinearLayout(Context context) {
        this(context, null);
    }

    public CheckableLinearLayout(Context context, AttributeSet attrs) throws Resources.NotFoundException {
        super(context, attrs);
        this.mCheckableIdList = new ArrayList();
        this.mCheckableList = new ArrayList();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.background});
        Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(0);
        if (drawable != null) {
            setDrawable(drawable);
        }
        typedArrayObtainStyledAttributes.recycle();
        TypedArray typedArrayObtainStyledAttributes2 = context.getTheme().obtainStyledAttributes(attrs, new int[]{R.attr.related_checkable, R.attr.related_checkables}, 0, 0);
        if (typedArrayObtainStyledAttributes2.hasValue(0)) {
            this.mCheckableIdList.add(Integer.valueOf(typedArrayObtainStyledAttributes2.getResourceId(0, 0)));
        }
        if (typedArrayObtainStyledAttributes2.hasValue(1)) {
            TypedArray typedArrayObtainTypedArray = getResources().obtainTypedArray(typedArrayObtainStyledAttributes2.getResourceId(1, 0));
            for (int i = 0; i < typedArrayObtainTypedArray.length(); i++) {
                this.mCheckableIdList.add(Integer.valueOf(typedArrayObtainTypedArray.getResourceId(i, 0)));
            }
        }
        typedArrayObtainStyledAttributes2.recycle();
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mCheckableList.clear();
        Iterator<Integer> it = this.mCheckableIdList.iterator();
        while (it.hasNext()) {
            KeyEvent.Callback callbackFindViewById = findViewById(it.next().intValue());
            if (callbackFindViewById != null && (callbackFindViewById instanceof Checkable)) {
                this.mCheckableList.add((Checkable) callbackFindViewById);
            }
        }
    }

    @Override // android.view.View
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    @Override // android.widget.Checkable
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override // android.widget.Checkable
    public void toggle() {
        setChecked(!this.mChecked);
    }

    @Override // android.widget.Checkable
    public void setChecked(boolean checked) {
        if (this.mChecked != checked) {
            this.mChecked = checked;
            Iterator<Checkable> it = this.mCheckableList.iterator();
            while (it.hasNext()) {
                it.next().setChecked(checked);
            }
            refreshDrawableState();
            OnCheckedChangeListener onCheckedChangeListener = this.mOnCheckedChangeListener;
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(this, this.mChecked);
            }
            OnCheckedChangeListener onCheckedChangeListener2 = this.mOnCheckedChangeWidgetListener;
            if (onCheckedChangeListener2 != null) {
                onCheckedChangeListener2.onCheckedChanged(this, this.mChecked);
            }
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.mOnCheckedChangeListener = listener;
    }

    void setOnCheckedChangeWidgetListener(OnCheckedChangeListener listener) {
        this.mOnCheckedChangeWidgetListener = listener;
    }

    @Override // android.view.ViewGroup, android.view.View
    public int[] onCreateDrawableState(int extraSpace) {
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(iArrOnCreateDrawableState, CHECKED_STATE_SET);
        }
        return iArrOnCreateDrawableState;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mDrawable != null) {
            this.mDrawable.setState(getDrawableState());
            invalidate();
        }
    }

    public void setDrawable(Drawable d) {
        if (d != null) {
            Drawable drawable = this.mDrawable;
            if (drawable != null) {
                drawable.setCallback(null);
                unscheduleDrawable(this.mDrawable);
            }
            d.setCallback(this);
            d.setState(getDrawableState());
            d.setVisible(getVisibility() == 0, false);
            this.mDrawable = d;
            d.setState(null);
        }
        refreshDrawableState();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mDrawable;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.checked = isChecked();
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setChecked(savedState.checked);
        requestLayout();
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        boolean checked;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.checked = (Boolean) in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(this.checked);
        }

        public String toString() {
            return "CheckableLinearLayout.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " checked=" + this.checked + "}";
        }
    }
}