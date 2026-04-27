package com.whisperyao.dsplayer;

import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair;
import com.whisperyao.dsplayer.datasource.network.vo.ApiPath;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.AudioStationAPI;
import com.whisperyao.dsplayer.net.WebAPI;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class RadioEditor {
    private final static String ADD = "add";
    private final static String CONTAINER = "container";
    private final static String DATA = "data";
    private final static String DESC = "desc";
    public static String FAVORITE = "Favorite";
    private final static String FORMAT = "format";
    private final static String GETSTREAMID = "getstreamid";
    private final static String ID = "id";
    private final static String LIMIT = "limit";
    private final static String LOG = "RadioEditor";
    private final static String OFFSET = "offset";
    private final static String RADIO_JSON = "radio_json";
    private final static String STREAM_ID = "stream_id";
    private final static String TITLE = "title";
    private final static String TYPE = "type";
    private final static String UPDATERADIOS = "updateradios";
    private final static String URL = "url";
    public static String USERDEFINED = "UserDefined";
    public static String VAL_MEDIA_SERVER = "mediaserver";
    public static String VAL_RADIO = "radio";

    public static void retrieveStreamInfo(SongItem song) {
        try {
            JSONObject jSONObjectOptJSONObject = getStreamId(song).optJSONObject(DATA);
            if (jSONObjectOptJSONObject != null) {
                song.setStreamId(jSONObjectOptJSONObject.optString(STREAM_ID));
                song.setFormat(jSONObjectOptJSONObject.optString(FORMAT));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject getStreamId(SongItem songItem) throws IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PROXY);
        if (knownAPI == null) {
            SynoLog.e("ApiManager", "SYNO.AudioStation.Proxy api doesn't exist");
            return new JSONObject();
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        SynoLog.d(LOG, "getStreamId url = " + strMakeAddress);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair(ID, Utilities.escapeId(songItem.getID().trim())));
        arrayList.add(new BasicKeyValuePair(TYPE, songItem.isRadio() ? VAL_RADIO : VAL_MEDIA_SERVER));
        String strString = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_PROXY, GETSTREAMID, arrayList).body().string();
        SynoLog.d(LOG, "getStreamId result = " + strString);
        try {
            return new JSONObject(strString);
        } catch (JSONException e) {
            JSONObject jSONObject = new JSONObject();
            e.printStackTrace();
            return jSONObject;
        }
    }

    public static void doAddRadio(String container, String title, String url, String desc, ConnectionManager.GetHttpPost got) throws IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_RADIO);
        if (knownAPI == null) {
            SynoLog.e("ApiManager", "SYNO.AudioStation.Radio api doesn't exist");
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        SynoLog.d(LOG, "doAddRadio url = " + strMakeAddress);
        got.onGetHttpPost(strMakeAddress);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair(CONTAINER, container));
        arrayList.add(new BasicKeyValuePair(TITLE, title));
        arrayList.add(new BasicKeyValuePair(URL, url));
        arrayList.add(new BasicKeyValuePair(DESC, desc));
        webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_RADIO, ADD, arrayList);
    }

    public static void doUpdateRadios(String container, int offset, int limit, String radio_json, ConnectionManager.GetHttpPost got) throws IOException {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_RADIO);
        if (knownAPI == null) {
            SynoLog.e("ApiManager", "SYNO.AudioStation.Radio api doesn't exist");
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        SynoLog.d(LOG, "doAddRadio url = " + strMakeAddress);
        got.onGetHttpPost(strMakeAddress);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair(CONTAINER, container));
        arrayList.add(new BasicKeyValuePair(OFFSET, String.valueOf(offset)));
        arrayList.add(new BasicKeyValuePair(LIMIT, String.valueOf(limit)));
        arrayList.add(new BasicKeyValuePair(RADIO_JSON, radio_json));
        webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_RADIO, UPDATERADIOS, arrayList);
    }
}
