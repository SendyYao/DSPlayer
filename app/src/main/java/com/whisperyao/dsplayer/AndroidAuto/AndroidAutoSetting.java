package com.whisperyao.dsplayer.AndroidAuto;

import java.util.HashSet;
import java.util.Set;

public class AndroidAutoSetting {
    private Set<DefaultPlaylist> mDefaultPlaylists = new HashSet();

    public enum DefaultPlaylist {
        FAVORITE_RADIO,
        RANDOM100,
        MOSTOFTEN,
        MOSTRECENT,
        RATING4,
        RATING5
    }

    public void setDefaultPlaylists(Set<DefaultPlaylist> defaultPlaylists) {
        this.mDefaultPlaylists = defaultPlaylists;
    }

    public boolean containsDefaultPlaylistFavoriteRadio() {
        return this.mDefaultPlaylists.contains(DefaultPlaylist.FAVORITE_RADIO);
    }

    public boolean containsDefaultPlaylistRandom100() {
        return this.mDefaultPlaylists.contains(DefaultPlaylist.RANDOM100);
    }

    public boolean containsDefaultPlaylistMostOften() {
        return this.mDefaultPlaylists.contains(DefaultPlaylist.MOSTOFTEN);
    }

    public boolean containsDefaultPlaylistMostRecent() {
        return this.mDefaultPlaylists.contains(DefaultPlaylist.MOSTRECENT);
    }

    public boolean containsDefaultPlaylistRating4() {
        return this.mDefaultPlaylists.contains(DefaultPlaylist.RATING4);
    }

    public boolean containsDefaultPlaylistRating5() {
        return this.mDefaultPlaylists.contains(DefaultPlaylist.RATING5);
    }
}
