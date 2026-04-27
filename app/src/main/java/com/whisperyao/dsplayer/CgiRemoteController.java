package com.whisperyao.dsplayer;

import android.text.TextUtils;
import com.google.gson.Gson;
import com.whisperyao.dsplayer.item.RendererItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.vos.CgiPlayingInfo;
import com.whisperyao.dsplayer.vos.PlayingInfo;
import com.whisperyao.dsplayer.vos.base.BaseRemotePlayerResponseVo;
import com.whisperyao.dsplayer.vos.cgi.CgiRemotePlayerResponseVo;
import com.synology.sylib.syhttp3.SyHttpClient;
import com.synology.sylib.syhttp3.requestBody.SyFormEncodingBuilder;
import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CgiRemoteController extends RemoteController {

    private static final String ACTION = "action";
    private static final String APPEND = "append";
    private static final String DELETE = "delete";
    private static final String DELETEALL = "deleteall";
    private static final String IDLIST = "idlist";
    private static final String ITEMS = "items";
    private static final String JUMPPLAY = "jumpplay";
    private static final String LIMIT = "limit";
    private static final String LIST = "list";
    private static final String LOG = "CgiRemoteController";
    private static final String MODE = "mode";
    private static final String NEXT = "next";
    private static final String PLAY = "play";
    private static final String PLAYID = "playid";
    private static final String POSITION = "position";
    private static final String PREV = "prev";
    private static final String REPLACE = "replace";
    private static final String RESETREFRESH = "resetrefresh";
    private static final String SETMODE = "setmode";
    private static final String SETPOSITION = "setposition";
    private static final String SETSHUFFLE = "setshuffle";
    private static final String SETVOLUME = "setvolume";
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String TOTAL = "total";
    private static final String TRUE = "true";
    private static final String VOLUME = "volume";

    @Override
    protected void control_setGroupPlayer(String groupPlayerId, List<String> subPlayersIds) {
    }

    @Override
    protected void control_setMultiVolume(Map<String, Integer> subplayerVolumes) {
    }

    @Override
    protected void control_updateTracks(LinkedList<SongItem> songlist, int start, int limit, int[] list, int newPos) {
    }

    private JSONObject doUSBControl(List<BasicKeyValuePair> params) {
        if (Common.getCookieStore() == null) {
            return new JSONObject();
        }
        SyHttpClient httpClient = ConnectionManager.getHttpClient();
        String strMakeAddress = Common.makeAddress(Common.getBaseUrl(), Common.USB_CONTROLLER_CGI);
        params.add(new BasicKeyValuePair("player", String.valueOf(Common.getPlayerIndex())));
        String str = LOG;
        SynoLog.d(str, "doUSBControl params = " + params);
        Request requestBuild = null;
        try {
            requestBuild = new Request.Builder()
                    .url(strMakeAddress).header(Common.CLIENT_AGENT, "android")
                    .post(new SyFormEncodingBuilder()
                            .addAll(params)
                            .build())
                    .build();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
        try {
            String strString = httpClient.newCall(requestBuild).execute().body().string();
            SynoLog.d(str, "doUSBControl result = " + strString);
            return new JSONObject(strString);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    @Override
    public ConnectionManager.ResourceType getResourceType() {
        return ConnectionManager.ResourceType.CGI;
    }

    @Override
    protected LinkedList<SongItem> control_getPlayingQueue() {
        LinkedList<SongItem> linkedList = new LinkedList<>();
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", "list"));
        try {
            JSONArray jSONArray = doUSBControl(arrayList).getJSONArray("items");
            int length = jSONArray.length();
            for (int i = 0; i < length; i++) {
                linkedList.add(SongItem.fromCgiUSBJson(jSONArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return linkedList;
    }

    @Override
    protected LinkedList<SongItem> control_getPlayingQueue(int offset, int limit) {
        return new LinkedList<>();
    }

    @Override
    protected int control_getQueueSize() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", "list"));
        arrayList.add(new BasicKeyValuePair("limit", "1"));
        try {
            return doUSBControl(arrayList).getInt(TOTAL);
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    protected void control_play() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", PLAY));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_pause() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", PLAY));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_next() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", NEXT));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_prev() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", PREV));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_stop() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", STOP));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_seek(long pos) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", SETPOSITION));
        arrayList.add(new BasicKeyValuePair(POSITION, String.valueOf(pos / 1000)));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_setRepeatMode(Common.RepeatMode mode) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", SETMODE));
        arrayList.add(new BasicKeyValuePair("mode", String.valueOf(mode.getId())));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_setShuffleMode(Common.ShuffleMode mode) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", SETSHUFFLE));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_setVolume(int volume) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", SETVOLUME));
        arrayList.add(new BasicKeyValuePair("volume", String.valueOf(volume)));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_clearQueue() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", DELETEALL));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_removeTracks(LinkedList<SongItem> songlist, Integer[] list, int newPos) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", DELETE));
        arrayList.add(new BasicKeyValuePair(IDLIST, TextUtils.join(",", list)));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_jumpPlay(int pos) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", JUMPPLAY));
        arrayList.add(new BasicKeyValuePair(PLAYID, String.valueOf(pos)));
        doUSBControl(arrayList);
    }

    private void resetRefresh() {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", RESETREFRESH));
        doUSBControl(arrayList);
    }

    @Override
    protected void control_enqueue(String idList, Common.PlaybackAction action, int position, boolean play) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        if (Common.PlaybackAction.PLAY_NOW == action) {
            arrayList.add(new BasicKeyValuePair("action", REPLACE));
        } else {
            arrayList.add(new BasicKeyValuePair("action", APPEND));
        }
        if (play) {
            arrayList.add(new BasicKeyValuePair(PLAY, TRUE));
        }
        arrayList.add(new BasicKeyValuePair(IDLIST, idList));
        doUSBControl(arrayList);
    }

    @Override
    protected SongItem control_reloadSong(int playpos) {
        ArrayList<BasicKeyValuePair> arrayList = new ArrayList<>();
        arrayList.add(new BasicKeyValuePair("action", "list"));
        arrayList.add(new BasicKeyValuePair(START, String.valueOf(playpos)));
        arrayList.add(new BasicKeyValuePair("limit", "1"));
        try {
            return SongItem.fromCgiUSBJson(doUSBControl(arrayList).getJSONArray("items").getJSONObject(0));
        } catch (JSONException e) {
            e.printStackTrace();
            return SongItem.generateNoneSong();
        }
    }

    @Override
    protected PlayingInfo control_doPollingStatus() {
        if (Common.getCookieStore() == null) {
            return null;
        }
        try {
            String str = Common.makeAddress(Common.getBaseUrl(), Common.POLLING_STATUS_CGI) + "?_dc=" + new Date().getTime() + ("&player=" + Common.getPlayerIndex());
            SyHttpClient httpClient = ConnectionManager.getHttpClient();
            Request requestBuild = new Request.Builder().url(str).get().build();
            httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
            JSONObject jSONObject = new JSONObject(httpClient.newCall(requestBuild).execute().body().string());
            if (jSONObject.getJSONObject("data").getInt("RefreshFlag") > 0) {
                resetRefresh();
            }
            return new Gson().fromJson(jSONObject.getJSONObject("data").toString(), (Type) CgiPlayingInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected List<RendererItem> control_doEnumRenderer() {
        ArrayList<RendererItem> arrayList = new ArrayList<>();
        if (!Common.haveRenderer() && Common.haveRemotePlayer()) {
            arrayList.add(RendererItem.fromRemotePlayerResponseVo(CgiRemotePlayerResponseVo.CgiRemotePlayerVo.getUSBItem()));
        }
        if (Common.haveRenderer() && Common.haveRemotePlayer()) {
            SyHttpClient httpClient = ConnectionManager.getHttpClient();
            String strMakeAddress = Common.makeAddress(Common.getBaseUrl(), Common.USB_CONTROLLER_CGI);
            ArrayList<BasicKeyValuePair> arrayList2 = new ArrayList<>();
            arrayList2.add(new BasicKeyValuePair("action", "listrenderer"));

            Request requestBuild = null;
            try {
                requestBuild = new Request.Builder().url(strMakeAddress).post(new SyFormEncodingBuilder().addAll(arrayList2).build()).build();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            httpClient.setCookieHandler(new CookieManager(Common.getCookieStore(), CookiePolicy.ACCEPT_ALL));
            try {
                String strString = httpClient.newCall(requestBuild).execute().body().string();
                SynoLog.d(LOG, "doEnumRenderer result = " + strString);
                for (BaseRemotePlayerResponseVo.BaseRemotePlayerVo baseRemotePlayerVo : ((BaseRemotePlayerResponseVo) new Gson().fromJson(strString, CgiRemotePlayerResponseVo.class)).getRemotePlayerList()) {
                    arrayList.add(RendererItem.fromRemotePlayerResponseVo(baseRemotePlayerVo));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }
}
