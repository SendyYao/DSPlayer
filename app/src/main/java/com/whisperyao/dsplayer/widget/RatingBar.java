package com.whisperyao.dsplayer.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import com.whisperyao.dsplayer.R;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes2.dex */
public class RatingBar extends LinearLayout {
    private static int LEVEL_PER_STAR = 10;
    private float mCurrentStars;
    private boolean mIsIndicator;
    private int mMaxStars;
    private OnRatingChangeListener mOnRatingChangeListener;
    private Space mStarNoRating;
    private int mStarResId;
    private List<ImageView> mStarViewList;
    private Space mSymmestryView;

    public interface OnRatingChangeListener {
        void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser);
    }

    private class OnClickStarListener implements View.OnClickListener {
        private int mScore;

        OnClickStarListener(int score) {
            this.mScore = score;
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            if (RatingBar.this.mIsIndicator) {
                return;
            }
            RatingBar.this.performClickStar(this.mScore);
        }
    }

    public RatingBar(Context context) {
        super(context);
        this.mMaxStars = 5;
        this.mStarViewList = new ArrayList();
        this.mCurrentStars = 0.0f;
        init(context, null);
    }

    public RatingBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMaxStars = 5;
        this.mStarViewList = new ArrayList();
        this.mCurrentStars = 0.0f;
        init(context, attrs);
    }

    public RatingBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mMaxStars = 5;
        this.mStarViewList = new ArrayList();
        this.mCurrentStars = 0.0f;
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(attrs, new int[]{com.whisperyao.dsplayer.R.attr.isIndicator, com.whisperyao.dsplayer.R.attr.star}, 0, 0);
        try {
            this.mStarResId = typedArrayObtainStyledAttributes.getResourceId(1, R.drawable.level_star_l);
            this.mIsIndicator = typedArrayObtainStyledAttributes.getBoolean(0, false);
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    @Override // android.view.View
    protected void onFinishInflate() throws Resources.NotFoundException {
        super.onFinishInflate();
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.star_l_width);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(dimensionPixelSize, dimensionPixelSize);
        Space space = new Space(getContext());
        this.mStarNoRating = space;
        space.setLayoutParams(layoutParams);
        this.mStarNoRating.setOnClickListener(new OnClickStarListener(0));
        addView(this.mStarNoRating);
        for (int i = 1; i <= this.mMaxStars; i++) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageResource(this.mStarResId);
            imageView.setOnClickListener(new OnClickStarListener(i));
            this.mStarViewList.add(imageView);
            imageView.setPadding(0, imageView.getHeight(), 0, imageView.getHeight());
            addView(imageView);
        }
        Space space2 = new Space(getContext());
        this.mSymmestryView = space2;
        space2.setLayoutParams(layoutParams);
        addView(this.mSymmestryView);
        setupIndicator();
    }

    public void setOnRatingChangeListener(OnRatingChangeListener listener) {
        this.mOnRatingChangeListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performClickStar(int whichStar) {
        setRating(whichStar, true);
    }

    private void notifyRatingChanged(float rating, boolean fromUser) {
        OnRatingChangeListener onRatingChangeListener = this.mOnRatingChangeListener;
        if (onRatingChangeListener != null) {
            onRatingChangeListener.onRatingChanged(this, rating, fromUser);
        }
    }

    public void setRating(float rating) {
        if (this.mCurrentStars != rating) {
            setRating(rating, false);
        }
    }

    public void setRating(float rating, boolean fromUser) {
        if (this.mCurrentStars != rating) {
            this.mCurrentStars = rating;
            changeRating(rating);
            notifyRatingChanged(rating, fromUser);
        }
    }

    private void changeRating(float rating) {
        int i;
        int i2 = (int) (rating * 10.0f);
        int size = this.mStarViewList.size();
        int i3 = 0;
        for (int i4 = 0; i4 < size; i4++) {
            int i5 = i2 - i3;
            if (i5 <= 0) {
                i = 0;
            } else {
                i = i5 >= LEVEL_PER_STAR ? 10 : 5;
            }
            this.mStarViewList.get(i4).setImageLevel(i);
            i3 += LEVEL_PER_STAR;
        }
    }

    public void setIsIndicator(boolean isIndicator) {
        this.mIsIndicator = isIndicator;
        setupIndicator();
    }

    private void setupIndicator() {
        this.mStarNoRating.setVisibility(this.mIsIndicator ? 8 : 0);
        this.mSymmestryView.setVisibility(this.mIsIndicator ? 8 : 0);
    }
}