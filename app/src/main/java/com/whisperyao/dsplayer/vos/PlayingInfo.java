package com.whisperyao.dsplayer.vos;

import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;

import java.util.Map;


public abstract class PlayingInfo {

    public static final int STOP_INDEX_NONE = -1;

    public static class DeviceNotFoundException extends Exception {
    }

    public static class NextworkException extends Exception {
    }

    public abstract long getAacTimeStamp();

    public abstract String getArtist();

    public abstract String getID();

    public abstract int getPlayPos();

    public abstract int getPosition();

    public abstract Common.RepeatMode getRepeatMode();

    public abstract Common.ShuffleMode getShuffleMode();

    public abstract int getStopIndex();

    public abstract Map<String, Integer> getSubPlayerVolume();

    public abstract long getTimeStamp();

    public abstract String getTitle();

    public abstract int getVolume();

    public abstract boolean isPause();

    public abstract boolean isPlaying();

    public abstract boolean isPreparing();

    public abstract boolean isStop();

    public abstract boolean needRefresh(long lastRefresh);

    public static PlayingInfo getDummyInfo() {
        ApiPlayingInfo apiPlayingInfo = new ApiPlayingInfo();
        apiPlayingInfo.index = -1;
        apiPlayingInfo.stop_index = -1;
        apiPlayingInfo.state = "stopped";
        apiPlayingInfo.position = 0;
        apiPlayingInfo.volume = 0;
        apiPlayingInfo.playlist_timestamp = 0L;
        apiPlayingInfo.aac_skip_timestamp = -1L;
        return apiPlayingInfo;
    }

    public boolean equalPlayState(PlayingInfo compareInfo) {
        return (compareInfo.isPlaying() && isPlaying()) || (compareInfo.isPreparing() && isPreparing()) || ((compareInfo.isPause() && isPause()) || (compareInfo.isStop() && isStop()));
    }

    public boolean equalMetaData(PlayingInfo compareInfo) {
        return compareInfo.getRepeatMode().equals(getRepeatMode()) && compareInfo.getShuffleMode().equals(getShuffleMode()) && compareInfo.getVolume() == getVolume() && compareInfo.getTitle().equals(getTitle()) && (!ConnectionManager.isUseWebAPI() ? compareInfo.getPosition() != getPosition() : !compareInfo.getID().equals(getID()));
    }

    public boolean equalPlayInfo(PlayingInfo compareInfo) {
        return compareInfo.getTitle().equals(getTitle()) && (!ConnectionManager.isUseWebAPI() ? compareInfo.getPosition() != getPosition() : !compareInfo.getID().equals(getID())) && compareInfo.getPlayPos() == getPlayPos();
    }
}