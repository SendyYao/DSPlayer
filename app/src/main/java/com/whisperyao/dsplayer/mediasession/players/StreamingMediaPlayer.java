package com.whisperyao.dsplayer.mediasession.players;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.synology.sylib.utilities.contextprovider.SynoContextProvider;
import com.synology.sylibx.synofile.PermissionUtils;
import com.synology.sylibx.synofile.SynoFile;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.AudioMediaPlayer;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.mediasession.PlayerAdapter;
import com.whisperyao.dsplayer.provider.DatabaseAccesser;
import com.whisperyao.dsplayer.proxy.PreDownloader;
import com.whisperyao.dsplayer.proxy.StreamProxy;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.CrashlyticsUtil;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.TranscodeSetting;
import com.whisperyao.dsplayer.util.Utilities;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

public class StreamingMediaPlayer extends PlayerAdapter {
    private static final int CMD_DELAY_RELEASE = 7;
    private static final int CMD_PAUSE = 4;
    private static final int CMD_REFRESH_POSITION = 6;
    private static final int CMD_RELEASE_NEXT = 9;
    private static final int CMD_SEEK = 3;
    private static final int CMD_SETDATA = 0;
    private static final int CMD_SETNEXT_DATA = 8;
    private static final int CMD_SET_VOLUME = 5;
    private static final int CMD_START = 1;
    private static final int CMD_STOP = 2;
    private static final String LOG = "StreamingMediaPlayer";
    private static final int REFRESH_POSITION_INTERVAL = 500;
    private static final int RELEASE_DELAYED_TIME = 1000;
    private final Handler commandHandler;
    private SongItem currentSong;
    private final Handler eventHandler;
    private final Looper looper;
    private final Context mainContext;
    private SongItem nextSong;
    private AudioMediaPlayer mediaPlayer = new AudioMediaPlayer();
    private AudioMediaPlayer nextPlayer = new AudioMediaPlayer();
    public PLAYER_STATUS status = PLAYER_STATUS.Idle;
    private NEXT_PLAYER_STATUS nextStatus = NEXT_PLAYER_STATUS.UnInitialized;
    private int bufferingPercent = 0;
    private boolean isInitialized = false;
    private int position = -1;
    private float playbackSpeed = 1.0f;
    private String debugStartFunctionTag = "";
    private String debugStateBeforeStart = "";
    private final MyOnPreparedListener preparedListener = new MyOnPreparedListener();
    private final AudioMediaPlayer.OnClockRateChangedListener clockRateChangedListener = newRate -> {
        StreamingMediaPlayer.this.playbackSpeed = newRate;
        StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(16);
    };
    private final AudioMediaPlayer.OnBufferingUpdateListener bufferingListener = (mplayer, percent) -> {
        if (percent != StreamingMediaPlayer.this.bufferingPercent) {
            SynoLog.i(StreamingMediaPlayer.LOG, "onBufferingUpdate : " + percent);
            StreamingMediaPlayer.this.bufferingPercent = percent;
            StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(11);
        }
    };
    private String statusWhenCompletion = "";
    private final AudioMediaPlayer.OnCompletionListener completionListener = mplayer -> {
        SynoLog.i(StreamingMediaPlayer.LOG, "onCompletion");
        PLAYER_STATUS player_status = StreamingMediaPlayer.this.status;
        StreamingMediaPlayer.this.status = PLAYER_STATUS.End;
        StreamingMediaPlayer.this.setPosition(0);
        if (StreamingMediaPlayer.this.mediaPlayer.hasNextPlayer()) {
            StreamingMediaPlayer.this.mediaPlayer.release();
            StreamingMediaPlayer.this.statusWhenCompletion = "Set to end from " + player_status + ", next: " + StreamingMediaPlayer.this.nextStatus + ", hasNext: " + StreamingMediaPlayer.this.mediaPlayer.hasNextPlayer();
            StreamingMediaPlayer streamingMediaPlayer = StreamingMediaPlayer.this;
            streamingMediaPlayer.mediaPlayer = streamingMediaPlayer.nextPlayer;
            StreamingMediaPlayer.this.keepDebugData("onCompletion");
            StreamingMediaPlayer.this.status = PLAYER_STATUS.Started;
            StreamingMediaPlayer streamingMediaPlayer2 = StreamingMediaPlayer.this;
            streamingMediaPlayer2.currentSong = streamingMediaPlayer2.nextSong;
            StreamingMediaPlayer.this.nextStatus = NEXT_PLAYER_STATUS.UnInitialized;
            StreamingMediaPlayer.this.nextPlayer = null;
            StreamingMediaPlayer.this.nextSong = null;
            StreamingMediaPlayer.this.mediaPlayer.setClockRateChangedListener(StreamingMediaPlayer.this.clockRateChangedListener);
            StreamingMediaPlayer.this.mediaPlayer.setOnBufferingUpdateListener(StreamingMediaPlayer.this.bufferingListener);
            StreamingMediaPlayer.this.mediaPlayer.setOnCompletionListener(StreamingMediaPlayer.this.completionListener);
            StreamingMediaPlayer.this.mediaPlayer.setOnErrorListener(StreamingMediaPlayer.this.errorListener);
            StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(13);
        } else {
            StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(1);
            StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(2);
        }
        StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(16);
    };

    private final AudioMediaPlayer.OnErrorListener errorListener = (mplayer, what, extra) -> {
        SynoLog.e(StreamingMediaPlayer.LOG, "onError : what : " + what + " , extra : " + extra);
        if (extra == -1007 || extra == -1010) {
            Message messageObtainMessage = StreamingMediaPlayer.this.commandHandler.obtainMessage(7);
            messageObtainMessage.obj = App.getContext().getResources().getString(R.string.msg_format_unsupport);
            StreamingMediaPlayer.this.eventHandler.sendMessage(messageObtainMessage);
            StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(1);
            StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(2);
        } else {
            StreamingMediaPlayer.this.eventHandler.removeMessages(10);
            StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(10);
        }
        StreamingMediaPlayer.this.mpRelease();
        return true;
    };

    private enum NEXT_PLAYER_STATUS {
        UnInitialized,
        Waiting,
        Preparing,
        PreparedWaiting,
        Prepared,
        Error
    }

    private enum PLAYER_STATUS {
        Idle,
        Preparing,
        Initialized,
        Started,
        Paused,
        End,
        Released,
        Error
    }

    public void updateEqualizer() {
        AudioMediaPlayer audioMediaPlayer = this.mediaPlayer;
        if (audioMediaPlayer != null) {
            audioMediaPlayer.updateEqualizer();
        }
        AudioMediaPlayer audioMediaPlayer2 = this.nextPlayer;
        if (audioMediaPlayer2 != null) {
            audioMediaPlayer2.updateEqualizer();
        }
    }

    public StreamingMediaPlayer(final Context context, final Handler handler) {
        String str = LOG;
        SynoLog.i(str, "construct");
        this.mainContext = context;
        this.eventHandler = handler;
        this.mediaPlayer.setWakeMode(context, 1);
        HandlerThread handlerThread = new HandlerThread(str, 5);
        handlerThread.start();
        this.looper = handlerThread.getLooper();
        this.commandHandler = new ServiceHandler(this.looper);
        this.eventHandler.sendEmptyMessage(12);
    }

    private class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // SynoLog.i("handleMessage", "PlayerStatus: " + StreamingMediaPlayer.this.status);
            switch (msg.what) {
                case CMD_SETDATA:
                    SynoLog.d(StreamingMediaPlayer.LOG, "handleMessage : CMD_SETDATA");
                    if (PLAYER_STATUS.Released == StreamingMediaPlayer.this.status) {
                        StreamingMediaPlayer.this.releaseNextPlayer();
                        StreamingMediaPlayer.this.mediaPlayer = new AudioMediaPlayer();
                    }
                    StreamingMediaPlayer.this.mpSetDataSource(msg.arg1, !(msg.obj instanceof Boolean) || (Boolean) msg.obj);
                    break;
                case CMD_START:
                    SynoLog.d(StreamingMediaPlayer.LOG, "handleMessage : CMD_START");
                    StreamingMediaPlayer.this.mpStart(msg.arg1);
                    break;
                case CMD_STOP:
                    SynoLog.d(StreamingMediaPlayer.LOG, "handleMessage : CMD_STOP");
                    StreamingMediaPlayer.this.mpStop();
                    break;
                case CMD_SEEK:
                    SynoLog.d(StreamingMediaPlayer.LOG, "handleMessage : CMD_SEEK");
                    StreamingMediaPlayer.this.mpSeek(msg.arg1);
                    break;
                case CMD_PAUSE:
                    SynoLog.d(StreamingMediaPlayer.LOG, "handleMessage : CMD_PAUSE");
                    StreamingMediaPlayer.this.mpPause();
                    break;
                case CMD_SET_VOLUME:
                    StreamingMediaPlayer.this.mpSetVolume((Float) msg.obj);
                    break;
                case CMD_REFRESH_POSITION:
                    StreamingMediaPlayer.this.mpRefreshPosition();
                    break;
                case CMD_DELAY_RELEASE:
                    StreamingMediaPlayer.this.delayRelease();
                    break;
                case CMD_SETNEXT_DATA:
                    SynoLog.d(StreamingMediaPlayer.LOG, "handleMessage : CMD_SETNEXT_DATA");
                    StreamingMediaPlayer.this.mpSetNextData();
                    break;
                case CMD_RELEASE_NEXT:
                    StreamingMediaPlayer.this.releaseNextPlayer();
                    break;
            }
        }
    }

    private void setPosition(final int value) {
        this.position = value;
    }

    public void start(final int preSeek) {
        Handler handler = this.commandHandler;
        handler.sendMessage(handler.obtainMessage(1, preSeek, 0));
    }

    public void pause() {
        this.commandHandler.sendEmptyMessage(4);
    }

    public void stop() {
        if (PLAYER_STATUS.Idle != this.status) {
            this.commandHandler.sendEmptyMessage(2);
        }
    }

    public void release() {
        this.isInitialized = false;
        if (this.commandHandler.hasMessages(7)) {
            this.commandHandler.removeMessages(7);
        }
        this.commandHandler.sendEmptyMessageDelayed(7, RELEASE_DELAYED_TIME);
    }

    private void delayRelease() {
        this.looper.quit();
        if (this.looper.getThread().isInterrupted()) {
            return;
        }
        this.looper.getThread().interrupt();
    }

    public boolean isInitialized() {
        return this.isInitialized;
    }

    public float getPlaybackSpeed() {
        return this.playbackSpeed;
    }

    @Override
    public long getBufferingPercent() {
        return this.bufferingPercent;
    }

    public int getPosition() {
        if (this.isInitialized && isPlaying() && !this.commandHandler.hasMessages(6)) {
            this.commandHandler.sendEmptyMessage(6);
        } else if (PLAYER_STATUS.End == this.status) {
            setPosition(0);
        }
        return this.position;
    }

    public int getDuration() {
        AudioMediaPlayer audioMediaPlayer;
        if (this.isInitialized && (audioMediaPlayer = this.mediaPlayer) != null) {
            try {
                return audioMediaPlayer.getDuration();
            } catch (IllegalStateException ignored) {
            }
        }
        return -1;
    }

    public void seek(final long position) {
        Handler handler = this.commandHandler;
        handler.sendMessage(handler.obtainMessage(3, (int) position, 0));
    }

    public void setVolume(final float vol) {
        Handler handler = this.commandHandler;
        handler.sendMessage(handler.obtainMessage(5, Float.valueOf(vol)));
    }

    private void mpSetDataSource(int preSeek, boolean play) {
        try {
            this.status = PLAYER_STATUS.Preparing;
            this.position = preSeek;
            this.bufferingPercent = 0;
            this.mediaPlayer.reset();
            setDataSource(this.currentSong, this.mediaPlayer);
            this.preparedListener.play = play;
            this.mediaPlayer.setOnPreparedListener(this.preparedListener);
            this.eventHandler.sendEmptyMessage(12);
            this.eventHandler.sendEmptyMessage(11);
            this.mediaPlayer.prepareAsync();
            this.mediaPlayer.setClockRateChangedListener(this.clockRateChangedListener);
            this.mediaPlayer.setOnBufferingUpdateListener(this.bufferingListener);
            this.mediaPlayer.setOnCompletionListener(this.completionListener);
            this.mediaPlayer.setOnErrorListener(this.errorListener);
        } catch (Exception unused) {
            unused.printStackTrace();
            mpRelease();
            this.status = PLAYER_STATUS.Error;
            this.eventHandler.sendEmptyMessage(16);
        }
    }

    private void setDataSource(SongItem song, AudioMediaPlayer player) throws IllegalStateException, SecurityException, IllegalArgumentException, IOException {
        player.setStreaming(false);
        if (playLocalFile(song)) {
            try {
                player.setDataSource(SynoContextProvider.get(), getPlayUri(song));
                return;
            } catch (Exception e) {
                player.setDataSource(getPlayPath(song, player));
                logGetPlayUriFailedEvent(song, e);
                return;
            }
        }
        player.setDataSource(getPlayPath(song, player));
    }

    private void logGetPlayUriFailedEvent(SongItem song, Exception e) {
        File parentFile = new SynoFile(DatabaseAccesser.getInstance().querySong(song).getCachePath()).getParentFile();
        CrashlyticsUtil.logException("SetDataSource", "SetDataSource failed: " + song.getCachePath() + Common.SZ_DATABASE_SEPARATOR + (parentFile != null ? PermissionUtils.checkGrantStatus(parentFile).isGranted() : false), e);
    }

    private void mpStart(final int preSeek) {
        if (PLAYER_STATUS.Initialized == this.status || PLAYER_STATUS.Paused == this.status || PLAYER_STATUS.End == this.status) {
            if (preSeek > 0) {
                this.mediaPlayer.seekTo(preSeek);
                mpRefreshPosition();
            }
            keepDebugData("mpStart");
            this.status = PLAYER_STATUS.Started;
            this.eventHandler.sendEmptyMessage(16);
            this.eventHandler.sendEmptyMessage(12);
            this.mediaPlayer.start();
            getPosition();
        }
    }

    private void keepDebugData(String key) {
        this.debugStartFunctionTag = key;
        this.debugStateBeforeStart = this.status.name();
    }

    public void logPlayerStatBeforeStartForeground(Throwable t) {
        CrashlyticsUtil.logException("FG", "Start from: " + this.debugStartFunctionTag + Common.SZ_DATABASE_SEPARATOR + this.debugStateBeforeStart, t);
    }

    private void mpStop() {
        if (PLAYER_STATUS.Released != this.status) {
            this.status = PLAYER_STATUS.End;
            setPosition(0);
            this.bufferingPercent = 0;
            this.currentSong = null;
            this.eventHandler.sendEmptyMessage(16);
            this.eventHandler.sendEmptyMessage(11);
            this.eventHandler.sendEmptyMessage(12);
            this.mediaPlayer.setClockRateChangedListener(null);
            this.mediaPlayer.setOnBufferingUpdateListener((AudioMediaPlayer.OnBufferingUpdateListener) null);
            this.mediaPlayer.setOnCompletionListener((AudioMediaPlayer.OnCompletionListener) null);
            this.mediaPlayer.setOnErrorListener((AudioMediaPlayer.OnErrorListener) null);
            try {
                this.mediaPlayer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            releaseNextPlayer();
            mpRelease();
        }
    }

    private void mpPause() {
        PLAYER_STATUS player_status = this.status;
        try {
            this.status = PLAYER_STATUS.Paused;
            this.eventHandler.sendEmptyMessage(16);
            if (isReleased() || !this.mediaPlayer.isPlaying()) {
                return;
            }
            this.mediaPlayer.pause();
        } catch (Throwable th) {
            CrashlyticsUtil.logException("mpPause", "Failed to pause from " + player_status + Common.SZ_DATABASE_SEPARATOR + this.statusWhenCompletion, th);
            throw th;
        }
    }

    private void mpRefreshPosition() {
        if (isPlaying()) {
            try {
                this.position = this.mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            this.commandHandler.removeMessages(6);
            this.commandHandler.sendEmptyMessageDelayed(6, REFRESH_POSITION_INTERVAL);
        }
    }

    private void mpSeek(final int position) {
        if (isPlaying()) {
            this.mediaPlayer.seekTo(position);
            this.eventHandler.sendEmptyMessage(16);
            mpRefreshPosition();
        }
    }

    private void mpSetVolume(final float vol) {
        SynoLog.d(LOG, "mpSetVolume : " + vol);
        if (isPlaying()) {
            this.mediaPlayer.setVolume(vol, vol);
        }
    }

    private void mpRelease() {
        SynoLog.i(LOG, "release");
        this.isInitialized = false;
        this.status = PLAYER_STATUS.Released;
        this.eventHandler.sendEmptyMessage(16);
        this.eventHandler.sendEmptyMessage(12);
        this.mediaPlayer.release();
    }

    @Override
    public SongItem getCurrentMedia() {
        return this.currentSong;
    }

    @Override
    public boolean isPlaying() {
        return PLAYER_STATUS.Started == this.status;
    }

    public boolean isPreparing() {
        return PLAYER_STATUS.Preparing == this.status || PLAYER_STATUS.Initialized == this.status;
    }

    public boolean isPaused() {
        return PLAYER_STATUS.Paused == this.status;
    }

    public boolean isIdle() {
        return PLAYER_STATUS.Idle == this.status;
    }

    public boolean isEnd() {
        return PLAYER_STATUS.End == this.status;
    }

    public boolean isReleased() {
        return PLAYER_STATUS.Released == this.status;
    }

    public boolean isError() {
        return PLAYER_STATUS.Error == this.status;
    }

    class MyOnPreparedListener implements AudioMediaPlayer.OnPreparedListener {
        public boolean play = true;

        MyOnPreparedListener() {
        }

        @Override
        public void onPrepared(final AudioMediaPlayer mplayer) {
            SynoLog.i(StreamingMediaPlayer.LOG, "onPrepared : duration : " + mplayer.getDuration());
            StreamingMediaPlayer.this.status = PLAYER_STATUS.Initialized;
            StreamingMediaPlayer.this.isInitialized = true;
            StreamingMediaPlayer streamingMediaPlayer = StreamingMediaPlayer.this;
            streamingMediaPlayer.position = streamingMediaPlayer.getPosition();
            if (StreamingMediaPlayer.this.nextStatus == NEXT_PLAYER_STATUS.PreparedWaiting) {
                StreamingMediaPlayer.this.mediaPlayer.setNextPlayer(StreamingMediaPlayer.this.nextPlayer);
                StreamingMediaPlayer.this.nextStatus = NEXT_PLAYER_STATUS.Prepared;
            }
            if (this.play) {
                StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(9);
            } else {
                StreamingMediaPlayer.this.status = PLAYER_STATUS.Paused;
            }
            StreamingMediaPlayer.this.eventHandler.sendEmptyMessage(16);
        }
    }

    public void setOnSeekCompleteListener(AudioMediaPlayer.OnSeekCompleteListener listener) {
        this.mediaPlayer.setOnSeekCompleteListener(listener);
    }

    public void setCurrentSong(SongItem song, final int preSeek, boolean play) {
        if (song == null) {
            SynoLog.e(LOG, "current song may not be null");
            return;
        }
        this.currentSong = song;
        this.isInitialized = false;
        Handler handler = this.commandHandler;
        handler.sendMessage(handler.obtainMessage(0, preSeek, 0, play));
    }

    public void setNextSong(SongItem song) {
        setNextSong(song, false);
    }

    public void setNextSong(SongItem song, Boolean onDownloadCompleted) {
        if (this.currentSong == null) {
            SynoLog.i(LOG, "current has no song to play");
            return;
        }
        if (song == null) {
            SynoLog.i(LOG, "set next song null");
            this.nextSong = null;
            this.commandHandler.sendEmptyMessage(9);
        } else {
            if (song.equals(this.nextSong) && !onDownloadCompleted) {
                SynoLog.i(LOG, "already has the same next song : " + song.getFilePath());
                return;
            }
            SynoLog.i(LOG, "set next song : " + song.getFilePath());
            if (this.nextStatus != NEXT_PLAYER_STATUS.UnInitialized) {
                this.commandHandler.sendEmptyMessage(9);
            }
            this.nextSong = song;
            this.commandHandler.sendEmptyMessage(8);
        }
    }

    private void releaseNextPlayer() {
        SynoLog.i(LOG, "releaseNextPlayer");
        if (this.mediaPlayer.hasNextPlayer()) {
            this.mediaPlayer.setNextPlayer(null);
        }
        AudioMediaPlayer audioMediaPlayer = this.nextPlayer;
        if (audioMediaPlayer != null) {
            audioMediaPlayer.setOnErrorListener((AudioMediaPlayer.OnErrorListener) null);
            this.nextPlayer.reset();
            this.nextPlayer.release();
            this.nextPlayer = null;
        }
        this.nextStatus = NEXT_PLAYER_STATUS.UnInitialized;
    }

    private void mpSetNextData() {
        String str = LOG;
        SynoLog.d(str, "mpSetNextData");
        if (this.nextSong == null) {
            this.nextStatus = NEXT_PLAYER_STATUS.UnInitialized;
            SynoLog.i(str, "next song is null");
            return;
        }
        AudioMediaPlayer audioMediaPlayer = this.mediaPlayer;
        if (audioMediaPlayer != null && audioMediaPlayer.isDownloading()) {
            this.nextStatus = NEXT_PLAYER_STATUS.Waiting;
            SynoLog.i(str, "current song is downloading");
            return;
        }
        this.nextStatus = NEXT_PLAYER_STATUS.Preparing;
        try {
            AudioMediaPlayer audioMediaPlayer2 = new AudioMediaPlayer();
            this.nextPlayer = audioMediaPlayer2;
            audioMediaPlayer2.setWakeMode(this.mainContext, 1);
            setDataSource(this.nextSong, this.nextPlayer);
            this.nextPlayer.setOnPreparedListener((AudioMediaPlayer.OnPreparedListener) mplayer -> {
                SynoLog.i(StreamingMediaPlayer.LOG, "next song onPrepared : duration : " + mplayer.getDuration());
                if (!StreamingMediaPlayer.this.isInitialized) {
                    StreamingMediaPlayer.this.nextStatus = NEXT_PLAYER_STATUS.PreparedWaiting;
                } else {
                    StreamingMediaPlayer.this.mediaPlayer.setNextPlayer(mplayer);
                    StreamingMediaPlayer.this.nextStatus = NEXT_PLAYER_STATUS.Prepared;
                }
            });
            this.nextPlayer.setOnErrorListener((AudioMediaPlayer.OnErrorListener) (mplayer, what, extra) -> {
                SynoLog.e(StreamingMediaPlayer.LOG, "nextPlayer onError : what : " + what + " , extra : " + extra);
                StreamingMediaPlayer.this.releaseNextPlayer();
                return true;
            });
            this.nextPlayer.prepareAsync();
        } catch (Exception unused) {
            releaseNextPlayer();
        }
    }

    private boolean playLocalFile(final SongItem song) {
        if (song == null) {
            SynoLog.d(LOG, "metadata null");
            return false;
        }
        boolean zCheckHasSongCachedAndPassQuality = Utilities.checkHasSongCachedAndPassQuality(song);
        SynoLog.d(LOG, "isCacheSongPassQuality: " + zCheckHasSongCachedAndPassQuality);
        return zCheckHasSongCachedAndPassQuality;
    }

    private Uri getPlayUri(final SongItem song) {
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        SongItem songItemQuerySong = databaseAccesser.querySong(song);
        SynoFile synoFile = new SynoFile(songItemQuerySong.getCachePath());
        databaseAccesser.hitSong(songItemQuerySong, songItemQuerySong.getHitCount() + 1);
        databaseAccesser.close();
        SynoLog.d(LOG, "getPlayLocalFilePath : " + synoFile.getPath());
        return synoFile.getUri(true);
    }

    private String getPlayPath(final SongItem song, final AudioMediaPlayer player) {
        String playUrl = ".";
        if (song == null) {
            return ".";
        }
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        if (!Common.isLogin() || !Common.getDsId().equals(song.getDsId())) {
            if (player.equals(this.mediaPlayer)) {
                Message messageObtainMessage = this.eventHandler.obtainMessage(8);
                messageObtainMessage.obj = this.mainContext.getResources().getString(R.string.msg_file_not_fount) + StringUtils.SPACE + song.getTitle();
                this.eventHandler.sendMessage(messageObtainMessage);
            }
        } else if (Common.getSongCacheFolder() == null || AudioPreference.getSongCacheLimit() == 0 || song.isRadio()) {
            player.setStreaming(true);
            playUrl = ConnectionManager.getPlayUrl(song);
        } else {
            Utilities.removeCachedFile(song);
            PreDownloader preDownloader = new PreDownloader(this.mainContext, song, new OnSongDownloadedListener());
            preDownloader.start();
            StreamProxy streamProxy = new StreamProxy();
            streamProxy.init(preDownloader);
            streamProxy.start();
            player.setProxy(streamProxy);
            player.setDownloader(preDownloader);
            playUrl = streamProxy.getUrl() + getUrlExt(song);
        }
        databaseAccesser.close();
        SynoLog.d(LOG, "getPlayPath : " + playUrl);
        return playUrl;
    }

    private String getUrlExt(SongItem song) {
        TranscodeSetting.TranscodeFormat format = AudioPreference.getTranscodeSetting().getFormat();
        boolean zIsStreamAudio = Utilities.isStreamAudio(song, false);
        if (song.hasHttpURL()) {
            return "00." + song.getFormat();
        }
        if (!zIsStreamAudio && Common.getTranscodeType().supportTranscoding()) {
            if (Common.getTranscodeType().supportMP3() && format.isMp3()) {
                return "00.mp3";
            }
            return "00.wav";
        }
        return "00." + Utilities.getExt(song.getFilePath());
    }

    private class OnSongDownloadedListener implements PreDownloader.OnDownloadCompletedListener {
        private OnSongDownloadedListener() {
        }

        @Override
        public void onDownloadCompleted(SongItem songItem) {
            SynoLog.d(StreamingMediaPlayer.LOG, "onDownloadCompleted");
            if (StreamingMediaPlayer.this.nextStatus == NEXT_PLAYER_STATUS.Waiting) {
                StreamingMediaPlayer.this.commandHandler.sendEmptyMessage(8);
            }
            if (songItem.isFile()) {
                DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
                SongItem songItemQuerySong = databaseAccesser != null ? databaseAccesser.querySong(songItem) : null;
                if (songItemQuerySong != null && songItemQuerySong.equals(StreamingMediaPlayer.this.currentSong)) {
                    databaseAccesser.hitSong(songItemQuerySong, songItemQuerySong.getHitCount() + 1);
                }
                databaseAccesser.close();
                if (songItem.equals(StreamingMediaPlayer.this.nextSong)) {
                    StreamingMediaPlayer.this.setNextSong(songItem, true);
                }
            }
        }
    }

    public void notifyLogout() {
        AudioMediaPlayer audioMediaPlayer = this.mediaPlayer;
        if (audioMediaPlayer != null && (audioMediaPlayer.isDownloading() || this.mediaPlayer.isStreaming())) {
            this.eventHandler.sendEmptyMessage(14);
        }
        AudioMediaPlayer audioMediaPlayer2 = this.nextPlayer;
        if (audioMediaPlayer2 != null) {
            if (audioMediaPlayer2.isDownloading() || this.nextPlayer.isStreaming()) {
                setNextSong(null);
            }
        }
    }

    public boolean isDownloading(SongItem songitem) {
        SongItem songItem;
        SongItem songItem2;
        AudioMediaPlayer audioMediaPlayer = this.mediaPlayer;
        if (audioMediaPlayer != null && audioMediaPlayer.isDownloading() && (songItem2 = this.currentSong) != null && songItem2.equals(songitem)) {
            return true;
        }
        AudioMediaPlayer audioMediaPlayer2 = this.nextPlayer;
        return audioMediaPlayer2 != null && audioMediaPlayer2.isDownloading() && (songItem = this.nextSong) != null && songItem.equals(songitem);
    }
}