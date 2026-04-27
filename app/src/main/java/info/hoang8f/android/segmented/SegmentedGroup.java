package info.hoang8f.android.segmented;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import com.whisperyao.dsplayer.R;


public class SegmentedGroup extends RadioGroup {
    private int mCheckedTextColor;
    private Float mCornerRadius;
    private LayoutSelector mLayoutSelector;
    private int mMarginDp;
    private int mTintColor;
    private Resources resources;

    public SegmentedGroup(Context context) {
        super(context);
        this.mCheckedTextColor = -1;
        Resources resources = getResources();
        this.resources = resources;
        this.mTintColor = resources.getColor(R.color.radio_button_selected_color);
        this.mMarginDp = (int) getResources().getDimension(R.dimen.radio_button_stroke_border);
        this.mCornerRadius = Float.valueOf(getResources().getDimension(R.dimen.radio_button_conner_radius));
        this.mLayoutSelector = new LayoutSelector(this.mCornerRadius.floatValue());
    }

    private void initAttrs(AttributeSet attributeSet) {
        TypedArray typedArrayObtainStyledAttributes = getContext().getTheme().obtainStyledAttributes(attributeSet, new int[]{com.whisperyao.dsplayer.R.attr.sc_border_width, com.whisperyao.dsplayer.R.attr.sc_checked_text_color, com.whisperyao.dsplayer.R.attr.sc_corner_radius, com.whisperyao.dsplayer.R.attr.sc_tint_color}, 0, 0);
        try {
            this.mMarginDp = (int) typedArrayObtainStyledAttributes.getDimension(0x00000000, getResources().getDimension(R.dimen.radio_button_stroke_border));
            this.mCornerRadius = Float.valueOf(typedArrayObtainStyledAttributes.getDimension(0x00000002, getResources().getDimension(R.dimen.radio_button_conner_radius)));
            this.mTintColor = typedArrayObtainStyledAttributes.getColor(0x00000003, getResources().getColor(R.color.radio_button_selected_color));
            this.mCheckedTextColor = typedArrayObtainStyledAttributes.getColor(0x00000001, getResources().getColor(android.R.color.white));
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    public SegmentedGroup(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCheckedTextColor = -1;
        Resources resources = getResources();
        this.resources = resources;
        this.mTintColor = resources.getColor(R.color.radio_button_selected_color);
        this.mMarginDp = (int) getResources().getDimension(R.dimen.radio_button_stroke_border);
        this.mCornerRadius = Float.valueOf(getResources().getDimension(R.dimen.radio_button_conner_radius));
        initAttrs(attributeSet);
        this.mLayoutSelector = new LayoutSelector(this.mCornerRadius.floatValue());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        updateBackground();
    }

    public void setTintColor(int i) {
        this.mTintColor = i;
        updateBackground();
    }

    public void setTintColor(int i, int i2) {
        this.mTintColor = i;
        this.mCheckedTextColor = i2;
        updateBackground();
    }

    public void updateBackground() {
        int childCount = super.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            updateBackground(childAt);
            if (i == childCount - 1) {
                return;
            }
            RadioGroup.LayoutParams layoutParams = (RadioGroup.LayoutParams) childAt.getLayoutParams();
            RadioGroup.LayoutParams layoutParams2 = new RadioGroup.LayoutParams(layoutParams.width, layoutParams.height, layoutParams.weight);
            if (getOrientation() == 0) {
                layoutParams2.setMargins(0, 0, -this.mMarginDp, 0);
            } else {
                layoutParams2.setMargins(0, 0, 0, -this.mMarginDp);
            }
            childAt.setLayoutParams(layoutParams2);
        }
    }

    private void updateBackground(View view) {
        int selected = this.mLayoutSelector.getSelected();
        int unselected = this.mLayoutSelector.getUnselected();
        ((Button) view).setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_pressed}, new int[]{-16842919, -16842912}, new int[]{-16842919, android.R.attr.state_checked}}, new int[]{-7829368, this.mTintColor, this.mCheckedTextColor}));
        Drawable drawableMutate = this.resources.getDrawable(selected).mutate();
        Drawable drawableMutate2 = this.resources.getDrawable(unselected).mutate();
        GradientDrawable gradientDrawable = (GradientDrawable) drawableMutate;
        gradientDrawable.setColor(this.mTintColor);
        gradientDrawable.setStroke(this.mMarginDp, this.mTintColor);
        GradientDrawable gradientDrawable2 = (GradientDrawable) drawableMutate2;
        gradientDrawable2.setStroke(this.mMarginDp, this.mTintColor);
        gradientDrawable.setCornerRadii(this.mLayoutSelector.getChildRadii(view));
        gradientDrawable2.setCornerRadii(this.mLayoutSelector.getChildRadii(view));
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{-16842912}, drawableMutate2);
        stateListDrawable.addState(new int[]{android.R.attr.state_checked}, drawableMutate);
        view.setBackground(stateListDrawable);
    }

    private class LayoutSelector {
        private final int SELECTED_LAYOUT = R.drawable.radio_checked;
        private final int UNSELECTED_LAYOUT = R.drawable.radio_unchecked;
        private int child;
        private int children;
        private float r;
        private final float r1;
        private final float[] rBot;
        private final float[] rDefault;
        private final float[] rLeft;
        private final float[] rMiddle;
        private final float[] rRight;
        private final float[] rTop;
        private float[] radii;

        public LayoutSelector(float f) {
            float fApplyDimension = TypedValue.applyDimension(1, 0.1f, SegmentedGroup.this.getResources().getDisplayMetrics());
            this.r1 = fApplyDimension;
            this.children = -1;
            this.child = -1;
            this.r = f;
            this.rLeft = new float[]{f, f, fApplyDimension, fApplyDimension, fApplyDimension, fApplyDimension, f, f};
            this.rRight = new float[]{fApplyDimension, fApplyDimension, f, f, f, f, fApplyDimension, fApplyDimension};
            this.rMiddle = new float[]{fApplyDimension, fApplyDimension, fApplyDimension, fApplyDimension, fApplyDimension, fApplyDimension, fApplyDimension, fApplyDimension};
            this.rDefault = new float[]{f, f, f, f, f, f, f, f};
            this.rTop = new float[]{f, f, f, f, fApplyDimension, fApplyDimension, fApplyDimension, fApplyDimension};
            this.rBot = new float[]{fApplyDimension, fApplyDimension, fApplyDimension, fApplyDimension, f, f, f, f};
        }

        private int getChildren() {
            return SegmentedGroup.this.getChildCount();
        }

        private int getChildIndex(View view) {
            return SegmentedGroup.this.indexOfChild(view);
        }

        private void setChildRadii(int i, int i2) {
            if (this.children == i && this.child == i2) {
                return;
            }
            this.children = i;
            this.child = i2;
            if (i == 1) {
                this.radii = this.rDefault;
                return;
            }
            if (i2 == 0) {
                this.radii = SegmentedGroup.this.getOrientation() == 0 ? this.rLeft : this.rTop;
            } else if (i2 == i - 1) {
                this.radii = SegmentedGroup.this.getOrientation() == 0 ? this.rRight : this.rBot;
            } else {
                this.radii = this.rMiddle;
            }
        }

        public int getSelected() {
            return this.SELECTED_LAYOUT;
        }

        public int getUnselected() {
            return this.UNSELECTED_LAYOUT;
        }

        public float[] getChildRadii(View view) {
            setChildRadii(getChildren(), getChildIndex(view));
            return this.radii;
        }
    }
}