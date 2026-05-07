package com.whisperyao.dsplayer;

import android.os.Bundle;
import com.synology.sylib.syhttp3.SyHttpClient;
import com.synology.sylib.syhttp3.relay.utils.RelayUtil;
import com.whisperyao.dsplayer.datasource.network.vo.ApiPath;
import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.AudioStationAPI;
import com.whisperyao.dsplayer.net.WebAPI;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.CallMonitor;
import com.whisperyao.dsplayer.util.SyhttpInitializer;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.vos.api.pin.PinListResponseVo;
import com.whisperyao.dsplayer.vos.api.pin.PinResponseVo;
import com.whisperyao.dsplayer.vos.api.pin.UnpinResponseVo;
import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ConnectionManager {
    private static final String LOG_TAG = "ConnectionManager";

    private static SyHttpClient sClient;

    private static AbstractNetManager sNetMgr;

    public interface GetHttpPost {
        void onGetHttpPost(Object tag);
    }

    public static boolean loadCoverCgi() {
        return !WebAPI.getInstance().isSupportApi() && (sNetMgr instanceof CgiEnumerator);
    }

    public static boolean canEditRating(boolean isOnline) {
        validateNetMgr(isOnline);
        return sNetMgr.canEditRating();
    }

    public static boolean canEditRating(boolean isOnline, SongItem song) {
        validateNetMgr(isOnline);
        return song != null && song.isOnDS() && sNetMgr.isWithRating() && sNetMgr.canEditRating(song);
    }

    public static void doSetRating(List<String> ids, int rating) throws IOException {
        validateNetMgr(true);
        sNetMgr.doSetRating(ids, rating);
    }

    public static boolean canSupportAddToNext() {
        validateNetMgr();
        if (Common.isPlayModeRenderer()) {
            return sNetMgr.canSupportAddToNext();
        }
        return true;
    }

    public static boolean canSupportPin() {
        validateNetMgr();
        return sNetMgr.isSupportPin();
    }
    public static boolean canShareSong(boolean isOnline, SongItem song) {
        validateNetMgr(isOnline);
        SynoLog.d("Common canShareSong()", "song: " + song);
        return song != null && song.isOnDS() && sNetMgr.canPublicShare() && sNetMgr.canShareSong(song);
    }

    public static boolean canSharePlaylist(boolean isOnline) {
        validateNetMgr(isOnline);
        return sNetMgr.canPublicShare();
    }

    public static boolean canSupportGenreArtist(boolean isOnline) {
        validateNetMgr(isOnline);
        return sNetMgr.canSupportGenreArtist();
    }

    public static boolean hasHomepage() {
        return canSupportPin();
    }

    public static PinResponseVo pin(final String type, final HashMap<String, String> criteria, final String name) throws Exception {
        validateNetMgr();
        return sNetMgr.pin(type, criteria, name);
    }

    public static UnpinResponseVo unPin(List<String> idList) throws Exception {
        validateNetMgr();
        return sNetMgr.unpin(idList);
    }

    public static BaseVo rename(final String id, final String name) throws Exception {
        validateNetMgr();
        return sNetMgr.rename(id, name);
    }

    public static BaseVo reorder(List<String> idList) throws Exception {
        validateNetMgr();
        return sNetMgr.reorder(idList);
    }

    public static String getCoverUrl(String songId) {
        return getCoverUrl(songId, false);
    }

    public static String getCoverUrl(String songId, boolean appendSid) {
        String coverUrl;
        if (loadCoverCgi()) {
            coverUrl = new CgiEnumerator().getCoverUrl(songId);
        } else {
            coverUrl = new ApiEnumerator().getCoverUrl(songId, appendSid);
        }
        return RelayUtil.getRealURL(coverUrl, AudioPreference.getHttpsPref());
    }

    public static JSONObject doPollingRadioInfo(String stream_id) {
        validateNetMgr();
        return sNetMgr.doPollingRadioInfo(stream_id);
    }

    public static void deleteRadioInfo(String stream_id) {
        validateNetMgr();
        sNetMgr.deleteRadioInfo(stream_id);
    }

    public static boolean isUseWebAPI() {return true;}

    public static SyHttpClient getHttpClient() {
        if (sClient == null) {
            synchronized (ConnectionManager.class) {
                if (sClient == null) {
                    sClient = SyhttpInitializer.generateClient(App.connectionManager);
                }
            }
        }
        return sClient;
    }

    private static String getPlayUrl(SongItem song, boolean isForChromecast) {
        validateNetMgr();
        return RelayUtil.getRealURL(sNetMgr.getPlayUrl(song, isForChromecast), AudioPreference.getHttpsPref());
    }

    public static String getPlayUrl(SongItem song) {
        return RelayUtil.getRealURL(getPlayUrl(song, false), AudioPreference.getHttpsPref());
    }

    public static String getOriginalPlayUrl(SongItem song) {
        validateNetMgr();
        return sNetMgr.getPlayUrl(song, false);
    }

    public static ResourceType getResourceType() {
        validateNetMgr();
        return sNetMgr.getResourceType();
    }

    public static List<SongItem> doSearch(Common.SearchCategory category, String query) throws JSONException, IOException {
        validateNetMgr(true);
        return sNetMgr.doSearch(category, query);
    }

    public static boolean isWithRating(boolean isOnline) {
        validateNetMgr(isOnline);
        return sNetMgr.isWithRating();
    }

    public static PinListResponseVo doEnumPins() throws Exception {
        validateNetMgr();
        return sNetMgr.doEnumPins();
    }

    public static void doEnumContainer(boolean isOnline, Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws JSONException, IOException {
        validateNetMgr(isOnline);
        sNetMgr.doEnumContainer(type, bundle, cachePath, pageNum);
    }

    public static void doEnumContainerSongs(boolean isOnline, Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws JSONException, IOException {
        validateNetMgr(isOnline);
        if (Common.ContainerType.RANDOM100_MODE.equals(type)) {
            sNetMgr.doEnumRandom100(cachePath);
        } else {
            sNetMgr.doEnumContainerSongs(type, bundle, cachePath, pageNum);
        }
    }

    public static void doEnumFolderSongs(boolean isOnline, String key, boolean recursive, String cachePath, int pageNum) throws Exception {
        validateNetMgr(isOnline);
        sNetMgr.doEnumFolderSongs(key, recursive, cachePath, pageNum);
    }

    public static BasePlaylistResponseVo doEnumPlaylist(boolean isOnline, int pageNum) throws Exception {
        validateNetMgr(isOnline);
        return sNetMgr.doEnumPlaylist(pageNum);
    }

    public static void doEnumPlaylistSongs(boolean isOnline, PlaylistItem playlistItem, String cachePath, int pageNum) throws JSONException, IOException {
        validateNetMgr(isOnline);
        if (Common.CAT_RANDOM100_ID.equals(playlistItem.getID())) {
            sNetMgr.doEnumRandom100(cachePath);
        } else {
            sNetMgr.doEnumPlaylistSongs(playlistItem, cachePath, pageNum);
        }
    }

    public static void doEnumRadios(String key, String cachePath, int pageNum) throws IOException {
        validateNetMgr();
        sNetMgr.doEnumRadio(key, cachePath, pageNum);
    }

    public static int requestToGetRating(SongItem song) throws JSONException, IOException {
        validateNetMgr(true);
        return sNetMgr.requestToGetSongRating(song);
    }

    private static void validateNetMgr(boolean isOnline) {
        AbstractNetManager abstractNetManager = sNetMgr;
        if (abstractNetManager != null && isOnline != abstractNetManager.isOnline()) {
            sNetMgr = null;
        }
        if (sNetMgr == null) {
            if (!Common.isLogin() || !isOnline) {
                sNetMgr = new LocalEnumerator();
                return;
            }
            ApiPath knownAPI = WebAPI.getInstance().getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_INFO);
            SynoLog.i("validateNetMgr", "knownAPI != null: " + knownAPI);
            if (knownAPI != null && knownAPI.getMaxVersion() >= 2) {
                SynoLog.i("validateNetMgr", "knownAPI: " + knownAPI + " " + "maxVersion: " + knownAPI.getMaxVersion());
                sNetMgr = new ApiEnumerator();
            } else {
                sNetMgr = new CgiEnumerator();
            }
        }
        CallMonitor.hit("validateNetMgr", 3);
        SynoLog.i("validateNetMgr", "sNetMgr: " + sNetMgr);
    }

    public static void validateNetMgr() {
        validateNetMgr(true);
    }

    public enum ResourceType {
        CGI,
        API,
        LOCAL;

        public boolean isOnline() {
            return !equals(LOCAL);
        }
    }
}
