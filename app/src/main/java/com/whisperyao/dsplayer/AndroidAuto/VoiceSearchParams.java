package com.whisperyao.dsplayer.AndroidAuto;

import android.os.Bundle;
import android.text.TextUtils;

/* loaded from: classes.dex */
public final class VoiceSearchParams {
    public String album;
    public String artist;
    public String genre;
    public boolean isAlbumFocus;
    public boolean isAny;
    public boolean isArtistFocus;
    public boolean isGenreFocus;
    public boolean isSongFocus;
    public boolean isUnstructured;
    public final String query;
    public String song;

    public VoiceSearchParams(String query, Bundle extras) {
        this.query = query;
        if (TextUtils.isEmpty(query)) {
            this.isAny = true;
            return;
        }
        if (extras == null) {
            this.isUnstructured = true;
            return;
        }
        String string = extras.getString("android.intent.extra.focus");
        if (TextUtils.equals(string, "vnd.android.cursor.item/genre")) {
            this.isGenreFocus = true;
            String string2 = extras.getString("android.intent.extra.genre");
            this.genre = string2;
            if (TextUtils.isEmpty(string2)) {
                this.genre = query;
                return;
            }
            return;
        }
        if (TextUtils.equals(string, "vnd.android.cursor.item/artist")) {
            this.isArtistFocus = true;
            this.genre = extras.getString("android.intent.extra.genre");
            this.artist = extras.getString("android.intent.extra.artist");
        } else {
            if (TextUtils.equals(string, "vnd.android.cursor.item/album")) {
                this.isAlbumFocus = true;
                this.album = extras.getString("android.intent.extra.album");
                this.genre = extras.getString("android.intent.extra.genre");
                this.artist = extras.getString("android.intent.extra.artist");
                return;
            }
            if (TextUtils.equals(string, "vnd.android.cursor.item/audio")) {
                this.isSongFocus = true;
                this.song = extras.getString("android.intent.extra.title");
                this.album = extras.getString("android.intent.extra.album");
                this.genre = extras.getString("android.intent.extra.genre");
                this.artist = extras.getString("android.intent.extra.artist");
                return;
            }
            this.isUnstructured = true;
        }
    }

    public String toString() {
        return "query=" + this.query + " isAny=" + this.isAny + " isUnstructured=" + this.isUnstructured + " isGenreFocus=" + this.isGenreFocus + " isArtistFocus=" + this.isArtistFocus + " isAlbumFocus=" + this.isAlbumFocus + " isSongFocus=" + this.isSongFocus + " genre=" + this.genre + " artist=" + this.artist + " album=" + this.album + " song=" + this.song;
    }
}
