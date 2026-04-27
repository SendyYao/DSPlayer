package com.whisperyao.dsplayer.vos.cgi;

import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CgiPlaylistResponseVo extends BasePlaylistResponseVo {
    private List<CgiPlaylistVo> items;
    private int total;

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getRealTotal() {
        return this.total;
    }

    @Override
    public int getValidTotal() {
        int i = this.total;
        for (CgiPlaylistVo item : this.items) {
            if (item.isRandom()) {
                i--;
            }
        }
        return i;
    }

    @Override
    public List<? extends BasePlaylistResponseVo.BasePlaylistVo> getPlaylists() {
        List<CgiPlaylistVo> list = this.items;
        return list != null ? list : new ArrayList<>();
    }

    public List<CgiPlaylistVo> getItems() {
        return this.items;
    }

    public static class CgiPlaylistVo extends BasePlaylistResponseVo.BasePlaylistVo {
        private static final String NORMAL_PLAYLIST = "normal_playlist";
        private static final String SMART_PLAYLIST = "smart_playlist";
        private String id;
        private String name;
        private boolean personal = false;
        private String type;

        @Override
        public boolean isSharedSongs() {
            return false;
        }

        @Override
        public boolean isRandom() {
            String str = this.id;
            if (str == null) {
                return false;
            }
            try {
                return Integer.parseInt(str) < 0;
            } catch (NumberFormatException unused) {
                return false;
            }
        }

        private boolean isWithId() {
            return this.id != null;
        }

        @Override
        public boolean isOldVersion() {
            return !isPersonal() || !isWithId();
        }

        @Override
        public boolean isNormal() {
            return NORMAL_PLAYLIST.equalsIgnoreCase(this.type);
        }

        @Override
        public boolean isSmart() {
            return SMART_PLAYLIST.equalsIgnoreCase(this.type);
        }

        @Override
        public boolean isPersonal() {
            return this.personal;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getID() {
            if (isPersonal()) {
                if (isWithId()) {
                    return this.id;
                }
                return this.name;
            }
            return this.id;
        }
    }
}
