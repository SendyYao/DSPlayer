package com.whisperyao.dsplayer.item;

public class LocalPlaylistSongItem extends SongItem {
    private int mRelationId;

    public LocalPlaylistSongItem(Item.ItemType type, String id, String title) {
        super(type, id, title);
        this.mRelationId = -1;
    }

    public void setRelationId(int id) {
        this.mRelationId = id;
    }

    public int getRelationId() {
        return this.mRelationId;
    }
}
