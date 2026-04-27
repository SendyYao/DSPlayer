package com.whisperyao.dsplayer.net;

import com.synology.sylib.syhttp3.requestBody.SyFormEncodingBuilder;
import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.datasource.network.vo.ApiPath;
import com.whisperyao.dsplayer.util.SynoLog;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;

public class WebAPI {
    public static final String API = "api";
    private static final String QUERY_PATH = "query.cgi";
    public static final String VERSION = "version";
    public static final String GETINFO = "getinfo";
    public static final String GETLYRICS = "getlyrics";
    public static final String LIST_DEFAULT_GENRE = "list_default_genre";
    public static final String SEARCHLYRICS = "searchlyrics";

    private static WebAPI instance;
    private HashMap<String, ApiPath> mKnownAPIs;
    private final CapabilityHolder mCapabilityHolder = new CapabilityHolder();

    private static class CapabilityHolder {
        boolean supportAddToNext;
        boolean supportFolderRecursive;
        boolean supportGenreArtist;
        boolean supportPin;
        boolean supportPlaylistSharing;
        boolean supportRating;

        private CapabilityHolder() {
            this.supportRating = false;
            this.supportPlaylistSharing = false;
            this.supportFolderRecursive = false;
            this.supportGenreArtist = false;
            this.supportPin = false;
            this.supportAddToNext = false;
        }
    }

    private WebAPI() {
        this.mKnownAPIs = new HashMap<>();
        HashMap<String, ApiPath> map = new HashMap<>();
        this.mKnownAPIs = map;
        map.put("SYNO.API.Info", new ApiPath(1, 1, QUERY_PATH));
    }

    public static WebAPI newInstance() {
        WebAPI webAPI = new WebAPI();
        instance = webAPI;
        return webAPI;
    }

    public static WebAPI getInstance() {
        if (instance == null) {
            instance = new WebAPI();
        }
        return instance;
    }

    public boolean isSupportApi() {
        return this.mKnownAPIs.containsKey(AudioStationAPI.SYNO_AUDIOSTATION_INFO);
    }

    public boolean canSupportPin() {
        return this.mCapabilityHolder.supportPin;
    }

    public boolean canSupportAddToNext() {
        return this.mCapabilityHolder.supportAddToNext;
    }

    public boolean canSupportRating() {
        return this.mCapabilityHolder.supportRating;
    }

    public boolean canSupportFolderRecursive() {
        return this.mCapabilityHolder.supportFolderRecursive;
    }

    public boolean canSupportGenreArtist() {
        return this.mCapabilityHolder.supportGenreArtist;
    }


    public ApiPath getKnownAPI(final String apiName) {
        return this.mKnownAPIs.get(apiName);
    }

    public void setKnownAPIs(Map<String, ? extends ApiPath> knownAPIs) {
        clearKnownAPIs();
        if (knownAPIs != null) {
            this.mKnownAPIs.putAll(knownAPIs);
            resolveCapability();
        }
    }

    private void clearKnownAPIs() {
        this.mKnownAPIs.clear();
        this.mKnownAPIs.put("SYNO.API.Info", new ApiPath(1, 1, QUERY_PATH));
        resolveCapability();
    }

    private void resolveCapability() {
        ApiPath knownAPI = getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_SONG);
        boolean z = false;
        this.mCapabilityHolder.supportRating = knownAPI != null && knownAPI.getMaxVersion() >= 2;
        ApiPath knownAPI2 = getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST);
        this.mCapabilityHolder.supportPlaylistSharing = knownAPI2 != null && knownAPI2.getMaxVersion() >= 2;
        ApiPath knownAPI3 = getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_FOLDER);
        this.mCapabilityHolder.supportFolderRecursive = knownAPI3 != null && knownAPI3.getMaxVersion() >= 3;
        ApiPath knownAPI4 = getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_ARTIST);
        this.mCapabilityHolder.supportGenreArtist = knownAPI4 != null && knownAPI4.getMaxVersion() >= 3;
        ApiPath knownAPI5 = getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PIN);
        this.mCapabilityHolder.supportPin = knownAPI5 != null && knownAPI5.getMaxVersion() >= 1;
        ApiPath knownAPI6 = getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER);
        if (knownAPI6 != null && knownAPI6.getMaxVersion() >= 3) {
            z = true;
        }
        this.mCapabilityHolder.supportAddToNext = z;
    }

    public class WebApiPin {
        public static final String CRITERIA = "criteria";
        public static final String ID = "id";
        public static final String ITEMS = "items";
        public static final String LIMIT = "limit";
        public static final String LIST = "list";
        public static final String NAME = "name";
        public static final String OFFSET = "offset";
        public static final String PIN = "pin";
        public static final String RENAME = "rename";
        public static final String REORDER = "reorder";
        public static final String TYPE = "type";
        public static final String UNPIN = "unpin";

        public WebApiPin() {
        }
    }


    public Response doRequest(final String url, final String apiName, final String method) throws IOException {
        return doRequest(url, apiName, method, 1, new ArrayList<>());
    }

    public Response doRequest(final String url, final String apiName, final String method, final List<BasicKeyValuePair> params) throws IOException {
        return doRequest(url, apiName, method, 1, params);
    }

    public Response doRequest(final String url, final String apiName, final String method, final int version) throws IOException {
        return doRequest(url, apiName, method, version, new ArrayList());
    }

    public Response doRequest(final String url, final String apiName, final String method, final int version, List<BasicKeyValuePair> params) throws IOException {
        ApiPath apiPath = this.mKnownAPIs.get(apiName);
        if (version > apiPath.getMaxVersion() && version < apiPath.getMinVersion()) {
            SynoLog.e("WebAPI", "Unsupported API Version: " + version);
        }
        if (params == null) {
            params = new ArrayList<>();
        }
        params.add(new BasicKeyValuePair(API, apiName));
        params.add(new BasicKeyValuePair("method", method));
        params.add(new BasicKeyValuePair(VERSION, Integer.toString(version)));
        params.add(new BasicKeyValuePair("_sid", Common.getSID()));
        SynoLog.i("WebAPI", "apiName: " + apiName + "method: " + method + "version: " + version);
        SynoLog.i("WebAPI", "url: " + url + "params: " + params);
        if ("getstatus".equalsIgnoreCase(method)) {
            SynoLog.i("WebAPI", "api = " + apiName);
            for (BasicKeyValuePair basicKeyValuePair: params) {
                SynoLog.i("WebAPI", " param key: " + basicKeyValuePair.first + " , value: " + basicKeyValuePair.second);
            }
        }
        return ConnectionManager.getHttpClient().newCall(new Request.Builder().url(url).post(new SyFormEncodingBuilder().addAll(params).build()).build()).execute();
    }
}
