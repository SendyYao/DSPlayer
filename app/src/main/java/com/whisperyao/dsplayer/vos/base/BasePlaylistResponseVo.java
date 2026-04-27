package com.whisperyao.dsplayer.vos.base;

import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;

import java.util.List;

public abstract class BasePlaylistResponseVo extends BaseVo {
    public abstract int getOffset();

    public abstract List<? extends BasePlaylistVo> getPlaylists();

    public abstract int getRealTotal();

    public abstract int getValidTotal();

    public static abstract class BasePlaylistVo {
        public abstract String getID();

        public abstract String getName();

        public BaseSharingInfoVo getSharingInfo() {
            return null;
        }

        public abstract boolean isNormal();

        public abstract boolean isOldVersion();

        public abstract boolean isPersonal();

        public abstract boolean isRandom();

        public abstract boolean isSharedSongs();

        public abstract boolean isSmart();

        public BaseSharingInfoVo.SharingStatusVo getSharingStatus() {
            return new BaseSharingInfoVo.SharingStatusVo() {
                @Override
                public boolean isExpired() {
                    return false;
                }

                @Override
                public boolean isInvalid() {
                    return false;
                }

                @Override
                public boolean isNone() {
                    return true;
                }

                @Override
                public boolean isValid() {
                    return false;
                }
            };
        }
    }
}
