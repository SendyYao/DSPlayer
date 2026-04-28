package com.whisperyao.dsplayer.item;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.text.TextUtils;
import com.google.android.gms.cast.HlsSegmentFormat;
//import com.google.firebase.sessions.settings.RemoteSettings;
import com.google.gson.Gson;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.model.data.PlayingQueueManager;
//import com.whisperyao.dsplayer.provider.AudioProvider;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.vos.api.ApiSongsResponseVo;
//import com.whisperyao.dsplayer.vos.api.ApiSongsResponseVo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;


public class SongItem extends Item {
    private static final String ADDITIONAL = "additional";
    private static final String ALBUM_ART_URL = "albumArtURL";
    private static final String CONTAINER = "container";
    private static final String COVER_URL = "cover_url";
    public static final int DOWNLOAD_TYPE_AUTOMATIC = 1;
    public static final int DOWNLOAD_TYPE_MANUAL = 2;
    private static final String FILE = "file";
    private static final String FOLDER = "folder";
    private static final String FORMAT = "format";
    private static final String IS_CONTAINER = "is_container";
    private static final String ITEMID = "item_id";
    static List<String> NO_TRANSCODE_FORMATS = Arrays.asList("aacp", HlsSegmentFormat.AAC, HlsSegmentFormat.MP3);
    private static final String RADIO_ITEM_PREFIX = "inetradio";
    private static final String RADIO_THUMB_IMAGE = "thumb_inetradio.png";
    private static final String RATING = "rating";
    private static final String REMOTE = "remote";
    private static final String RES = "res";
    private static final String SAMPLE = "sample";
    private static final String SIZE = "size";
    private static final String SONG_AUDIO = "song_audio";
    private static final String SONG_RATING = "song_rating";
    private static final String SONG_TAG = "song_tag";
    private static final String SONG_URL = "song_url";
    public static final String SQL_ALBUM = "album";
    public static final String SQL_ALBUM_ARTIST = "album_artist";
    public static final String SQL_ARTIST = "artist";
    public static final String SQL_BITRATE = "bitrate";
    public static final String SQL_CACHEBITRATE = "cache_bitrate";
    public static final String SQL_CACHEPATH = "cache_path";
    public static final String SQL_CHANNEL = "channel";
    public static final String SQL_CODEC = "codec";
    public static final String SQL_COMMENT = "comment";
    public static final String SQL_COMPOSER = "composer";
    public static final String SQL_CONTAINER = "container";
    public static final String SQL_COVER_PATH = "cover_path";
    public static final String SQL_DISC = "disc";
    public static final String SQL_DOWNLOAD_TYPE = "download_type";
    public static final String SQL_DSID = "dsid";
    public static final String SQL_DURATION = "duration";
    public static final String SQL_FILESIZE = "filesize";
    public static final String SQL_FREQUENCY = "frequency";
    public static final String SQL_GENRE = "genre";
    public static final String SQL_HITCOUNT = "hit_count";
    public static final String SQL_LYRIC_PATH = "lyric_path";
    public static final String SQL_PATH = "path";
    public static final String SQL_RATING = "rating";
    public static final String SQL_SONGID = "songid";
    public static final String SQL_TIMESTAMP = "timestamp";
    public static final String SQL_TITLE = "title";
    public static final String SQL_TRACK = "track";
    public static final String SQL_YEAR = "year";
    private static final String STATION = "station";
    private static final String STREAMID = "stream_id";
    private static final String URL = "url";
    private String mAlbum;
    private String mArtist;
    private long mBitrate;
    private long mCacheBitrate;
    private String mCachePath;
    private int mChannel;
    private String mCodec;
    private String mComment;
    private String mComposer;
    private String mContainer;
    private String mCoverPath;
    private String mCoverUrl;
    private int mDisc;
    private int mDownloadType;
    private String mDsId;
    private int mDuration;
    private String mFilePath;
    private long mFileSize;
    private String mFormat;
    private int mFrequency;
    private String mGenre;
    private int mHitCount;
    private String mLyricPath;
    private String mSongUrl;
    private String mStreamId;
    private long mTimeStamp;
    private int mTrack;
    private int mYear;

    public SongItem(Item.ItemType type, String id, String title) {
        super(type, id, title);
        this.mDsId = "";
        this.mArtist = "";
        this.mAlbum = "";
        this.mComposer = "";
        this.mGenre = "";
        this.mFilePath = "";
        this.mCachePath = "";
        this.mCoverPath = "";
        this.mLyricPath = "";
        this.mDownloadType = 1;
        this.mComment = "";
        this.mCoverUrl = "";
        this.mSongUrl = "";
        this.mDisc = 0;
        this.mTrack = 0;
        this.mYear = 0;
        this.mDuration = 0;
        this.mFrequency = 0;
        this.mChannel = 0;
        this.mHitCount = 0;
        this.mTimeStamp = 0L;
        this.mFileSize = 0L;
        this.mCodec = "";
        this.mContainer = "";
        this.mBitrate = 0L;
        this.mCacheBitrate = 0L;
        this.mStreamId = "";
        this.mFormat = "";
    }

    public boolean isVirtualSong() {
        return getID().startsWith("music_v") || getID().startsWith("music_p_v");
    }

    public void setStreamId(String id) {
        this.mStreamId = id;
    }

    public String getStreamId() {
        return this.mStreamId;
    }

    public void setFormat(String format) {
        this.mFormat = format;
    }

    public String getFormat() {
        return this.mFormat;
    }

    public static SongItem generateNoneSong() {
        return new SongItem(Item.ItemType.FILE_MODE, "", "");
    }

    public String getUniqueKey() {
        return Utilities.getMD5Code(this.mDsId + this.mID + this.mFilePath);
    }

    public void setDsId(String id) {
        this.mDsId = id;
    }

    public String getDsId() {
        return this.mDsId;
    }

    public void setArtist(String artist) {
        this.mArtist = artist;
    }

    public String getArtist() {
        return this.mArtist;
    }

    public void setAlbum(String album) {
        this.mAlbum = album;
    }

    public String getAlbum() {
        return this.mAlbum;
    }

    public void setComposer(String composer) {
        this.mComposer = composer;
    }

    public String getComposer() {
        return this.mComposer;
    }

    public void setGenre(String genre) {
        this.mGenre = genre;
    }

    public String getGenre() {
        return this.mGenre;
    }

    public void setFilePath(String path) {
        this.mFilePath = path;
    }

    public String getFilePath() {
        return this.mFilePath;
    }

    public void setCachePath(String path) {
        this.mCachePath = path;
    }

    public String getCachePath() {
        return this.mCachePath;
    }

    public void setCoverPath(String path) {
        this.mCoverPath = path;
    }

    public String getCoverPath() {
        return this.mCoverPath;
    }

    public void setLyricPath(String path) {
        this.mLyricPath = path;
    }

    public String getLyricPath() {
        return this.mLyricPath;
    }

    public void setDownloadType(int key) {
        this.mDownloadType = key;
    }

    public int getDownloadType() {
        return this.mDownloadType;
    }

    public void setComment(String comment) {
        this.mComment = comment;
    }

    public String getComment() {
        return this.mComment;
    }

    public void setCoverUrl(String url) {
        this.mCoverUrl = url;
    }

    public String getCoverUrl() {
        return this.mCoverUrl;
    }

    public void setSongUrl(String url) {
        this.mSongUrl = url;
    }

    public String getSongUrl() {
        return this.mSongUrl;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public int getDuration() {
        return this.mDuration;
    }

    public void setFrequency(int frequency) {
        this.mFrequency = frequency;
    }

    public int getFrequency() {
        return this.mFrequency;
    }

    public void setChannel(int channel) {
        this.mChannel = channel;
    }

    public int getChannel() {
        return this.mChannel;
    }

    public void setHitCount(int count) {
        this.mHitCount = count;
    }

    public int getHitCount() {
        return this.mHitCount;
    }

    public void setDisc(int disc) {
        this.mDisc = disc;
    }

    public int getDisc() {
        return this.mDisc;
    }

    public void setTrack(int track) {
        this.mTrack = track;
    }

    public int getTrack() {
        return this.mTrack;
    }

    public void setYear(int year) {
        this.mYear = year;
    }

    public int getYear() {
        return this.mYear;
    }

    public void setTimeStamp(long time) {
        this.mTimeStamp = time;
    }

    public long getTimeStamp() {
        return this.mTimeStamp;
    }

    public String getTimeString() {
        return Utilities.makeTimeString(this.mDuration);
    }


    public void setFileSize(long filesize) {
        this.mFileSize = filesize;
    }

    public long getFileSize() {
        return this.mFileSize;
    }

    public void setBitrate(long bitrate) {
        this.mBitrate = bitrate;
    }

    public long getBitrate() {
        return this.mBitrate;
    }

    public void setCodec(String codec) {
        this.mCodec = codec;
    }

    public String getCodec() {
        return this.mCodec;
    }

    public void setContainer(String container) {
        this.mContainer = container;
    }

    public String getContainer() {
        return this.mContainer;
    }

    public void setCacheBitrate(long bitrate) {
        this.mCacheBitrate = bitrate;
    }

    public long getCacheBitrate() {
        return this.mCacheBitrate;
    }

    @Override
    public Bundle getBundle() {
        Bundle bundle = super.getBundle();
        bundle.putString("dsid", this.mDsId);
        bundle.putString("artist", this.mArtist);
        bundle.putString("album", this.mAlbum);
        bundle.putString("composer", this.mComposer);
        bundle.putString("genre", this.mGenre);
        bundle.putString("album_artist", this.mAlbumArtist);
        bundle.putString("path", this.mFilePath);
        bundle.putString(SQL_CACHEPATH, this.mCachePath);
        bundle.putString(SQL_COVER_PATH, this.mCoverPath);
        bundle.putString(SQL_LYRIC_PATH, this.mLyricPath);
        bundle.putInt(SQL_DOWNLOAD_TYPE, this.mDownloadType);
        bundle.putString(COVER_URL, this.mCoverUrl);
        bundle.putInt("duration", this.mDuration);
        bundle.putInt(SQL_FREQUENCY, this.mFrequency);
        bundle.putInt(SQL_CHANNEL, this.mChannel);
        bundle.putInt(SQL_HITCOUNT, this.mHitCount);
        bundle.putInt(SQL_DISC, this.mDisc);
        bundle.putInt(SQL_TRACK, this.mTrack);
        bundle.putInt(SQL_YEAR, this.mYear);
        bundle.putLong(SQL_TIMESTAMP, this.mTimeStamp);
        bundle.putLong(SQL_FILESIZE, this.mFileSize);
        bundle.putLong(SQL_BITRATE, this.mBitrate);
        bundle.putString(SQL_CODEC, this.mCodec);
        bundle.putString("container", this.mContainer);
        bundle.putString(FORMAT, this.mFormat);
        bundle.putString(STREAMID, this.mStreamId);
        bundle.putInt("rating", getSongRating());
        return bundle;
    }

    public static SongItem fromBundle(final Bundle bundle) {
        SongItem songItem = new SongItem(Item.ItemType.valueOf(bundle.getString("type")), bundle.getString("id"), bundle.getString("title"));
        songItem.setDsId(bundle.getString("dsid"));
        songItem.setArtist(bundle.getString("artist"));
        songItem.setAlbum(bundle.getString("album"));
        songItem.setComposer(bundle.getString("composer"));
        songItem.setGenre(bundle.getString("genre"));
        songItem.setAlbumArtist(bundle.getString("album_artist"));
        songItem.setFilePath(bundle.getString("path"));
        songItem.setCachePath(bundle.getString(SQL_CACHEPATH));
        songItem.setCoverPath(bundle.getString(SQL_COVER_PATH));
        songItem.setLyricPath(bundle.getString(SQL_LYRIC_PATH));
        songItem.setDownloadType(bundle.getInt(SQL_DOWNLOAD_TYPE));
        songItem.setCoverUrl(bundle.getString(COVER_URL));
        songItem.setDuration(bundle.getInt("duration"));
        songItem.setFrequency(bundle.getInt(SQL_FREQUENCY));
        songItem.setChannel(bundle.getInt(SQL_CHANNEL));
        songItem.setHitCount(bundle.getInt(SQL_HITCOUNT));
        songItem.setDisc(bundle.getInt(SQL_DISC));
        songItem.setTrack(bundle.getInt(SQL_TRACK));
        songItem.setYear(bundle.getInt(SQL_YEAR));
        songItem.setTimeStamp(bundle.getLong(SQL_TIMESTAMP));
        songItem.setFileSize(bundle.getLong(SQL_FILESIZE));
        songItem.setBitrate(bundle.getLong(SQL_BITRATE));
        songItem.setCodec(bundle.getString(SQL_CODEC, ""));
        songItem.setContainer(bundle.getString("container", ""));
        songItem.setFormat(bundle.getString(FORMAT));
        songItem.setStreamId(bundle.getString(STREAMID));
        songItem.setSongRating(bundle.getInt("rating", -1));
        return songItem;
    }

    public String toJsonString() {
        return new Gson().toJson(this);
    }

    public static SongItem fromJsonString(String jsonString) {
        return (SongItem) new Gson().fromJson(jsonString, SongItem.class);
    }

    public static SongItem fromApiJson(final JSONObject jsonObj) throws JSONException {
        String str = "";
        String strOptString = jsonObj.optString("type", "");
        if ("file".compareToIgnoreCase(strOptString) == 0 || REMOTE.compareToIgnoreCase(strOptString) == 0) {
            SongItem songItem = new SongItem("file".compareToIgnoreCase(strOptString) == 0 ? Item.ItemType.FILE_MODE : Item.ItemType.RADIO_MODE, jsonObj.getString("id"), jsonObj.getString("title"));
            songItem.setDsId(Common.getDsId());
            songItem.setFilePath(jsonObj.getString("path"));
            JSONObject jSONObject = jsonObj.getJSONObject("additional");
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject(SONG_TAG);
            if (jSONObjectOptJSONObject != null) {
                songItem.setArtist(jSONObjectOptJSONObject.optString("artist", ""));
                songItem.setAlbum(jSONObjectOptJSONObject.optString("album", ""));
                songItem.setComposer(jSONObjectOptJSONObject.optString("composer", ""));
                songItem.setGenre(jSONObjectOptJSONObject.optString("genre", ""));
                songItem.setAlbumArtist(jSONObjectOptJSONObject.optString("album_artist", ""));
                songItem.setComment(jSONObjectOptJSONObject.optString(SQL_COMMENT, ""));
                songItem.setDisc(jSONObjectOptJSONObject.optInt(SQL_DISC));
                songItem.setTrack(jSONObjectOptJSONObject.optInt(SQL_TRACK));
                songItem.setYear(jSONObjectOptJSONObject.optInt(SQL_YEAR));
            }
            JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject(SONG_AUDIO);
            if (jSONObjectOptJSONObject2 != null) {
                songItem.setDuration(jSONObjectOptJSONObject2.getInt("duration"));
                songItem.setFrequency(jSONObjectOptJSONObject2.getInt(SQL_FREQUENCY));
                songItem.setChannel(jSONObjectOptJSONObject2.getInt(SQL_CHANNEL));
                songItem.setBitrate(jSONObjectOptJSONObject2.getLong(SQL_BITRATE));
                songItem.setCodec(jSONObjectOptJSONObject2.optString(SQL_CODEC, ""));
                songItem.setContainer(jSONObjectOptJSONObject2.optString("container", ""));
                songItem.setFileSize(jSONObjectOptJSONObject2.getLong(SQL_FILESIZE));
            }
            songItem.setSongRating((songItem.isOnDS() && jSONObject.has(SONG_RATING)) ? jSONObject.getJSONObject(SONG_RATING).getInt("rating") : -1);
            return songItem;
        }
        if ("folder".compareToIgnoreCase(strOptString) == 0) {
            try {
                if (jsonObj.getBoolean(Item.IS_PERSONAL)) {
                    str = " (home)";
                }
            } catch (JSONException unused) {
            }
            return new SongItem(Item.ItemType.DIRECTORY_MODE, jsonObj.getString("id"), jsonObj.getString("title") + str);
        }
        if ("container".compareToIgnoreCase(strOptString) == 0) {
            return new SongItem(Item.ItemType.CONTAINER_MODE, jsonObj.getString("id"), jsonObj.getString("title"));
        }
        if (STATION.compareToIgnoreCase(strOptString) != 0) {
            return null;
        }
        SongItem songItem2 = new SongItem(Item.ItemType.RADIO_MODE, jsonObj.getString("id"), jsonObj.getString("title"));
        songItem2.setDsId(Common.getDsId());
        songItem2.setFilePath(jsonObj.optString("url", ""));
        return songItem2;
    }

    public static SongItem fromCgiJson(final JSONObject jsonObj) throws JSONException {
        if (jsonObj.has("folder")) {
            return new SongItem(Item.ItemType.DIRECTORY_MODE, jsonObj.getString("id"), jsonObj.getString("title"));
        }
        Bundle bundle = new Bundle();
        bundle.putString(SQL_SONGID, jsonObj.optString("id", ""));
        bundle.putString("path", jsonObj.optString("path", ""));
        bundle.putInt("duration", jsonObj.optInt("duration"));
        if (isRadioItem(bundle)) {
            if (Boolean.valueOf(jsonObj.optBoolean(IS_CONTAINER, false)).booleanValue()) {
                return new SongItem(Item.ItemType.CONTAINER_MODE, jsonObj.getString("id"), jsonObj.getString("title"));
            }
            SongItem songItem = new SongItem(Item.ItemType.RADIO_MODE, jsonObj.getString("id"), jsonObj.getString("title"));
            songItem.setDsId(Common.getDsId());
            songItem.setFilePath(jsonObj.getString("path"));
            return songItem;
        }
        SongItem songItem2 = new SongItem(Item.ItemType.FILE_MODE, jsonObj.getString("id"), jsonObj.getString("title"));
        songItem2.setDsId(Common.getDsId());
        songItem2.setArtist(jsonObj.optString("artist", ""));
        songItem2.setAlbum(jsonObj.optString("album", ""));
        songItem2.setGenre(jsonObj.optString("genre", ""));
        songItem2.setFilePath(jsonObj.optString("path", ""));
        songItem2.setComment(jsonObj.optString(SQL_COMMENT, ""));
        songItem2.setDisc(jsonObj.optInt(SQL_DISC));
        songItem2.setTrack(jsonObj.optInt(SQL_TRACK));
        songItem2.setYear(jsonObj.optInt(SQL_YEAR));
        songItem2.setDuration(jsonObj.optInt("duration"));
        songItem2.setFrequency(jsonObj.optInt(SQL_FREQUENCY));
        songItem2.setChannel(jsonObj.optInt(SQL_CHANNEL));
        songItem2.setBitrate(jsonObj.optLong(SQL_BITRATE));
        songItem2.setFileSize(jsonObj.optLong(SQL_FILESIZE));
        return songItem2;
    }

    public static SongItem fromCgiUSBJson(final JSONObject jsonObj) throws JSONException {
        String string = jsonObj.getString("item_id");
        if (jsonObj.has("folder")) {
            return new SongItem(Item.ItemType.DIRECTORY_MODE, string.substring(string.indexOf(64) + 1), jsonObj.getString("title"));
        }
        Item.ItemType itemType = Item.ItemType.FILE_MODE;
        Bundle bundle = new Bundle();
        bundle.putString(ALBUM_ART_URL, jsonObj.optString(ALBUM_ART_URL, ""));
        bundle.putString("path", jsonObj.optString("res", ""));
        bundle.putInt("duration", jsonObj.optInt("duration"));
        if (isRadioItem(bundle)) {
            itemType = Item.ItemType.RADIO_MODE;
        }
        SongItem songItem = new SongItem(itemType, string.substring(string.indexOf(64) + 1), jsonObj.getString("title"));
        songItem.setDsId(Common.getDsId());
        songItem.setArtist(jsonObj.optString("artist", ""));
        songItem.setAlbum(jsonObj.optString("album", ""));
        songItem.setGenre(jsonObj.optString("genre", ""));
        songItem.setFilePath(jsonObj.optString("res", ""));
        songItem.setDuration(jsonObj.optInt("duration"));
        songItem.setFrequency(jsonObj.optInt(SAMPLE));
        songItem.setChannel(jsonObj.optInt(SQL_CHANNEL));
        songItem.setBitrate(jsonObj.optLong(SQL_BITRATE));
        songItem.setFileSize(jsonObj.optLong(SIZE));
        return songItem;
    }

    public static List<SongItem> fromApiVo(ApiSongsResponseVo Vo) {
        List<ApiSongsResponseVo.SongVo> songs = Vo.getSongs();
        ArrayList arrayList = new ArrayList();
        Iterator<ApiSongsResponseVo.SongVo> it = songs.iterator();
        while (it.hasNext()) {
            arrayList.add(fromApiVo(it.next()));
        }
        return arrayList;
    }

    public static SongItem fromApiVo(ApiSongsResponseVo.SongVo song) {
        if ("file".compareToIgnoreCase(song.getType()) == 0 || REMOTE.compareToIgnoreCase(song.getType()) == 0) {
            SongItem songItem = new SongItem("file".compareToIgnoreCase(song.getType()) == 0 ? Item.ItemType.FILE_MODE : Item.ItemType.RADIO_MODE, song.getId(), song.getTitle());
            songItem.setDsId(Common.getDsId());
            songItem.setFilePath(song.getPath());
            ApiSongsResponseVo.SongVo.AdditionalVo additionalVo = song.getAdditionalVo();
            ApiSongsResponseVo.SongVo.AdditionalVo.SongTagVo songTagVo = additionalVo.getSongTagVo();
            if (songTagVo != null) {
                songItem.setArtist(songTagVo.getArtist());
                songItem.setAlbum(songTagVo.getAlbum());
                songItem.setComposer(songTagVo.getComposer());
                songItem.setGenre(songTagVo.getGenre());
                songItem.setAlbumArtist(songTagVo.getAlbumArtist());
                songItem.setComment(songTagVo.getComment());
                songItem.setDisc(songTagVo.getDisc());
                songItem.setTrack(songTagVo.getTrack());
                songItem.setYear(songTagVo.getYear());
            }
            ApiSongsResponseVo.SongVo.AdditionalVo.SongAudioVo songAudioVo = additionalVo.getSongAudioVo();
            if (songAudioVo != null) {
                songItem.setDuration(songAudioVo.getDuration());
                songItem.setFrequency(songAudioVo.getFrequency());
                songItem.setChannel(songAudioVo.getChannel());
                songItem.setBitrate(songAudioVo.getBitRate());
                songItem.setFileSize(songAudioVo.getFileSize());
                songItem.setCodec(songAudioVo.getCodec());
                songItem.setContainer(songAudioVo.getContainer());
            }
            songItem.setSongRating(additionalVo.getSongRatingVo() != null ? song.getRating() : -1);
            return songItem;
        }
        if ("folder".compareToIgnoreCase(song.getType()) == 0) {
            return new SongItem(Item.ItemType.DIRECTORY_MODE, song.getId(), song.getTitle() + (song.isPersonal() ? " (home)" : ""));
        }
        if ("container".compareToIgnoreCase(song.getType()) == 0) {
            return new SongItem(Item.ItemType.CONTAINER_MODE, song.getId(), song.getTitle());
        }
        if (STATION.compareToIgnoreCase(song.getType()) != 0) {
            return null;
        }
        SongItem songItem2 = new SongItem(Item.ItemType.RADIO_MODE, song.getId(), song.getTitle());
        songItem2.setDsId(Common.getDsId());
        songItem2.setFilePath(song.getUrl());
        return songItem2;
    }

    public static String getSQLCreateTableCols() {
        return ((((((((((((((((((((((("id integer primary key autoincrement, dsid text not null, songid text, title text, ") + "artist text, ") + "album text, ") + "composer text, ") + "genre text, ") + "album_artist text, ") + "path text not null, ") + "duration integer, ") + "filesize long, ") + "bitrate long, ") + "channel integer, ") + "frequency integer, ") + "cache_path text, ") + "timestamp long default 0, ") + "hit_count int default 0, ") + "cover_path text, ") + "lyric_path text, ") + "disc integer, ") + "track integer, ") + "year integer, ") + "comment text, ") + "cache_bitrate long, ") + "rating integer default -1, ") + "download_type int default 1";
    }

    @SuppressLint("Range")
    public static SongItem fromQueryCursor(final Cursor cursor) {
        SongItem songItem = new SongItem(Item.ItemType.FILE_MODE, cursor.getString(cursor.getColumnIndex(SQL_SONGID)), cursor.getString(cursor.getColumnIndex("title")));
        songItem.setDsId(cursor.getString(cursor.getColumnIndex("dsid")));
        songItem.setArtist(cursor.getString(cursor.getColumnIndex("artist")));
        songItem.setAlbum(cursor.getString(cursor.getColumnIndex("album")));
        songItem.setComposer(cursor.getString(cursor.getColumnIndex("composer")));
        songItem.setGenre(cursor.getString(cursor.getColumnIndex("genre")));
        songItem.setAlbumArtist(cursor.getString(cursor.getColumnIndex("album_artist")));
        songItem.setFilePath(cursor.getString(cursor.getColumnIndex("path")));
        songItem.setCachePath(cursor.getString(cursor.getColumnIndex(SQL_CACHEPATH)));
        songItem.setCacheBitrate(cursor.getLong(cursor.getColumnIndex(SQL_CACHEBITRATE)));
        songItem.setComment(cursor.getString(cursor.getColumnIndex(SQL_COMMENT)));
        songItem.setTimeStamp(cursor.getLong(cursor.getColumnIndex(SQL_TIMESTAMP)));
        songItem.setHitCount(cursor.getInt(cursor.getColumnIndex(SQL_HITCOUNT)));
        songItem.setCoverPath(cursor.getString(cursor.getColumnIndex(SQL_COVER_PATH)));
        songItem.setLyricPath(cursor.getString(cursor.getColumnIndex(SQL_LYRIC_PATH)));
        songItem.setDisc(cursor.getInt(cursor.getColumnIndex(SQL_DISC)));
        songItem.setTrack(cursor.getInt(cursor.getColumnIndex(SQL_TRACK)));
        songItem.setYear(cursor.getInt(cursor.getColumnIndex(SQL_YEAR)));
        songItem.setDuration(cursor.getInt(cursor.getColumnIndex("duration")));
        songItem.setFrequency(cursor.getInt(cursor.getColumnIndex(SQL_FREQUENCY)));
        songItem.setChannel(cursor.getInt(cursor.getColumnIndex(SQL_CHANNEL)));
        songItem.setFileSize(cursor.getLong(cursor.getColumnIndex(SQL_FILESIZE)));
        songItem.setBitrate(cursor.getLong(cursor.getColumnIndex(SQL_BITRATE)));
        if (cursor.getColumnIndex("rating") != -1) {
            songItem.setRating(cursor.getInt(cursor.getColumnIndex("rating")));
        }
        if (cursor.getColumnIndex(SQL_DOWNLOAD_TYPE) != -1) {
            songItem.setDownloadType(cursor.getInt(cursor.getColumnIndex(SQL_DOWNLOAD_TYPE)));
        }
        return songItem;
    }

    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("dsid", getDsId());
        contentValues.put(SQL_SONGID, getID());
        contentValues.put("title", getTitle());
        contentValues.put("artist", getArtist());
        contentValues.put("album", getAlbum());
        contentValues.put("composer", getComposer());
        contentValues.put("genre", getGenre());
        contentValues.put("album_artist", getAlbumArtist());
        contentValues.put("path", getFilePath());
        contentValues.put(SQL_TIMESTAMP, Long.valueOf(getTimeStamp()));
        contentValues.put(SQL_HITCOUNT, Integer.valueOf(getHitCount()));
        contentValues.put(SQL_COVER_PATH, getCoverPath());
        contentValues.put(SQL_LYRIC_PATH, getLyricPath());
        contentValues.put(SQL_DOWNLOAD_TYPE, Integer.valueOf(getDownloadType()));
        contentValues.put("duration", Integer.valueOf(getDuration()));
        contentValues.put(SQL_FREQUENCY, Integer.valueOf(getFrequency()));
        contentValues.put(SQL_CHANNEL, Integer.valueOf(getChannel()));
        contentValues.put(SQL_FILESIZE, Long.valueOf(getFileSize()));
        contentValues.put(SQL_BITRATE, Long.valueOf(getBitrate()));
        contentValues.put(SQL_COMMENT, getComment());
        contentValues.put(SQL_DISC, Integer.valueOf(getDisc()));
        contentValues.put(SQL_TRACK, Integer.valueOf(getTrack()));
        contentValues.put(SQL_YEAR, Integer.valueOf(getYear()));
        contentValues.put("rating", Integer.valueOf(getSongRating()));
        SynoLog.d("getContentValues", "args = " + contentValues.toString());
        return contentValues;
    }

    public int getSongRating() {
        return (int) this.mRating;
    }

    public void setSongRating(int rating) {
        this.mRating = rating;
    }

    private static boolean isRadioItem(final Bundle bundle) {
        if ((bundle.containsKey(SQL_SONGID) ? bundle.getString(SQL_SONGID) : "").startsWith(RADIO_ITEM_PREFIX)) {
            return true;
        }
        if ((bundle.containsKey(ALBUM_ART_URL) ? bundle.getString(ALBUM_ART_URL) : "").contains(RADIO_THUMB_IMAGE)) {
            return true;
        }
        return (bundle.containsKey("path") ? bundle.getString("path") : "").toLowerCase(Locale.getDefault()).startsWith("http") && (bundle.containsKey("duration") ? bundle.getInt("duration") : 0) == 0;
    }

    public boolean hasHttpURL() {
        String filePath = getFilePath();
        return filePath != null && filePath.toLowerCase(Locale.getDefault()).startsWith("http");
    }

    public String getSongDescription() {
        if (TextUtils.isEmpty(this.mArtist)) {
            return !TextUtils.isEmpty(this.mAlbum) ? "" + this.mAlbum : "";
        }
        String str = "" + this.mArtist;
        if (TextUtils.isEmpty(this.mAlbum)) {
            return str;
        }
        // RemoteSettings.FORWARD_SLASH_STRING
        return (str + "/") + this.mAlbum;
    }


    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return toJsonString().equals(((SongItem) obj).toJsonString());
    }

    public boolean equalsByIdPathTrack(SongItem song) {
        return song != null && this.mDsId.equals(song.getDsId()) && this.mFilePath.equals(song.getFilePath()) && this.mTrack == song.getTrack();
    }

    public boolean isFile() {
        return isOnDS() || isRemoteFile();
    }

    public boolean isOnDS() {
        return getType().isFile();
    }

    public boolean isOnRemote() {
        return getType().isRadioItem();
    }

    public boolean isRemoteFile() {
        return getType().isRadioItem() && getDuration() > 0;
    }

    public boolean isRadio() {
        return getType().isRadioItem() && getDuration() == 0;
    }

    public boolean isNoTranscodeRadio() {
        String format = getFormat();
        if (format != null) {
            return NO_TRANSCODE_FORMATS.contains(format.toLowerCase(Locale.getDefault())) && isRadio();
        }
        return false;
    }

    public String getMediaId() {
        if (isRadio()) {
            return PlayingQueueManager.PREFIX_RADIO + getUniqueKey();
        }
        return PlayingQueueManager.PREFIX_SONG + getUniqueKey();
    }

    public MediaDescriptionCompat toDescription() {
        return new MediaDescriptionCompat.Builder().setMediaId(getMediaId()).setTitle(this.mTitle).setSubtitle(this.mArtist).setDescription(this.mAlbum).build();
    }
}