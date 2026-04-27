package com.whisperyao.dsplayer.vos;

import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.item.SongItem;

import io.reactivex.rxjava3.annotations.SchedulerSupport;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ApiPlayingInfo extends PlayingInfo {
    PlayModeVo play_mode;
    long playlist_timestamp;
    int position;
    SongVo song;
    String state;
    Map<String, Integer> subplayer_volume;
    int volume;
    long aac_skip_timestamp = 0;
    int index = 0;
    int stop_index = -1;

    private class PlayModeVo {
        String repeat;
        boolean shuffle;

        private PlayModeVo() {
        }
    }

    private class SongVo {
        String id;
        String title;

        private SongVo() {
        }
    }

    @Override
    public Common.RepeatMode getRepeatMode() {
        PlayModeVo playModeVo = this.play_mode;
        if (playModeVo == null) {
            return Common.RepeatMode.NONE;
        }
        return Common.RepeatMode.valueOf(playModeVo.repeat.toUpperCase(Locale.getDefault()));
    }

    @Override
    public Common.ShuffleMode getShuffleMode() {
        PlayModeVo playModeVo = this.play_mode;
        if (playModeVo == null) {
            return Common.ShuffleMode.NONE;
        }
        if (playModeVo.shuffle) {
            return Common.ShuffleMode.AUTO;
        }
        return Common.ShuffleMode.NONE;
    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    @Override
    public Map<String, Integer> getSubPlayerVolume() {
        Map<String, Integer> map = this.subplayer_volume;
        return map != null ? map : new HashMap();
    }

    @Override
    public int getPosition() {
        return this.position;
    }

    @Override
    public int getPlayPos() {
        return this.index;
    }

    @Override
    public long getTimeStamp() {
        return this.playlist_timestamp;
    }

    @Override
    public String getID() {
        SongVo songVo = this.song;
        return (songVo == null || songVo.id == null) ? "" : this.song.id;
    }

    @Override
    public String getTitle() {
        SongVo songVo = this.song;
        return (songVo == null || songVo.title == null) ? "" : this.song.title;
    }

    @Override
    public boolean isPlaying() {
        return this.state.equalsIgnoreCase("playing");
    }

    @Override
    public boolean isPreparing() {
        return this.state.equalsIgnoreCase("transitioning") || this.state.equalsIgnoreCase("waiting");
    }

    @Override
    public boolean isPause() {
        return this.state.equalsIgnoreCase("pause");
    }

    @Override
    public boolean isStop() {
        // MediaRouteProviderProtocol.SERVICE_DATA_ERROR
        return this.state.equalsIgnoreCase("stopped") || this.state.equalsIgnoreCase(SchedulerSupport.NONE) || this.state.equalsIgnoreCase("error");
    }

    @Override
    public boolean needRefresh(long lastRefresh) {
        return this.playlist_timestamp > lastRefresh;
    }

    @Override
    public String getArtist() {
        return "";
    }

    @Override
    public int getStopIndex() {
        return this.stop_index;
    }

    @Override
    public long getAacTimeStamp() {
        return this.aac_skip_timestamp;
    }
}
