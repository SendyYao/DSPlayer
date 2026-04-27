package com.whisperyao.dsplayer;

import android.media.MediaPlayer;
import android.media.MediaTimestamp;
import android.media.audiofx.Equalizer;
import android.os.Handler;
import android.util.Log;
import com.whisperyao.dsplayer.playing.EqualizerSettings;
import com.whisperyao.dsplayer.proxy.PreDownloader;
import com.whisperyao.dsplayer.proxy.StreamProxy;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.util.Utils;

import java.io.IOException;

public class AudioMediaPlayer extends MediaPlayer {
    private static final int EXTRA_MEDIA_ERROR_IO_BELOW_4 = -1005;
    private static final String LOG = "AudioMediaPlayer";
    private static final int WHAT_ENOSYS = -38;
    public Equalizer mEqualizer;
    private AudioMediaPlayer nextPlayer = null;
    private OnPreparedListener onPreparedListener = null;
    private OnBufferingUpdateListener onBufferingUpdateListener = null;
    private OnSeekCompleteListener onSeekCompleteListener = null;
    private OnCompletionListener onCompletionListener = null;
    private OnErrorListener onErrorListener = null;
    private OnClockRateChangedListener mClockRateChangedListener = null;
    private StreamProxy mStreamProxy = null;
    private PreDownloader mDownloader = null;
    private boolean directStreaming = false;
    private final Handler mHandler = new Handler();
    private final RadioCompletionHandler mRadioCompletionHandler = new RadioCompletionHandler();
    private final Runnable mReconnectRunnable = () -> {
        if (!AudioMediaPlayer.this.mRadioCompletionHandler.isPlaying()) {
            AudioMediaPlayer.this.tryReplay();
        } else {
            SynoLog.e(AudioMediaPlayer.LOG, "The AudioMediaPlayer is playing. ignore the re-connect event");
        }
    };

    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(AudioMediaPlayer mplayer, int percent);
    }

    public interface OnClockRateChangedListener {
        void onRateChanged(float newRate);
    }

    public interface OnCompletionListener {
        void onCompletion(AudioMediaPlayer mplayer);
    }

    public interface OnErrorListener {
        boolean onError(AudioMediaPlayer mplayer, int what, int extra);
    }

    public interface OnPreparedListener {
        void onPrepared(AudioMediaPlayer mplayer);
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete(AudioMediaPlayer mplayer);
    }

    private static class RadioCompletionHandler {
        private static final int REPLAYING_RADIO_COUNT = 5;
        private int mCountReplayingRadio;
        private boolean mIsPlaying;
        private boolean mIsPlayingRadio;
        private String mPath;

        private RadioCompletionHandler() {
            this.mIsPlayingRadio = false;
            this.mIsPlaying = false;
        }

        public void notifyStart(boolean isPlayingRadio) {
            this.mIsPlayingRadio = isPlayingRadio;
            this.mCountReplayingRadio = 0;
            this.mIsPlaying = true;
        }

        public void notifyEnd() {
            this.mIsPlaying = false;
        }

        public boolean isPlaying() {
            return this.mIsPlaying;
        }

        private boolean isPlayingRadio() {
            return this.mIsPlayingRadio;
        }

        public void notifyReplay() {
            this.mCountReplayingRadio++;
        }

        public boolean isNeedToTryReplayingRadio() {
            return isPlayingRadio() && this.mCountReplayingRadio < 5;
        }

        public void setPath(String path) {
            this.mPath = path;
        }

        public String getPath() {
            return this.mPath;
        }
    }

    public AudioMediaPlayer() {
        setAudioStreamType(3);
        setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                AudioMediaPlayer.this.mRadioCompletionHandler.notifyStart(mp.getDuration() == 0);
                SynoLog.d(AudioMediaPlayer.LOG, this + " onPrepared");
                if (AudioMediaPlayer.this.onPreparedListener != null) {
                    AudioMediaPlayer.this.onPreparedListener.onPrepared(AudioMediaPlayer.this);
                }
            }
        });
        setOnBufferingUpdateListener((MediaPlayer.OnBufferingUpdateListener) (mp, percent) -> {
            if (AudioMediaPlayer.this.onBufferingUpdateListener != null) {
                AudioMediaPlayer.this.onBufferingUpdateListener.onBufferingUpdate(AudioMediaPlayer.this, percent);
            }
        });
        setOnSeekCompleteListener((MediaPlayer.OnSeekCompleteListener) mp -> {
            if (AudioMediaPlayer.this.onSeekCompleteListener != null) {
                AudioMediaPlayer.this.onSeekCompleteListener.onSeekComplete(AudioMediaPlayer.this);
            }
        });
        setOnCompletionListener((MediaPlayer.OnCompletionListener) mp -> {
            SynoLog.d(AudioMediaPlayer.LOG, "onComplete " + mp);
            AudioMediaPlayer.this.mRadioCompletionHandler.notifyEnd();
            if (AudioMediaPlayer.this.mRadioCompletionHandler.isNeedToTryReplayingRadio()) {
                AudioMediaPlayer.this.doInternalOnCompletionForRadio();
            } else {
                try {
                    AudioMediaPlayer.this.doInternalOnCompletion();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });
        setOnErrorListener((MediaPlayer.OnErrorListener) (mp, what, extra) -> {
            SynoLog.e(AudioMediaPlayer.LOG, "onError " + mp + ", what: " + what + ",  extra: " + extra);
            if (AudioMediaPlayer.this.mRadioCompletionHandler.isPlayingRadio()) {
                if (AudioMediaPlayer.this.mRadioCompletionHandler.isPlaying()) {
                    return AudioMediaPlayer.this.doInternalOnError(what, extra);
                }
                if (what == AudioMediaPlayer.WHAT_ENOSYS) {
                    return true;
                }
                if (extra == -1004 || extra == AudioMediaPlayer.EXTRA_MEDIA_ERROR_IO_BELOW_4) {
                    return false;
                }
                return AudioMediaPlayer.this.doInternalOnError(what, extra);
            }
            return AudioMediaPlayer.this.doInternalOnError(what, extra);
        });
        if (EqualizerSettings.getInstance().enableEqualizer()) {
            try {
                this.mEqualizer = new Equalizer(0, getAudioSessionId());
                updateEqualizer();
            } catch (Exception e) {
                Log.e(LOG, " Equalizer failed. " + e);
            }
        }
        if (Utils.isSdk28()) {
            setOnMediaTimeDiscontinuityListener(new MediaPlayer.OnMediaTimeDiscontinuityListener() {
                private float clock = 0.0f;

                @Override
                public void onMediaTimeDiscontinuity(MediaPlayer mediaPlayer, MediaTimestamp mediaTimestamp) {
                    float mediaClockRate = mediaTimestamp.getMediaClockRate();
                    if (this.clock != mediaClockRate) {
                        this.clock = mediaClockRate;
                        if (AudioMediaPlayer.this.mClockRateChangedListener != null) {
                            AudioMediaPlayer.this.mClockRateChangedListener.onRateChanged(this.clock);
                        }
                    }
                }
            });
        }
    }

    public void updateEqualizer() {
        try {
            if (this.mEqualizer == null) {
                this.mEqualizer = new Equalizer(0, getAudioSessionId());
            }
            EqualizerSettings.getInstance().updateEqualizer(this.mEqualizer);
        } catch (Exception e) {
            Log.e(LOG, " Equalizer failed. " + e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void doInternalOnCompletionForRadio() {
        this.mHandler.removeCallbacks(this.mReconnectRunnable);
        this.mHandler.postDelayed(this.mReconnectRunnable, 10000L);
    }

    private void doInternalOnCompletion() throws Throwable {
        SynoLog.d(LOG, this + " onCompletion");
        if ((this.mRadioCompletionHandler.isPlayingRadio() || !Utilities.supportGapless()) && hasNextPlayer()) {
            this.nextPlayer.start();
        }
        OnCompletionListener onCompletionListener = this.onCompletionListener;
        if (onCompletionListener != null) {
            onCompletionListener.onCompletion(this);
        }
    }

    private boolean doInternalOnError(int what, int extra) {
        SynoLog.e(LOG, this + " doInternalOnError, what = " + what + ", extra = " + extra);
        PreDownloader preDownloader = this.mDownloader;
        if (preDownloader != null) {
            preDownloader.setError();
        }
        OnErrorListener onErrorListener = this.onErrorListener;
        if (onErrorListener != null) {
            return onErrorListener.onError(this, what, extra);
        }
        return false;
    }

    private void tryReplay() {
        this.mRadioCompletionHandler.notifyReplay();
        try {
            reset();
            recoverDataSource();
            prepareAsync();
        } catch (IOException | IllegalArgumentException | IllegalStateException |
                 SecurityException e) {
            e.printStackTrace();
        }
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        this.onBufferingUpdateListener = listener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        this.onSeekCompleteListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.onErrorListener = listener;
    }

    @Override
    public void setDataSource(String path) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        super.setDataSource(path);
        this.mRadioCompletionHandler.setPath(path);
    }

    private void recoverDataSource() throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        super.setDataSource(this.mRadioCompletionHandler.getPath());
    }

    public void setNextPlayer(AudioMediaPlayer next) {
        if (next != null || hasNextPlayer()) {
            this.nextPlayer = next;
            if (Utilities.supportGapless()) {
                try {
                    if (this.mRadioCompletionHandler.isPlayingRadio()) {
                        return;
                    }
                    setNextMediaPlayer(next);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    this.nextPlayer = null;
                }
            }
        }
    }

    public boolean hasNextPlayer() {
        return this.nextPlayer != null;
    }

    public void setProxy(StreamProxy proxy) {
        StreamProxy streamProxy = this.mStreamProxy;
        if (streamProxy != null) {
            streamProxy.stop();
        }
        this.mStreamProxy = proxy;
    }

    public void setClockRateChangedListener(OnClockRateChangedListener listener) {
        this.mClockRateChangedListener = listener;
    }

    public void setDownloader(PreDownloader downloader) {
        PreDownloader preDownloader = this.mDownloader;
        if (preDownloader != null) {
            preDownloader.stop();
        }
        this.mDownloader = downloader;
    }

    public boolean isDownloading() {
        if (this.mDownloader == null) {
            return false;
        }
        return !mDownloader.isCompleted();
    }

    @Override
    public void release() {
        StreamProxy streamProxy = this.mStreamProxy;
        if (streamProxy != null) {
            streamProxy.stop();
        }
        PreDownloader preDownloader = this.mDownloader;
        if (preDownloader != null) {
            preDownloader.stop();
        }
        Equalizer equalizer = this.mEqualizer;
        if (equalizer != null) {
            equalizer.release();
        }
        super.release();
    }

    public void setStreaming(boolean streaming) {
        this.directStreaming = streaming;
    }

    public boolean isStreaming() {
        StreamProxy streamProxy = this.mStreamProxy;
        if (streamProxy == null || !streamProxy.isStreaming()) {
            return this.directStreaming;
        }
        return true;
    }
}
