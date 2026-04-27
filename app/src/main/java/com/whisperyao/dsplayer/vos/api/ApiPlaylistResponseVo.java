package com.whisperyao.dsplayer.vos.api;


import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo;
import com.whisperyao.dsplayer.vos.base.BaseSharingInfoVo;
import java.util.ArrayList;
import java.util.List;

public class ApiPlaylistResponseVo extends BasePlaylistResponseVo {
    private ApiPlaylistDataVo data;

    public static class ApiPlaylistAdditionalVo {
        private ApiPlaylistSharingInfoVo sharing_info;
    }

    public static class ApiPlaylistDataVo {
        private int offset;
        private List<ApiPlaylistVo> playlists;
        private int total;
    }

    @Override
    public int getRealTotal() {
        ApiPlaylistDataVo apiPlaylistDataVo = this.data;
        if (apiPlaylistDataVo != null) {
            return apiPlaylistDataVo.total;
        }
        return 0;
    }

    @Override
    public int getValidTotal() {
        return getRealTotal();
    }

    @Override
    public int getOffset() {
        ApiPlaylistDataVo apiPlaylistDataVo = this.data;
        if (apiPlaylistDataVo != null) {
            return apiPlaylistDataVo.offset;
        }
        return 0;
    }

    @Override
    public List<? extends BasePlaylistResponseVo.BasePlaylistVo> getPlaylists() {
        ApiPlaylistDataVo apiPlaylistDataVo = this.data;
        if (apiPlaylistDataVo != null && apiPlaylistDataVo.playlists != null) {
            return this.data.playlists;
        }
        return new ArrayList();
    }

    public static class ApiPlaylistVo extends BasePlaylistResponseVo.BasePlaylistVo {
        private static final String NORMAL = "normal";
        private static final String PERSONAL = "personal";
        private static final String PLAYLIST_ID_SHARED_SONG = "playlist_personal_normal/__SYNO_AUDIO_SHARED_SONGS__";
        private ApiPlaylistAdditionalVo additional;
        private String id;
        private String library;
        private String name;
        private ApiPlaylistSharingInfoVo.ApiSharingStatus sharing_status;
        private String type;

        @Override
        public boolean isOldVersion() {
            return false;
        }

        @Override
        public boolean isRandom() {
            return false;
        }

        @Override
        public boolean isSharedSongs() {
            return "playlist_personal_normal/__SYNO_AUDIO_SHARED_SONGS__".equals(getID());
        }

        @Override
        public boolean isNormal() {
            return "normal".equalsIgnoreCase(this.type);
        }

        @Override
        public boolean isSmart() {
            return !isNormal();
        }

        @Override
        public boolean isPersonal() {
            return "personal".equalsIgnoreCase(this.library);
        }

        @Override
        public BaseSharingInfoVo.SharingStatusVo getSharingStatus() {
            return new ApiPlaylistSharingInfoVo.ApiSharingStatusVo(this.sharing_status);
        }

        @Override
        public BaseSharingInfoVo getSharingInfo() {
            ApiPlaylistAdditionalVo apiPlaylistAdditionalVo = this.additional;
            if (apiPlaylistAdditionalVo == null || apiPlaylistAdditionalVo.sharing_info == null) {
                return null;
            }
            return this.additional.sharing_info;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getID() {
            return this.id;
        }
    }
}
