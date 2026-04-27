package com.whisperyao.dsplayer;

import com.google.gson.Gson;
import com.whisperyao.dsplayer.datasource.network.vo.ApiPath;
import com.whisperyao.dsplayer.fragment.LyricFragment;
import com.whisperyao.dsplayer.homepage.PinManager;
import com.whisperyao.dsplayer.item.RendererItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.AudioStationAPI;
import com.whisperyao.dsplayer.net.WebAPI;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.vos.ApiPlayingInfo;
import com.whisperyao.dsplayer.vos.PlayingInfo;
import com.whisperyao.dsplayer.vos.api.ApiRemotePlayerResponseVo;
import com.whisperyao.dsplayer.vos.base.BaseRemotePlayerResponseVo;
import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ApiRemoteController extends RemoteController {
    private static final String ACTION = "action";
    private static final String ALL = "all";
    private static final String CONTROL = "control";
    private static final String GETPLAYLIST = "getplaylist";
    private static final String GETSTATUS = "getstatus";
    private static final String ID = "id";
    private static final String LOG = "ApiRemoteController";
    private static final String NEXT = "next";
    private static final String PASSWORD = "password";
    private static final String PAUSE = "pause";
    private static final String PLAY = "play";
    private static final String PREV = "prev";
    private static final String SEEK = "seek";
    private static final String SETMULTIPLE = "setmultiple";
    private static final String SETPASSWORD = "setpassword";
    private static final String SET_REPEAT = "set_repeat";
    private static final String SET_SHUFFLE = "set_shuffle";
    private static final String SET_VOLUME = "set_volume";
    private static final String STOP = "stop";
    private static final String SUBPLAYER_ID = "subplayer_id";
    private static final String TESTPASSWORD = "testpassword";
    private static final String TYPE = "type";
    private static final String UPDATEPLAYLIST = "updateplaylist";
    private static final String VALUE = "value";

    private JSONObject doRemotePlayerRequestWithDefaultPlayer(String method, List<BasicKeyValuePair> params) {
        params.add(new BasicKeyValuePair("id", Common.getPlayerUniqueId()));
        return doRemotePlayerRequest(method, params);
    }

    private JSONObject doRemotePlayerRequest(String method, List<BasicKeyValuePair> params) {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER);
        if (knownAPI == null) {
            return new JSONObject();
        }
        try {
            return new JSONObject(webAPI.doRequest(Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath()), AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER, method, knownAPI.getMaxVersion(), params).body().string());
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    @Override
    public ConnectionManager.ResourceType getResourceType() {
        return ConnectionManager.ResourceType.API;
    }

    @Override
    protected LinkedList<SongItem> control_getPlayingQueue() {
        LinkedList<SongItem> linkedList = new LinkedList<>();
        ArrayList arrayList = new ArrayList();
        arrayList.add(new BasicKeyValuePair(LyricFragment.ADDITIONAL, "song_tag,song_audio,song_rating"));
        arrayList.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, "0"));
        try {
            JSONArray jSONArray = doRemotePlayerRequestWithDefaultPlayer(GETPLAYLIST, arrayList).getJSONObject("data").getJSONArray("songs");
            int length = jSONArray.length();
            for (int i = 0; i < length; i++) {
                linkedList.add(SongItem.fromApiJson(jSONArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return linkedList;
    }

    @Override
    protected LinkedList<SongItem> control_getPlayingQueue(int offset, int limit) {
        LinkedList<SongItem> linkedList = new LinkedList<>();
        ArrayList arrayList = new ArrayList();
        Gson gson = new Gson();
        arrayList.add(new BasicKeyValuePair(LyricFragment.ADDITIONAL, "song_tag,song_audio,song_rating"));
        arrayList.add(new BasicKeyValuePair("offset", gson.toJson(Integer.valueOf(offset))));
        arrayList.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, gson.toJson(Integer.valueOf(limit))));
        try {
            JSONArray jSONArray = doRemotePlayerRequestWithDefaultPlayer(GETPLAYLIST, arrayList).getJSONObject("data").getJSONArray("songs");
            int length = jSONArray.length();
            for (int i = 0; i < length; i++) {
                linkedList.add(SongItem.fromApiJson(jSONArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return linkedList;
    }

    @Override
    protected int control_getQueueSize() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, "8192"));
        try {
            return doRemotePlayerRequestWithDefaultPlayer(GETPLAYLIST, arrayList).getJSONObject("data").getInt("total");
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    protected void control_play() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", PLAY));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override
    protected void control_pause() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", PAUSE));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override
    protected void control_next() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", NEXT));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override
    protected void control_prev() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", PREV));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override // com.synology.dsaudio.RemoteController
    protected void control_stop() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", STOP));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override
    protected void control_seek(long pos) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", SEEK));
        arrayList.add(new BasicKeyValuePair("value", String.valueOf(pos / 1000)));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override
    protected void control_setRepeatMode(Common.RepeatMode mode) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", SET_REPEAT));
        arrayList.add(new BasicKeyValuePair("value", mode.name().toLowerCase(Locale.getDefault())));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override // com.synology.dsaudio.RemoteController
    protected void control_setShuffleMode(Common.ShuffleMode mode) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", SET_SHUFFLE));
        arrayList.add(new BasicKeyValuePair("value", String.valueOf(Common.ShuffleMode.AUTO.equals(mode))));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override
    protected void control_setVolume(int volume) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", SET_VOLUME));
        arrayList.add(new BasicKeyValuePair("value", String.valueOf(volume)));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override
    protected void control_setMultiVolume(Map<String, Integer> subplayerVolumes) {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<String> arrayList2 = new ArrayList<>();
        for (String str : subplayerVolumes.keySet()) {
            arrayList.add(str);
            // TimeModel.NUMBER_FORMAT
            arrayList2.add(String.format(Locale.ENGLISH, "%", subplayerVolumes.get(str).intValue()));
        }
        String strCreateJoinedEscapedIdList = Utilities.createJoinedEscapedIdList(arrayList);
        String strCreateJoinedEscapedIdList2 = Utilities.createJoinedEscapedIdList(arrayList2);
        ArrayList<BasicKeyValuePair> arrayList3 = new ArrayList<>();
        arrayList3.add(new BasicKeyValuePair("action", SET_VOLUME));
        arrayList3.add(new BasicKeyValuePair(SUBPLAYER_ID, strCreateJoinedEscapedIdList));
        arrayList3.add(new BasicKeyValuePair("value", strCreateJoinedEscapedIdList2));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList3);
    }

    @Override
    protected void control_setGroupPlayer(String groupPlayerId, List<String> subPlayersIds) {
        String strCreateJoinedEscapedIdList = Utilities.createJoinedEscapedIdList(subPlayersIds);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("id", groupPlayerId));
        arrayList.add(new BasicKeyValuePair(SUBPLAYER_ID, strCreateJoinedEscapedIdList));
        doRemotePlayerRequest(SETMULTIPLE, arrayList);
    }

    @Override
    protected void control_clearQueue() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("offset", "0"));
        arrayList.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, String.valueOf(control_getQueueSize())));
        arrayList.add(new BasicKeyValuePair("updated_index", "-1"));
        doRemotePlayerRequestWithDefaultPlayer(UPDATEPLAYLIST, arrayList);
    }

    @Override
    protected void control_removeTracks(LinkedList<SongItem> songlist, Integer[] list, int newPos) {
        if (list.length == 0) {
            return;
        }
        List<Integer> listAsList = Arrays.asList(list);
        int iIntValue = list[0];
        int iIntValue2 = list[list.length - 1];
        String strValueOf = String.valueOf(iIntValue);
        String strValueOf2 = String.valueOf((iIntValue2 - iIntValue) + 1);
        ArrayList<SongItem> arrayList = new ArrayList<>();
        while (iIntValue <= iIntValue2) {
            if (!listAsList.contains(iIntValue)) {
                arrayList.add(songlist.get(iIntValue));
            }
            iIntValue++;
        }
        String strCreateIdList = Utilities.createIdList(arrayList);
        ArrayList<BasicKeyValuePair> arrayList2 = new ArrayList<>();
        arrayList2.add(new BasicKeyValuePair("offset", strValueOf));
        arrayList2.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, strValueOf2));
        arrayList2.add(new BasicKeyValuePair("songs", strCreateIdList));
        arrayList2.add(new BasicKeyValuePair("updated_index", String.valueOf(newPos)));
        doRemotePlayerRequestWithDefaultPlayer(UPDATEPLAYLIST, arrayList2);
    }

    @Override
    protected void control_updateTracks(LinkedList<SongItem> songlist, int start, int limit, int[] list, int newPos) {
        if (list.length == 0) {
            return;
        }
        ArrayList<SongItem> arrayList = new ArrayList<>();
        for (int i : list) {
            arrayList.add(songlist.get(i));
        }
        String strCreateIdList = Utilities.createIdList(arrayList);
        ArrayList<BasicKeyValuePair> arrayList2 = new ArrayList<>();
        arrayList2.add(new BasicKeyValuePair("offset", String.valueOf(start)));
        arrayList2.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, String.valueOf(limit)));
        arrayList2.add(new BasicKeyValuePair("songs", strCreateIdList));
        arrayList2.add(new BasicKeyValuePair("updated_index", String.valueOf(newPos)));
        doRemotePlayerRequestWithDefaultPlayer(UPDATEPLAYLIST, arrayList2);
    }

    @Override
    protected void control_jumpPlay(int pos) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", PLAY));
        arrayList.add(new BasicKeyValuePair("value", String.valueOf(pos)));
        doRemotePlayerRequestWithDefaultPlayer(CONTROL, arrayList);
    }

    @Override
    protected void control_enqueue(String idList, Common.PlaybackAction action, int position, boolean play) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        int iControl_getQueueSize = control_getQueueSize();
        if (Common.PlaybackAction.PLAY_NOW == action) {
            arrayList.add(new BasicKeyValuePair("offset", "0"));
            arrayList.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, String.valueOf(iControl_getQueueSize)));
            arrayList.add(new BasicKeyValuePair("songs", idList));
            arrayList.add(new BasicKeyValuePair("updated_index", "-1"));
            arrayList.add(new BasicKeyValuePair("keep_shuffle_order", "false"));
        } else if (Common.PlaybackAction.ADD_NEXT == action) {
            arrayList.add(new BasicKeyValuePair("offset", String.valueOf(position)));
            arrayList.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, "0"));
            arrayList.add(new BasicKeyValuePair("songs", idList));
            arrayList.add(new BasicKeyValuePair("keep_shuffle_order", "true"));
        } else {
            arrayList.add(new BasicKeyValuePair("offset", String.valueOf(iControl_getQueueSize)));
            arrayList.add(new BasicKeyValuePair(WebAPI.WebApiPin.LIMIT, "0"));
            arrayList.add(new BasicKeyValuePair("songs", idList));
            arrayList.add(new BasicKeyValuePair("keep_shuffle_order", "false"));
            position += iControl_getQueueSize;
        }
        doRemotePlayerRequestWithDefaultPlayer(UPDATEPLAYLIST, arrayList);
        if (play) {
            control_jumpPlay(position);
        }
    }

    @Override
    protected SongItem control_reloadSong(int playpos) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        WebAPI webAPI = WebAPI.getInstance();
        String str = AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYERSTATUS;
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYERSTATUS);
        if (knownAPI == null) {
            str = AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER;
            knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER);
        }
        String str2 = str;
        if (knownAPI == null) {
            return null;
        }
        arrayList.add(new BasicKeyValuePair("id", Common.getPlayerUniqueId()));
        arrayList.add(new BasicKeyValuePair(LyricFragment.ADDITIONAL, "song_tag,song_audio,song_rating"));
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject = new JSONObject(webAPI.doRequest(strMakeAddress, str2, GETSTATUS, knownAPI.getMaxVersion(), arrayList).body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            return SongItem.fromApiJson(jSONObject.getJSONObject("data").getJSONObject(PinManager.SONG));
        } catch (JSONException e2) {
            e2.printStackTrace();
            return SongItem.generateNoneSong();
        }
    }

    @Override
    protected PlayingInfo control_doPollingStatus() throws PlayingInfo.DeviceNotFoundException, PlayingInfo.NextworkException {
        WebAPI webAPI = WebAPI.getInstance();
        String str = AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYERSTATUS;
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYERSTATUS);
        if (knownAPI == null) {
            str = AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER;
            knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER);
        }
        String str2 = str;
        if (knownAPI == null) {
            return null;
        }
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("id", Common.getPlayerUniqueId()));
        arrayList.add(new BasicKeyValuePair(LyricFragment.ADDITIONAL, "song_tag,song_audio,subplayer_volume"));
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject = new JSONObject(webAPI.doRequest(Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath()), str2, GETSTATUS, knownAPI.getMaxVersion(), arrayList).body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // MediaRouteProviderProtocol.SERVICE_DATA_ERROR
        if (!jSONObject.has("error")) {
            try {
                return (PlayingInfo) new Gson().fromJson(jSONObject.getJSONObject("data").toString(), ApiPlayingInfo.class);
            } catch (JSONException e2) {
                e2.printStackTrace();
                throw new PlayingInfo.NextworkException();
            }
        }
        throw new PlayingInfo.DeviceNotFoundException();
    }

    public JSONObject control_testPassword(String player_id) {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER);
        if (knownAPI == null) {
            return new JSONObject();
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        String str = LOG;
        SynoLog.i(str, "url = " + strMakeAddress);
        ArrayList arrayList = new ArrayList();
        arrayList.add(new BasicKeyValuePair("id", player_id));
        try {
            String strString = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER, TESTPASSWORD, arrayList).body().string();
            SynoLog.i(str, "testPassword result = " + strString);
            return new JSONObject(strString);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public JSONObject control_setPassword(String player_id, String passwd) {
        WebAPI webAPI = WebAPI.getInstance();
        ApiPath knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER);
        if (knownAPI == null) {
            return new JSONObject();
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        String str = LOG;
        SynoLog.i(str, "url = " + strMakeAddress);
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("id", player_id));
        arrayList.add(new BasicKeyValuePair("password", passwd));
        try {
            String strString = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER, SETPASSWORD, arrayList).body().string();
            SynoLog.i(str, "setPassword result = " + strString);
            return new JSONObject(strString);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    @Override
    protected List<RendererItem> control_doEnumRenderer() {
        WebAPI webAPI;
        ApiPath knownAPI;
        ArrayList<RendererItem> arrayList = new ArrayList<>();
        if (!Common.haveRemotePlayer() || (knownAPI = (webAPI = WebAPI.getInstance()).getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER)) == null) {
            return arrayList;
        }
        String strMakeAddress = Common.makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.getPath());
        ArrayList<BasicKeyValuePair> arrayList2 = new ArrayList<>();
        arrayList2.add(new BasicKeyValuePair("type", "all"));
        arrayList2.add(new BasicKeyValuePair(LyricFragment.ADDITIONAL, "subplayer_list"));
        try {
            String strString = webAPI.doRequest(strMakeAddress, AudioStationAPI.SYNO_AUDIOSTATION_REMOTEPLAYER, "list", knownAPI.getMaxVersion(), arrayList2).body().string();
            SynoLog.d(LOG, "doEnumRenderer result = " + strString);
            for (BaseRemotePlayerResponseVo.BaseRemotePlayerVo baseRemotePlayerVo : ((BaseRemotePlayerResponseVo) new Gson().fromJson(strString, ApiRemotePlayerResponseVo.class)).getRemotePlayerList()) {
                arrayList.add(RendererItem.fromRemotePlayerResponseVo(baseRemotePlayerVo));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }
}