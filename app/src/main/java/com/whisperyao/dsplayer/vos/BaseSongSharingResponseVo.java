package com.whisperyao.dsplayer.vos;

import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;

public abstract class BaseSongSharingResponseVo extends BaseVo {
    public static final BaseSongSharingResponseVo DefaultBaseSongSharingResponseVo = new BaseSongSharingResponseVo() { // from class: com.synology.dsaudio.vos.BaseSongSharingResponseVo.1
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public String getUrl() {
            return "";
        }
    };

    public abstract String getUrl();

    public abstract boolean isEnabled();
}
