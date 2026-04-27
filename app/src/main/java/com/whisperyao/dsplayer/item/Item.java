package com.whisperyao.dsplayer.item;

import android.os.Bundle;

/* loaded from: classes2.dex */
public class Item {

    public static final String ID = "id";
    public static final String IS_PERSONAL = "is_personal";
    public static final int RATING_INVALID = -1;
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    protected String mAlbumArtist;
    protected String mDisplayArtist;
    protected String mID;
    private IconStatus mIconStatus;
    protected boolean mMarked;
    protected String mTitle;
    private ItemType mType;
    protected float mRating = -1;
    private boolean mAllSongs = false;

    public enum IconStatus {
        NONE,
        PLAYING
    }

    public enum ItemType {
        NOTSURED_MODE,
        CONTAINER_MODE,
        DIRECTORY_MODE,
        FILE_MODE,
        RATING_MODE,
        RADIO_MODE,
        HEADER_MODE,
        PERSONAL_NORMAL_NEW(ShareType.PERSONAL, PLSType.NOAMAL, true),
        PERSONAL_NORMAL_OLD(ShareType.PERSONAL, PLSType.NOAMAL, false),
        PERSONAL_SMART_NEW(ShareType.PERSONAL, PLSType.SMART, true),
        PERSONAL_SMART_OLD(ShareType.PERSONAL, PLSType.SMART, false),
        SHARED_NORMAL_NEW(ShareType.SHARED, PLSType.NOAMAL, true),
        SHARED_NORMAL_OLD(ShareType.SHARED, PLSType.NOAMAL, false),
        SHARED_SMART_NEW(ShareType.SHARED, PLSType.SMART, true),
        SHARED_SMART_OLD(ShareType.SHARED, PLSType.SMART, false),
        LOCAL_PLAYLIST_NORMAL;

        private boolean hasPersonalID;
        private PLSType plsType;
        private ShareType shareType;

        private enum PLSType {
            NOAMAL,
            SMART
        }

        private enum ShareType {
            PERSONAL,
            SHARED
        }

        ItemType() {
            this(null, null, false);
        }

        ItemType(ShareType s, PLSType p, boolean b) {
            this.shareType = s;
            this.plsType = p;
            this.hasPersonalID = b;
        }

        protected boolean isPersonal() {
            return ShareType.PERSONAL.equals(this.shareType);
        }

        protected boolean isNormalPls() {
            return PLSType.NOAMAL.equals(this.plsType);
        }

        protected boolean hasNewPlaylistID() {
            return this.hasPersonalID;
        }

        public boolean isPlayListItem() {
            return equals(PERSONAL_NORMAL_NEW) || equals(PERSONAL_NORMAL_OLD) || equals(PERSONAL_SMART_NEW) || equals(PERSONAL_SMART_OLD) || equals(SHARED_NORMAL_NEW) || equals(SHARED_NORMAL_OLD) || equals(SHARED_SMART_NEW) || equals(SHARED_SMART_OLD) || equals(LOCAL_PLAYLIST_NORMAL);
        }

        public boolean isContainer() {
            return equals(CONTAINER_MODE);
        }

        public boolean isRadioItem() {
            return equals(RADIO_MODE);
        }

        public boolean isFile() {
            return equals(FILE_MODE);
        }

        public boolean isDirectory() {
            return equals(DIRECTORY_MODE);
        }
    }

    public Item(ItemType type, String id, String title) {
        if (type.isPlayListItem()) {
            boolean z = this instanceof PlaylistItem;
        }
        this.mID = id;
        this.mType = type;
        this.mTitle = title;
    }

    public String getID() {
        return this.mID;
    }

    public void setID(String id) {
        this.mID = id;
    }

    public void setAllSongs(boolean flag) {
        this.mAllSongs = flag;
    }

    public boolean isAllSongs() {
        return this.mAllSongs;
    }

    public ItemType getType() {
        return this.mType;
    }

    public boolean isPersonal() {
        return this.mType.isPersonal();
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public void setRating(float rating) {
        this.mRating = rating;
    }

    public float getRating() {
        return this.mRating;
    }

    public boolean isWithRating() {
        return this.mRating >= 0.0f;
    }

    public void setAlbumArtist(String album_artist) {
        this.mAlbumArtist = album_artist;
    }

    public String getAlbumArtist() {
        String str = this.mAlbumArtist;
        return str == null ? "" : str;
    }

    public void setDisplayArtist(String display_artist) {
        this.mDisplayArtist = display_artist;
    }

    public String getDisplayArtist() {
        return this.mDisplayArtist;
    }

    public void setMarked(boolean marked) {
        this.mMarked = marked;
    }

    public boolean isMarked() {
        return this.mMarked;
    }

    public void setIconStatus(IconStatus status) {
        this.mIconStatus = status;
    }

    public IconStatus getIconStatus() {
        return this.mIconStatus;
    }

    public Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("id", this.mID);
        bundle.putString("title", this.mTitle);
        bundle.putString("type", this.mType.name());
        return bundle;
    }

    public static Item fromBundle(Bundle bundle) {
        return new Item(ItemType.valueOf(bundle.getString("type")), bundle.getString("id"), bundle.getString("title"));
    }
}