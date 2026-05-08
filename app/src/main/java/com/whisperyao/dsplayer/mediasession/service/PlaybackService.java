package com.whisperyao.dsplayer.mediasession.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.AndroidAuto.VoiceSearchParams;
import com.whisperyao.dsplayer.AudioFocusManager;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.LocalEnumerator;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.ServiceOperator;
import com.whisperyao.dsplayer.UDCEvent;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.mediasession.PlayerAdapter;
import com.whisperyao.dsplayer.mediasession.players.StreamingMediaPlayer;
import com.whisperyao.dsplayer.net.WebAPI;
import com.whisperyao.dsplayer.playing.EqualizerSettings;
import com.whisperyao.dsplayer.playing.NowPlayingManager;
import com.whisperyao.dsplayer.provider.DatabaseAccesser;
import com.whisperyao.dsplayer.util.DeviceCustomization;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class PlaybackService extends AndroidAutoService {

    private static final String AVRCP_META_CHANGED = "com.android.music.metachanged";
    private static final String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";

    private static final String CUSTOM_ACTION_MOSTOFTEN = "com.synology.dsaudio.mediabrowserservice.action.mostoften";
    private static final String CUSTOM_ACTION_RANDOM100 = "com.synology.dsaudio.mediabrowserservice.action.random100";
    private static final String CUSTOM_ACTION_REPEAT = "com.synology.dsaudio.mediabrowserservice.action.repeat";
    private static final String CUSTOM_ACTION_SHUFFLE = "com.synology.dsaudio.mediabrowserservice.action.shuffle";

    private static final int IDLE_DELAY = 60000;

    private static final String LOG = "PlaybackService";

    public static final int TRACK_ENDED = 1;
    public static final int RELEASE_WAKELOCK = 2;
    public static final int SERVER_DIED = 3;
    public static final int PLAY_NEXT = 5;
    public static final int OPEN_CURRENT = 6;
    public static final int PLAY_FAILED = 7;
    public static final int PLAY_FAILED_NOTFOUND = 8;
    public static final int START_PLAYING = 9;
    public static final int OPEN_TIMEOUT = 10;
    public static final int BUFFERING_CHANGED = 11;
    public static final int PREPARE_CHANGED = 12;
    public static final int START_PLAY_NEXT = 13;
    public static final int STOP_PLAY = 14;
    public static final int PLAY_STATE_CHANGED = 16;

    private static Timer mTimer = new Timer();

    @Inject
    Context mContext;
    private String mLastRadioStreamId;
    private PowerManager.WakeLock mWakeLock;

    @Inject
    PowerManager powerManager;
    // (PowerManager) App.getContext().getSystemService(Context.POWER_SERVICE)
    private int mOpenFailedCounter = 0;
    private boolean mWasPlaying = false;
    private boolean mReloadCompleted = false;
    private int mPreSeekTime = 0;
    private boolean mStopPollingStatusWork = true;
    private ThreadWork mStopRadioWork;
    private WifiReceiver mWifiReceiver;
    private StreamingMediaPlayer mPlayer;
    private boolean mOnPlayCalledBeforeQueueReload = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final PlayerAdapter dummy = new PlayerAdapter() {
        public long getBufferingPercent() { return 0; }
        public SongItem getCurrentMedia() { return null; }
        public boolean isPlaying() { return false; }
    };
    private final EqualizerSettings.Callback equalizerCallback = new EqualizerSettings.Callback() {
        @Override
        public void updateEqualizer() {
            mPlayer.updateEqualizer();
        }
    };
    private final AudioFocusManager.PlaybackProxy mPlaybackProxy = new AudioFocusManager.PlaybackProxy() {
        @Override
        public boolean isRemotePlayer() {
            return false;
        }

        @Override
        public void setRemoteVolume(int newVolume) {
        }

        @Override
        public boolean isPlaying() {
            return PlaybackService.this.isPlaying();
        }

        @Override
        public boolean isPreparing() {
            return PlaybackService.this.isPreparing();
        }

        @Override
        public boolean hasAudioToPlay() {
            return PlaybackService.this.hasAudioToPlay();
        }

        @Override
        public void play() {
            PlaybackService.this.onPlay();
        }

        @Override
        public void pause() {
            PlaybackService.this.onPause();
        }

        @Override
        public void toDuckVolume() {
            if (PlaybackService.this.mPlayer != null) {
                PlaybackService.this.mPlayer.setVolume(0.3f);
            }
        }

        @Override
        public void toNormalVolume() {
            if (PlaybackService.this.mPlayer != null) {
                PlaybackService.this.mPlayer.setVolume(1.0f);
            }
        }

        @Override
        public int getRemoteVolume() {
            return PlaybackService.this.getVolume();
        }
    };
    private final CacheManager.OnRatingChangeObserver mOnRatingChangeObserver = songList -> {
        SongItem songItem = PlaybackService.this.playingQueueManager.getSongItem();
        if (songItem == null) {
            return;
        }
        for (SongItem songItem2 : songList) {
            if (songItem2.getDsId().equals(songItem.getDsId()) && songItem2.getID().equals(songItem.getID())) {
                songItem.setSongRating(songItem2.getSongRating());
            }
        }
        CacheManager.getInstance().adjustRating(PlaybackService.this.playingQueueManager.getQueue());
        PlaybackService.this.getNowPlayingManager().saveQueue(PlaybackService.this.playingQueueManager.getQueue());
    };

    // =========================
    // Handler：主播放器状态机
    // =========================
    private final Handler mMediaplayerHandler = new MediaPlayerHandler(this);
    private static class MediaPlayerHandler extends Handler {

        private final WeakReference<PlaybackService> serviceRef;

        MediaPlayerHandler(PlaybackService service) {
            serviceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaybackService service = serviceRef.get();
            if (service == null) {
                return;
            }

            SynoLog.d("mMediaplayerHandler", "message, what: " + msg.what);

            switch (msg.what) {

                case TRACK_ENDED:
                    service.handleTrackEnded();
                    break;

                case RELEASE_WAKELOCK:
                    if (!service.mWakeLock.isHeld()) {
                        service.mWakeLock.acquire(30000);
                    }
                    if (service.mWakeLock.isHeld()) {
                        service.mWakeLock.release();
                    }
                    break;

                case SERVER_DIED:
                    service.handleServerDied();
                    break;

                case PLAY_NEXT:
                    service.onSkipToNext();
                    break;

                case OPEN_CURRENT:
                    if (msg.obj instanceof Boolean) {
                        service.openCurrent(msg.arg1, (Boolean) msg.obj);
                    } else {
                        service.openCurrent(msg.arg1);
                    }
                    break;

                case PLAY_FAILED:
                    Toast.makeText(service.mContext,
                            (String) msg.obj,
                            Toast.LENGTH_SHORT).show();
                    break;

                case PLAY_FAILED_NOTFOUND:
                    Toast.makeText(service.mContext, (String) msg.obj, Toast.LENGTH_SHORT)
                            .show();

                    service.removeTracks(new int[]{service.playingQueueManager.getPlayIndex()});

                    service.onStop();
                    break;

                case START_PLAYING:
                    service.onPlay();
                    break;

                case OPEN_TIMEOUT:
                    service.openTimeout();
                    break;

                case BUFFERING_CHANGED:
                    service.notifyChange(ServiceOperator.BUFFERING_CHANGED);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        service.setNewState(service.getMState());
                    }
                    break;

                case PREPARE_CHANGED:
                    service.notifyChange(ServiceOperator.PREPARE_CHANGED);
                    break;

                case START_PLAY_NEXT:
                    service.handleStartPlayNext();
                    break;

                case STOP_PLAY:
                    service.onStop();
                    break;

                case PLAY_STATE_CHANGED:
                    service.notifyChange(ServiceOperator.PLAYSTATE_CHANGED);
                    service.updatePlaybackState();
                    break;
            }
        }
    }
    private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private static class DelayedStopHandler extends Handler {

        private final WeakReference<PlaybackService> serviceRef;

        DelayedStopHandler(PlaybackService service) {
            super(Looper.getMainLooper());
            this.serviceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaybackService service = serviceRef.get();
            if (service == null) return;

            SynoLog.i(LOG, "mDelayedStopHandler handleMessage");

            if (service.isPlaying()
                    || service.hasAudioToPlay()
                    || service.audioFocusManager.isResumeAfterInterrupt()
                    || service.mMediaplayerHandler.hasMessages(1)
                    || !ServiceOperator.isUIClosed()) {
                return;
            }

            service.saveQueue();
            service.stopSelf(-1);
        }
    }
    private final BroadcastReceiver mLoginStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SynoLog.i("mLoginStatusListener", "onReceive : action = " + intent.getAction());
            // Common.LOGIN_STATUS
            boolean booleanExtra = intent.getBooleanExtra("login_status", false);
            SynoLog.i("mLoginStatusListener", "isLogin = " + booleanExtra);
            if (booleanExtra) {
                return;
            }
            PlaybackService.this.mPlayer.notifyLogout();
        }
    };

    @Override
    protected PlayerAdapter getPlayerAdapter() {
        StreamingMediaPlayer streamingMediaPlayer = this.mPlayer;
        return streamingMediaPlayer != null ? streamingMediaPlayer : this.dummy;
    }

    @Override
    protected void logStartForeground(Throwable throwable) {
        StreamingMediaPlayer streamingMediaPlayer = this.mPlayer;
        if (streamingMediaPlayer != null) {
            streamingMediaPlayer.logPlayerStatBeforeStartForeground(throwable);
        }
    }

    @Override
    protected void setVolume(int volume) {
        super.setVolume(volume);
        this.audioManager.setStreamVolume(3, volume, 0);
    }

    @Override
    protected int getVolume() {
        return this.audioManager.getStreamVolume(3);
    }

    public void startProxyPolling() {

        if (!mStopPollingStatusWork) return;

        mStopPollingStatusWork = false;

        new Thread(() -> {

            while (!mStopPollingStatusWork && isPlayingRadio()) {

                SongItem song = playingQueueManager.getSongItem();
                if (song == null) break;

                try {
                    JSONObject obj = ConnectionManager.doPollingRadioInfo(song.getStreamId());

                    if (obj != null && obj.optBoolean("success")) {

                        String title = obj.has("data")
                                ? obj.optJSONObject("data").optString("title")
                                : obj.optString("artist");

                        if (!TextUtils.equals(song.getArtist(), title)) {
                            song.setArtist(title);
                            notifyChange(ServiceOperator.META_CHANGED);
                        }
                    }

                    mLastRadioStreamId = song.getStreamId();

                    Thread.sleep(3000);

                } catch (Exception ignored) {}
            }

            mStopPollingStatusWork = true;

        }).start();
    }

    public void stopProxyPolling() {
        mStopPollingStatusWork = true;

        if (TextUtils.isEmpty(mLastRadioStreamId)) {
            return;
        }

        executorService.execute(() -> {
            ConnectionManager.deleteRadioInfo(mLastRadioStreamId);
            mLastRadioStreamId = null;
        });
    }

    public void terminatePrevStopRadioWork() {
        ThreadWork threadWork = this.mStopRadioWork;
        if (threadWork != null) {
            threadWork.endThread();
            this.mStopRadioWork = null;
        }
    }

    public void stopRadioAfterDelay(final long delayInterval) {
        terminatePrevStopRadioWork();
        ThreadWork threadWork = new ThreadWork() {
            @Override
            public void onWorking() {
                try {
                    Thread.sleep(delayInterval);
                    if (PlaybackService.this.isPlaying() || !PlaybackService.this.isPlayingRadio()) {
                        return;
                    }
                    PlaybackService.this.onStop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        this.mStopRadioWork = threadWork;
        threadWork.startWork();
    }

    private synchronized void startTimer(UpdateStatusTimer updateTimer) {
        Timer timer = mTimer;
        if (timer == null) {
            Timer timer2 = new Timer();
            mTimer = timer2;
            timer2.schedule(updateTimer, 0L, 1000L);
        } else {
            timer.cancel();
            mTimer.purge();
            Timer timer3 = new Timer();
            mTimer = timer3;
            timer3.schedule(updateTimer, 0L, 1000L);
        }
    }
    private synchronized void stopTimer() {
        Timer timer = mTimer;
        if (timer != null) {
            timer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    private class UpdateStatusTimer extends TimerTask {
        UpdateStatusTimer() {
        }

        @Override
        public void run() {
            if (PlaybackService.this.getReloadSuccessful()) {
                PlaybackService.this.saveStatus();
            }
            if (DeviceCustomization.supportForceNext()) {
                PlaybackService.this.checkMissingOnComplete();
            }
        }
    }

    public void saveStatus() {
        StreamingMediaPlayer streamingMediaPlayer = this.mPlayer;
        if (streamingMediaPlayer != null) {
            if (streamingMediaPlayer.isPlaying() || this.mPlayer.isPreparing() || this.mPlayer.isEnd()) {
                this.getNowPlayingManager().saveStatus(true, this.playingQueueManager.getPlayIndex(), position(), getRepeatMode(), getShuffleMode());
            } else if (this.mPlayer.isPaused()) {
                this.getNowPlayingManager().saveStatus(false, this.playingQueueManager.getPlayIndex(), position(), getRepeatMode(), getShuffleMode());
                stopTimer();
            } else {
                this.getNowPlayingManager().saveStatus(false, -1, 0, getRepeatMode(), getShuffleMode());
                stopTimer();
            }
        }
    }

    private void savePauseStatus() {
        this.getNowPlayingManager()
                .saveStatus(
                        false,
                        this.playingQueueManager.getPlayIndex(),
                        position(),
                        getRepeatMode(),
                        getShuffleMode()
                );
        stopTimer();
    }

    private void checkMissingOnComplete() {
        if (isPlayingRadio() || position() - duration() <= 2000) {
            return;
        }
        SynoLog.e(LOG, "OnComplete missing: position = " + position() + ", duration = " + duration());
        onSkipToNext();
    }

    @Override
    public void onCreate() {
        SynoLog.d(LOG, "onCrate");
        super.onCreate();
        EqualizerSettings.getInstance().addCallback(this.equalizerCallback);
        if (this.mPlayer == null) {
            this.mPlayer = new StreamingMediaPlayer(this, this.mMediaplayerHandler);
        }
        this.audioFocusManager.setup(this, this.mPlaybackProxy);
        PowerManager.WakeLock wakeLockNewWakeLock = this.powerManager.newWakeLock(1, getClass().getName());
        this.mWakeLock = wakeLockNewWakeLock;
        wakeLockNewWakeLock.setReferenceCounted(false);
        this.mDelayedStopHandler.sendMessageDelayed(this.mDelayedStopHandler.obtainMessage(), DateUtils.MILLIS_PER_MINUTE);
        Utils.registerReceiver(this, this.mLoginStatusListener, new IntentFilter("com.synology.dsaudio.NOTIFY_LOGIN_STATUS"), false);
        CacheManager.getInstance().registerOnRatingChangeObserver(this.mOnRatingChangeObserver);
        WifiReceiver wifiReceiver = new WifiReceiver();
        this.mWifiReceiver = wifiReceiver;
        registerReceiver(wifiReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    public void onDestroy() {
        boolean isPlaying = isPlaying();
        if (isPlaying) {
            savePauseStatus();
        }
        stop(isPlaying);
        setMetadata(generateNotPlayingItem());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(0);
        }
        if (this.mWakeLock != null) {
            mWakeLock.release();
        }
        CacheManager.getInstance().unregisterOnRatingChangeObserver(this.mOnRatingChangeObserver);
        CacheManager.getInstance().clearCache();
        this.mPlayer.release();
        stopProxyPolling();
        terminatePrevStopRadioWork();
        this.audioFocusManager.release();
        unregisterReceiver(this.mLoginStatusListener);
        unregisterReceiver(this.mWifiReceiver);
        EqualizerSettings.getInstance().removeCallback(this.equalizerCallback);
        mMediaplayerHandler.removeCallbacksAndMessages(null);
        executorService.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void doReloadAll(boolean isFromCar) {
        int playPos;
        ArrayList<SongItem> arrayList = new ArrayList<>(Arrays.asList(getNowPlayingManager().loadQueue()));
        NowPlayingManager.NowPlayingStatus status = this.getNowPlayingManager().getStatus();
        Common.ShuffleMode shuffleMode = Common.ShuffleMode.NONE;
        Common.RepeatMode repeatMode = Common.RepeatMode.NONE;
        if (status != null) {
            if (status.getPlayPos() != -1) {
                playPos = status.getPlayPos();
                this.mPreSeekTime = status.getPreSeekTime();
            } else {
                playPos = 0;
            }
            if (status.getShuffleMode() != null) {
                shuffleMode = status.getShuffleMode();
            }
            if (status.getRepeatMode() != null) {
                repeatMode = status.getRepeatMode();
            }
        } else {
            playPos = 0;
        }
        setQueue(arrayList, playPos);
        this.mCallback.onSetShuffleMode(shuffleMode.ordinal());
        this.mCallback.onSetRepeatMode(repeatMode.ordinal());
        this.mMediaplayerHandler.sendMessageDelayed(this.mMediaplayerHandler.obtainMessage(6, this.mPreSeekTime, 0, Boolean.valueOf(isFromCar && this.mOnPlayCalledBeforeQueueReload)), 100L);
        this.mOnPlayCalledBeforeQueueReload = false;
        updateSessionQueue();
        this.mReloadCompleted = true;

    }

    // =========================
    // NOTIFY
    // =========================
    private void notifyChange(String what) {
        Intent i = new Intent(what);
        i.setPackage(getPackageName());
        sendBroadcast(i);

        if (ServiceOperator.PLAYSTATE_CHANGED.equals(what)) {
            bluetoothNotifyChange(AVRCP_PLAYSTATE_CHANGED);
        } else if (ServiceOperator.META_CHANGED.equals(what)) {
            bluetoothNotifyChange(AVRCP_META_CHANGED);
        }
    }

    public void openTimeout() {
        if (this.mPlayer.isInitialized()) {
            return;
        }
        SongItem currentMedia = this.mPlayer.getCurrentMedia();
        gotoIdleState();
        setMetadata(generateNotPlayingItem());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(1);
        }
        int i = this.mOpenFailedCounter + 1;
        this.mOpenFailedCounter = i;
        if (i < this.playingQueueManager.getQueueSize() && this.playingQueueManager.getQueueSize() > 1) {
            this.mMediaplayerHandler.removeMessages(5);
            this.mMediaplayerHandler.sendEmptyMessageDelayed(5, 100L);
        } else {
            this.mOpenFailedCounter = 0;
        }
        Message messageObtainMessage = this.mMediaplayerHandler.obtainMessage(7);
        messageObtainMessage.obj = getString(R.string.playback_failed) + StringUtils.SPACE + (currentMedia == null ? "" : currentMedia.getTitle());
        this.mMediaplayerHandler.sendMessage(messageObtainMessage);
    }

    private void requestToGetCurrentSongRating() {
        final SongItem songItem = this.playingQueueManager.getSongItem();
        if (ConnectionManager.canEditRating(true, songItem)) {
            // new Thread(() -> CacheManager.getInstance().requestToGetRating());
            SynoLog.d("PlaybackService", "CacheManager.getInstance().requestToGetRating())");
        }
    }

    private void openCurrent(int seekPos) {
        openCurrent(seekPos, true);
    }

    private void openCurrent(int seekPos, boolean play) {

        SongItem song = playingQueueManager.getSongItem();
        // SynoLog.d(LOG, "openCurrent, song: " + song.getTitle());
        if (song == null) return;

        if (song.isRadio()) seekPos = 0;

        if (mPlayer == null) {
            mPlayer = new StreamingMediaPlayer(this, mMediaplayerHandler);
        }

        if (!mPlayer.isIdle()) {
            release();
            mPlayer.release();

            mPlayer = new StreamingMediaPlayer(this, mMediaplayerHandler);
        }

        acquireWifiLock();

        SynoLog.i(LOG, "setCurrentSong");
        mPlayer.setCurrentSong(song, seekPos, play);

        setMetadata(song);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(play ? 6 : 2);
        }

        mPreSeekTime = seekPos;

        setNextSong();
        requestToGetCurrentSongRating();
    }

    private void setNextSong() {
        if (mPlayer != null) {
            mPlayer.setNextSong(playingQueueManager.next(false, false));
        }
    }

    @Override
    protected long getCurrentPosition() {
        if (this.mPlayer == null) {
            return 0L;
        }
        return this.mPlayer.getPosition();
    }

    @Override
    protected float getCurrentPlaybackSpeed() {
        StreamingMediaPlayer streamingMediaPlayer = this.mPlayer;
        if (streamingMediaPlayer == null) {
            return 0.0f;
        }
        return streamingMediaPlayer.getPlaybackSpeed();
    }

    // =========================
    // PLAYER CORE
    // =========================
    @Override
    protected void onPlay() {
        SynoLog.d(LOG, "onPlay");
        if (!mPlayer.isInitialized() && !mPlayer.isEnd()) {
            mMediaplayerHandler.sendEmptyMessageDelayed(OPEN_CURRENT, 100);
            return;
        }

        terminatePrevStopRadioWork();

        audioFocusManager.tryToGetAudioFocus();
        acquireWifiLock();

        mPlayer.start(mPreSeekTime);
        mPreSeekTime = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(6);
        }
        startTimer(new UpdateStatusTimer());

        if (isPlayingRadio()) startProxyPolling();

        if (!mWasPlaying) {
            mWasPlaying = true;
            notifyChange(ServiceOperator.PLAYSTATE_CHANGED);
        }

        notifyChange(ServiceOperator.META_CHANGED);
    }

    @Override
    protected void onStop() {
        stop(false);
    }

    public void stop(boolean record) {
        audioFocusManager.giveUpAudioFocus();

        if (record && mReloadCompleted) {
            saveStatus();
            saveQueue();
        }

        release();

        setMetadata(generateNotPlayingItem());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(1);
        }

        if (ServiceOperator.isUIClosed()) {
            stopSelf();
        }
    }

    public void release() {
        if (mPlayer.isIdle()) return;

        mMediaplayerHandler.removeMessages(OPEN_CURRENT);

        gotoIdleState();

        mWasPlaying = false;
        mPlayer.stop();

        notifyChange(ServiceOperator.PREPARE_CHANGED);
        notifyChange(ServiceOperator.PLAYSTATE_CHANGED);

        releaseWifiLock();
        stopProxyPolling();
    }

    @Override
    protected void onPause() {
        mPreSeekTime = -1;

        mPlayer.pause();
        mWasPlaying = false;

        gotoIdleState();
        releaseWifiLock();

        stopProxyPolling();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(2);
        }

        stopRadioAfterDelay(10000);
        saveStatus();
    }

    // =========================
    // BASIC STATUS
    // =========================
    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    public boolean isPause() {
        return mPlayer != null && mPlayer.isPaused();
    }

    // =========================
    // SEEK / SKIP
    // =========================
    @Override
    protected void onSkipToPrevious() {
        SongItem prev = playingQueueManager.previous();
        mPreSeekTime = 0;

        if (prev == null) {
            onStop();
        } else {
            mMediaplayerHandler.sendEmptyMessageDelayed(OPEN_CURRENT, 100);
        }
    }

    @Override
    protected void onSkipToNext() {
        SongItem next = playingQueueManager.next(true, true);
        mPreSeekTime = 0;

        if (next == null) {
            onStop();
        } else {
            mMediaplayerHandler.sendEmptyMessageDelayed(OPEN_CURRENT, 100);
        }
    }

    @Override
    protected void onSkipToQueueItem(long id) {
        super.onSkipToQueueItem(id);
        this.mPreSeekTime = 0;
        this.mMediaplayerHandler.removeMessages(6);
        this.mMediaplayerHandler.sendEmptyMessageDelayed(6, 100L);
    }

    // =========================
    // STATE HELPERS
    // =========================
    private void gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendMessageDelayed(
                mDelayedStopHandler.obtainMessage(),
                DateUtils.MILLIS_PER_MINUTE
        );
    }

    public void updateTracks(int start, int limit, @NonNull ArrayList<Integer> ids) {
        if (ids.isEmpty()) { return; }

        List<SongItem> queue = new ArrayList<>(playingQueueManager.getQueue());

        List<SongItem> movedTracks = new ArrayList<>(ids.size());

        for (Integer sourceIndex : ids) {
            movedTracks.add(queue.get(sourceIndex));
        }

        if (start + limit > start) {
            queue.subList(start, start + limit).clear();
        }

        queue.addAll(start, movedTracks);

        int playIndex = playingQueueManager.getPlayIndex();
        int newIndex = ids.indexOf(playIndex);

        if (newIndex >= 0) {
            playIndex = start + newIndex;
        }

        setQueue(queue, playIndex);

        getNowPlayingManager().saveQueue(playingQueueManager.getQueue());

        updateSessionQueue();
    }

    private int updateTracksInternal(int start, int limit, final ArrayList<Integer> ids) {
        List<SongItem> currentQueue =
                new ArrayList<>(this.playingQueueManager.getQueue());

        List<SongItem> reorderedTracks = new ArrayList<>();

        for (Integer index : ids) {
            reorderedTracks.add(currentQueue.get(index));
        }

        if (start + limit > start) {
            currentQueue.subList(start, start + limit).clear();
        }

        currentQueue.addAll(start, reorderedTracks);

        int playIndex = this.playingQueueManager.getPlayIndex();
        int reorderedIndex = ids.indexOf(playIndex);

        if (reorderedIndex >= 0) {
            playIndex = start + reorderedIndex;
        }

        setQueue(currentQueue, playIndex);

        return reorderedTracks.size();
    }

    public void removeTracks(@NonNull int[] ids) {
        if (ids.length == 0) { return; }

        ArrayList<SongItem> queue = new ArrayList<>(playingQueueManager.getQueue());

        int playIndex = playingQueueManager.getPlayIndex();
        int removedCount = 0;

        for (int i = ids.length - 1; i >= 0; i--) {
            int removeIndex = ids[i];

            if (removeIndex < 0 || removeIndex >= queue.size()) {
                continue;
            }

            queue.remove(removeIndex);

            if (removeIndex == playIndex) {
                playIndex = -1;
            } else if (removeIndex < playIndex) {
                playIndex--;
            }

            removedCount++;
        }

        if (removedCount == 0) {
            return;
        }

        setQueue(queue, 0);

        if (queue.isEmpty()) {
            onStop();
            playingQueueManager.setPlayIndex(-1);
        } else if (playIndex >= 0) {
            playingQueueManager.setPlayIndex(playIndex);
        } else {
            onStop();
            playingQueueManager.setPlayIndex(0);
        }

        getNowPlayingManager().saveQueue(playingQueueManager.getQueue());

        updateSessionQueue();
    }

    private int removeTracksInternal(final int[] ids) {
        ArrayList<SongItem> arrayList = new ArrayList<>(this.playingQueueManager.getQueue());
        int playIndex = this.playingQueueManager.getPlayIndex();
        int i = 0;
        for (int length = ids.length - 1; length >= 0; length--) {
            if (ids[length] < arrayList.size()) {
                int i2 = ids[length];
                arrayList.remove(i2);
                if (i2 == playIndex) {
                    playIndex = -1;
                } else if (i2 < playIndex) {
                    playIndex--;
                }
                i++;
            }
        }
        setQueue(arrayList, 0);
        if (this.playingQueueManager.getQueueSize() == 0) {
            onStop();
            this.playingQueueManager.setPlayIndex(-1);
        } else if (playIndex >= 0) {
            this.playingQueueManager.setPlayIndex(playIndex);
        } else {
            onStop();
            this.playingQueueManager.setPlayIndex(0);
        }
        return i;
    }

    public Boolean isPlayingRadio() {
        SongItem songItem = this.playingQueueManager.getSongItem();
        return songItem != null && songItem.isRadio();
    }

    public boolean hasAudioToPlay() {
        return this.playingQueueManager.getSongItem() != null;
    }

    public int duration() {
        return (mPlayer != null && mPlayer.isInitialized()) ? mPlayer.getDuration() : -1;
    }

    public int position() {
        return (mPlayer != null && mPlayer.isInitialized()) ? mPlayer.getPosition() : -1;
    }

    @Override
    protected void onSeekTo(long pos) {
        if (mPlayer == null || !mPlayer.isInitialized()) return;

        if (pos < 0) pos = 0;

        audioFocusManager.tryToGetAudioFocus();
        mPlayer.seek(pos);

        notifyChange(ServiceOperator.PLAYSTATE_CHANGED);
    }

    public boolean isPreparing() {
        return mPlayer != null && mPlayer.isPreparing();
    }

    @Override
    protected void setRepeatMode(int repeatMode) {
        this.playingQueueManager.setRepeatMode(getRepeatMode());
        setNextSong();
        super.setRepeatMode(repeatMode);
        saveStatus();
    }

    @Override
    protected void setShuffleMode(int shuffleMode) {
        if (getShuffleMode().isEnabled()) {
            this.playingQueueManager.shuffle();
        } else {
            this.playingQueueManager.unShuffle();
        }
        setNextSong();
        super.setShuffleMode(shuffleMode);
        saveStatus();
    }

    @Override
    protected void setCustomAction(PlaybackStateCompat.Builder stateBuilder) {
        if (Common.isLogin()) {
            stateBuilder.addCustomAction(
                    CUSTOM_ACTION_RANDOM100,
                    getString(R.string.random_100),
                    R.drawable.tool_random100
            );
        }
        stateBuilder.addCustomAction(
                CUSTOM_ACTION_MOSTOFTEN,
                getString(R.string.most_often_played),
                R.drawable.tool_often
        );
        if (this.playingQueueManager.getQueueSize() > 0) {
            stateBuilder.addCustomAction(CUSTOM_ACTION_SHUFFLE, getString(R.string.shuffle), getShuffleResId());
            stateBuilder.addCustomAction(CUSTOM_ACTION_REPEAT, getString(R.string.repeat), getRepeatResId());
        }
    }

    @Override
    protected MediaSessionCallback initMediaSessionCallback() {
        return new SessionCallback();
    }

    private class SessionCallback extends AbstractMediaBrowserService.MediaSessionCallback {
        @Override
        public void onSetCaptioningEnabled(boolean enabled) { }

        @Override
        public void onSetRating(RatingCompat rating) { }

        @Override
        public void onSetRating(RatingCompat rating, Bundle extras) { }

        private SessionCallback() { super(); }

        @Override
        public void onPlay() {
            SynoLog.d("PlaybackService", "onPlay");
            if (PlaybackService.this.isPlaying()) {
                SynoLog.d(PlaybackService.LOG, "Call onPlay but the player is playing now");
                return;
            }
            if (PlaybackService.this.isPause()) {
                PlaybackService.this.onPlay();
                return;
            }
            if (!PlaybackService.this.mReloadCompleted) {
                PlaybackService.this.mOnPlayCalledBeforeQueueReload = true;
            }
            PlaybackService.this.gotoIdleState();
            if (PlaybackService.this.playingQueueManager.getSongItem() != null) {
                PlaybackService.this.mMediaplayerHandler.removeMessages(6);
                PlaybackService.this.mMediaplayerHandler.sendEmptyMessageDelayed(6, 100L);
            }
        }

        @Override
        public void onPause() {
            PlaybackService.this.onPause();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            PlaybackService.this.onSkipToQueueItem(id);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            SynoLog.d("PlaybackService", "onCustomAction: " + action);
            super.onCustomAction(action, extras);
            action.hashCode();
            switch (action) {
                case CUSTOM_ACTION_ADD_NEXT:
                case CUSTOM_ACTION_ADD_ONLY:
                case CUSTOM_ACTION_ADD_PLAY:
                case CUSTOM_ACTION_BY_SITUATION:
                case CUSTOM_ACTION_PLAY_NOW:
                    PlaybackService.this.enqueue(action);
                    break;
                case CUSTOM_ACTION_REPEAT:
                    PlaybackService.this.onRepeatClick();
                    break;
                case CUSTOM_ACTION_MOSTOFTEN:
                    PlaybackService.this.enumSongsAndPlay(LocalEnumerator.MOST_OFTEN_PLAYED);
                    break;
                case CUSTOM_ACTION_REORDER:
                    PlaybackService.this.updateTracks(extras.getInt("start"), extras.getInt(WebAPI.WebApiPin.LIMIT), extras.getIntegerArrayList("ids"));
                    break;
                case CUSTOM_ACTION_RANDOM100:
                    PlaybackService.this.enumSongsAndPlay(Common.CAT_RANDOM100_ID);
                    break;
                case CUSTOM_ACTION_REMOVE_BYID:
                    PlaybackService.this.removeTracks(extras.getIntArray("ids"));
                    break;
                case CUSTOM_ACTION_SHUFFLE:
                    PlaybackService.this.onShuffleClick();
                    break;
            }
        }

        private boolean browsedSongsMatchesCurrentQueue() {
            List<SongItem> queue = PlaybackService.this.playingQueueManager.getQueue();
            if (PlaybackService.this.getMBrowsedSongList().size() != queue.size()) {
                return false;
            }
            for (int i = 0; i < queue.size(); i++) {
                if (!queue.get(i).equals(PlaybackService.this.getMBrowsedSongList().get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            if (PlaybackService.this.getUdcCurrentSupportMediaId() != null) {
                Bundle bundle = new Bundle();
                bundle.putString(UDCEvent.KEY_FEATURE, PlaybackService.this.getUdcCurrentSupportMediaId());
                // PlaybackService.this.firebaseAnalyticsUtil.logEvent(UDCEvent.EVENT__ANDROID_AUTO_FEATURE, bundle);
            }
            PlaybackService playbackService = PlaybackService.this;
            playbackService.setMetadata(playbackService.generatePreparingItem());
            int i = 0;
            while (true) {
                if (i >= PlaybackService.this.getMBrowsedSongList().size()) {
                    i = 0;
                    break;
                } else if (mediaId.equals(PlaybackService.this.getMBrowsedSongList().get(i).getMediaId())) {
                    break;
                } else {
                    i++;
                }
            }
            if (!browsedSongsMatchesCurrentQueue()) {
                PlaybackService playbackService2 = PlaybackService.this;
                playbackService2.setQueue(playbackService2.getMBrowsedSongList(), i);
                PlaybackService.this.updateSessionQueue();
            } else {
                PlaybackService.this.playingQueueManager.setPlayIndex(i);
            }
            PlaybackService.this.setMetadata(PlaybackService.this.playingQueueManager.getSongItem());
            PlaybackService.this.mMediaplayerHandler.sendMessageDelayed(PlaybackService.this.mMediaplayerHandler.obtainMessage(6, 0, 0, true), 100L);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            SynoLog.e(PlaybackService.LOG, " playFromSearch. " + query);
            VoiceSearchParams voiceSearchParams = new VoiceSearchParams(query, extras);
            SynoLog.e(PlaybackService.LOG, " VoiceSearchParams. " + voiceSearchParams);
            if (voiceSearchParams.isAny) {
                SynoLog.e(PlaybackService.LOG, " empty query. ");
                onPlay();
            } else {
                PlaybackService.this.searchSongsAndPlay(voiceSearchParams);
            }
        }

        @Override
        public void onClearQueue() {
            super.onClearQueue();
            PlaybackService.this.setQueue(new ArrayList<>(), -1);
            PlaybackService.this.getNowPlayingManager().clearQueue();
        }
    }

    @Override
    public void doBeforeSwitchIfNecessary(List<? extends SongItem> listToSave) {
        savePauseStatus();
        saveQueue(listToSave);
        stop(false);
        this.mReloadCompleted = false;
    }

    public void updateEqualizer() {
        if (mPlayer != null) {
            mPlayer.updateEqualizer();
        }
    }

    protected boolean isDownloading(Bundle songBundle) {
        return this.mPlayer.isDownloading(SongItem.fromBundle(songBundle));
    }

    // =========================
    // RADIO / WIFI RECEIVER
    // =========================
    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            ConnectivityManager cm =
                    (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo info = cm.getActiveNetworkInfo();

            if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                if (isPlaying()) acquireWifiLock();
            } else {
                releaseWifiLock();
            }
        }
    }

    @Override
    protected void enqueueImpl(final String action) {

        boolean addSongsToNext = false;

        int queueSize = playingQueueManager.getQueueSize();
        boolean startPlayback = true;

        if (TextUtils.equals(AbstractMediaBrowserService.CUSTOM_ACTION_PLAY_NOW, action)) {

            // 立即播放：清空当前队列位置逻辑
            queueSize = 0;

        } else if (TextUtils.equals(AbstractMediaBrowserService.CUSTOM_ACTION_ADD_NEXT, action)) {

            // 插入到下一首
            addSongsToNext = playingQueueManager.addSongsToNext(
                    playingQueueManager.popTempQueue(),
                    !(isPause() || isPlaying())
            );

        } else {

            // 普通追加到队列末尾
            playingQueueManager.appendQueue(
                    playingQueueManager.popTempQueue(),
                    playingQueueManager.getQueueSize() - 1
            );

            if (getShuffleMode().isEnabled()) {
                playingQueueManager.shuffle();
            }

        }

        updateSessionQueue();

        // 是否触发自动播放
        if (!TextUtils.equals(AbstractMediaBrowserService.CUSTOM_ACTION_PLAY_NOW, action)
                && !TextUtils.equals(AbstractMediaBrowserService.CUSTOM_ACTION_ADD_PLAY, action)
                && (!TextUtils.equals(AbstractMediaBrowserService.CUSTOM_ACTION_BY_SITUATION, action)
                || mWasPlaying
                || isPreparing()
                || isPause())) {

            startPlayback = addSongsToNext;
        }

        // 如果当前没有播放内容，触发启动播放
        if (queueSize == 0) {
            SynoLog.d(LOG, "触发自动播放");
            mMediaplayerHandler.sendMessageDelayed(
                    mMediaplayerHandler.obtainMessage(6, 0, 0, startPlayback),
                    100L
            );
        }

        // 更新 UI 状态
        setMetadata(playingQueueManager.getSongItem());
        // 保存队列
        this.getNowPlayingManager().saveQueue(playingQueueManager.getQueue());
    }

    private void bluetoothNotifyChange(String what) {
        SongItem song = playingQueueManager.getSongItem();

        Intent i = new Intent(what);

        if (song == null || !mWasPlaying) {
            i.putExtra("playing", false);
        } else {
            i.putExtra("artist", song.getArtist());
            i.putExtra("album", song.getAlbum());
            i.putExtra("title", song.getTitle());
            i.putExtra("playing", true);
            i.putExtra("duration", duration());
            i.putExtra("position", position());
        }

        sendBroadcast(i);
    }

    // =========================
    // TRACK END LOGIC
    // =========================
    private void handleTrackEnded() {
        mOpenFailedCounter = 0;

        if (getRepeatMode() == Common.RepeatMode.ONE) {
            restartCurrentTrack();
        } else {
            gotoIdleState();
            onSkipToNext();
        }
    }

    private void restartCurrentTrack() {
        DatabaseAccesser db = DatabaseAccesser.getInstance();
        SongItem song = playingQueueManager.getSongItem();

        SongItem full = db != null ? db.querySong(song) : null;
        if (full != null) {
            db.hitSong(full, full.getHitCount() + 1);
        }
        if (db != null) db.close();

        mPlayer.start(0);
    }

    private void handleServerDied() {
        if (mWasPlaying) {
            mWasPlaying = false;
            onSkipToNext();
        } else {
            mMediaplayerHandler.sendEmptyMessageDelayed(OPEN_CURRENT, 100);
        }
    }

    private void handleStartPlayNext() {
        SongItem current = playingQueueManager.getSongItem();

        DatabaseAccesser db = DatabaseAccesser.getInstance();
        SongItem full = db != null ? db.querySong(current) : null;

        if (full != null) {
            db.hitSong(full, full.getHitCount() + 1);
        }
        if (db != null) db.close();

        SongItem next = playingQueueManager.next(false, true);

        setNextSong();
        setMetadata(next);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(6);
        }
        requestToGetCurrentSongRating();

        if (isPlayingRadio()) {
            startProxyPolling();
        }
    }

    private void updatePlaybackState() {
        if (mPlayer == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mPlayer.isPlaying()) {
                setNewState(3);
            }
            else if (mPlayer.isPaused()) setNewState(2);
            else setNewState(1);
        }

    }

    private void updateSessionQueue() {
        // MediaSession queue refresh
        setQueue(playingQueueManager.getQueue(), playingQueueManager.getPlayIndex());
    }

    private void saveQueue() {
        getNowPlayingManager().saveQueue(playingQueueManager.getQueue());
    }

}
