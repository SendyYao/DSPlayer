package com.whisperyao.dsplayer.datasource.network.vo.base;

import com.whisperyao.dsplayer.ConnectionManager;

import java.util.List;

public interface BaseAudioInfoVo {
    int getBuildVer();

    String getDSid();

    int getMajorVer();

    int getMinorVer();

    int getPlayingQueueMax();

    ConnectionManager.ResourceType getServerType();

    List<String> getTranscode();

    boolean haveRemotePlayer();

    boolean isSuccess();

    boolean permitPlaylist();

    boolean permitPublicSharing();

    boolean permitStream();
}
