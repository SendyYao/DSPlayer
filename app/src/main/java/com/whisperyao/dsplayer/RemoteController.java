package com.whisperyao.dsplayer;

import com.whisperyao.dsplayer.item.RendererItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.vos.PlayingInfo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class RemoteController {
    private static RemoteController sRemote;

    protected abstract void control_clearQueue();

    protected abstract List<RendererItem> control_doEnumRenderer();

    protected abstract PlayingInfo control_doPollingStatus() throws PlayingInfo.DeviceNotFoundException, PlayingInfo.NextworkException;

    protected abstract void control_enqueue(String idList, Common.PlaybackAction action, int position, boolean play);

    protected abstract LinkedList<SongItem> control_getPlayingQueue();

    protected abstract LinkedList<SongItem> control_getPlayingQueue(int offset, int limit);

    protected abstract int control_getQueueSize();

    protected abstract void control_jumpPlay(int pos);

    protected abstract void control_next();

    protected abstract void control_pause();

    protected abstract void control_play();

    protected abstract void control_prev();

    protected abstract SongItem control_reloadSong(int playpos);

    protected abstract void control_removeTracks(LinkedList<SongItem> songlist, Integer[] list, int newPos);

    protected abstract void control_seek(long pos);

    protected abstract void control_setGroupPlayer(String groupPlayerId, List<String> subPlayersIds);

    protected abstract void control_setMultiVolume(Map<String, Integer> subplayerVolumes);

    protected abstract void control_setRepeatMode(Common.RepeatMode mode);

    protected abstract void control_setShuffleMode(Common.ShuffleMode mode);

    protected abstract void control_setVolume(int volume);

    protected abstract void control_stop();

    protected abstract void control_updateTracks(LinkedList<SongItem> songlist, int start, int limit, int[] list, int newPos);

    protected abstract ConnectionManager.ResourceType getResourceType();

    public static LinkedList<SongItem> getPlayingQueue() {
        valitadeController();
        return sRemote.control_getPlayingQueue();
    }

    public static LinkedList<SongItem> getPlayingQueue(int offset, int limit) {
        valitadeController();
        return sRemote.control_getPlayingQueue(offset, limit);
    }

    public static int getQueueSize() {
        valitadeController();
        return sRemote.control_getQueueSize();
    }

    public static void play() {
        valitadeController();
        sRemote.control_play();
    }

    public static void pause() {
        valitadeController();
        sRemote.control_pause();
    }

    public static void next() {
        valitadeController();
        sRemote.control_next();
    }

    public static void prev() {
        valitadeController();
        sRemote.control_prev();
    }

    public static void stop() {
        valitadeController();
        sRemote.control_stop();
    }

    public static void seek(long pos) {
        valitadeController();
        sRemote.control_seek(pos);
    }

    public static void setRepeatMode(Common.RepeatMode mode) {
        valitadeController();
        sRemote.control_setRepeatMode(mode);
    }

    public static void setShuffleMode(Common.ShuffleMode mode) {
        valitadeController();
        sRemote.control_setShuffleMode(mode);
    }

    public static void setVolume(int volume) {
        valitadeController();
        sRemote.control_setVolume(volume);
    }

    public static void setMultiVolume(Map<String, Integer> subplayerVolumes) {
        valitadeController();
        sRemote.control_setMultiVolume(subplayerVolumes);
    }

    public static void setGroupPlayer(String playerId, List<String> subPlayersIds) {
        valitadeController();
        sRemote.control_setGroupPlayer(playerId, subPlayersIds);
    }

    public static void clearQueue() {
        valitadeController();
        sRemote.control_clearQueue();
    }

    public static void removeTracks(LinkedList<SongItem> songlist, Integer[] list, int newPos) {
        valitadeController();
        sRemote.control_removeTracks(songlist, list, newPos);
    }

    public static void updateTracks(LinkedList<SongItem> songlist, int start, int limit, int[] list, int newPos) {
        valitadeController();
        sRemote.control_updateTracks(songlist, start, limit, list, newPos);
    }

    public static void jumpPlay(int pos) {
        valitadeController();
        sRemote.control_jumpPlay(pos);
    }

    public static void enqueue(String idList, Common.PlaybackAction action, int position, boolean play) {
        valitadeController();
        sRemote.control_enqueue(idList, action, position, play);
    }

    public static SongItem reloadSong(int playpos) {
        valitadeController();
        return sRemote.control_reloadSong(playpos);
    }

    public static PlayingInfo doPollingStatus() throws PlayingInfo.DeviceNotFoundException, PlayingInfo.NextworkException {
        valitadeController();
        return sRemote.control_doPollingStatus();
    }

    public static List<RendererItem> doEnumRenderer() {
        valitadeController();
        return sRemote.control_doEnumRenderer();
    }

    private static void valitadeController() {
        if (sRemote != null && !ConnectionManager.getResourceType().equals(sRemote.getResourceType())) {
            sRemote = null;
        }
        if (sRemote == null) {
            if (ConnectionManager.isUseWebAPI()) {
                sRemote = new ApiRemoteController();
            } else {
                sRemote = new CgiRemoteController();
            }
        }
    }
}
