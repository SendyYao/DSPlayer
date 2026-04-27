package com.whisperyao.dsplayer.vos.base;

import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;

import java.util.List;

public abstract class BaseRemotePlayerResponseVo extends BaseVo {

    public static abstract class BaseRemotePlayerVo {

        public enum BaseRemotePlayerType {
            usb,
            bluetooth,
            upnp,
            airplay,
            unknown
        }

        public abstract String getId();

        public abstract String getName();

        public abstract int getPlayerIndex();

        public abstract BaseRemotePlayerType getPlayerType();

        public abstract List<? extends BaseRemotePlayerVo> getSubPlayerList();

        public abstract boolean isGroupPlayer();

        public abstract boolean isPasswordProtected();

        public abstract boolean isUsbSpeaker();

        public abstract boolean supportSeek();

        public abstract boolean supportSetVolume();
    }

    public abstract List<? extends BaseRemotePlayerVo> getRemotePlayerList();
}
