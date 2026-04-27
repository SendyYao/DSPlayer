package com.whisperyao.dsplayer.vos;

import com.whisperyao.dsplayer.Common;
import java.util.HashMap;
import java.util.Map;

public class CgiPlayingInfo extends PlayingInfo {
    String Artist;
    int CurrentIndex;
    int Duration;
    int PlayerState;
    int Position;
    int RefreshFlag;
    int Repeat;
    int Shuffle;
    String SongPath;
    String Title;
    int Volume;

    @Override
    public long getAacTimeStamp() {
        return 0L;
    }

    @Override
    public int getStopIndex() {
        return -1;
    }

    @Override
    public long getTimeStamp() {
        return 0L;
    }

    @Override
    public boolean isPreparing() {
        return false;
    }

    @Override
    public Common.RepeatMode getRepeatMode() {
        return Common.RepeatMode.Companion.fromId(this.Repeat);
    }

    @Override
    public Common.ShuffleMode getShuffleMode() {
        return Common.ShuffleMode.Companion.fromId(this.Shuffle);
    }

    @Override
    public int getVolume() {
        return this.Volume;
    }

    @Override
    public Map<String, Integer> getSubPlayerVolume() {
        return new HashMap();
    }

    @Override
    public int getPosition() {
        return this.Position;
    }

    @Override
    public int getPlayPos() {
        return this.CurrentIndex;
    }

    @Override
    public String getID() {
        return this.Title;
    }

    @Override
    public String getTitle() {
        return this.Title;
    }

    @Override
    public boolean isPlaying() {
        return this.PlayerState == 2;
    }

    @Override
    public boolean isPause() {
        return this.PlayerState == 3;
    }

    @Override
    public boolean isStop() {
        return this.PlayerState == 0;
    }

    @Override
    public boolean needRefresh(long lastRefresh) {
        return this.RefreshFlag > 0;
    }

    @Override
    public String getArtist() {
        return this.Artist;
    }
}