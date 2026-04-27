package com.whisperyao.dsplayer;

import android.os.Bundle;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair;
import com.whisperyao.dsplayer.datasource.network.vo.ApiPath;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.AudioStationAPI;
import com.whisperyao.dsplayer.net.WebAPI;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.TranscodeSetting;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.vos.api.ApiPlaylistResponseVo;
import com.whisperyao.dsplayer.vos.api.ApiSongsResponseVo;
import com.whisperyao.dsplayer.vos.api.pin.PinListResponseVo;
import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import okhttp3.Response;

public class ApiEnumerator extends AbstractNetManager {
    private static final String LOG = "ApiEnumerator";
    private static final String AVG_RATING = "avg_rating";

    private static final String FILTER = "filter";

    private static final String RECURSIVE = "recursive";
    private static final String SHARING_INFO = "sharing_info";
    private static final String SORT_BY = "sort_by";
    private static final String SONG_RATING = "song_rating";
    private static final String SONGS_LIMIT = "songs_limit";
    private static final String SONGS_OFFSET = "songs_offset";
    private static final String SORT_DIRECTION = "sort_direction";
    private static final String STREAM_ID = "stream_id";


    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public ConnectionManager.ResourceType getResourceType() {
        return ConnectionManager.ResourceType.API;
    }

    @Override
    protected boolean isWithRating() {
        return WebAPI.getInstance().canSupportRating();
    }

    @Override
    protected boolean isSupportPin() {
        return WebAPI.getInstance().canSupportPin();
    }

    @Override
    protected boolean canSupportAddToNext() {
        return WebAPI.getInstance().canSupportAddToNext();
    }

    @Override
    protected boolean canSupportGenreArtist() {
        return WebAPI.getInstance().canSupportGenreArtist();
    }

    @Override
    protected String getPlayUrl(SongItem song, boolean isForChromecast) {
        WebAPI webAPI = WebAPI.getInstance();
        String sid = Common.getSID();

        TranscodeSetting transcodeSetting = AudioPreference.getTranscodeSetting();
        TranscodeSetting.TranscodeDownloadQuality preferredDownloadQuality =
                Common.getPreferredDownloadQuality(song, transcodeSetting, isForChromecast);

        String playUrl;

        if (song.hasHttpURL()) {
            ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PROXY);
            if (knownAPI == null) {
                SynoLog.e(LOG, "SYNO.AudioStation.Proxy api doesn't exist");
                return "";
            }

            RadioEditor.retrieveStreamInfo(song);

            boolean isStreamFormat = Utilities.isStreamFormat(
                    song.getFormat(),
                    song.getFrequency(),
                    isForChromecast
            );

            String baseUrl =
                    Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath())
                            + "?api=SYNO.AudioStation.Proxy"
                            + "&method=stream"
                            + "&version=1"
                            + "&stream_id=" + song.getStreamId()
                            + "&_sid=" + sid;

            if (isStreamFormat || song.isNoTranscodeRadio()) {
                playUrl = baseUrl
                        + "&format=raw"
                        + "&ext=." + song.getFormat();
            } else if (Common.getTranscodeType().supportMP3()) {
                playUrl = baseUrl
                        + "&format=mp3"
                        + "&ext=.mp3";
            } else {
                playUrl = baseUrl
                        + "&format=raw"
                        + "&ext=." + song.getFormat();
            }
        } else {
            boolean isStreamAudio = Utilities.isStreamAudio(song, isForChromecast);
            boolean needTranscode = false;

            if (Common.getTranscodeType().supportTranscoding()) {
                if (isStreamAudio) {
                    boolean needBitrateTranscode = false;

                    if (Common.getTranscodeType().supportMP3()
                            && transcodeSetting.isFormatMp3()
                            && !preferredDownloadQuality.isOriginal()) {

                        if (song.getBitrate() > preferredDownloadQuality.getBitrate()) {
                            needBitrateTranscode = true;
                        }
                    }

                    if (transcodeSetting.containsForceFormat(
                            Utilities.toStreamAudio(song.getFilePath()))
                            || needBitrateTranscode) {
                        needTranscode = true;
                    }
                } else {
                    needTranscode = true;
                }
            }

            boolean shouldTranscode = song.isVirtualSong() || needTranscode;

            ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_STREAM);
            if (knownAPI == null) {
                SynoLog.e(LOG, "SYNO.AudioStation.Stream api doesn't exist");
                return "";
            }

            if ((!Utilities.isAAC(song) || Utilities.isALAC(song)) && shouldTranscode) {
                String baseUrl =
                        Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath())
                                + "?api=SYNO.AudioStation.Stream"
                                + "&method=transcode"
                                + "&version=1"
                                + "&id=" + Utilities.escapeIdForUrl(song.getID());

                if (Common.getTranscodeType().supportMP3()
                        && transcodeSetting.isFormatMp3()) {

                    int bitrate = preferredDownloadQuality.isOriginal()
                            ? TranscodeSetting.TranscodeDownloadQuality.LOW.getBitrate()
                            : preferredDownloadQuality.getBitrate();

                    song.setCacheBitrate(bitrate);

                    playUrl = baseUrl
                            + "&format=mp3"
                            + "&_sid=" + sid
                            + "&bitrate=" + bitrate
                            + "&ext=.mp3";
                } else if (Common.getTranscodeType().supportWAV()) {
                    playUrl = baseUrl
                            + "&format=wav"
                            + "&_sid=" + sid
                            + "&ext=.wav";
                } else {
                    playUrl = baseUrl;
                }
            } else {
                playUrl =
                        Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath())
                                + "?api=SYNO.AudioStation.Stream"
                                + "&method=stream"
                                + "&version=1"
                                + "&id=" + Utilities.escapeIdForUrl(song.getID())
                                + "&_sid=" + sid
                                + "&ext=." + Utilities.getExt(song.getFilePath());
            }
        }

        if (isForChromecast) {
            playUrl += "&position=0";
        }

        SynoLog.d(LOG, "getPlayUrl : " + playUrl);

        return playUrl;
    }

    private int getLimit(int pageNum) {
        return -1 == pageNum ? -1 : 1024;
    }

    private int getOffset(int pageNum) {
        if (-1 == pageNum) {
            return 0;
        }
        return (pageNum - 1) * 1024;
    }

    protected String getCoverUrl(String songId, boolean appendSid) {
        String str;
        if (TextUtils.isEmpty(songId)) {
            return "";
        }
        ApiPath knownAPI = WebAPI.getInstance().getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_COVER);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Cover doesn't exist");
            return "";
        }
        String str2 = ((Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath()) + "?api=SYNO.AudioStation.Cover") + "&method=getsongcover") + "&version=1";
        try {
            Integer.parseInt(songId);
            str = str2 + "&id=music_" + songId;
        } catch (Exception unused) {
            str = str2 + "&id=" + songId;
        }
        if (appendSid) {
            str = str + "&_sid=" + Common.getSID();
        }
//        SynoLog.d(LOG, "getCoverUrl : " + str);
        return str;
    }

    private List<BasicKeyValuePair> getBasicParams(int pageNum) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("offset", String.valueOf(getOffset(pageNum))));
        arrayList.add(new BasicKeyValuePair("limit", String.valueOf(getLimit(pageNum))));
        arrayList.add(new BasicKeyValuePair("library", getPersonalLibraryValue()));
        return arrayList;
    }

    private JSONObject getJsonObjectFromReader(InputStream content) throws IOException {
        long jCurrentTimeMillis = System.currentTimeMillis();
        JSONObject jSONObject = new JSONObject();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(content));
        StringBuilder sb = new StringBuilder();
        for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
            sb.append(line);
        }
        bufferedReader.close();
        String string = sb.toString();
        SynoLog.d(LOG, "strRet = " + string);
        try {
            jSONObject = new JSONObject(string);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SynoLog.d(LOG, "process : " + (System.currentTimeMillis() - jCurrentTimeMillis));
        return jSONObject;
    }

    @Override
    protected List<SongItem> doSearch(Common.SearchCategory category, String key) throws IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_SONG);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Song doesn't exist");
            return new ArrayList<>();
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        List<BasicKeyValuePair> basicParams = getBasicParams(1);
        if (category.equals(Common.SearchCategory.ALL)) {
            basicParams.add(new BasicKeyValuePair(Common.SearchCategory.TITLE.name().toLowerCase(Locale.getDefault()), key));
            basicParams.add(new BasicKeyValuePair(Common.SearchCategory.ALBUM.name().toLowerCase(Locale.getDefault()), key));
            basicParams.add(new BasicKeyValuePair(Common.SearchCategory.ARTIST.name().toLowerCase(Locale.getDefault()), key));
            basicParams.add(new BasicKeyValuePair(Common.SearchCategory.GENRE.name().toLowerCase(Locale.getDefault()), key));
            basicParams.add(new BasicKeyValuePair(Common.SearchCategory.COMPOSER.name().toLowerCase(Locale.getDefault()), key));
        } else {
            basicParams.add(new BasicKeyValuePair(category.name().toLowerCase(Locale.getDefault()), key));
        }
        basicParams.add(new BasicKeyValuePair("additional", "song_tag,song_audio,song_rating"));
        for (BasicKeyValuePair basicKeyValuePair : basicParams) {
            SynoLog.d(LOG, "doSearch params = (" + basicKeyValuePair.first + " , " + basicKeyValuePair.second + ")");
        }
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_SONG, "search", basicParams);
        if (responseDoRequest.isSuccessful()) {
            try {
                ApiSongsResponseVo apiSongsResponseVo = new Gson().fromJson(
                        getJsonObjectFromReader(responseDoRequest.body().byteStream()).getJSONObject("data").toString(), ApiSongsResponseVo.class);
                if (apiSongsResponseVo != null && apiSongsResponseVo.getSong(0) != null) {
                    return SongItem.fromApiVo(apiSongsResponseVo);
                }
            } catch (JsonSyntaxException | JSONException e) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }
        handleError(responseDoRequest.code());
        return new ArrayList<>();
    }

    @Override
    protected void deleteRadioInfo(String stream_id) {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PROXY);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Proxy doesn't exist");
            return;
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair(STREAM_ID, stream_id));
        try {
            SynoLog.d(LOG, "deleteRadioInfo result = " + webAPI.doRequest(
                    strMakeAddress,
                    AudioStationAPI.SYNO_AUDIOSTATION_PROXY,
                    "deletesonginfo",
                    knownAPI.getMaxVersion(),
                    arrayList)
                    .body()
                    .string());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected JSONObject doPollingRadioInfo(String stream_id) {
        JSONObject jSONObject = new JSONObject();
        if (Common.getCookieStore() == null) {
            return jSONObject;
        }
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PROXY);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Proxy doesn't exist");
            return new JSONObject();
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair(STREAM_ID, stream_id));
        try {
            String strString = webAPI.doRequest(
                    strMakeAddress,
                    AudioStationAPI.SYNO_AUDIOSTATION_PROXY,
                    "getsonginfo",
                    knownAPI.getMaxVersion(),
                    arrayList)
                    .body()
                    .string();
            SynoLog.d(LOG, "doPollingRadioInfo result = " + strString);
            return new JSONObject(strString);
        } catch (Exception e) {
            e.printStackTrace();
            return jSONObject;
        }

    }

    @Override
    protected void doEnumLyrics(String strFilename, String id) throws IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = WebAPI.getInstance().getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_LYRICS);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Lyrics doesn't exist");
            return;
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        SynoLog.d(LOG, "doEnumLyrics url = " + strMakeAddress);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("id", id));
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_LYRICS, WebAPI.GETLYRICS, arrayList);
        if (responseDoRequest.isSuccessful()) {
            downloadStream(responseDoRequest, strFilename);
        }

    }

    @Override
    protected PinListResponseVo doEnumPins() throws WebAPIErrorException, IOException {
        Response responseDoRequest = null;
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("offset", String.valueOf(0)));
        arrayList.add(new BasicKeyValuePair("limit", String.valueOf(-1)));
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PIN);
        if (knownAPI != null) {
            try {
                responseDoRequest = webAPI.doRequest(
                        Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath()),
                        AudioStationAPI.SYNO_AUDIOSTATION_PIN,
                        "list",
                        1,
                        arrayList
                );
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            if (responseDoRequest.isSuccessful()) {
                PinListResponseVo pinListResponseVo = new Gson().fromJson(new JsonReader(new InputStreamReader(responseDoRequest.body().byteStream())), PinListResponseVo.class);
                if (pinListResponseVo.getError() != null && pinListResponseVo.getError().getCode() == 105) {
                    throw new WebAPIErrorException(105);
                }
                responseDoRequest.close();
                SynoLog.d(LOG, pinListResponseVo.toString());
                return pinListResponseVo;
            }
            handleError(responseDoRequest.code());
            return null;
        }
        SynoLog.e(LOG, "api SYNO.AudioStation.Pin doesn't exist");
        return null;

    }

    @Override
    protected void doEnumContainer(Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws IOException {
        String str;
        String str2;
        List<BasicKeyValuePair> basicParams = getBasicParams(pageNum);
        String str3 = AudioStationAPI.SYNO_AUDIOSTATION_ARTIST;
        switch (type) {
            case ALBUM_MODE:
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_ALBUM;
                str = "list";
                break;
            case LATEST_ALBUM_MODE:
                basicParams.add(new BasicKeyValuePair(SORT_BY, "time"));
                basicParams.add(new BasicKeyValuePair(SORT_DIRECTION, "desc"));
                basicParams.add(new BasicKeyValuePair("limit", "50"));
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_ALBUM;
                str = "list";
                break;
            case ARTIST_ALBUM_MODE:
                if (bundle.containsKey("artist")) {
                    basicParams.add(new BasicKeyValuePair("artist", bundle.getString("artist")));
                }
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_ALBUM;
                str = "list";
                break;
            case GENRE_ALBUM_MODE:
                if (bundle.containsKey("genre")) {
                    basicParams.add(new BasicKeyValuePair("genre", bundle.getString("genre")));
                }
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_ALBUM;
                str = "list";
                break;
            case GENRE_ARTIST_MODE:
                if (bundle.containsKey("genre")) {
                    basicParams.add(new BasicKeyValuePair("genre", bundle.getString("genre")));
                }
                if (bundle.containsKey(PinManager.GENRE_FILTER)) {
                    basicParams.add(new BasicKeyValuePair(PinManager.GENRE_FILTER, bundle.getString(PinManager.GENRE_FILTER)));
                }
                str = "list";
                break;
            case GENRE_ARTIST_ALBUM_MODE:
                if (bundle.containsKey("genre")) {
                    basicParams.add(new BasicKeyValuePair("genre", bundle.getString("genre")));
                }
                if (bundle.containsKey("artist")) {
                    basicParams.add(new BasicKeyValuePair("artist", bundle.getString("artist")));
                }
                if (bundle.containsKey(PinManager.GENRE_FILTER)) {
                    basicParams.add(new BasicKeyValuePair(PinManager.GENRE_FILTER, bundle.getString(PinManager.GENRE_FILTER)));
                }
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_ALBUM;
                str = "list";
                break;
            case COMPOSER_ALBUM_MODE:
                if (bundle.containsKey("composer")) {
                    basicParams.add(new BasicKeyValuePair("composer", bundle.getString("composer")));
                }
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_ALBUM;
                str = "list";
                break;
            case GENRE_MODE:
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_GENRE;
                str = "list";
                break;
            case HOMEPAGE_DEFAULT_GENRE_MODE:
                str = WebAPI.LIST_DEFAULT_GENRE;
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_GENRE;
                break;
            case ARTIST_MODE:
                str = "list";
                break;
            case COMPOSER_MODE:
                str2 = AudioStationAPI.SYNO_AUDIOSTATION_COMPOSER;
                str3 = str2;
                str = "list";
                break;
            case SEARCH_ARTIST_MODE:
                if (bundle.containsKey("key")) {
                    basicParams.add(new BasicKeyValuePair(FILTER, bundle.getString("key")));
                }
                str = "list";
                break;
            case SEARCH_ALBUM_MODE:
                if (bundle.containsKey("key")) {
                    basicParams.add(new BasicKeyValuePair(FILTER, bundle.getString("key")));
                }
                str3 = AudioStationAPI.SYNO_AUDIOSTATION_ALBUM;
                str = "list";
                break;
            default:
                SynoLog.e(LOG, "unsupported type : " + type.name());
                str2 = "";
                str3 = str2;
                str = "list";
                break;
        }
        if (TextUtils.isEmpty(str3)) {
            return;
        }
        basicParams.add(new BasicKeyValuePair("additional", AVG_RATING));
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(str3);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api " + str3 + " doesn't exist");
            return;
        }
        int maxVersion = webAPI.canSupportRating() ? 2 : 1;
        if (knownAPI.getMaxVersion() > 2) {
            maxVersion = knownAPI.getMaxVersion();
        }
        Response responseDoRequest = webAPI.doRequest(Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath()), str3, str, maxVersion, basicParams);
        if (responseDoRequest.isSuccessful()) {
            downloadStream(responseDoRequest, cachePath);
        } else {
            handleError(responseDoRequest.code());
        }

    }

    @Override
    protected void doEnumContainerSongs(Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws JSONException, IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_SONG);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Song doesn't exist");
            return;
        }
        int maxVersion = webAPI.canSupportRating() ? 2 : 1;
        if (knownAPI.getMaxVersion() > 2) {
            maxVersion = knownAPI.getMaxVersion();
        }
        int i = maxVersion;
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        List<BasicKeyValuePair> basicParams = getBasicParams(pageNum);
        switch (type) {
            case ALBUM_MODE:
            case LATEST_ALBUM_MODE:
            case SEARCH_ALBUM_MODE:
                basicParams.add(new BasicKeyValuePair("album", bundle.getString("album")));
                break;
            case ARTIST_ALBUM_MODE:
                basicParams.add(new BasicKeyValuePair("artist", bundle.getString("artist")));
                if (bundle.containsKey("album")) {
                    basicParams.add(new BasicKeyValuePair("album", bundle.getString("album")));
                    break;
                }
                break;
            case GENRE_ALBUM_MODE:
                basicParams.add(new BasicKeyValuePair("genre", bundle.getString("genre")));
                if (bundle.containsKey("album")) {
                    basicParams.add(new BasicKeyValuePair("album", bundle.getString("album")));
                    break;
                }
                break;
            case GENRE_ARTIST_MODE:
                if (bundle.containsKey("genre")) {
                    basicParams.add(new BasicKeyValuePair("genre", bundle.getString("genre")));
                }
                if (bundle.containsKey("artist")) {
                    basicParams.add(new BasicKeyValuePair("artist", bundle.getString("artist")));
                    break;
                }
                break;
            case GENRE_ARTIST_ALBUM_MODE:
                if (bundle.containsKey("genre")) {
                    basicParams.add(new BasicKeyValuePair("genre", bundle.getString("genre")));
                }
                if (bundle.containsKey("artist")) {
                    basicParams.add(new BasicKeyValuePair("artist", bundle.getString("artist")));
                }
                if (bundle.containsKey("album")) {
                    basicParams.add(new BasicKeyValuePair("album", bundle.getString("album")));
                    break;
                }
                break;
            case COMPOSER_ALBUM_MODE:
                basicParams.add(new BasicKeyValuePair("composer", bundle.getString("composer")));
                if (bundle.containsKey("album")) {
                    basicParams.add(new BasicKeyValuePair("album", bundle.getString("album")));
                    break;
                }
                break;
            case GENRE_MODE:
                if (bundle.containsKey("genre")) {
                    basicParams.add(new BasicKeyValuePair("genre", bundle.getString("genre")));
                    break;
                }
                break;
            case HOMEPAGE_DEFAULT_GENRE_MODE:
            default:
                SynoLog.d(LOG, "unsupported type : " + type.name());
                break;
            case ARTIST_MODE:
            case SEARCH_ARTIST_MODE:
                basicParams.add(new BasicKeyValuePair("artist", bundle.getString("artist")));
                break;
            case COMPOSER_MODE:
                basicParams.add(new BasicKeyValuePair("composer", bundle.getString("composer")));
                break;
            case RATING_MODE:
                int i2 = bundle.getInt("song_rating_level");
                // TimeModel.NUMBER_FORMAT
                String str = String.format(Locale.ENGLISH, "%d", Integer.valueOf(i2));
                basicParams.add(new BasicKeyValuePair("song_rating_meq", str));
                if (i2 == 0) {
                    basicParams.add(new BasicKeyValuePair("song_rating_leq", str));
                }
                basicParams.add(new BasicKeyValuePair(SORT_BY, SONG_RATING));
                basicParams.add(new BasicKeyValuePair(SORT_DIRECTION, "asc"));
                break;
        }
        if (bundle.containsKey("album_artist")) {
            basicParams.add(new BasicKeyValuePair("album_artist", bundle.getString("album_artist")));
        }
        if (bundle.containsKey(PinManager.GENRE_FILTER)) {
            basicParams.add(new BasicKeyValuePair(PinManager.GENRE_FILTER, bundle.getString(PinManager.GENRE_FILTER)));
        }
        basicParams.add(new BasicKeyValuePair("additional", "song_tag,song_audio,song_rating"));
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_SONG, "list", i, basicParams);
        if (responseDoRequest.isSuccessful()) {
            downloadStream(responseDoRequest, cachePath);
        } else {
            handleError(responseDoRequest.code());
        }

    }

    @Override
    protected void doEnumFolderSongs(String key, boolean recursive, String cachePath, int pageNum) throws IOException {
        int i;
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_FOLDER);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Folder doesn't exist");
            return;
        }
        int i2 = webAPI.canSupportRating() ? 2 : 1;
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        List<BasicKeyValuePair> basicParams = getBasicParams(pageNum);
        if (!TextUtils.isEmpty(key)) {
            basicParams.add(new BasicKeyValuePair("id", key));
        }
        basicParams.add(new BasicKeyValuePair("additional", "song_tag,song_audio,song_rating"));
        SynoLog.d(LOG, "doEnumFolderSongs params = " + basicParams);
        if (webAPI.canSupportFolderRecursive()) {
            basicParams.add(new BasicKeyValuePair(RECURSIVE, Boolean.toString(recursive)));
            i = 3;
        } else {
            i = i2;
        }
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_FOLDER, "list", i, basicParams);

        if (responseDoRequest.isSuccessful()) {
            downloadStream(responseDoRequest, cachePath);
        } else {
            SynoLog.d(LOG, responseDoRequest.toString());
            handleError(responseDoRequest.code());
        }
    }

    @Override
    protected BasePlaylistResponseVo doEnumPlaylist(int pageNum) throws WebAPIErrorException, IOException {
        Response responseDoRequest = null;
        List<BasicKeyValuePair> basicParams = getBasicParams(pageNum);
        basicParams.add(new BasicKeyValuePair("additional", SHARING_INFO));
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST);
        if (knownAPI != null) {
            try {
                // webAPI.canSupportPlaylistSharing()
                responseDoRequest = webAPI.doRequest(
                        Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath()),
                        AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST,
                        "list",
                        true ? 2 : 1,
                        basicParams
                );
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            if (responseDoRequest.isSuccessful()) {
                BasePlaylistResponseVo basePlaylistResponseVo = new Gson().fromJson(
                        new JsonReader(new InputStreamReader(responseDoRequest.body().byteStream())),
                        ApiPlaylistResponseVo.class);
                if (basePlaylistResponseVo.getError() != null && basePlaylistResponseVo.getError().getCode() == 105) {
                    throw new WebAPIErrorException(105);
                }
                return basePlaylistResponseVo;
            }
            handleError(responseDoRequest.code());
            return null;
        }
        SynoLog.e(LOG, "api SYNO.AudioStation.Playlist doesn't exist");
        return null;

    }

    @Override
    protected void doEnumRandom100(String cachePath) throws JSONException, IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_SONG);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Song doesn't exist");
            return;
        }
        int i = webAPI.canSupportRating() ? 2 : 1;
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("offset", "0"));
        arrayList.add(new BasicKeyValuePair("limit", "100"));
        arrayList.add(new BasicKeyValuePair("library", getPersonalLibraryValue()));
        arrayList.add(new BasicKeyValuePair(SORT_BY, "random"));
        arrayList.add(new BasicKeyValuePair("additional", "song_tag,song_audio,song_rating"));
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_SONG, "list", i, arrayList);
        if (responseDoRequest.isSuccessful()) {
            downloadStream(responseDoRequest, cachePath);
        } else {
            handleError(responseDoRequest.code());
        }
    }

    @Override
    protected void doEnumRadio(String key, String cachePath, int pageNum) throws IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_RADIO);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Radio doesn't exist");
            return;
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        List<BasicKeyValuePair> basicParams = getBasicParams(pageNum);
        if (!TextUtils.isEmpty(key)) {
            basicParams.add(new BasicKeyValuePair("container", key));
        }
        basicParams.add(new BasicKeyValuePair("additional", "song_tag,song_audio"));
        SynoLog.d(LOG, "doEnumRadio params = " + basicParams.toString());
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_RADIO, "list", basicParams);
        if (responseDoRequest.isSuccessful()) {
            downloadStream(responseDoRequest, cachePath);
        } else {
            handleError(responseDoRequest.code());
        }
    }

    @Override
    protected void doEnumPlaylistSongs(PlaylistItem playlistItem, String cachePath, int pageNum) throws JSONException, IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Playlist doesn't exist");
            return;
        }
        int i = webAPI.canSupportRating() ? 2 : 1;
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        String id = playlistItem.getID();
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair(SONGS_OFFSET, String.valueOf(getOffset(pageNum))));
        arrayList.add(new BasicKeyValuePair(SONGS_LIMIT, String.valueOf(getLimit(pageNum))));
        arrayList.add(new BasicKeyValuePair("library", getPersonalLibraryValue()));
        arrayList.add(new BasicKeyValuePair("id", id));
        arrayList.add(new BasicKeyValuePair("additional", "songs,songs_song_tag,songs_song_audio,songs_song_rating"));
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST, WebAPI.GETINFO, i, arrayList);
        if (responseDoRequest.isSuccessful()) {
            downloadStream(responseDoRequest, cachePath);
        } else {
            handleError(responseDoRequest.code());
        }

    }

    public void doSearchLyrics(String strFilename, SongItem song) throws IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = WebAPI.getInstance().getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_LYRICSSEARCH);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.LyricsSearch doesn't exist");
            return;
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        SynoLog.d(LOG, "doSearchLyrics url = " + strMakeAddress);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("artist", song.getArtist()));
        arrayList.add(new BasicKeyValuePair("title", song.getTitle()));
        arrayList.add(new BasicKeyValuePair("limit", "1"));
        arrayList.add(new BasicKeyValuePair("additional", "full_lyrics"));
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_LYRICSSEARCH, WebAPI.SEARCHLYRICS, arrayList);
        if (responseDoRequest.isSuccessful()) {
            downloadStream(responseDoRequest, strFilename);
        }
    }

    public static void downloadStream(Response httpResponse, String cachePath) throws IOException {
        InputStream inputStreamByteStream = httpResponse.body().byteStream();
        File file = new File(cachePath);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(cachePath);
        byte[] bArr = new byte[1024];
        while (true) {
            int i = inputStreamByteStream.read(bArr);
            if (i > 0) {
                fileOutputStream.write(bArr, 0, i);
                fileOutputStream.flush();
            } else {
                fileOutputStream.close();
                inputStreamByteStream.close();
                httpResponse.close();
                return;
            }
        }
    }

    @Override
    protected int requestToGetSongRating(SongItem song) throws IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_SONG);
        if (knownAPI == null) {
            SynoLog.e(LOG, "api SYNO.AudioStation.Song doesn't exist");
            return -1;
        }
        int i = webAPI.canSupportRating() ? 2 : 1;
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        List<BasicKeyValuePair> basicParams = getBasicParams(1);
        basicParams.add(new BasicKeyValuePair("id", Utilities.escapeId(song.getID())));
        basicParams.add(new BasicKeyValuePair("additional", SONG_RATING));
        Response responseDoRequest = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_SONG, WebAPI.GETINFO, i, basicParams);
        if (responseDoRequest.isSuccessful()) {
            try {
                ApiSongsResponseVo apiSongsResponseVo = new Gson().fromJson(getJsonObjectFromReader(responseDoRequest.body().byteStream()).getJSONObject("data").toString(), ApiSongsResponseVo.class);
                if (apiSongsResponseVo != null && apiSongsResponseVo.getSong(0) != null) {
                    return apiSongsResponseVo.getSong(0).getRating();
                }
            } catch (JsonSyntaxException | JSONException e) {
                e.printStackTrace();
            }
            return -1;
        }
        handleError(responseDoRequest.code());
        return -1;
    }


}
