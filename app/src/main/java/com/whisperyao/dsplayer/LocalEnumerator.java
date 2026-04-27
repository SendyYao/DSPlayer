package com.whisperyao.dsplayer;

import android.os.Bundle;

import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.provider.DatabaseAccesser;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class LocalEnumerator extends AbstractNetManager{

    private static final String LOG = "LocalEnumerator";

    public static final String MOST_OFTEN_PLAYED = "[__MOST_FREQUENT_LISTEN__]";
    public static final String MOST_RECENT_PLAYED = "[__MOST_RECENT_ADDED__]";

    @Override
    public ConnectionManager.ResourceType getResourceType() {
        return ConnectionManager.ResourceType.LOCAL;
    }

    @Override
    protected boolean isWithRating() {
        return false;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    protected boolean canSupportAddToNext() {
        return true;
    }

    @Override
    protected boolean canSupportGenreArtist() {
        return true;
    }

    @Override
    protected String getPlayUrl(SongItem song, boolean isForChromeCast) {
        SynoLog.d(LOG, "getPlayUrl : " + song.getCachePath());
        return song.getCachePath();
    }

    @Override
    protected List<SongItem> doSearch(Common.SearchCategory category, String key) {
        SynoLog.d(LOG, "doSearch : category=" + category.toString() + ", key=" + key);
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        List<SongItem> listSearchDB = databaseAccesser.searchDB(category, key);
        databaseAccesser.close();
        return listSearchDB;
    }

    @Override
    protected void deleteRadioInfo(String stream_id) { }

    @Override
    protected JSONObject doPollingRadioInfo(String stream_id) {
        return new JSONObject();
    }

    @Override
    protected void doEnumLyrics(String strFilename, String id) { }

    @Override
    protected void doEnumContainer(Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws JSONException, IOException {
        String dsId = Common.isRemotePlayer() ? Common.getDsId() : null;
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        JSONArray jSONArrayDoEnumContainer = databaseAccesser.doEnumContainer(type, bundle, dsId);
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("total", jSONArrayDoEnumContainer.length());
        jSONObject.put("items", jSONArrayDoEnumContainer);
        writeToFile(cachePath, jSONObject.toString());
        databaseAccesser.close();
    }

    @Override
    protected void doEnumContainerSongs(Common.ContainerType type, Bundle bundle, String cachePath, int pageNum) throws JSONException, IOException {
        String dsId = Common.isRemotePlayer() ? Common.getDsId() : null;
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        SongItem[] songItemArrDoEnumContainerSongs = databaseAccesser.doEnumContainerSongs(type, bundle, dsId);
        JSONObject jSONObject = new JSONObject();
        JSONArray jSONArray = new JSONArray();
        jSONObject.put("total", songItemArrDoEnumContainerSongs.length);
        for (SongItem songItem : songItemArrDoEnumContainerSongs) {
            jSONArray.put(songItem.toJsonString());
        }
        jSONObject.put("items", jSONArray);
        SynoLog.d(LOG, "doEnumContainerSongs result : " + jSONObject);
        writeToFile(cachePath, jSONObject.toString());
        databaseAccesser.close();

    }

    public SongItem[] doEnumContainerSongList(Common.ContainerType type, Bundle bundle) {
        return DatabaseAccesser.getInstance().doEnumContainerSongs(type, bundle, Common.isRemotePlayer() ? Common.getDsId() : null);
    }

    @Override
    protected void doEnumFolderSongs(String key, boolean recursive, String cachePath, int pageNum) throws JSONException, IOException {
        String dsId = Common.isRemotePlayer() ? Common.getDsId() : null;
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        SongItem[] songItemArrDoEnumAllSongs = databaseAccesser.doEnumAllSongs(dsId);
        JSONObject jSONObject = new JSONObject();
        JSONArray jSONArray = new JSONArray();
        jSONObject.put("total", songItemArrDoEnumAllSongs.length);
        for (SongItem songItem : songItemArrDoEnumAllSongs) {
            jSONArray.put(songItem.toJsonString());
        }
        jSONObject.put("items", jSONArray);
        SynoLog.d(LOG, "doEnumFolderSongs result : " + jSONObject);
        writeToFile(cachePath, jSONObject.toString());
        databaseAccesser.close();

    }

    @Override
    protected BasePlaylistResponseVo doEnumPlaylist(int pageNum) { return null; }

    @Override
    protected void doEnumRandom100(String cachePath) { }

    @Override
    protected void doEnumRadio(String key, String cachePath, int pageNum) { }

    @Override
    protected void doEnumPlaylistSongs(PlaylistItem playlistItem, String cachePath, int pageNum) throws JSONException, IOException {
        String dsId = Common.isRemotePlayer() ? Common.getDsId() : null;
        String id = playlistItem.getID();
        Bundle bundle = new Bundle();
        bundle.putString("id", id);
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        SongItem[] songItemArrDoEnumContainerSongs = databaseAccesser.doEnumContainerSongs(Common.ContainerType.PLAYLIST_MODE, bundle, dsId);
        databaseAccesser.close();
        JSONObject jSONObject = new JSONObject();
        JSONArray jSONArray = new JSONArray();
        jSONObject.put("total", songItemArrDoEnumContainerSongs.length);
        for (SongItem songItem : songItemArrDoEnumContainerSongs) {
            jSONArray.put(songItem.toJsonString());
        }
        jSONObject.put("items", jSONArray);
        SynoLog.d(LOG, "doEnumPlaylistSongs result : " + jSONObject);
        writeToFile(cachePath, jSONObject.toString());
    }

    private void writeToFile(String cachePath, String jsonString) throws IOException {
        File file = new File(cachePath);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file.getPath());
        fileWriter.write(jsonString);
        fileWriter.close();
    }

    @Override
    protected int requestToGetSongRating(SongItem song) {
        return -1;
    }

}
