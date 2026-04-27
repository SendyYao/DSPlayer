package com.whisperyao.dsplayer.widget;

import android.os.CountDownTimer;

public class SleepTimer extends CountDownTimer {

    private static final int MILLIS_IN_ONE_SECOND = 1000;

    private long mOriginalSecondsInFuture;
    private long mRestSecondsInFuture;
    private SleepTimerCallback mSleepTimerCallback;

    public interface SleepTimerCallback {
        void onFinish();

        void onTick();
    }

    public SleepTimer(long secondInFuture, SleepTimerCallback callbacks) {
        super(secondInFuture * MILLIS_IN_ONE_SECOND, MILLIS_IN_ONE_SECOND);
        this.mOriginalSecondsInFuture = secondInFuture;
        this.mRestSecondsInFuture = secondInFuture;
        this.mSleepTimerCallback = callbacks;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        if (mSleepTimerCallback != null) {
            mSleepTimerCallback.onTick();
        }
        mRestSecondsInFuture--;
    }

    @Override
    public void onFinish() {
        mRestSecondsInFuture = 0L;

        if (mSleepTimerCallback != null) {
            mSleepTimerCallback.onFinish();
        }
    }

    public long getOriginalSeconds() {
        return mOriginalSecondsInFuture;
    }

    public long getRestSeconds() {
        return mRestSecondsInFuture;
    }
}