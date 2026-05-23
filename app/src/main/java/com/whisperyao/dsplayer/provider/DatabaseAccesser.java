package com.whisperyao.dsplayer.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.TextUtils;
import com.synology.sylib.util.IOUtils;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.LocalEnumerator;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DatabaseAccesser {
    private static final String DATABASE_NAME = "DSaudio_LocalCache_DB";

    private static DatabaseAccesser instance;
    private static final String LOG_TAG = "DatabaseAccesser";
    protected static final String SONGLIST_TABLE_NAME = "song_table";

    private SQLiteDatabase mDB;
    private static int openCounter;
    private MyDatabaseHelper mDBHelper;

    public SQLiteDatabase getDB() {
        if (this.mDB == null) {
            this.mDB = this.mDBHelper.getWritableDatabase();
        }
        return this.mDB;
    }

    public boolean isClosed() {
        return !this.mDB.isOpen();
    }

    public static synchronized DatabaseAccesser getInstance() {
        DatabaseAccesser databaseAccesser = instance;
        if (databaseAccesser == null || databaseAccesser.isClosed()) {
            instance = new DatabaseAccesser();
        }
        openCounter++;
        return instance;

    }

    private DatabaseAccesser() {
        openCounter = 0;
        MyDatabaseHelper myDatabaseHelper = new MyDatabaseHelper(App.getContext());
        this.mDBHelper = myDatabaseHelper;
        try {
            this.mDB = myDatabaseHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            e.printStackTrace();
            SynoLog.e(LOG_TAG, "Opening the database failed");
        }

    }

    private void updateTimeAndArgs(final SongItem song, final ContentValues args) {
        int iUpdate;
        args.put(SongItem.SQL_TIMESTAMP, Long.valueOf(System.currentTimeMillis()));
        synchronized (this.mDB) {
            iUpdate = this.mDB.update(SONGLIST_TABLE_NAME, args, "dsid = ? AND path= ? AND track= ?  ", new String[]{song.getDsId(), song.getFilePath(), Integer.toString(song.getTrack())});
        }
        SynoLog.d(LOG_TAG, iUpdate + " update SongItem = " + song.getCachePath() + ", args = " + args);
    }

    private void checkRowExist(final SongItem song) {
        Cursor cursorQuery;
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, null, "dsid = ? AND path= ? AND track= ? ", new String[]{song.getDsId(), song.getFilePath(), Integer.toString(song.getTrack())}, null, null, null);
            if (cursorQuery.getCount() == 0) {
                this.mDB.insert(SONGLIST_TABLE_NAME, null, song.getContentValues());
            }
        }
        IOUtils.closeSilently(cursorQuery);
    }

    public void addLyric(final SongItem song, final String lyricPath) {
        SynoLog.d(LOG_TAG, "addLyric");
        checkRowExist(song);
        ContentValues contentValues = new ContentValues();
        contentValues.put(SongItem.SQL_LYRIC_PATH, lyricPath);
        updateTimeAndArgs(song, contentValues);
    }

    public void addCover(final SongItem song, final String coverPath) {
        checkRowExist(song);
        ContentValues contentValues = new ContentValues();
        contentValues.put(SongItem.SQL_COVER_PATH, coverPath);
        updateTimeAndArgs(song, contentValues);
    }

    public static SongItem[] parseSongsFromCursor(final Cursor cursor) {
        SongItem[] songItemArr = new SongItem[cursor.getCount()];
        if (cursor.moveToFirst()) {
            int i = 0;
            while (true) {
                int i2 = i + 1;
                songItemArr[i] = SongItem.fromQueryCursor(cursor);
                if (!cursor.moveToNext()) {
                    break;
                }
                i = i2;
            }
        }
        IOUtils.closeSilently(cursor);
        return songItemArr;
    }

    private String getConditionForDownloaded() {
        return "cache_path IS NOT NULL AND cache_path != ''";
    }

    public SongItem querySong(final SongItem song) {
        Cursor cursorQuery;
        if (song == null) {
            return null;
        }
        synchronized (mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, null, "dsid = ? AND path= ? AND track= ? AND " + getConditionForDownloaded(), new String[]{song.getDsId(), song.getFilePath(), Integer.toString(song.getTrack())}, null, null, null);
        }
        SongItem[] songsFromCursor = parseSongsFromCursor(cursorQuery);
        SynoLog.d(LOG_TAG, "querySong : " + song.getFilePath() + ", total : " + songsFromCursor.length);
        if (songsFromCursor.length > 0) {
            return songsFromCursor[0];
        }
        return null;
    }

    public void addSong(final SongItem song) {
        SynoLog.d(LOG_TAG, "addSong");
        checkRowExist(song);
        ContentValues contentValues = new ContentValues();
        contentValues.put(SongItem.SQL_CACHEPATH, song.getCachePath());
        contentValues.put(SongItem.SQL_HITCOUNT, song.getHitCount());
        contentValues.put(SongItem.SQL_CACHEBITRATE, song.getCacheBitrate());
        contentValues.put(SongItem.SQL_DOWNLOAD_TYPE, song.getDownloadType());
        updateTimeAndArgs(song, contentValues);
    }

    public void updateSong(final SongItem song) {
        int iUpdate;
        String str = LOG_TAG;
        SynoLog.d(str, "updateSong");
        ContentValues contentValues = new ContentValues();
        contentValues.put(SongItem.SQL_CACHEBITRATE, song.getCacheBitrate());
        contentValues.put(SongItem.SQL_CACHEPATH, song.getCachePath());
        contentValues.put(SongItem.SQL_DOWNLOAD_TYPE, song.getDownloadType());
        synchronized (this.mDB) {
            iUpdate = this.mDB.update(SONGLIST_TABLE_NAME, contentValues, "dsid = ? AND path= ? AND track= ? ", new String[]{song.getDsId(), song.getFilePath(), Integer.toString(song.getTrack())});
        }
        // SynoLog.d(str, iUpdate + " update SongItem = " + song.getCachePath() + ", args = " + contentValues);
    }

    public void updateSongRating(List<SongItem> songItemList) {
        SQLiteDatabase sQLiteDatabase;
        synchronized (this.mDB) {
            try {
                this.mDB.beginTransaction();
                for (SongItem songItem : songItemList) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(SongItem.SQL_RATING, songItem.getSongRating());
                    this.mDB.update(SONGLIST_TABLE_NAME, contentValues, "dsid = ? AND songid= ?", new String[]{songItem.getDsId(), songItem.getID()});
                }
                this.mDB.setTransactionSuccessful();
                sQLiteDatabase = this.mDB;
            } catch (SQLException unused) {
                sQLiteDatabase = this.mDB;
            } catch (Throwable th) {
                this.mDB.endTransaction();
                throw th;
            }
            sQLiteDatabase.endTransaction();
        }
    }

    public int deleteSong(final SongItem song) {
        int iDelete;
        if (song == null) {
            return 0;
        }
        synchronized (this.mDB) {
            iDelete = this.mDB.delete(SONGLIST_TABLE_NAME, "dsid = ? AND path= ? AND track= ? AND cache_path= ?", new String[]{song.getDsId(), song.getFilePath(), Integer.toString(song.getTrack()), song.getCachePath()});
        }
        SynoLog.d(LOG_TAG, "delete SongItem = " + song.getCachePath() + ", row number = " + iDelete);
        return iDelete;
    }

    public int deleteAllSongs(int downloadType) {
        String str;
        String[] strArr;
        int iDelete;
        if (downloadType == 1) {
            str = "download_type=?";
            strArr = new String[]{String.valueOf(1)};
        } else if (downloadType == 2) {
            str = "download_type=?";
            strArr = new String[]{String.valueOf(2)};
        } else {
            str = null;
            strArr = null;
        }
        synchronized (this.mDB) {
            iDelete = this.mDB.delete(SONGLIST_TABLE_NAME, str, strArr);
        }
        SynoLog.d(LOG_TAG, " deleteAllSongs row number = " + iDelete);
        return iDelete;
    }

    public void deleteAllManual() {
        synchronized (this.mDB) {
            deleteAllSongs(2);
            this.mDB.delete(AudioProvider.LOCALPLAYLIST_TABLE_NAME, null, null);
            this.mDB.delete(AudioProvider.LOCALPLAYLIST_SONG_RELATION_TABLE_NAME, null, null);
        }
    }

    public int doEnumAllSongsCount(int downloadType) {
        Cursor cursorQuery;
        String conditionForDownloaded = getConditionForDownloaded();
        String[] strArr = new String[0];
        if (downloadType == 1) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "download_type=?");
            strArr = (String[]) ArrayUtils.addAll(strArr, String.valueOf(1));
        } else if (downloadType == 2) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "download_type=?");
            strArr = (String[]) ArrayUtils.addAll(strArr, String.valueOf(2));
        }
        String str = conditionForDownloaded;
        String[] strArr2 = strArr;
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, null, str, strArr2, null, null, "title ASC");
        }
        int count = cursorQuery.getCount();
        IOUtils.closeSilently(cursorQuery);
        return count;
    }



    public SongItem[] doEnumRotateCandidate() {
        Cursor cursorQuery;
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, null, String.format("%s AND %s", getConditionForDownloaded(), "download_type = ? "), new String[]{String.valueOf(1)}, null, null, "timestamp ASC", " 0, 1");
        }
        return parseSongsFromCursor(cursorQuery);
    }

    public SongItem[] doEnumAllSongs() {
        return doEnumAllSongs(null, 0);
    }

    public SongItem[] doEnumAllSongs(String dsid) {
        return doEnumAllSongs(dsid, 0);
    }

    public SongItem[] doEnumAllSongs(String dsid, int downloadType) {
        Cursor cursorQuery;
        String conditionForDownloaded = getConditionForDownloaded();
        String[] strArr = new String[0];
        if (dsid != null) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "dsid=?");
            strArr = ArrayUtils.addAll(strArr, dsid);
        }
        if (downloadType == 1) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "download_type=?");
            strArr = ArrayUtils.addAll(strArr, String.valueOf(1));
        } else if (downloadType == 2) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "download_type=?");
            strArr = ArrayUtils.addAll(strArr, String.valueOf(2));
        }
        String str = conditionForDownloaded;
        String[] strArr2 = strArr;
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, null, str, strArr2, null, null, "title ASC");
        }
        return parseSongsFromCursor(cursorQuery);
    }

    private boolean enumArtist(Common.ContainerType type) {
        return type == Common.ContainerType.ARTIST_MODE || type == Common.ContainerType.SEARCH_ARTIST_MODE || type == Common.ContainerType.GENRE_ARTIST_MODE;
    }

    public JSONArray doEnumContainer(final Common.ContainerType type, final Bundle bundle, String dsid) {
        Cursor cursorQuery;
        String str;
        if (enumArtist(type)) {
            return doEnumContainerArtist(type, bundle, dsid);
        }
        String conditionForDownloaded = getConditionForDownloaded();
        String[] strArr = new String[0];
        if (dsid != null) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "dsid=?");
            strArr = ArrayUtils.addAll(strArr, dsid);
        }
        switch (type) {
            case COMPOSER_ALBUM_MODE:
                conditionForDownloaded = conditionForDownloaded + " AND composer= ?";
                strArr = ArrayUtils.addAll(strArr, bundle.getString("composer"));
                break;
            case ARTIST_ALBUM_MODE:
                String string = bundle.getString("artist");
                if (TextUtils.isEmpty(string)) {
                    str = conditionForDownloaded + " AND artist= ? AND album_artist= ?";
                } else {
                    str = conditionForDownloaded + " AND (artist= ? OR album_artist= ?)";
                }
                conditionForDownloaded = str;
                strArr = ArrayUtils.addAll(strArr, string, string);
                break;
            case GENRE_ARTIST_MODE:
                if (bundle.containsKey("genre")) {
                    String string2 = bundle.getString("genre");
                    conditionForDownloaded = conditionForDownloaded + " AND genre= ?";
                    String[] strArr2 = new String[1];
                    if (string2 == null) {
                        string2 = "";
                    }
                    strArr2[0] = string2;
                    strArr = ArrayUtils.addAll(strArr, strArr2);
                    break;
                }
                break;
            case GENRE_ARTIST_ALBUM_MODE:
                if (bundle.containsKey("genre")) {
                    String string3 = bundle.getString("genre");
                    conditionForDownloaded = conditionForDownloaded + " AND genre= ?";
                    String[] strArr3 = new String[1];
                    if (string3 == null) {
                        string3 = "";
                    }
                    strArr3[0] = string3;
                    strArr = ArrayUtils.addAll(strArr, strArr3);
                }
                if (bundle.containsKey("artist")) {
                    String string4 = bundle.getString("artist");
                    conditionForDownloaded = conditionForDownloaded + " AND (artist= ? OR album_artist= ?)";
                    strArr = ArrayUtils.addAll(strArr, string4, string4);
                    break;
                }
                break;
            case ALBUM_MODE:
                break;
            case SEARCH_ALBUM_MODE:
                conditionForDownloaded = conditionForDownloaded + " AND album LIKE ?";
                strArr = ArrayUtils.addAll(strArr, "%" + bundle.getString("key") + "%");
                break;
            default:
                return doEnumContainer(type, dsid);
        }
        String str2 = conditionForDownloaded;
        String[] strArr4 = strArr;
        String[] strArr5 = {"album", "album_artist", "artist", "COUNT(distinct artist)", "AVG(rating)"};
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, strArr5, str2, strArr4, "album,album_artist,album_artist", null, null, null);
        }
        JSONArray jSONArray = new JSONArray();
        if (cursorQuery.moveToFirst()) {
            do {
                try {
                    String string5 = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("album"));
                    String string6 = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("artist"));
                    String string7 = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("album_artist"));
                    int i = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("COUNT(distinct artist)"));
                    float f = cursorQuery.getFloat(cursorQuery.getColumnIndexOrThrow("AVG(rating)"));
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("name", string5);
                    jSONObject.put("album_artist", string7);
                    if (!TextUtils.isEmpty(string7) || i != 1) {
                        string6 = string7;
                    }
                    jSONObject.put(PinManager.DISPLAY_ARTIST, string6);
                    jSONObject.put("avg_rating", f);
                    jSONArray.put(jSONObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } while (cursorQuery.moveToNext());
        }
        IOUtils.closeSilently(cursorQuery);
        return jSONArray;
    }

    private JSONArray doEnumContainerArtist(final Common.ContainerType type, Bundle bundle, String dsid) {
        String string;
        String str;
        String str2;
        Cursor cursorQuery;
        Cursor cursorQuery2;
        if (bundle.containsKey("artist")) {
            string = bundle.getString("artist");
        } else {
            string = bundle.getString("key");
        }
        String conditionForDownloaded = getConditionForDownloaded();
        String conditionForDownloaded2 = getConditionForDownloaded();
        String[] strArr = new String[0];
        String[] strArr2 = new String[0];
        JSONArray jSONArray = new JSONArray();
        if (dsid != null) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "dsid=?");
            conditionForDownloaded2 = String.format("%s AND %s", conditionForDownloaded2, "dsid=?");
            strArr = ArrayUtils.addAll(strArr, dsid);
            strArr2 = ArrayUtils.addAll(strArr2, dsid);
        }
        if (bundle.containsKey("genre")) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "genre=?");
            conditionForDownloaded2 = String.format("%s AND %s", conditionForDownloaded2, "genre=?");
            strArr = ArrayUtils.addAll(strArr, bundle.getString("genre"));
            strArr2 = ArrayUtils.addAll(strArr2, bundle.getString("genre"));
        }
        if (type != Common.ContainerType.SEARCH_ARTIST_MODE) {
            str2 = conditionForDownloaded2 + " AND (album_artist  != ''   )";
            str = conditionForDownloaded + " AND (album_artist = ''   )";
        } else {
            str = conditionForDownloaded + " AND (artist LIKE ?)";
            strArr = ArrayUtils.addAll(strArr, "%" + string + "%");
            strArr2 = ArrayUtils.addAll(strArr2, "%" + string + "%");
            str2 = conditionForDownloaded2 + " AND (album_artist LIKE ?)";
        }
        String[] strArr3 = strArr2;
        ArrayList<String> arrayList = new ArrayList<>();
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, new String[]{"album_artist"}, str2, strArr3, "album_artist", null, null, null);
        }
        if (cursorQuery.moveToFirst()) {
            do {
                String string2 = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("album_artist"));
                if (!arrayList.contains(string2)) {
                    arrayList.add(string2);
                }
            } while (cursorQuery.moveToNext());
        }
        IOUtils.closeSilently(cursorQuery);
        synchronized (this.mDB) {
            cursorQuery2 = this.mDB.query(SONGLIST_TABLE_NAME, new String[]{"artist"}, str, strArr, "artist", null, null, null);
        }
        if (cursorQuery2.moveToFirst()) {
            do {
                String string3 = cursorQuery2.getString(cursorQuery2.getColumnIndexOrThrow("artist"));
                if (!arrayList.contains(string3)) {
                    arrayList.add(string3);
                }
            } while (cursorQuery2.moveToNext());
        }
        IOUtils.closeSilently(cursorQuery2);
        Collections.sort(arrayList, (lhs, rhs) -> Common.sortTitle(lhs).compareToIgnoreCase(Common.sortTitle(rhs)));
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            String str3 = (String) it.next();
            try {
                JSONObject jSONObject = new JSONObject();
                jSONObject.put("name", str3);
                if (TextUtils.isEmpty(str3)) {
                    jSONArray.put(0, jSONObject);
                } else {
                    jSONArray.put(jSONObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jSONArray;
    }

    private JSONArray doEnumContainer(final Common.ContainerType type, String dsid) {
        String str;
        String[] strArr;
        Cursor cursorQuery;
        String conditionForDownloaded = getConditionForDownloaded();
        String[] strArr2 = new String[0];
        if (dsid != null) {
            conditionForDownloaded = String.format("%s AND %s", conditionForDownloaded, "dsid=?");
            strArr2 = ArrayUtils.addAll(strArr2, dsid);
        }
        String str2 = conditionForDownloaded;
        String[] strArr3 = strArr2;
        JSONArray jSONArray = new JSONArray();
        if (type == Common.ContainerType.GENRE_MODE) {
            str = "genre";
            strArr = new String[]{"genre"};
        } else if (type == Common.ContainerType.COMPOSER_MODE) {
            str = "composer";
            strArr = new String[]{"composer"};
        } else {
            SynoLog.e(LOG_TAG, "unsupported type : " + type.name());
            return jSONArray;
        }
        String[] strArr4 = strArr;
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, strArr4, str2, strArr3, str, null, null, null);
        }
        if (cursorQuery.moveToFirst()) {
            do {
                try {
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("name", cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(str)));
                    jSONArray.put(jSONObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } while (cursorQuery.moveToNext());
        }
        IOUtils.closeSilently(cursorQuery);
        return jSONArray;
    }


    public SongItem[] doEnumContainerSongs(final Common.ContainerType type, final Bundle bundle, String dsid) {
        Cursor cursorQuery;

        String condition = getConditionForDownloaded();
        String[] args = new String[0];

        if (dsid != null) {
            condition = String.format(
                    "%s AND %s",
                    condition,
                    "dsid=?"
            );

            args = ArrayUtils.addAll(args, dsid);
        }

        String artistJoin =
                TextUtils.isEmpty(bundle.getString("artist"))
                        ? " AND "
                        : " OR ";

        switch (type) {

            case COMPOSER_ALBUM_MODE:
                if (bundle.containsKey("album")) {
                    if (bundle.containsKey("album_artist")) {
                        condition +=
                                " AND composer=? AND album=? AND album_artist=?";

                        args = ArrayUtils.addAll(
                                args,
                                bundle.getString("composer"),
                                bundle.getString("album"),
                                bundle.getString("album_artist")
                        );
                    } else {
                        condition +=
                                " AND composer=? AND album=?";

                        args = ArrayUtils.addAll(
                                args,
                                bundle.getString("composer"),
                                bundle.getString("album")
                        );
                    }
                } else {
                    condition += " AND composer=?";

                    args = ArrayUtils.addAll(
                            args,
                            bundle.getString("composer")
                    );
                }
                break;

            case ARTIST_ALBUM_MODE:
                if (bundle.containsKey("album")) {
                    if (bundle.containsKey("album_artist")) {
                        condition +=
                                " AND (artist=?"
                                        + artistJoin +
                                        "album_artist=?) AND album=? AND album_artist=?";

                        args = ArrayUtils.addAll(
                                args,
                                bundle.getString("artist"),
                                bundle.getString("artist"),
                                bundle.getString("album"),
                                bundle.getString("album_artist")
                        );
                    } else {
                        condition +=
                                " AND (artist=?"
                                        + artistJoin +
                                        "album_artist=?) AND album=?";

                        args = ArrayUtils.addAll(
                                args,
                                bundle.getString("artist"),
                                bundle.getString("artist"),
                                bundle.getString("album")
                        );
                    }
                } else {
                    condition +=
                            " AND (artist=?"
                                    + artistJoin +
                                    "album_artist=?)";

                    args = ArrayUtils.addAll(
                            args,
                            bundle.getString("artist"),
                            bundle.getString("artist")
                    );
                }
                break;

            case GENRE_ARTIST_MODE:
                if (bundle.containsKey("genre")) {
                    condition += " AND genre=?";
                    args = ArrayUtils.addAll(
                            args,
                            bundle.getString("genre")
                    );
                }

                if (bundle.containsKey("artist")) {
                    String artist =
                            bundle.getString("artist");

                    condition +=
                            " AND (artist=? OR album_artist=?)";

                    args = ArrayUtils.addAll(
                            args,
                            artist,
                            artist
                    );
                }
                break;

            case GENRE_ARTIST_ALBUM_MODE:
                if (bundle.containsKey("genre")) {
                    condition += " AND genre=?";
                    args = ArrayUtils.addAll(
                            args,
                            bundle.getString("genre")
                    );
                }

                if (bundle.containsKey("artist")) {
                    String artist =
                            bundle.getString("artist");

                    condition +=
                            " AND (artist=? OR album_artist=?)";

                    args = ArrayUtils.addAll(
                            args,
                            artist,
                            artist
                    );
                }

                if (bundle.containsKey("album")) {
                    condition += " AND album=?";

                    args = ArrayUtils.addAll(
                            args,
                            bundle.getString("album")
                    );

                    if (bundle.containsKey("album_artist")) {
                        condition +=
                                " AND album_artist=?";

                        args = ArrayUtils.addAll(
                                args,
                                bundle.getString("album_artist")
                        );
                    }
                }
                break;

            case ALBUM_MODE:
            case SEARCH_ALBUM_MODE:
                if (bundle.containsKey("album_artist")) {
                    condition +=
                            " AND album=? AND album_artist=?";

                    args = ArrayUtils.addAll(
                            args,
                            bundle.getString("album"),
                            bundle.getString("album_artist")
                    );
                } else {
                    condition += " AND album=?";

                    args = ArrayUtils.addAll(
                            args,
                            bundle.getString("album")
                    );
                }
                break;

            case GENRE_MODE:
                condition += " AND genre=?";
                args = ArrayUtils.addAll(
                        args,
                        bundle.getString("genre")
                );
                break;

            case COMPOSER_MODE:
                condition += " AND composer=?";
                args = ArrayUtils.addAll(
                        args,
                        bundle.getString("composer")
                );
                break;

            case SEARCH_ARTIST_MODE:
            case ARTIST_MODE:
                condition +=
                        " AND (artist=?" + artistJoin + "album_artist=?)";

                args = ArrayUtils.addAll(args, bundle.getString("artist"), bundle.getString("artist"));
                break;

            case PLAYLIST_MODE:
                if (bundle.getString("id")
                        .equals(LocalEnumerator.MOST_RECENT_PLAYED)) {
                    return doEnumRecentSongs(dsid);
                }

                if (bundle.getString("id")
                        .equals(LocalEnumerator.MOST_OFTEN_PLAYED)) {
                    return doEnumPopularSongs(dsid);
                }
                break;

            case RATING_MODE:
                int rating =
                        bundle.getInt("song_rating_level");

                if (rating == 0) {
                    condition +=
                            " AND (rating is null OR rating=0)";
                } else {
                    condition += " AND rating>=?";

                    args = ArrayUtils.addAll(
                            args,
                            String.valueOf(rating)
                    );
                }
                break;

            default:
                SynoLog.e(
                        LOG_TAG,
                        "unsupported type : " + type.name()
                );
                return doEnumAllSongs();
        }

        synchronized (mDB) {
            cursorQuery = mDB.query(
                    SONGLIST_TABLE_NAME,
                    null,
                    condition,
                    args,
                    null,
                    null,
                    type == Common.ContainerType.RATING_MODE
                            ? "rating ASC, album ASC, disc ASC, track ASC, path ASC"
                            : "album ASC, disc ASC, track ASC, path ASC"
            );
        }

        return parseSongsFromCursor(cursorQuery);
    }

    private SongItem[] doEnumRecentSongs(String dsid) {
        Cursor cursorQuery;
        String str = getConditionForDownloaded() + " AND hit_count > 0";
        String[] strArr = new String[0];
        if (dsid != null) {
            str = String.format("%s AND %s", str, "dsid=?");
            strArr = ArrayUtils.addAll(strArr, dsid);
        }
        String str2 = str;
        String[] strArr2 = strArr;
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, null, str2, strArr2, null, null, "timestamp DESC", " 0, 100");
        }
        return parseSongsFromCursor(cursorQuery);
    }

    private SongItem[] doEnumPopularSongs(String dsid) {
        Cursor cursorQuery;
        String str = getConditionForDownloaded() + " AND hit_count > 0";
        String[] strArr = new String[0];
        if (dsid != null) {
            str = String.format("%s AND %s", str, "dsid=?");
            strArr = ArrayUtils.addAll(strArr, dsid);
        }
        String str2 = str;
        String[] strArr2 = strArr;
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, null, str2, strArr2, null, null, "hit_count DESC", " 0, 100");
        }
        return parseSongsFromCursor(cursorQuery);
    }


    public void hitSong(final SongItem song, final int hitCount) {
        SynoLog.d(LOG_TAG, "hitSong");
        ContentValues contentValues = new ContentValues();
        contentValues.put(SongItem.SQL_HITCOUNT, Integer.valueOf(hitCount));
        updateTimeAndArgs(song, contentValues);
    }

    public void resetHitSong(final ArrayList<SongItem> songs) {
        for (SongItem song : songs) {
            hitSong(song, 0);
        }
    }

    public List<SongItem> searchDB(Common.SearchCategory category, String key) {
        String strConcat;
        String[] strArr;
        Cursor cursorQuery;
        String str;
        String[] strArr2 = new String[0];
        if (Common.SearchCategory.ALL != category) {
            if (category == Common.SearchCategory.TITLE) {
                str = "title";
            } else if (category == Common.SearchCategory.ALBUM) {
                str = "album";
            } else if (category == Common.SearchCategory.ARTIST) {
                str = "artist";
            } else if (category == Common.SearchCategory.GENRE) {
                str = "genre";
            } else if (category == Common.SearchCategory.COMPOSER) {
                str = "composer";
            } else {
                SynoLog.e("DatabaseAccesser", "Unsupported search category: " + category.toString());
                return new ArrayList<>();
            }
            strConcat = str.concat(" LIKE ?");
            strArr = (String[]) ArrayUtils.addAll(strArr2, "%" + key + "%");
        } else {
            strConcat = "title LIKE ? OR album LIKE ? OR artist LIKE ? OR genre LIKE ? OR composer LIKE ?";
            String str2 = "%" + key + "%";
            strArr = ArrayUtils.addAll(strArr2, str2, str2, str2, str2, str2);
        }
        String str3 = strConcat;
        String[] strArr3 = strArr;
        synchronized (this.mDB) {
            cursorQuery = this.mDB.query(SONGLIST_TABLE_NAME, null, str3, strArr3, null, null, null, null);
        }
        ArrayList<SongItem> arrayList = new ArrayList<>();
        while (cursorQuery != null && cursorQuery.moveToNext()) {
            SongItem songItemFromQueryCursor = SongItem.fromQueryCursor(cursorQuery);
            if (songItemFromQueryCursor != null) {
                arrayList.add(songItemFromQueryCursor);
            }
        }
        IOUtils.closeSilently(cursorQuery);
        return arrayList;
    }

    public synchronized void close() {
        int i = openCounter - 1;
        openCounter = i;
        if (i == 0) {
            synchronized (this.mDB) {
                instance = null;
                this.mDB.close();
                this.mDBHelper.close();
            }
        }
    }


    private static class MyDatabaseHelper extends SQLiteOpenHelper {
        public MyDatabaseHelper(final Context context) {
            super(context, DatabaseAccesser.DATABASE_NAME, null, 6);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) throws SQLException {
            SynoLog.d("MyDatabaseHelper", "onCreate");
            db.execSQL("CREATE TABLE IF NOT EXISTS song_table (" + SongItem.getSQLCreateTableCols() + ");");
            db.execSQL("CREATE TABLE IF NOT EXISTS localPlaylistInfo (" + AudioProvider.Localplaylist_Table.getSQLCreateTableCols() + ");");
            db.execSQL(AudioProvider.Localplaylist_Song_Relation_Table.CREATE_TABLE);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) throws SQLException {
            SynoLog.i(DatabaseAccesser.LOG_TAG, "onUpgrade(): oldVersion:" + oldVersion + ", newVersion:" + newVersion);
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE song_table ADD COLUMN cache_bitrate long;");
                for (SongItem songItem : DatabaseAccesser.parseSongsFromCursor(db.query(DatabaseAccesser.SONGLIST_TABLE_NAME, null, null, null, null, null, null))) {
                    String cachePath = songItem.getCachePath();
                    long bitrate = songItem.getBitrate();
                    if (cachePath != null && cachePath.endsWith(".mp3") && Utilities.checkPathAvailable(cachePath)) {
                        SynoLog.i(DatabaseAccesser.LOG_TAG, "updateCacheBitrate " + cachePath + ", to " + bitrate);
                        updateCacheBitrate(db, songItem, bitrate);
                    }
                }
            }
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE song_table ADD COLUMN rating integer;");
            }
            if (oldVersion < 4) {
                db.execSQL("CREATE TABLE IF NOT EXISTS localPlaylistInfo (" + AudioProvider.Localplaylist_Table.getSQLCreateTableCols() + ");");
                db.execSQL(AudioProvider.Localplaylist_Song_Relation_Table.CREATE_TABLE_V5);
            }
            if (oldVersion < 5) {
                db.execSQL("ALTER TABLE song_table ADD COLUMN download_type integer default 1 ;");
            }
            if (oldVersion < 6) {
                db.execSQL(AudioProvider.Localplaylist_Song_Relation_Table.CREATE_TABLE);
                Cursor cursorRawQuery = db.rawQuery("SELECT localPlaylist_song_relation.playlist_dsid,localPlaylist_song_relation.playlist_id,localPlaylist_song_relation.song_dsid,localPlaylist_song_relation.song_path,song_table.songid FROM song_table INNER JOIN localPlaylist_song_relation ON song_table.path = localPlaylist_song_relation.song_path", null);
                if (cursorRawQuery != null && cursorRawQuery.moveToFirst()) {
                    do {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.PLAYLIST_DSID, cursorRawQuery.getString(0));
                        contentValues.put(AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.PLAYLIST_ID, cursorRawQuery.getString(1));
                        contentValues.put(AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.SONG_DSID, cursorRawQuery.getString(2));
                        contentValues.put(AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.SONG_PATH, cursorRawQuery.getString(3));
                        contentValues.put(AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.SONG_ID, cursorRawQuery.getString(4));
                        db.insert(AudioProvider.LOCALPLAYLIST_SONG_RELATION_TABLE_NAME, null, contentValues);
                    } while (cursorRawQuery.moveToNext());
                }
                if (cursorRawQuery != null && !cursorRawQuery.isClosed()) {
                    cursorRawQuery.close();
                }
                db.execSQL("DROP TABLE IF EXISTS localPlaylist_song_relation");
            }
        }

        private void updateCacheBitrate(final SQLiteDatabase db, SongItem song, long bitrate) {
            int iUpdate;
            ContentValues contentValues = new ContentValues();
            contentValues.put(SongItem.SQL_CACHEBITRATE, Long.valueOf(bitrate));
            synchronized (db) {
                iUpdate = db.update(DatabaseAccesser.SONGLIST_TABLE_NAME, contentValues, "dsid = ? AND path= ? AND track= ?  ", new String[]{song.getDsId(), song.getFilePath(), Integer.toString(song.getTrack())});
            }
            SynoLog.d(DatabaseAccesser.LOG_TAG, iUpdate + " update SongItem = " + song.getCachePath() + ", args = " + contentValues.toString());
        }
    }


}
