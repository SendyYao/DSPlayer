package com.whisperyao.dsplayer;

import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.cast.HlsSegmentFormat;
import com.google.gson.Gson;
import com.synology.sylib.syhttp3.SyHttpClient;
import com.synology.sylib.syhttp3.requestBody.SyFormEncodingBuilder;
import com.synology.sylib.syhttp3.requestBody.SyRequestBody;
import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.playing.Player;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.TranscodeSetting;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo;
import com.whisperyao.dsplayer.vos.cgi.CgiPlaylistResponseVo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import okhttp3.Request;
import okhttp3.Response;

public class CgiEnumerator extends AbstractNetManager {

    private static final String LOG = "CgiEnumerator";
    private static final String ACTION = "action";
    private static final String ALBUM_ENUM = "album_enum";
    private static final String ALL_PLAYLIST_ENUM = "all_playlist_enum";
    private static final String ARTIST_ENUM = "artist_enum";
    private static final String GENRE_ENUM = "genre_enum";
    private static final String GET_LYRICS = "get_lyrics";

    private static final String LATEST_ALBUM = "latest_album";
    private static final String PLAYLIST_ENUM = "playlist_enum";
    private static final String RADIO_ENUM = "radio_enum";
    private static final String PLAYLIST_SONG_ENUM = "playlist_song_enum";
    private static final String PLS_ID = "pls_id";
    private static final String PERSONAL_PLS_ID = "personal_pls_id";
    private static final String SEARCH_KEY = "search_key";
    private static final String SMARTPLAYLIST_ENUM = "smartplaylist_enum";
    private static final String SMARTPLS_SONG_ENUM = "smartpls_song_enum";
    private static final String YES = "yes";

    @Override
    public ConnectionManager.ResourceType getResourceType() {
        return ConnectionManager.ResourceType.CGI;
    }

    @Override
    protected boolean isWithRating() {
        return false;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    protected boolean canSupportAddToNext() {
        return false;
    }

    @Override
    protected boolean canSupportGenreArtist() {
        return false;
    }

    @Override
    protected String getPlayUrl(SongItem song, boolean isForChromecast) {
        String sid = Common.getSID();
        boolean isStreamAudio = Utilities.isStreamAudio(song, isForChromecast);

        String playUrl;

        if (song.hasHttpURL()) {
            SynoLog.d(LOG, "hasHttpURL");

            song.setFormat(HlsSegmentFormat.MP3);

            playUrl = Common.makeAddress(Common.getBaseUrl(), Common.PORXY_CGI)
                    + "?url=" + song.getFilePath()
                    + "&sessionid=" + sid
                    + "&ext=.mp3";

        } else if (!isStreamAudio && Common.getTranscodeType().supportTranscoding()) {
            String baseUrl = Common.makeAddress(
                    Common.getBaseUrl(),
                    Common.TRANSCODER_CGI
            ) + "?id=" + Utilities.escapeIdForUrl(song.getID());

            TranscodeSetting transcodeSetting =
                    AudioPreference.getTranscodeSetting();

            if (Common.getTranscodeType().supportMP3()
                    && transcodeSetting.isFormatMp3()) {

                playUrl = baseUrl
                        + "&type=mp3"
                        + "&size=" + ((song.getDuration() + 1) * 16000)
                        + "&sessionid=" + sid
                        + "&ext=.mp3";

            } else if (Common.getTranscodeType().supportWAV()) {
                int channel = song.getChannel();

                if (channel <= 0) {
                    channel = 2;
                }

                playUrl = baseUrl
                        + "&type=wav"
                        + "&size=" + ((song.getDuration() + 1) * 88200 * channel)
                        + "&sessionid=" + sid
                        + "&ext=.wav";

            } else {
                playUrl = baseUrl;
            }

        } else {
            playUrl = Common.makeAddress(Common.getBaseUrl(), Common.STREAM_CGI)
                    + "?action=streaming&id=" + Utilities.escapeIdForUrl(song.getID())
                    + "&sessionid=" + sid
                    + "&ext=." + Utilities.getExt(song.getFilePath());
        }

        SynoLog.d(LOG, "getPlayUrl : " + playUrl);

        return playUrl;
    }

    @Override
    protected List<SongItem> doSearch(Common.SearchCategory category, String key) throws JSONException, IOException {
        Player player = Common.getPlayerStatusManager().getPlayer();
        SyHttpClient httpClient = ConnectionManager.getHttpClient();
        String strMakeAddress = Common.makeAddress(Common.getBaseUrl(), Common.ENUMERATE_CGI);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", "search"));
        arrayList.add(new BasicKeyValuePair(SEARCH_KEY, key));
        arrayList.add(new BasicKeyValuePair("limit", String.valueOf(1000)));
        if (!category.equals(Common.SearchCategory.ALL)) {
            arrayList.add(new BasicKeyValuePair("category", category.name().toLowerCase(Locale.getDefault())));
        }
        arrayList.add(new BasicKeyValuePair(Common.CLIENT_AGENT, "android"));
        if (player.isPlayModeRenderer()) {
            arrayList.add(new BasicKeyValuePair(Common.CLIENT_MODE, "USB_CONTROL"));
        }
        arrayList.add(new BasicKeyValuePair("transcode_type", Common.getTranscodeType().getString()));
        arrayList.add(new BasicKeyValuePair("library", AbstractNetManager.getPersonalLibraryValue()));
        Request requestBuild = new Request.Builder().url(strMakeAddress).post(new SyFormEncodingBuilder().addAll(arrayList).build()).build();
        httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
        return parseSearchResult(new JSONObject(httpClient.newCall(requestBuild).execute().body().string()));
    }

    @Override
    protected void deleteRadioInfo(String stream_id) { }

    @Override
    protected JSONObject doPollingRadioInfo(String stream_id) {
        JSONObject jSONObject = new JSONObject();
        if (Common.getCookieStore() == null) {
            return jSONObject;
        }
        try {
            String str = Common.makeAddress(Common.getBaseUrl(), Common.PORXY_CGI) + "?action=getsonginfo";
            SyHttpClient httpClient = ConnectionManager.getHttpClient();
            Request requestBuild = new Request.Builder().url(str).get().build();
            httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
            String strString = httpClient.newCall(requestBuild).execute().body().string();
            SynoLog.d(LOG, "doPollingRadioInfo result = " + strString);
            return new JSONObject(strString);
        } catch (Exception e) {
            e.printStackTrace();
            return jSONObject;
        }

    }

    @Override
    protected void doEnumLyrics(String strFilename, String id) throws IOException {
        SyHttpClient httpClient = ConnectionManager.getHttpClient();
        String strMakeAddress = Common.makeAddress(Common.getBaseUrl(), Common.ENUMERATE_CGI);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", GET_LYRICS));
        arrayList.add(new BasicKeyValuePair("id", id));
        Request requestBuild = new Request.Builder().url(strMakeAddress).post(new SyFormEncodingBuilder().addAll(arrayList).build()).build();
        httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
        Response responseExecute = httpClient.newCall(requestBuild).execute();
        if (responseExecute.isSuccessful()) {
            downloadStream(responseExecute, strFilename);
        }

    }

    private List<SongItem> parseSearchResult(JSONObject jsonobj) throws JSONException {
        ArrayList<SongItem> arrayList = new ArrayList<>();
        JSONArray jSONArray = jsonobj.getJSONArray("items");
        int length = jSONArray.length();
        for (int i = 0; i < length; i++) {
            SongItem songItemFromCgiJson = SongItem.fromCgiJson(jSONArray.getJSONObject(i));
            if (songItemFromCgiJson != null && songItemFromCgiJson.isFile()) {
                arrayList.add(songItemFromCgiJson);
            }
        }
        return arrayList;
    }


    @Override
    protected void doEnumContainer(Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws IOException {
        SyHttpClient httpClient = ConnectionManager.getHttpClient();
        String strMakeAddress = Common.makeAddress(Common.getBaseUrl(), Common.ENUMERATE_CGI);
        String str = LOG;
        SynoLog.d(str, "doEnumContainer url = " + strMakeAddress);
        List<BasicKeyValuePair> basicParams = getBasicParams();
        String str2 = ALBUM_ENUM;
        String str3 = "";
        String string = null;
        switch (type) {
            case ALBUM_MODE:
                break;
            case LATEST_ALBUM_MODE:
                basicParams.add(new BasicKeyValuePair(LATEST_ALBUM, String.valueOf(100)));
                break;
            case GENRE_MODE:
                str2 = GENRE_ENUM;
                break;
            case ARTIST_MODE:
                str2 = ARTIST_ENUM;
                break;
            case ARTIST_ALBUM_MODE:
                string = bundle.getString("artist");
                str3 = "artist_name";
                break;
            case GENRE_ALBUM_MODE:
                string = bundle.getString("genre");
                str3 = "genre_name";
                break;
            default:
                SynoLog.e(str, "unsupported type : " + type.name());
                str2 = "";
                break;
        }
        if (!TextUtils.isEmpty(str2)) {
            basicParams.add(new BasicKeyValuePair("action", str2));
        }
        if (!TextUtils.isEmpty(str3) && string != null) {
            basicParams.add(new BasicKeyValuePair(str3, string));
        }
        SynoLog.d(str, "doEnumContainer params = " + basicParams);
        Request requestBuild = new Request.Builder().url(strMakeAddress).post(new SyFormEncodingBuilder().addAll(basicParams).build()).build();
        httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
        Response responseExecute = httpClient.newCall(requestBuild).execute();
        if (responseExecute.isSuccessful()) {
            downloadStream(responseExecute, cachePath);
        } else {
            handleError(responseExecute.code());
        }

    }

    @Override
    protected void doEnumContainerSongs(Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws IOException {

        SyHttpClient client = ConnectionManager.getHttpClient();

        String url = Common.makeAddress(Common.getBaseUrl(), "iPhone/enumerate.cgi");

        SynoLog.d(LOG, "doEnumContainerSongs url = " + url);

        List<BasicKeyValuePair> params = getBasicParams();

        params.add(new BasicKeyValuePair("action", "song_enum"));

        switch (type) {

            // album only
            case ALBUM_MODE:
            case SEARCH_ALBUM_MODE:
                params.add(new BasicKeyValuePair("album_name", bundle.getString("album")));
                break;

            // genre only
            case GENRE_MODE:
                params.add(new BasicKeyValuePair("genre_name", bundle.getString("genre")));
                break;

            // artist only
            case ARTIST_MODE:
            case SEARCH_ARTIST_MODE:
                params.add(new BasicKeyValuePair("artist_name", bundle.getString("artist")));
                break;

            // artist + optional album
            case ARTIST_ALBUM_MODE:
                params.add(new BasicKeyValuePair("artist_name", bundle.getString("artist")));

                if (bundle.containsKey("album")) {
                    params.add(new BasicKeyValuePair("album_name", bundle.getString("album")));
                }
                break;

            // genre + optional album
            case GENRE_ALBUM_MODE:
            case GENRE_ARTIST_ALBUM_MODE:
                params.add(new BasicKeyValuePair("genre_name", bundle.getString("genre")));

                if (bundle.containsKey("album")) {
                    params.add(new BasicKeyValuePair("album_name", bundle.getString("album")));
                }
                break;

            default:
                SynoLog.d(LOG, "unsupported type : " + type.name());
                break;
        }

        SynoLog.d(LOG, "doEnumContainerSongs params = " + params);

        SyRequestBody requestBody = new SyFormEncodingBuilder().addAll(params).build();

        Request request = new Request.Builder().url(url).post(requestBody).build();

        CookieManager cookieManager = new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL);

        client.setCookieHandler(cookieManager);

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            downloadStream(response, cachePath);
        } else {
            handleError(response.code());
        }
    }

    @Override
    protected void doEnumFolderSongs(String key, boolean recursive, String cachePath, int pageNum) {

    }

    @Override
    protected BasePlaylistResponseVo doEnumPlaylist(int pageNum) throws Exception {
        File tempFile = File.createTempFile(".playlist.", null, App.getContext().getCacheDir());

        try {
            String cachePath = tempFile.getPath();

            if (Common.createPersonalPlaylist()) {
                doEnumPlaylistInternal(
                        Common.ContainerType.PLAYLIST_MODE,
                        cachePath,
                        pageNum
                );
            } else {
                doEnumOldPlayList(cachePath);
            }

            return new Gson().fromJson(
                    new FileReader(tempFile),
                    CgiPlaylistResponseVo.class
            );

        } finally {
            tempFile.delete();
        }
    }

    @Override
    protected void doEnumRandom100(String cachePath) throws IOException {
        SyHttpClient httpClient = ConnectionManager.getHttpClient();
        String strMakeAddress = Common.makeAddress(Common.getBaseUrl(), Common.ENUMERATE_CGI);
        String str = LOG;
        SynoLog.d(str, "doEnumRandom100 url = " + strMakeAddress);
        List<BasicKeyValuePair> basicParams = getBasicParams();
        basicParams.add(new BasicKeyValuePair("action", PLAYLIST_SONG_ENUM));
        basicParams.add(new BasicKeyValuePair(PLS_ID, "-3"));
        SynoLog.d(str, "doEnumRandom100 params = " + basicParams.toString());
        Request requestBuild = new Request.Builder().url(strMakeAddress).post(new SyFormEncodingBuilder().addAll(basicParams).build()).build();
        httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
        Response responseExecute = httpClient.newCall(requestBuild).execute();
        if (responseExecute.isSuccessful()) {
            downloadStream(responseExecute, cachePath);
        } else {
            handleError(responseExecute.code());
        }

    }

    @Override
    protected void doEnumRadio(String key, String cachePath, int pageNum) throws IOException {
        SyHttpClient httpClient = ConnectionManager.getHttpClient();
        String strMakeAddress = Common.makeAddress(Common.getBaseUrl(), Common.ENUMERATE_CGI);
        SynoLog.d(LOG, "doEnumRadios url = " + strMakeAddress);
        List<BasicKeyValuePair> basicParams = getBasicParams();
        basicParams.add(new BasicKeyValuePair("action", RADIO_ENUM));
        if (!TextUtils.isEmpty(key)) {
            basicParams.add(new BasicKeyValuePair("id", key));
        }
        Request requestBuild = new Request.Builder().url(strMakeAddress).post(new SyFormEncodingBuilder().addAll(basicParams).build()).build();
        httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
        Response responseExecute = httpClient.newCall(requestBuild).execute();
        if (responseExecute.isSuccessful()) {
            downloadStream(responseExecute, cachePath);
        } else {
            handleError(responseExecute.code());
        }

    }

    @Override
    protected void doEnumPlaylistSongs(PlaylistItem playlistItem, String cachePath, int pageNum) throws IOException {
        SyHttpClient httpClient = ConnectionManager.getHttpClient();
        String strMakeAddress = Common.makeAddress(Common.getBaseUrl(), Common.ENUMERATE_CGI);
        String str = LOG;
        SynoLog.d(str, "doEnumPlaylistSongs url = " + strMakeAddress);
        List<BasicKeyValuePair> basicParams = getBasicParams();
        String id = playlistItem.getID();
        if (playlistItem.isPersonal()) {
            basicParams.add(new BasicKeyValuePair("personal", YES));
        }
        if (playlistItem.isNormal()) {
            basicParams.add(new BasicKeyValuePair("action", PLAYLIST_SONG_ENUM));
            basicParams.add(new BasicKeyValuePair(PLS_ID, id));
        } else {
            basicParams.add(new BasicKeyValuePair("action", SMARTPLS_SONG_ENUM));
            basicParams.add(new BasicKeyValuePair(PLS_ID, id));
        }
        if (playlistItem.hasNewPlaylistID()) {
            basicParams.add(new BasicKeyValuePair(PERSONAL_PLS_ID, id));
        }
        SynoLog.d(str, "doEnumPlaylistSongs params = " + basicParams.toString());
        Request requestBuild = new Request.Builder().url(strMakeAddress).post(new SyFormEncodingBuilder().addAll(basicParams).build()).build();
        httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
        Response responseExecute = httpClient.newCall(requestBuild).execute();
        if (responseExecute.isSuccessful()) {
            downloadStream(responseExecute, cachePath);
        } else {
            handleError(responseExecute.code());
        }

    }

    private void doEnumOldPlayList(String cachePath) throws IOException {
        StringBuilder str = new StringBuilder();
        File cacheDir = App.getContext().getCacheDir();
        File fileCreateTempFile = File.createTempFile(".playlist.", null, cacheDir);
        File fileCreateTempFile2 = File.createTempFile(".playlist.", null, cacheDir);
        doEnumPlaylistInternal(Common.ContainerType.PLAYLIST_MODE, fileCreateTempFile.getPath(), -1);
        doEnumPlaylistInternal(Common.ContainerType.SMARTPLAYLIST_MODE, fileCreateTempFile2.getPath(), -1);
        JSONArray jSONArray = new JSONArray();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileCreateTempFile));
            String str2 = "";
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                } else {
                    str2 = str2 + line;
                }
            }
            bufferedReader.close();
            SynoLog.d(LOG, "normal playlist = " + str2);
            JSONArray jSONArray2 = new JSONObject(str2).getJSONArray("items");
            int length = jSONArray2.length();
            for (int i = 0; i < length; i++) {
                JSONObject jSONObject = jSONArray2.getJSONObject(i);
                jSONObject.put("type", "normal_playlist");
                jSONArray.put(jSONObject);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        fileCreateTempFile.delete();
        try {
            BufferedReader bufferedReader2 = new BufferedReader(new FileReader(fileCreateTempFile2));
            while (true) {
                String line2 = bufferedReader2.readLine();
                if (line2 == null) {
                    break;
                } else {
                    str.append(line2);
                }
            }
            bufferedReader2.close();
            SynoLog.d(LOG, "smart palylist = " + str);
            JSONArray jSONArray3 = new JSONObject(str.toString()).getJSONArray("items");
            int length2 = jSONArray3.length();
            for (int i2 = 0; i2 < length2; i2++) {
                JSONObject jSONObject2 = jSONArray3.getJSONObject(i2);
                jSONObject2.put("type", "smart_playlist");
                jSONArray.put(jSONObject2);
            }
        } catch (IOException | JSONException e3) {
            e3.printStackTrace();
        }
        fileCreateTempFile2.delete();
        JSONObject jSONObject3 = new JSONObject();
        try {
            jSONObject3.put("items", jSONArray);
            jSONObject3.put("total", jSONArray.length());
            // FirebaseAnalytics.Param.SUCCESS
            jSONObject3.put("success", true);
        } catch (JSONException e5) {
            e5.printStackTrace();
        }
        File file = new File(cachePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            fileWriter.write(jSONObject3.toString());
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e6) {
            e6.printStackTrace();
        }
    }

    private void doEnumPlaylistInternal(Common.ContainerType type, String cachePath, int pageNum) throws IOException {

        SyHttpClient httpClient = ConnectionManager.getHttpClient();

        String url = Common.makeAddress(
                Common.getBaseUrl(),
                Common.ENUMERATE_CGI
        );

        SynoLog.d(LOG, "doEnumContainer url = " + url);

        List<BasicKeyValuePair> params = getBasicParams();

        String action;

        switch (type) {
            case SMARTPLAYLIST_MODE:
                action = SMARTPLAYLIST_ENUM;
                break;

            case PLAYLIST_MODE:
                if (Common.createPersonalPlaylist()) {
                    action = ALL_PLAYLIST_ENUM;
                } else {
                    action = PLAYLIST_ENUM;
                }
                break;

            default:
                SynoLog.e(LOG, "unsupported type : " + type.name());
                action = "";
                break;
        }

        if (!TextUtils.isEmpty(action)) {
            params.add(new BasicKeyValuePair(ACTION, action));
        }

        SynoLog.d(LOG, "doEnumContainer params = " + params);

        Request request = new Request.Builder()
                .url(url)
                .post(new SyFormEncodingBuilder().addAll(params).build())
                .build();

        httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));

        Response response = httpClient.newCall(request).execute();

        if (response.isSuccessful()) {
            downloadStream(response, cachePath);
        } else {
            handleError(response.code());
        }
    }

    private List<BasicKeyValuePair> getBasicParams() {
        Player player = Common.getPlayerStatusManager().getPlayer();
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair(Common.CLIENT_AGENT, "android"));
        if (player.isPlayModeRenderer()) {
            arrayList.add(new BasicKeyValuePair(Common.CLIENT_MODE, "USB_CONTROL"));
        }
        arrayList.add(new BasicKeyValuePair("transcode_type", Common.getTranscodeType().getString()));
        arrayList.add(new BasicKeyValuePair("library", getPersonalLibraryValue()));
        return arrayList;
    }

    protected String getCoverUrl(String songId) {
        if (TextUtils.isEmpty(songId)) {
            return "";
        }
        return ((Common.makeAddress(Common.getBaseUrl(), Common.ENUMERATE_CGI) + "?action=get_cover") + "&music_id=" + songId) + "&library=" + AbstractNetManager.getPersonalLibraryValue();
    }

    @Override
    protected int requestToGetSongRating(SongItem song) {
        return -1;
    }
}
