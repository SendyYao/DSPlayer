package com.whisperyao.dsplayer.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dagger.android.DaggerContentProvider;
import jakarta.inject.Inject;

public class AudioProvider extends DaggerContentProvider {
    public static final String AUTHORITY = "com.synology.dsaudio.db";
    private static final int ENUM_PLAYLIST_SONGS = 3;
    public static final String LOCALPLAYLIST_SONG_RELATION_TABLE_NAME = "local_playlist_song_relation";
    public static final String LOCALPLAYLIST_SONG_RELATION_TABLE_NAME_V5 = "localPlaylist_song_relation";
    public static final String LOCALPLAYLIST_TABLE_NAME = "localPlaylistInfo";
    private static final String PATH_ENUM_PLAYLIST_SONGS = "enum_playlist_songs";
    private static final String PATH_PLAYLISTS = "playlists";
    private static final String PATH_PLAYLISTS_SONGS_RELATION = "playlists_songs_relation";
    private static final int PLAYLISTS = 1;
    private static final int PLAYLISTS_SONGS_RELATION = 2;
    private static final UriMatcher sUriMatcher = null;

    @Inject
    ContentResolver mContentResolver;
    private SQLiteDatabase mDB = null;
    public static final Uri CONTENT_URI_PLAYLISTS = Uri.parse("content://com.synology.dsaudio.db/playlists");
    public static final Uri CONTENT_URI_PLAYLISTS_SONGS_RELATION = Uri.parse("content://com.synology.dsaudio.db/playlists_songs_relation");
    public static final Uri CONTENT_URI_ENUM_PLAYLIST_SONGS = Uri.parse("content://com.synology.dsaudio.db/enum_playlist_songs");

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        String str;
        this.mDB = DatabaseAccesser.getInstance().getDB();
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == 1) {
            str = LOCALPLAYLIST_TABLE_NAME;
        } else if (iMatch == 2) {
            str = LOCALPLAYLIST_SONG_RELATION_TABLE_NAME;
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int iDelete = this.mDB.delete(str, where, whereArgs);
        this.mContentResolver.notifyChange(uri, null);
        return iDelete;
    }


    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues values) {
        String str;
        this.mDB = DatabaseAccesser.getInstance().getDB();
        if (values == null) {
            values = new ContentValues();
        }
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == 1) {
            str = LOCALPLAYLIST_TABLE_NAME;
        } else if (iMatch == 2) {
            str = LOCALPLAYLIST_SONG_RELATION_TABLE_NAME;
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        long jInsert = this.mDB.insert(str, null, values);
        if (jInsert > 0) {
            Uri uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert);
            this.mContentResolver.notifyChange(uriWithAppendedId, null);
            return uriWithAppendedId;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        this.mDB = DatabaseAccesser.getInstance().getDB();
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == 1) {
            sQLiteQueryBuilder.setTables(LOCALPLAYLIST_TABLE_NAME);
        } else {
            if (iMatch != 2) {
                if (iMatch == 3) {
                    sQLiteQueryBuilder.setTables(LOCALPLAYLIST_TABLE_NAME);
                    return this.mDB.rawQuery(" SELECT * , local_playlist_song_relation._relation_id FROM song_table INNER JOIN local_playlist_song_relation ON (song_table.songid = local_playlist_song_relation.song_id AND song_table.dsid = local_playlist_song_relation.song_dsid) WHERE local_playlist_song_relation.playlist_dsid = ?  AND local_playlist_song_relation.playlist_id = ? ", selectionArgs);
                }
                throw new IllegalArgumentException("Unknown URI: " + uri);
            }
            sQLiteQueryBuilder.setTables(LOCALPLAYLIST_SONG_RELATION_TABLE_NAME);
        }
        Cursor cursorQuery = sQLiteQueryBuilder.query(this.mDB, projection, selection, selectionArgs, null, null, sortOrder);
        cursorQuery.setNotificationUri(this.mContentResolver, uri);
        return cursorQuery;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        String str;
        this.mDB = DatabaseAccesser.getInstance().getDB();
        if (values == null) {
            values = new ContentValues();
        }
        int iMatch = sUriMatcher.match(uri);
        if (iMatch == 1) {
            str = LOCALPLAYLIST_TABLE_NAME;
        } else if (iMatch == 2) {
            str = LOCALPLAYLIST_SONG_RELATION_TABLE_NAME;
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int iUpdate = this.mDB.update(str, values, where, whereArgs);
        this.mContentResolver.notifyChange(uri, null);
        return iUpdate;
    }


    public static class Localplaylist_Table {

        public static class Localplaylist_Column implements BaseColumns {
            public static final String SQL_DSID = "dsid";
            public static final String SQL_PLAYLISTID = "playlistid";
            public static final String SQL_TITLE = "title";
        }

        public static String getSQLCreateTableCols() {
            return ("id integer primary key autoincrement, dsid text not null, playlistid text not null, title text, ") + " unique ( dsid , playlistid ) ";
        }

        public static String getSortOrder() {
            return "title ASC";
        }

        public static String[] getProjection() {
            return new String[]{"dsid", Localplaylist_Column.SQL_PLAYLISTID, "title"};
        }

        public static String getSelection() {
            return " ( dsid = ? ) AND (playlistid = ? ) ";
        }

        public static String[] getSelectionArgs(String dsid, String playlist_id) {
            return new String[]{dsid, playlist_id};
        }
    }

    public static class Localplaylist_Song_Relation_Table {
        public static final String CREATE_TABLE = "create table if not exists local_playlist_song_relation (_relation_id integer primary key autoincrement, playlist_dsid text not null, playlist_id text not null, song_dsid text not null, song_path text not null, song_id text not null);";

        @Deprecated
        public static final String CREATE_TABLE_V5 = "create table if not exists local_playlist_song_relation (_relation_id integer primary key autoincrement, playlist_dsid text not null, playlist_id text not null, song_dsid text not null, song_path text not null);";

        public static class Local_Relation_Column implements BaseColumns {
            public static final String PLAYLIST_DSID = "playlist_dsid";
            public static final String PLAYLIST_ID = "playlist_id";
            public static final String SONG_DSID = "song_dsid";
            public static final String SONG_ID = "song_id";
            public static final String SONG_PATH = "song_path";
            public static final String _RELATION_ID = "_relation_id";
        }

        public static String[] getProjection() {
            return new String[]{Local_Relation_Column.PLAYLIST_DSID, Local_Relation_Column.PLAYLIST_ID, Local_Relation_Column.SONG_DSID, Local_Relation_Column.SONG_PATH, Local_Relation_Column.SONG_ID};
        }

        public static String getIDSelection() {
            return " ( _relation_id = ?  )";
        }

        public static String getPlaylistSelection() {
            return " ( playlist_dsid = ?  AND playlist_id = ? )";
        }

        public static String getSelection() {
            return " ( playlist_dsid = ?  AND playlist_id = ?  AND song_dsid = ? AND song_id = ?   ) ";
        }

        public static String[] getIDSelectionArgs(int id) {
            return new String[]{Integer.toString(id)};
        }

        public static String[] getSelectionArgs(String playlist_dsid, String playlist_id, String song_dsid, String song_path, String song_id) {
            String[] strArr = {playlist_dsid, playlist_id, song_dsid, song_path};
            strArr[4] = song_id;
            return strArr;
        }
    }


}
