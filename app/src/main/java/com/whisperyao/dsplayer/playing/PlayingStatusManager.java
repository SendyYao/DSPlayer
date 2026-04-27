package com.whisperyao.dsplayer.playing;

import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;

import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.synology.ThreadWork;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.RemoteController;
import com.whisperyao.dsplayer.ServiceOperator;
import com.whisperyao.dsplayer.item.RendererItem;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.widget.SleepTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import jakarta.inject.Inject;

public class PlayingStatusManager {
    private static final String LOG = "PlayingStatusManager";
    private static final Player sLocalPlayer = Player.generateLocalInstance();
    private int currentVolume;
    private final ChromeCastHelper mChromeCastHelper;
    public Subject<Boolean> switchPlayer = PublishSubject.create();
    private Player mPlayer = Player.generateLocalInstance();
    private PlayerVolume mCurrentPlayingStatus = PlayerVolume.getDummyInstance();
    private final List<Player> mPlayerLists = new ArrayList<>();
    private final List<Player> mPlayersForRemotePlayer = new ArrayList<>();
    private final List<Player> mPlayersForChromeCast = new ArrayList<>();
    private final List<OnPlayerLocalityChangedObserver> mOnPlayerLocalityChangedObserverList = new ArrayList<>();
    private final List<OnPlayerStatusChangedObserver> mOnPlayerStatusChangedObserverList = new ArrayList<>();
    private SleepTimer mSleepTimer = null;
    private final List<PlayerSetObserver> mPlayerSetObservers = new ArrayList<>();

    public interface LoadPlayerCallback {
        void onPostLoad();

        void onPreLoad();
    }

    public interface OnPlayerLocalityChangedObserver {
        void onPlayerLocalityChanged();
    }

    public interface OnPlayerStatusChangedObserver {
        void onPlayerStatusChanged();
    }

    public interface PlayerSetObserver {
        void onPlayerChange(Player player);

        void onPlayerSetChanged();
    }

    public enum PLAY_MODE {
        STREAMING,
        RENDERER,
        CHROMECAST;

        public boolean isStreaming() {
            return STREAMING.equals(this);
        }

        public boolean isRenderer() {
            return RENDERER.equals(this);
        }

        public boolean isChromeCast() {
            return CHROMECAST.equals(this);
        }
    }

    public void setCurrentVolume(int v) {
        this.currentVolume = v;
    }

    public int getCurrentVolume() {
        return this.currentVolume;
    }

    @Inject
    public PlayingStatusManager(ChromeCastHelper chromeCastHelper) {
        this.mChromeCastHelper = chromeCastHelper;
        chromeCastHelper.setOnRouteSetChangedListener(() -> {
            loadChromeCast();
            mergeAllPlayers();
        });
    }


    public MediaRouter getMediaRouter() {
        return this.mChromeCastHelper.getRouter();
    }

    public MediaRouteSelector getMediaRouteSelector() {
        return this.mChromeCastHelper.getSelector();
    }

    public static class PlayerVolume {
        private static final String EXTRA_SUBPLAYES_VOLUMES = "subplayers_volumes";
        private static final String EXTRA_VOLUME = "volume";
        private boolean mIsDummy;
        private Map<String, Integer> mSubPlayersVolumes;
        private int mVolume;

        public static PlayerVolume getDummyInstance() {
            PlayerVolume playerVolume = new PlayerVolume();
            playerVolume.mIsDummy = true;
            return playerVolume;
        }

        public static PlayerVolume fromBundle(Bundle bundle) {
            if (bundle == null) {
                return getDummyInstance();
            }
            PlayerVolume playerVolume = new PlayerVolume();
            playerVolume.mIsDummy = false;
            playerVolume.mVolume = bundle.getInt("volume", 0);
            if (bundle.containsKey(EXTRA_SUBPLAYES_VOLUMES)) {
                HashMap map = new HashMap();
                Bundle bundle2 = bundle.getBundle(EXTRA_SUBPLAYES_VOLUMES);
                for (String str : bundle2.keySet()) {
                    map.put(str, Integer.valueOf(bundle2.getInt(str)));
                }
                playerVolume.mSubPlayersVolumes = map;
            }
            return playerVolume;
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt("volume", this.mVolume);
            Map<String, Integer> map = this.mSubPlayersVolumes;
            if (map != null && map.size() > 0) {
                Bundle bundle2 = new Bundle();
                for (String str : this.mSubPlayersVolumes.keySet()) {
                    bundle2.putInt(str, this.mSubPlayersVolumes.get(str).intValue());
                }
                bundle.putBundle(EXTRA_SUBPLAYES_VOLUMES, bundle2);
            }
            return bundle;
        }

        public boolean isDummy() {
            return this.mIsDummy;
        }

        public int getVolume() {
            return this.mVolume;
        }

        public void setVolume(int volume) {
            this.mVolume = volume;
        }

        public Map<String, Integer> getSubPlayersVolumes() {
            return this.mSubPlayersVolumes;
        }

        public void setSubPlayersVolumes(Map<String, Integer> subPlayersVolumes) {
            this.mSubPlayersVolumes = subPlayersVolumes;
        }
    }

    public static Player getLocalPlayer() {
        return sLocalPlayer;
    }

    public void requestLoadPlayers(final LoadPlayerCallback callbacks) {
        new ThreadWork() {
            @Override
            public void preWork() {
                LoadPlayerCallback loadPlayerCallback = callbacks;
                if (loadPlayerCallback != null) {
                    loadPlayerCallback.onPreLoad();
                }
                PlayingStatusManager.this.loadChromeCast();
            }

            @Override
            public void onWorking() {
                PlayingStatusManager.this.loadRemotePlayers();
            }

            @Override
            public void postWork() {
                PlayingStatusManager.this.mergeAllPlayers();
                LoadPlayerCallback loadPlayerCallback = callbacks;
                if (loadPlayerCallback != null) {
                    loadPlayerCallback.onPostLoad();
                }
            }
        }.startWork();
    }

    private void loadRemotePlayers() {
        ArrayList<Player> arrayList = new ArrayList<>();
        for (RendererItem rendererItem : RemoteController.doEnumRenderer()) {
            arrayList.add(Player.generateRendererInstance(rendererItem));
        }
        synchronized (this.mPlayersForRemotePlayer) {
            this.mPlayersForRemotePlayer.clear();
            this.mPlayersForRemotePlayer.addAll(arrayList);
        }
    }

    public void loadChromeCast() {
        if (Common.isLogin()) {
            this.mChromeCastHelper.loadRoutes();
            synchronized (this.mPlayersForChromeCast) {
                this.mPlayersForChromeCast.clear();
                for (MediaRouter.RouteInfo routeInfo : this.mChromeCastHelper.getLoadedRoutes()) {
                    this.mPlayersForChromeCast.add(Player.generateChromeCastInstance(routeInfo));
                }
            }
            return;
        }
        synchronized (this.mPlayersForChromeCast) {
            this.mPlayersForChromeCast.clear();
        }
    }

    private void mergeAllPlayers() {
        ArrayList<Player> arrayList = new ArrayList<>();
        arrayList.clear();
        arrayList.add(sLocalPlayer);
        synchronized (this.mPlayersForRemotePlayer) {
            arrayList.addAll(this.mPlayersForRemotePlayer);
        }
        synchronized (this.mPlayersForChromeCast) {
            arrayList.addAll(this.mPlayersForChromeCast);
        }
        synchronized (this.mPlayerLists) {
            this.mPlayerLists.clear();
            this.mPlayerLists.addAll(arrayList);
            int iIndexOf = this.mPlayerLists.indexOf(this.mPlayer);
            if (iIndexOf >= 0) {
                this.mPlayer = this.mPlayerLists.get(iIndexOf);
            } else {
                SynoLog.d(LOG, String.format("The old player %s is not found in the new player list.", this.mPlayer.toString()));
            }
        }
        onPlayerListChanged();
    }

    public void registerPlayerSetChanged(PlayerSetObserver observer) {
        this.mPlayerSetObservers.add(observer);
    }

    public void unregisterPlayerSetChanged(PlayerSetObserver observer) {
        this.mPlayerSetObservers.remove(observer);
    }

    private void notifyDataSetChanged() {
        for (PlayerSetObserver mPlayerSetObserver : this.mPlayerSetObservers) {
            mPlayerSetObserver.onPlayerSetChanged();
        }
    }

    private void notifyPlayerChanged(Player player) {
        for (PlayerSetObserver mPlayerSetObserver : this.mPlayerSetObservers) {
            mPlayerSetObserver.onPlayerChange(player);
        }
    }

    public Player getPlayer() {
        return this.mPlayer;
    }

    public Player findPlayer(String playerId) {
        for (Player player : this.mPlayerLists) {
            if (player.getUniqueId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    public void loadLastPlayer() throws Exception {
        this.mPlayer = loadLastPlayerInternal();
    }

    private Player loadLastPlayerInternal() {
        String playerMode = AudioPreference.getPlayerMode(PLAY_MODE.STREAMING.name());
        String playerId = AudioPreference.getPlayerId(null);
        try {
            if (!TextUtils.isEmpty(playerMode) && !TextUtils.isEmpty(playerId)) {
                PLAY_MODE play_modeValueOf = PLAY_MODE.valueOf(playerMode);
                if (play_modeValueOf.isChromeCast()) {
                    loadChromeCast();
                    Player playerGenerateUnknownInstance = Player.generateUnknownInstance(play_modeValueOf, playerId);
                    ArrayList<Player> arrayList = new ArrayList<>();
                    for (MediaRouter.RouteInfo routeInfo : this.mChromeCastHelper.getLoadedRoutes()) {
                        arrayList.add(Player.generateChromeCastInstance(routeInfo));
                    }
                    int iIndexOf = arrayList.indexOf(playerGenerateUnknownInstance);
                    if (iIndexOf == -1) {
                        throw new Exception("Cannot recover ChromeCast link");
                    }
                    return arrayList.get(iIndexOf);
                }
            }
        } catch (Exception ignored) {
        }
        return Player.generateLocalInstance();
    }

    public void setPlayer(final Player player) {
        if (!isCurrentPlayer(player)) {
            ServiceOperator.clearRemoteSongItem();
            boolean z = this.mPlayer.isRemotePlayer() != player.isRemotePlayer();
            this.mPlayer = player;
            notifyPlayerChanged(player);
            AudioPreference.setPlayerMode(player.getModeName());
            AudioPreference.setPlayerId(player.getUniqueId());
            if (z) {
                notifyPlayerLocalityChanged();
            }
        }
    }

    public boolean isCurrentPlayer(Player player) {
        return this.mPlayer.equals(player);
    }

    public List<Player> getPlayers() {
        return this.mPlayerLists;
    }

    private void onPlayerListChanged() {
        notifyDataSetChanged();
    }

    public List<String> getPlayerNameList() {
        ArrayList<String> arrayList = new ArrayList<>();
        for (Player mPlayerList : this.mPlayerLists) {
            arrayList.add(mPlayerList.getName());
        }
        return arrayList;
    }

    public void registerOnPlayerLocalityChangedObserver(OnPlayerLocalityChangedObserver observer) {
        this.mOnPlayerLocalityChangedObserverList.add(observer);
    }

    public void unregisterOnPlayerLocalityChangedObserver(OnPlayerLocalityChangedObserver observer) {
        this.mOnPlayerLocalityChangedObserverList.remove(observer);
    }

    private void notifyPlayerLocalityChanged() {
        for (OnPlayerLocalityChangedObserver onPlayerLocalityChangedObserver : this.mOnPlayerLocalityChangedObserverList) {
            onPlayerLocalityChangedObserver.onPlayerLocalityChanged();
        }
    }

    public void registerOnPlayerStatusChangedObserver(OnPlayerStatusChangedObserver observer) {
        this.mOnPlayerStatusChangedObserverList.add(observer);
    }

    public void unregisterOnPlayerStatusChangedObserver(OnPlayerStatusChangedObserver observer) {
        this.mOnPlayerStatusChangedObserverList.remove(observer);
    }

    private void notifyOnPlayerStatusChanged() {
        for (OnPlayerStatusChangedObserver onPlayerStatusChangedObserver : this.mOnPlayerStatusChangedObserverList) {
            onPlayerStatusChangedObserver.onPlayerStatusChanged();
        }
    }

    public PLAY_MODE getPlayMode() {
        return getPlayer().getPlayMode();
    }

    public boolean isPlayModeStreaming() {
        return getPlayMode().isStreaming();
    }

    public boolean isPlayModeRenderer() {
        return getPlayMode().isRenderer();
    }

    public boolean isPlayModeChromeCast() {
        return getPlayMode().isChromeCast();
    }

    public boolean isRemotePlayer() {
        return this.mPlayer.isRemotePlayer();
    }

    public String getPlayerUniqueId() {
        return this.mPlayer.getUniqueId();
    }

    public String getPlayerName() {
        return this.mPlayer.getName();
    }

    public int getPlayerIndex() {
        if (this.mPlayer.getRenderer() == null) {
            return -1;
        }
        return this.mPlayer.getRenderer().getIndex();
    }

    public void setPlayerInfoStreaming() {
        setPlayer(sLocalPlayer);
    }

    public void setCurrentPlayingStatus(PlayerVolume playingStatus) {
        this.mCurrentPlayingStatus = playingStatus;
        setCurrentVolume(playingStatus.getVolume());
    }

    public PlayerVolume getCurrentPlayingStatus() {
        return this.mCurrentPlayingStatus;
    }

    public void setSleepTimer(int seconds, final MediaControllerCompat controller) {
        SleepTimer sleepTimer = this.mSleepTimer;
        if (sleepTimer != null) {
            sleepTimer.cancel();
            this.mSleepTimer = null;
        }
        if (seconds == 0) {
            notifyOnPlayerStatusChanged();
            return;
        }
        SleepTimer sleepTimer2 = new SleepTimer(seconds, new SleepTimer.SleepTimerCallback() {
            @Override
            public void onTick() {
                PlayingStatusManager.this.notifyOnPlayerStatusChanged();
            }

            @Override
            public void onFinish() {
                MediaControllerCompat mediaControllerCompat = controller;
                if (mediaControllerCompat != null) {
                    mediaControllerCompat.getTransportControls().pause();
                }
                PlayingStatusManager.this.notifyOnPlayerStatusChanged();
            }
        });
        this.mSleepTimer = sleepTimer2;
        sleepTimer2.start();
    }

    public long getSleepTimerRestTime() {
        SleepTimer sleepTimer = this.mSleepTimer;
        if (sleepTimer != null) {
            return sleepTimer.getRestSeconds();
        }
        return 0L;
    }
}