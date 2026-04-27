package com.whisperyao.dsplayer.mediasession;

import com.whisperyao.dsplayer.item.SongItem;

public abstract class PlayerAdapter {
    public abstract long getBufferingPercent();

    public abstract SongItem getCurrentMedia();

    public abstract boolean isPlaying();
}