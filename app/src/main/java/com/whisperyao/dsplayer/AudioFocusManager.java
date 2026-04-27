package com.whisperyao.dsplayer;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import androidx.preference.PreferenceManager;
import com.synology.AudioFocusHelper;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.DeviceCustomization;
import com.whisperyao.dsplayer.util.SynoLog;
import java.io.IOException;

public class AudioFocusManager {
    private static final String LOG = "AudioFocusManager";
    private AudioFocusHelper mAudioFocusHelper;
    private Context mContext;
    private PlaybackProxy mPlaybackProxy;
    private IRemoteClientVolumeDetector mRemoteClientVolumeDetector;
    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    private boolean mResumeAfterInterrupt = false;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences, str) -> {
        if (str.equals(AudioPreference.PREFERENCE_ENABLE_REMOTE_CONTROLLER)) {
            resetVolumeDetector(sharedPreferences.getBoolean(AudioPreference.PREFERENCE_ENABLE_REMOTE_CONTROLLER, true));
        }
    };
    private final AudioFocusHelper.MusicFocusable mMusicFocusable = new AudioFocusHelper.MusicFocusable() {
        boolean ifDuck = false;

        @Override
        public void onGainedAudioFocus() {
            SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " onGainedAudioFocus ");
            AudioFocusManager.this.mAudioFocus = AudioFocus.Focused;
            if (this.ifDuck) {
                AudioFocusManager.this.mPlaybackProxy.toNormalVolume();
                this.ifDuck = false;
            }
            if (AudioFocusManager.this.mResumeAfterInterrupt) {
                SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " mResumeAfterInterrupt = true , resume play ");
                AudioFocusManager.this.mPlaybackProxy.play();
                AudioFocusManager.this.mResumeAfterInterrupt = false;
            }
        }

        @Override
        public void onLostAudioFocus() {
            SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " onLostAudioFocus ");
            AudioFocusManager.this.mAudioFocus = AudioFocus.NoFocusNoDuck;
            if (AudioFocusManager.this.mPlaybackProxy.isPlaying()) {
                AudioFocusManager.this.giveUpAudioFocus();
                if (AudioFocusManager.this.mPlaybackProxy.isRemotePlayer()) {
                    return;
                }
                AudioFocusManager.this.mPlaybackProxy.pause();
                AudioFocusManager audioFocusManager = AudioFocusManager.this;
                audioFocusManager.mResumeAfterInterrupt = audioFocusManager.mPlaybackProxy.hasAudioToPlay();
                SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " pause music, mResumeAfterInterrupt = " + AudioFocusManager.this.mResumeAfterInterrupt);
            }
        }

        @Override
        public void onLostAudioFocusCanDuck() {
            SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " onLostAudioFocusCanDuck ");
            AudioFocusManager.this.mAudioFocus = AudioFocus.NoFocusCanDuck;
            if (AudioFocusManager.this.mPlaybackProxy.isPlaying()) {
                AudioFocusManager.this.mPlaybackProxy.toDuckVolume();
                this.ifDuck = true;
            }
        }
    };

    private enum AudioFocus {
        NoFocusNoDuck,
        NoFocusCanDuck,
        Focused
    }

    private interface IRemoteClientVolumeDetector {
        void startDetect();

        void stopDetect();
    }

    public interface PlaybackProxy {
        int getRemoteVolume();

        boolean hasAudioToPlay();

        boolean isPlaying();

        boolean isPreparing();

        boolean isRemotePlayer();

        void pause();

        void play();

        void setRemoteVolume(int newVolume);

        void toDuckVolume();

        void toNormalVolume();
    }


    public void setup(Context context, PlaybackProxy proxy) {
        String str = LOG;
        SynoLog.d(str, str + " setup ");
        this.mContext = context;
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this.preferenceChangeListener);
        this.mAudioFocusHelper = new AudioFocusHelper(context.getApplicationContext(), this.mMusicFocusable);
        this.mPlaybackProxy = proxy;
        if (proxy.isRemotePlayer() && DeviceCustomization.supportRemoveVolume() && AudioPreference.enableRemoteController(context)) {
            SynoLog.d(str, str + " new RemoteClientVolumeDetector ");
            this.mRemoteClientVolumeDetector = new RemoteClientVolumeDetector(context);
        } else {
            SynoLog.d(str, str + " new NullRemoteClientVolumeDetector ");
            this.mRemoteClientVolumeDetector = new NullRemoteClientVolumeDetector();
        }
    }

    public void release() {
        giveUpAudioFocus();
        PreferenceManager.getDefaultSharedPreferences(this.mContext).unregisterOnSharedPreferenceChangeListener(this.preferenceChangeListener);
        this.mRemoteClientVolumeDetector.stopDetect();
    }

    public void resetVolumeDetector(boolean enable) {
        String str = LOG;
        SynoLog.d(str, str + " resetVolumeDetector");
        this.mRemoteClientVolumeDetector.stopDetect();
        if (this.mPlaybackProxy.isRemotePlayer() && DeviceCustomization.supportRemoveVolume() && enable) {
            SynoLog.d(str, str + " new RemoteClientVolumeDetector ");
            this.mRemoteClientVolumeDetector = new RemoteClientVolumeDetector(this.mContext);
        } else {
            SynoLog.d(str, str + " new NullRemoteClientVolumeDetector ");
            this.mRemoteClientVolumeDetector = new NullRemoteClientVolumeDetector();
        }
        this.mRemoteClientVolumeDetector.startDetect();
    }

    public boolean isResumeAfterInterrupt() {
        return this.mResumeAfterInterrupt;
    }

    private boolean isWithAudioFocus() {
        return this.mAudioFocus == AudioFocus.Focused;
    }

    private static class NullRemoteClientVolumeDetector implements IRemoteClientVolumeDetector {
        private NullRemoteClientVolumeDetector() {
        }

        @Override
        public void startDetect() {
            SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " NullRemoteClientVolumeDetector startDetect ");
        }

        @Override
        public void stopDetect() {
            SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " NullRemoteClientVolumeDetector stopDetect ");
        }
    }

    private class RemoteClientVolumeDetector implements IRemoteClientVolumeDetector {
        private static final int DELAY_SET_VOLUME_TIME = 500;
        public static final int MESSAGE_SET_VOLUME = 1;
        private AudioManager mAudioManager;
        private Context mContext;
        private MediaPlayer mMediaPlayer;
        private int mOriginMediaVolume;
        private int mUserCurrentVolume;
        private final int mVolumeChangeUnit;
        private final int mVolumeMinStep;
        private int MAX_VOLUME = 100;
        private int VOLUME_LAVEL_COUNT = 10;
        private final int mMaxVolume = 100;
        private int mVolumeUpdate = 0;
        private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                if (AudioFocusManager.this.isWithAudioFocus()) {
                    int streamVolume = RemoteClientVolumeDetector.this.mAudioManager.getStreamVolume(3);
                    if (streamVolume > RemoteClientVolumeDetector.this.mUserCurrentVolume) {
                        RemoteClientVolumeDetector.this.mVolumeUpdate++;
                        RemoteClientVolumeDetector.this.onVolumeUp();
                    } else if (streamVolume < RemoteClientVolumeDetector.this.mUserCurrentVolume) {
                        RemoteClientVolumeDetector remoteClientVolumeDetector = RemoteClientVolumeDetector.this;
                        remoteClientVolumeDetector.mVolumeUpdate--;
                        RemoteClientVolumeDetector.this.onVolumeDown();
                    }
                    RemoteClientVolumeDetector.this.mAudioManager.setStreamVolume(3, RemoteClientVolumeDetector.this.mUserCurrentVolume, 0);
                    return;
                }
                RemoteClientVolumeDetector remoteClientVolumeDetector2 = RemoteClientVolumeDetector.this;
                remoteClientVolumeDetector2.mOriginMediaVolume = remoteClientVolumeDetector2.mAudioManager.getStreamVolume(3);
            }
        };
        private final String BLANK_MUSIC_FILENAME = "blank.mp3";
        private boolean mStarted = false;
        private RemoteVolumeHandler mHandler = new RemoteVolumeHandler();

        private class RemoteVolumeHandler extends Handler {
            private RemoteVolumeHandler() {
            }

            @Override
            public void handleMessage(final Message msg) {
                if (msg.what != 1) {
                    return;
                }
                AudioFocusManager.this.mPlaybackProxy.setRemoteVolume(msg.arg1);
                RemoteClientVolumeDetector.this.mVolumeUpdate = 0;
            }
        }

        private void updateCustomCurrentVolume() {
            int streamMaxVolume = this.mAudioManager.getStreamMaxVolume(3);
            int streamVolume = this.mAudioManager.getStreamVolume(3);
            this.mOriginMediaVolume = streamVolume;
            this.mUserCurrentVolume = streamVolume;
            int i = streamMaxVolume - 2;
            if (streamVolume > i) {
                this.mUserCurrentVolume = i;
            }
            if (this.mUserCurrentVolume < 2) {
                this.mUserCurrentVolume = 2;
            }
            this.mAudioManager.setStreamVolume(3, this.mUserCurrentVolume, 0);
        }

        private void resetMusicVolume() {
            this.mAudioManager.setStreamVolume(3, this.mOriginMediaVolume, 0);
        }

        private void requestSetVolume(final int newVolume) {
            Message message = new Message();
            message.what = 1;
            message.arg1 = newVolume;
            this.mHandler.removeMessages(1);
            this.mHandler.sendMessageDelayed(message, 500L);
        }

        private void onVolumeUp() {
            int remoteVolume = AudioFocusManager.this.mPlaybackProxy.getRemoteVolume();
            int i = this.mMaxVolume;
            if (remoteVolume < i) {
                int i2 = this.mVolumeChangeUnit;
                int i3 = ((remoteVolume / i2) + this.mVolumeUpdate) * i2;
                if (i3 < remoteVolume + this.mVolumeMinStep) {
                    i3 += i2;
                }
                if (i3 <= i) {
                    i = i3;
                }
                requestSetVolume(i);
            }
        }

        private void onVolumeDown() {
            int remoteVolume = AudioFocusManager.this.mPlaybackProxy.getRemoteVolume();
            if (remoteVolume > 0) {
                int i = this.mVolumeChangeUnit;
                int i2 = ((remoteVolume / i) + this.mVolumeUpdate) * i;
                if (i2 > remoteVolume - this.mVolumeMinStep) {
                    i2 -= i;
                }
                if (i2 < 0) {
                    i2 = 0;
                }
                requestSetVolume(i2);
            }
        }

        public RemoteClientVolumeDetector(Context context) {
            int i = 100 / 10;
            this.mVolumeChangeUnit = i;
            this.mVolumeMinStep = i / 2;
            this.mContext = context;
            this.mAudioManager = (AudioManager) context.getSystemService("audio");
        }

        @Override
        public void startDetect() throws IllegalStateException, IllegalArgumentException {
            SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " RemoteClientVolumeDetector startDetect ");
            if (AudioFocusManager.this.mPlaybackProxy.isPlaying() || AudioFocusManager.this.mPlaybackProxy.isPreparing()) {
                AudioFocusManager.this.tryToGetAudioFocus();
            }
            if (this.mStarted || !AudioFocusManager.this.isWithAudioFocus()) {
                return;
            }
            updateCustomCurrentVolume();
            this.mContext.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.mContentObserver);
            initAndStartMediaPlay();
            this.mStarted = true;
        }

        @Override
        public void stopDetect() throws IllegalStateException {
            SynoLog.d(AudioFocusManager.LOG, AudioFocusManager.LOG + " RemoteClientVolumeDetector stopDetect ");
            if (this.mStarted) {
                resetMusicVolume();
                this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
                stopAndReleaseMediaPlay();
                this.mStarted = false;
            }
        }

        private void initAndStartMediaPlay() throws IllegalStateException, IllegalArgumentException {
            try {
                MediaPlayer mediaPlayer = new MediaPlayer();
                this.mMediaPlayer = mediaPlayer;
                mediaPlayer.setLooping(true);
                AssetFileDescriptor assetFileDescriptorOpenFd = this.mContext.getAssets().openFd("blank.mp3");
                this.mMediaPlayer.setDataSource(assetFileDescriptorOpenFd.getFileDescriptor(), assetFileDescriptorOpenFd.getStartOffset(), assetFileDescriptorOpenFd.getLength());
                this.mMediaPlayer.prepare();
                if (this.mMediaPlayer.isPlaying()) {
                    return;
                }
                this.mMediaPlayer.start();
            } catch (IOException | IllegalArgumentException | IllegalStateException |
                     SecurityException e) {
                e.printStackTrace();
            }
        }

        private void stopAndReleaseMediaPlay() throws IllegalStateException {
            try {
                if (this.mMediaPlayer.isPlaying()) {
                    this.mMediaPlayer.stop();
                }
                this.mMediaPlayer.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void giveUpAudioFocus() {
        AudioFocusHelper audioFocusHelper;
        String str = LOG;
        SynoLog.d(str, str + " giveUpAudioFocus ");
        if (this.mAudioFocus == AudioFocus.Focused && (audioFocusHelper = this.mAudioFocusHelper) != null && audioFocusHelper.abandonFocus()) {
            this.mAudioFocus = AudioFocus.NoFocusNoDuck;
        }
    }

    public void tryToGetAudioFocus() {
        AudioFocusHelper audioFocusHelper;
        String str = LOG;
        SynoLog.d(str, str + " tryToGetAudioFocus ");
        if (this.mAudioFocus == AudioFocus.Focused || (audioFocusHelper = this.mAudioFocusHelper) == null || !audioFocusHelper.requestFocus()) {
            return;
        }
        this.mAudioFocus = AudioFocus.Focused;
    }

    public void toggleDetect(boolean playing) {
        if (this.mRemoteClientVolumeDetector == null) {
            return;
        }
        if (playing && AudioPreference.enableRemoteController(this.mContext)) {
            this.mRemoteClientVolumeDetector.startDetect();
        } else {
            this.mRemoteClientVolumeDetector.stopDetect();
        }
    }

    @Deprecated
    public synchronized void showRemoteControl(boolean isDisplay) {
    }
}
