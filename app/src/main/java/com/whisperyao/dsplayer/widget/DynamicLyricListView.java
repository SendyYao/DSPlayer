package com.whisperyao.dsplayer.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.util.SynoLog;

import java.util.ArrayList;

public class DynamicLyricListView extends ListView {
    private static final int TIME_SHIFT_REMOTE_MODE = 1000;
    private static final int UNLOCK_SCROLL = 1;
    private static final long UNLOCK_SCROLL_DELAY = 3000;
    private Handler ScrollStateHandler;
    private boolean isLockScroll;
    private DynamicLyricListAdapter mLyricAdapter;
    private int mSelPos;
    private ArrayList<Long> mTimeList;

    public DynamicLyricListView(Context context) {
        super(context);
        this.mLyricAdapter = null;
        this.mTimeList = null;
        this.mSelPos = 0;
        this.ScrollStateHandler = null;
        this.isLockScroll = false;
        init();
    }

    public DynamicLyricListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLyricAdapter = null;
        this.mTimeList = null;
        this.mSelPos = 0;
        this.ScrollStateHandler = null;
        this.isLockScroll = false;
        init();
    }

    public DynamicLyricListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLyricAdapter = null;
        this.mTimeList = null;
        this.mSelPos = 0;
        this.ScrollStateHandler = null;
        this.isLockScroll = false;
        init();
    }

    private void init() {
        setDivider(null);
        setCacheColorHint(0);
        setVerticalFadingEdgeEnabled(false);
        try {
            setOverScrollMode(2);
        } catch (NoSuchMethodError ignored) {
        }
        this.ScrollStateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    return;
                }
                DynamicLyricListView.this.isLockScroll = false;
            }
        };
        setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState != 0) {
                    if (scrollState != 1) {
                        return;
                    }
                    DynamicLyricListView.this.ScrollStateHandler.removeMessages(1);
                    DynamicLyricListView.this.isLockScroll = true;
                    return;
                }
                Message messageObtainMessage = DynamicLyricListView.this.ScrollStateHandler.obtainMessage(1);
                DynamicLyricListView.this.ScrollStateHandler.removeMessages(1);
                DynamicLyricListView.this.ScrollStateHandler.sendMessageDelayed(messageObtainMessage, DynamicLyricListView.UNLOCK_SCROLL_DELAY);
                DynamicLyricListView.this.requestFocus();
            }
        });
    }

    public void setAdapter(BaseAdapter adapter) {
        SynoLog.d("DynamicLyricListView", "adapter: " + adapter);
        DynamicLyricListAdapter dynamicLyricListAdapter = (DynamicLyricListAdapter) adapter;
        this.mLyricAdapter = dynamicLyricListAdapter;
        if (dynamicLyricListAdapter != null) {
            this.mTimeList = dynamicLyricListAdapter.getTimeList();
        }
        this.mSelPos = 0;
        super.setAdapter(adapter);
    }

    public void setSelection(long time) {
        if (this.mTimeList == null || this.mLyricAdapter == null) {
            return;
        }
        long j = time + (Common.isPlayModeRenderer() ? 1000L : 0L);
        while (this.mSelPos < this.mTimeList.size() - 1 && this.mTimeList.get(this.mSelPos + 1) < j) {
            try {
                this.mSelPos++;
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        while (true) {
            int i = this.mSelPos;
            if (i < 0 || this.mTimeList.get(i) <= j) {
                break;
            } else {
                this.mSelPos--;
            }
        }
        this.mLyricAdapter.setSelectPosition(this.mSelPos);
        this.mLyricAdapter.notifyDataSetChanged();
        if (this.isLockScroll || !checkOutOfRange(this.mSelPos)) {
            return;
        }
        int childCount = (getChildCount() / 2) - 1;
        try {
            super.smoothScrollToPositionFromTop(this.mSelPos + 1, getHeight() / 2);
        } catch (NoSuchMethodError unused2) {
            int i2 = this.mSelPos - childCount;
            if (i2 <= 0) {
                i2 = 0;
            }
            setSelection(i2);
        }
        requestFocus();
    }

    private boolean checkOutOfRange(int currentPos) {
        int firstVisiblePosition = getFirstVisiblePosition();
        if (currentPos < firstVisiblePosition) {
            return true;
        }
        int height = getHeight();
        int height2 = 0;
        for (int i = 0; i < getChildCount(); i++) {
            TextView textView = (TextView) getChildAt(i);
            float f = height2 / height;
            firstVisiblePosition++;
            if (firstVisiblePosition == currentPos && f < 0.3d) {
                return false;
            }
            height2 += textView.getHeight();
        }
        return true;
    }
}
