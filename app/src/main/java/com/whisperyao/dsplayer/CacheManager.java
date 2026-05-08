package com.whisperyao.dsplayer;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.whisperyao.dsplayer.datasource.network.vo.BaseVo;
import com.whisperyao.dsplayer.fragment.LyricFragment;
import com.whisperyao.dsplayer.item.HomePagePinItem;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.PlaylistItem;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.net.WebAPIErrorException;
import com.whisperyao.dsplayer.provider.DatabaseAccesser;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.ObjFile;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.vos.api.pin.PinItemVo;
import com.whisperyao.dsplayer.vos.api.pin.PinListResponseVo;
import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("CallToPrintStackTrace")
public class CacheManager {
    private static CacheManager sCacheManger;

    private static final String LOG = "CacheManager";
    private static final String CACHE_DIR = "/cache/";
    private static final String TOTAL = "total";
    private static final String PLAYLISTS = "playlists";
    private static final String RADIOS = "radios";
    private static final String SONGS = "songs";
    private static final String SONGS_TOTAL = "songs_total";
    private static final String PROFILE_DIR = "/profile/";
    public static final int MESSAGE_RATING_UPDATE = 1;
    public static final int MESSAGE_RECORD_RATING_FROM_USER = 2;

    private final Map<String, Integer> mRatingMap = new HashMap<>();
    private final List<OnRatingChangeObserver> mOnRatingChangeObservers = new ArrayList<>();
    private final File mRootDir = new File(Common.getDSaudioAppFolder());
    private final CacheWorkerHandler mHandler;
    private boolean bUpdatingRate = false;
    private boolean bRecordingRatingFromUser = false;

    public interface OnRatingChangeObserver {
        void onRatingChanged(List<SongItem> songItemList);
    }

    private CacheManager() {
        clearProfile();
        HandlerThread mWorkerThread = new HandlerThread("CacheManagerWorkerThead");
        mWorkerThread.start();
        this.mHandler = new CacheWorkerHandler(mWorkerThread.getLooper());
    }

    private class CacheWorkerHandler extends Handler {
        public CacheWorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            int i = msg.what;
            if (i == MESSAGE_RATING_UPDATE) {
                final List<SongItem> list = (List) msg.obj;
                final int i2 = msg.arg1;
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        CacheManager.this.ratingSongs(list, i2);
                        return null;
                    }
                }.execute();
            } else {
                if (i != 2) {
                    return;
                }
                final List<SongItem> list2 = (List) msg.obj;
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        CacheManager.this.recordRatingFromUser(list2);
                        return null;
                    }
                }.execute();
            }
        }
    }

    public void registerOnRatingChangeObserver(OnRatingChangeObserver observer) {
        this.mOnRatingChangeObservers.add(observer);
    }

    public void unregisterOnRatingChangeObserver(OnRatingChangeObserver observer) {
        this.mOnRatingChangeObservers.remove(observer);
    }

    private void notifyRatingChanged(List<SongItem> songList) {
        for (OnRatingChangeObserver mOnRatingChangeObserver : this.mOnRatingChangeObservers) {
            mOnRatingChangeObserver.onRatingChanged(songList);
        }
    }

    public boolean isUpdatingRate() {
        return this.mHandler.hasMessages(MESSAGE_RATING_UPDATE) || this.bUpdatingRate;
    }

    public void requestRatingSongs(final List<SongItem> songList, final int rating) {
        Message message = new Message();
        message.what = MESSAGE_RATING_UPDATE;
        message.obj = songList;
        message.arg1 = rating;
        this.mHandler.removeMessages(MESSAGE_RATING_UPDATE);
        this.mHandler.sendMessageDelayed(message, 500L);
    }

    public void ratingSongs(final List<SongItem> songList, final int rating) {
        this.bUpdatingRate = true;
        ArrayList<String> arrayList = new ArrayList<>();
        for (SongItem songItem : songList) {
            arrayList.add(songItem.getID());
            songItem.setSongRating(rating);
        }
        try {
            ConnectionManager.doSetRating(arrayList, rating);
        } catch (IOException e) {
            e.printStackTrace();
        }
        recordRatingFromUser(songList);
        this.bUpdatingRate = false;
    }

    public void recordRatingFromUser(List<SongItem> songList) {
        this.bRecordingRatingFromUser = true;
        DatabaseAccesser.getInstance().updateSongRating(songList);
        notifyRatingChanged(songList);
        this.bRecordingRatingFromUser = false;
    }

    public static CacheManager getInstance() {
        if (sCacheManger == null) {
            sCacheManger = new CacheManager();
        }
        return sCacheManger;
    }

    private void clearProfile() {
        File file = new File(this.mRootDir + PROFILE_DIR);
        if (file.exists()) {
            clearFolder(file);
            file.delete();
        }
    }

    public static class ItemSet<T> {
        private List<T> mItems;
        private int mTotal;

        public ItemSet() {
            this.mItems = new ArrayList<>();
            this.mTotal = 0;
        }

        ItemSet(int total, List<T> items) {
            new ArrayList<Item>();
            this.mTotal = total;
            this.mItems = items;
        }

        public int getTotal() {
            return this.mTotal;
        }

        public void setTotal(int total) {
            this.mTotal = total;
        }

        public List<T> getItemList() {
            return this.mItems;
        }

        public void setItemList(List<T> items) {
            this.mItems = items;
        }
    }

    private AbstractNetManager validateNetMgr(String dsid) {
        if (!Common.isLogin() || !Common.getDsId().equals(dsid)) {
            return null;
        }
        if (ConnectionManager.isUseWebAPI()) {
            return new ApiEnumerator();
        }
        return new CgiEnumerator();
    }

    private String getCacheDir() {
        String str = this.mRootDir + CACHE_DIR;
        File file = new File(str);
        if (!file.exists()) {
            file.mkdir();
        }
        return str;
    }

    private String getCachePrefixFolder(final String name) {
        String str = getCacheDir() + name;
        File file = new File(str);
        if (!file.exists()) {
            file.mkdir();
        }
        return str;
    }

    public void clearCache() {
        clearFolder(new File(getCacheDir()));
    }

    private void clearFolder(File dir) {
        String[] list;
        if (!dir.exists() || (list = dir.list()) == null || list.length == 0) {
            return;
        }
        for (String str : list) {
            // RemoteSettings.FORWARD_SLASH_STRING
            File file = new File(dir.getAbsolutePath() + "/" + str);
            if (file.isDirectory()) {
                clearFolder(file);
            }
            if (file.delete()) {
                SynoLog.d(LOG, "delete : " + file.getPath());
            } else {
                SynoLog.e(LOG, "fail to delete : " + file.getPath());
            }
        }
    }

    public void clearOnlineCache() {
        String[] list;
        File file = new File(getCacheDir());
        if (!file.exists() || !file.isDirectory() || (list = file.list()) == null || list.length == 0) {
            return;
        }
        for (String s : list) {
            if (s.startsWith("true")) {
                // RemoteSettings.FORWARD_SLASH_STRING
                File file2 = new File(file.getAbsolutePath() + "/" + s);
                if (file2.isDirectory()) {
                    clearFolder(file2);
                }
                if (file2.delete()) {
                    SynoLog.d(LOG, "delete : " + file2.getPath());
                } else {
                    SynoLog.e(LOG, "fail to delete : " + file2.getPath());
                }
            }
        }
    }

    public void clearPlaylistCache() {
        String[] list;
        File file = new File(getCacheDir());

        if (!file.exists() || !file.isDirectory() || (list = file.list()) == null || list.length == 0) {
            return;
        }

        for (String s : list) {
            if (!s.toLowerCase(Locale.getDefault()).contains("playlist")) {
                continue;
            }

            File file2 = new File(file, s);

            if (file2.isDirectory()) {
                clearFolder(file2);
            }

            if (file2.delete()) {
                SynoLog.d(LOG, "delete : " + file2.getPath());
            } else {
                SynoLog.e(LOG, "fail to delete : " + file2.getPath());
            }
        }
    }

    public void clearPlaylistSongCache(final PlaylistItem playlistItem) {
        clearFolder(new File(getCachePrefixFolder(getCachePrefixPlaylist(true, playlistItem))));
    }

    public boolean rotateSong(long needByte) {
        long songCacheLimit = AudioPreference.getSongCacheLimit();
        long autoCacheSize = AudioPreference.getAutoCacheSize();
        SynoLog.d(LOG, "[rotateSong]\ncacheSongSize = " + (autoCacheSize / 1024) + "KB\n limit = " + (songCacheLimit / 1024) + "KB\n free = " + ((songCacheLimit - autoCacheSize) / 1024) + "KB\n needByte = " + (needByte / 1024) + "KB");
        if (songCacheLimit >= 0 && songCacheLimit < needByte) {
            return false;
        }
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        while (Utilities.isSDCardFull() || (0 <= songCacheLimit && songCacheLimit - autoCacheSize < needByte)) {
            SongItem[] songItemArrDoEnumRotateCandidate = databaseAccesser.doEnumRotateCandidate();
            if (songItemArrDoEnumRotateCandidate.length == 0) {
                databaseAccesser.close();
                return false;
            }
            SongItem songItem = songItemArrDoEnumRotateCandidate[0];
            Utilities.subCacheByte(songItem);
            Utilities.removeFile(songItem.getCachePath());
            Utilities.removeFile(songItem.getCoverPath());
            Utilities.removeFile(songItem.getLyricPath());
            databaseAccesser.deleteSong(songItem);
            autoCacheSize = AudioPreference.getAutoCacheSize();
            SynoLog.d(LOG, "cacheSongSize = " + (autoCacheSize / 1024) + "KB");
        }
        databaseAccesser.close();
        return true;
    }

    public void deleteSong(SongItem song) {
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        SongItem songItemQuerySong = databaseAccesser.querySong(song);
        if (databaseAccesser.deleteSong(songItemQuerySong) > 0) {
            Utilities.subCacheByte(songItemQuerySong);
            Utilities.removeFile(songItemQuerySong.getCachePath());
            Utilities.removeFile(songItemQuerySong.getCoverPath());
            Utilities.removeFile(songItemQuerySong.getLyricPath());
        }
        databaseAccesser.close();
    }

    public JSONObject doEnumLyrics(SongItem song) throws IOException {
        if (!song.isOnDS()) {
            return new JSONObject();
        }
        String lyricPath = song.getLyricPath();
        SynoLog.e("getLyricPath", song.getLyricPath());
        File file = new File(lyricPath);
        if (!TextUtils.isEmpty(lyricPath) && file.exists()) {
            try {
                return getJsonObjectFromFile(file);
            } catch (IOException unused) {
                SynoLog.e(LOG, "No storage permission");
            }
        }
        AbstractNetManager abstractNetManagerValidateNetMgr = validateNetMgr(song.getDsId());
        String str = Common.getLyricFolder() + song.getUniqueKey();
        File file2 = new File(str);
        if (abstractNetManagerValidateNetMgr == null) {
            if (Common.isLogin() && ConnectionManager.isUseWebAPI()) {
                new ApiEnumerator().doSearchLyrics(str, song);
            } else {
                return new JSONObject();
            }
        } else {
            try {
                abstractNetManagerValidateNetMgr.doEnumLyrics(str, song.getID());
                JSONObject jsonObjectFromFile = getJsonObjectFromFile(file2);
                if (abstractNetManagerValidateNetMgr.getResourceType().equals(ConnectionManager.ResourceType.API)) {
                    JSONObject jSONObjectOptJSONObject = jsonObjectFromFile.optJSONObject("data");
                    if (TextUtils.isEmpty(jSONObjectOptJSONObject != null ? jSONObjectOptJSONObject.optString(LyricFragment.LYRICS, "") : "")) {
                        ((ApiEnumerator) abstractNetManagerValidateNetMgr).doSearchLyrics(str, song);
                    }
                }
                if (file2.exists()) {
                    DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
                    databaseAccesser.addLyric(song, str);
                    databaseAccesser.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new JSONObject();
            }
        }
        return getJsonObjectFromFile(file2);
    }

    public ItemSet<PlaylistItem> doEnumNormalPlaylist(boolean isShared) {
        BasePlaylistResponseVo basePlaylistResponseVoDoEnumPlaylist = PlaylistEditor.doEnumPlaylist(isShared);

        if (basePlaylistResponseVoDoEnumPlaylist == null) {
            return new ItemSet<>(0, new LinkedList<>());
        }

        List<PlaylistItem> list = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            list = basePlaylistResponseVoDoEnumPlaylist.getPlaylists().stream()
                    .filter(BasePlaylistResponseVo.BasePlaylistVo::isNormal)
                    .map(PlaylistItem::generateByPlaylistVo)
                    .filter(item -> !item.isPredefined())
                    .filter(item -> item.isPersonal() == !isShared)
                    .collect(Collectors.toList());
        }

        return new ItemSet<>(basePlaylistResponseVoDoEnumPlaylist.getValidTotal(), list);
    }

    public ItemSet<Item> doEnumContainerForContainer(boolean isOnline, Common.ContainerType type, Bundle bundle, int pageNum, boolean doRefresh) throws WebAPIErrorException {
        ItemSet<Item> itemSet = new ItemSet<>();
        try {
            return parseJsonToContainerList(isOnline, type, doEnumContainer(isOnline, type, bundle, pageNum, doRefresh));
        } catch (WebAPIErrorException e) {
            throw e;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return itemSet;
        }
    }

    private ItemSet<Item> parseJsonToContainerList(boolean isOnline, Common.ContainerType type, JSONObject jsonObject) throws JSONException, WebAPIErrorException {

        String additionalKey = "additional";
        List<Item> list = new LinkedList<>();

        String defaultTitle;
        String dataKey;

        if (type == Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE) {
            defaultTitle = App.getContext().getString(R.string.unknown_genre);
            dataKey = "default_genres";
        } else if (type == Common.ContainerType.GENRE_MODE) {
            defaultTitle = App.getContext().getString(R.string.unknown_genre);
            dataKey = "genres";
        } else if (type == Common.ContainerType.ARTIST_MODE
                || type == Common.ContainerType.SEARCH_ARTIST_MODE
                || type == Common.ContainerType.GENRE_ARTIST_MODE) {
            defaultTitle = App.getContext().getString(R.string.unknown_artist);
            dataKey = "artists";
        } else if (type == Common.ContainerType.COMPOSER_MODE) {
            defaultTitle = App.getContext().getString(R.string.unknown_composer);
            dataKey = "composers";
        } else {
            defaultTitle = App.getContext().getString(R.string.unknown_album);
            dataKey = "albums";
        }

        String albumArtistKey = "album_artist";
        String displayArtistKey = "display_artist";
        String itemsKey = "items";
        String nameKey = "name";
        String totalKey = "total";
        String ratingKey = "avg_rating";

        int total = 0;

        // =============================
        // OFFLINE MODE
        // =============================
        if (!isOnline) {
            total = jsonObject.getInt(totalKey);

            JSONArray items = jsonObject.getJSONArray(itemsKey);

            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.getJSONObject(i);

                String name = obj.optString(nameKey);

                Item item = new Item(
                        Item.ItemType.CONTAINER_MODE,
                        name,
                        TextUtils.isEmpty(name) ? defaultTitle : name
                );

                if ("albums".equals(dataKey)) {
                    item.setDisplayArtist(obj.optString(displayArtistKey));
                    item.setAlbumArtist(obj.optString(albumArtistKey));
                }

                if (obj.has(ratingKey)) {
                    item.setRating((float) obj.optDouble(ratingKey));
                }

                list.add(item);
            }

            return new ItemSet<>(total, list);
        }

        // =============================
        // ONLINE MODE
        // =============================
        if (ConnectionManager.isUseWebAPI()) {
            checkErrorCode(jsonObject);

            try {
                JSONObject data = jsonObject.getJSONObject("data");

                total = data.getInt(totalKey);

                JSONArray array = data.getJSONArray(dataKey);

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);

                    String name = obj.optString(nameKey);

                    Item item = new Item(Item.ItemType.CONTAINER_MODE, name, TextUtils.isEmpty(name) ? defaultTitle : name);

                    if (obj.has(additionalKey)) {
                        JSONObject additional = obj.getJSONObject(additionalKey);

                        if (additional.has(ratingKey)) {
                            JSONObject ratingObj = additional.getJSONObject(ratingKey);

                            float rating = (float) ratingObj.getDouble("rating");

                            item.setRating(rating);
                        }
                    }

                    if ("albums".equals(dataKey)) {
                        item.setDisplayArtist(obj.optString(displayArtistKey));
                        item.setAlbumArtist(obj.optString(albumArtistKey));
                    }

                    list.add(item);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return new ItemSet<>(total, list);
        }

        // =============================
        // OLD API MODE
        // =============================
        try {
            total = jsonObject.getInt(totalKey);

            JSONArray items = jsonObject.getJSONArray(itemsKey);

            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.getJSONObject(i);

                String name = obj.getString(nameKey);

                Item item;

                if (name.isEmpty()) {
                    item = new Item(Item.ItemType.CONTAINER_MODE, name, defaultTitle);
                } else {
                    item = new Item(Item.ItemType.CONTAINER_MODE, name, name);
                }

                list.add(item);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new ItemSet<>(total, list);
    }

    private String getKeyFromBundle(Bundle bundle) {
        String str = bundle.containsKey("genre") ? bundle.getString("genre") : "";
        if (bundle.containsKey("genre_filter")) {
            str = str + bundle.getString("genre_filter");
        }
        if (bundle.containsKey("artist")) {
            str = str + bundle.getString("artist");
        }
        if (bundle.containsKey("composer")) {
            str = str + bundle.getString("composer");
        }
        if (bundle.containsKey("album")) {
            str = str + bundle.getString("album");
        }
        return bundle.containsKey("key") ? str + bundle.getString("key") : str;
    }

    private JSONObject doEnumContainer(boolean isOnline, Common.ContainerType type, Bundle bundle, int pageNum, boolean doRefresh) throws JSONException, IOException {
        SynoLog.d(LOG, "doEnumContainer " + isOnline);
        String str = ("" + isOnline) + "_" + type.name();
        String keyFromBundle = getKeyFromBundle(bundle);
        if (!TextUtils.isEmpty(keyFromBundle)) {
            str = str + "_" + Utilities.getMD5Code(keyFromBundle);
        }
        String cachePrefixFolder = getCachePrefixFolder(str + "_" + AbstractNetManager.getPersonalLibraryValue());
        if (doRefresh) {
            clearFolder(new File(cachePrefixFolder));
        }
        // RemoteSettings.FORWARD_SLASH_STRING
        String str2 = cachePrefixFolder + "/" + pageNum + ".cache";
        File file = new File(str2);
        if (!file.exists()) {
            ConnectionManager.doEnumContainer(isOnline, type, bundle, str2, pageNum);
        }
        JSONObject jsonObjectFromFile = getJsonObjectFromFile(file);
        if (!isOnline) {
            file.delete();
        }
        return jsonObjectFromFile;
    }

    private ItemSet<SongItem> parseJsonToSongList(boolean isOnline, JSONObject jsonObject) throws JSONException, WebAPIErrorException {
        int i;
        ArrayList<SongItem> arrayList = new ArrayList<>();
        int i2 = 0;
        if (!isOnline) {
            try {
                JSONArray jSONArray = jsonObject.getJSONArray("items");
                int length = jSONArray.length();
                for (int i3 = 0; i3 < length; i3++) {
                    arrayList.add(SongItem.fromJsonString(jSONArray.getString(i3)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            if (ConnectionManager.isUseWebAPI()) {
                checkErrorCode(jsonObject);
                JSONObject jSONObject = jsonObject.getJSONObject("data");
                i = jSONObject.getInt(TOTAL);
                JSONArray jSONArray2 = jSONObject.getJSONArray(SONGS);
                int length2 = jSONArray2.length();
                while (i2 < length2) {
                    SongItem songItemFromApiJson = SongItem.fromApiJson(jSONArray2.getJSONObject(i2));
                    if (songItemFromApiJson != null && songItemFromApiJson.isFile()) {
                        arrayList.add(songItemFromApiJson);
                    }
                    i2++;
                }
            } else {
                JSONArray jSONArray3 = jsonObject.getJSONArray("items");
                i = jsonObject.getInt(TOTAL);
                int length3 = jSONArray3.length();
                while (i2 < length3) {
                    SongItem songItemFromCgiJson = SongItem.fromCgiJson(jSONArray3.getJSONObject(i2));
                    if (songItemFromCgiJson != null && songItemFromCgiJson.isFile()) {
                        arrayList.add(songItemFromCgiJson);
                    }
                    i2++;
                }
            }
            i2 = i;
        }
        return new ItemSet<>(i2, arrayList);
    }

    public ItemSet<SongItem> doEnumContainerSongsForContainer(boolean isOnline, final Common.ContainerType type, final Bundle bundle, int pageNum, boolean doRefresh) throws WebAPIErrorException {
        ItemSet<SongItem> itemSet = new ItemSet<>();
        try {
            boolean[] zArr = {false};
            itemSet = parseJsonToSongList(isOnline, doEnumContainerSongs(isOnline, type, bundle, pageNum, doRefresh, zArr));
            if (zArr[0]) {
                recordRatingFromDS(itemSet.getItemList());
            } else {
                adjustRating(itemSet.getItemList());
            }
        } catch (WebAPIErrorException e) {
            throw e;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return itemSet;
    }

    private JSONObject doEnumContainerSongs(boolean isOnline, final Common.ContainerType type, final Bundle bundle, int pageNum, boolean doRefresh, boolean[] isForcedLoaded) throws JSONException, IOException {
        SynoLog.d(LOG, "doEnumContainerSongs " + isOnline);
        String cachePrefixFolder = getCachePrefixFolder(((("" + isOnline) + "_" + type.name()) + "_" + Utilities.getMD5Code(bundle.toString())) + "_" + AbstractNetManager.getPersonalLibraryValue());
        if (doRefresh) {
            clearFolder(new File(cachePrefixFolder));
        }
        // RemoteSettings.FORWARD_SLASH_STRING
        String str = cachePrefixFolder + "/" + pageNum + ".cache";
        File file = new File(str);
        if (!file.exists()) {
            ConnectionManager.doEnumContainerSongs(isOnline, type, bundle, str, pageNum);
            isForcedLoaded[0] = true;
        }
        JSONObject jsonObjectFromFile = getJsonObjectFromFile(file);
        if (!isOnline) {
            file.delete();
        }
        return jsonObjectFromFile;
    }

    public ItemSet<HomePagePinItem> doEnumPins(boolean doRefresh) throws Throwable {
        int total;
        SynoLog.d(LOG, " doEnumPins ");
        File file = new File(getCachePrefixFolder(Common.ContainerType.HOMEPAGE_PIN_MODE.name()));
        if (doRefresh) {
            clearFolder(file);
        }
        File file2 = new File(file, "pin.cache");
        if (file2.exists()) {
            return (ItemSet) ObjFile.getObjectFromFile(file2, new TypeToken<ItemSet<HomePagePinItem>>() {
            }.getType());
        }
        LinkedList<HomePagePinItem> linkedList = new LinkedList<>();
        PinListResponseVo pinListResponseVoDoEnumPins = ConnectionManager.doEnumPins();
        SynoLog.i(LOG, "pinListResponseVoDoEnumPins");
        System.out.println(pinListResponseVoDoEnumPins.getItems());
        if (pinListResponseVoDoEnumPins != null) {
            for (PinItemVo pinItemVo : pinListResponseVoDoEnumPins.getItems()) {
                linkedList.add(HomePagePinItem.Companion.generateByPinItemVo(pinItemVo));
            }
            total = pinListResponseVoDoEnumPins.getTotal();
        } else {
            total = 0;
        }
        ItemSet<HomePagePinItem> itemSet = new ItemSet<HomePagePinItem>(total, linkedList);
        ObjFile.saveObjectToFile(itemSet, file2, ItemSet.class);
        return itemSet;
    }

    public ItemSet<SongItem> doEnumRadiosForRadios(final String key, int pageNum, boolean doRefresh) throws WebAPIErrorException {

        try {
            JSONObject json = doEnumRadios(key, pageNum, doRefresh);
            return parseJsonToRadioList(json);
        } catch (WebAPIErrorException e) {
            throw e;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return new ItemSet<>();
        }
    }

    private ItemSet<SongItem> parseJsonToRadioList(JSONObject jsonObject) throws JSONException, WebAPIErrorException {

        List<SongItem> radios = new LinkedList<>();
        int total = 0;

        try {
            if (ConnectionManager.isUseWebAPI()) {
                checkErrorCode(jsonObject);

                JSONObject data = jsonObject.getJSONObject("data");
                total = data.getInt(TOTAL);

                JSONArray radioArray = data.getJSONArray(RADIOS);

                for (int i = 0; i < radioArray.length(); i++) {
                    SongItem item = SongItem.fromApiJson(radioArray.getJSONObject(i));

                    if (item != null) {
                        radios.add(item);
                    }
                }

            } else {
                total = jsonObject.getInt(TOTAL);

                JSONArray itemsArray = jsonObject.getJSONArray("items");

                for (int i = 0; i < itemsArray.length(); i++) {
                    SongItem item = SongItem.fromCgiJson(
                            itemsArray.getJSONObject(i)
                    );

                    if (item != null) {
                        String title = Common.getRadioTitleByID(item.getID());

                        if (title != null) {
                            item.setTitle(title);
                        }

                        radios.add(item);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new ItemSet<>(total, radios);
    }

    private JSONObject doEnumRadios(final String key, int pageNum, boolean doRefresh) throws IOException {

        SynoLog.d(LOG, "doEnumRadios key : " + key);

        String cacheKey = Common.ContainerType.RADIO_MODE.name();

        if (!TextUtils.isEmpty(key)) {
            cacheKey += "_" + key;
        }

        cacheKey += "_" + AbstractNetManager.getPersonalLibraryValue();

        String cacheFolder = getCachePrefixFolder(cacheKey);

        if (doRefresh) {
            clearFolder(new File(cacheFolder));
        }

        String cachePath = cacheFolder + "/" + pageNum + ".cache";

        File cacheFile = new File(cachePath);

        if (!cacheFile.exists()) {
            ConnectionManager.doEnumRadios(key, cachePath, pageNum);
        }

        return getJsonObjectFromFile(cacheFile);
    }

    public ItemSet<SongItem> doEnumFolderSongsForFileSongList(boolean isOnline, final String key, boolean recursive, int pageNum, boolean doRefresh) {
        ItemSet<SongItem> itemSet = new ItemSet<>();
        try {
            boolean[] zArr = {false};
            itemSet = parseJsonToFileSongList(isOnline, doEnumFolderSongs(isOnline, key, recursive, pageNum, doRefresh, zArr));
            if (zArr[0]) {
                recordRatingFromDS(itemSet.getItemList());
            } else {
                adjustRating(itemSet.getItemList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemSet;
    }

    public ItemSet<SongItem> doEnumFolderSongsForSongFileList(boolean isOnline, final String key, boolean recursive, int pageNum, boolean doRefresh) throws WebAPIErrorException {
        ItemSet<SongItem> itemSet = new ItemSet<>();
        try {
            boolean[] zArr = {false};
            itemSet = parseJsonToSongFileList(isOnline, doEnumFolderSongs(isOnline, key, recursive, pageNum, doRefresh, zArr));
            if (zArr[0]) {
                recordRatingFromDS(itemSet.getItemList());
            } else {
                adjustRating(itemSet.getItemList());
            }
        } catch (WebAPIErrorException e) {
            throw e;
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        return itemSet;
    }

    private ItemSet<SongItem> parseJsonToSongFileList(boolean isOnline, JSONObject jsonObject) throws WebAPIErrorException {

        List<SongItem> list = new ArrayList<>();
        int total = 0;

        try {
            if (!isOnline) {
                // 本地模式
                total = jsonObject.getInt(TOTAL);

                JSONArray items = jsonObject.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    list.add(SongItem.fromJsonString(items.getString(i)));
                }

            } else if (ConnectionManager.isUseWebAPI()) {
                // WebAPI 模式
                checkErrorCode(jsonObject);

                JSONObject data = jsonObject.getJSONObject("data");
                total = data.getInt(TOTAL);

                JSONArray items = data.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    list.add(SongItem.fromApiJson(items.getJSONObject(i)));
                }

            } else {
                // CGI 模式
                total = jsonObject.getInt(TOTAL);

                JSONArray items = jsonObject.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    list.add(SongItem.fromCgiJson(items.getJSONObject(i)));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            // 保持和原逻辑一致：出错时仍返回已解析数据
        }

        return new ItemSet<>(total, list);
    }

    private JSONObject doEnumFolderSongs(boolean isOnline, final String key, boolean recursive, int pageNum, boolean doRefresh, boolean[] isForcedLoaded) throws Exception {
        SynoLog.d(LOG, "doEnumFolderSongs " + isOnline);
        String str = ("" + isOnline) + "_" + Common.ContainerType.FOLDER_MODE.name();
        if (!TextUtils.isEmpty(key)) {
            str = str + "_" + key;
        }
        if (recursive) {
            str = str + "_recursive";
        }
        String str2 = str + "_" + AbstractNetManager.getPersonalLibraryValue();
        String cachePrefixFolder = getCachePrefixFolder(str2);
        if (doRefresh) {
            clearFolder(new File(cachePrefixFolder));
        }
        // RemoteSettings.FORWARD_SLASH_STRING
        String str3 = getCachePrefixFolder(str2) + "/" + pageNum + ".cache";
        File file = new File(str3);
        if (!file.exists()) {
            ConnectionManager.doEnumFolderSongs(isOnline, key, recursive, str3, pageNum);
            isForcedLoaded[0] = true;
        }
        JSONObject jsonObjectFromFile = getJsonObjectFromFile(file);
        if (!isOnline) {
            file.delete();
        }
        return jsonObjectFromFile;
    }

    private JSONObject getJsonObjectFromFile(File file) throws IOException {
        long jCurrentTimeMillis = System.currentTimeMillis();
        JSONObject jSONObject = new JSONObject();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuffer = new StringBuilder();
        for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
            stringBuffer.append(line);
        }
        bufferedReader.close();
        String string = stringBuffer.toString();
        SynoLog.d(LOG, "strRet = " + string);
        try {
            jSONObject = new JSONObject(string);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SynoLog.d(LOG, "process : " + (System.currentTimeMillis() - jCurrentTimeMillis));
        return jSONObject;
    }

    private ItemSet<SongItem> parseJsonToFileSongList(boolean isOnline, JSONObject jsonObject) throws JSONException, WebAPIErrorException {
        ArrayList arrayList = new ArrayList();
        int i = 0;
        if (!isOnline) {
            try {
                JSONArray jSONArray = jsonObject.getJSONArray("items");
                int length = jSONArray.length();
                while (i < length) {
                    arrayList.add(SongItem.fromJsonString(jSONArray.getString(i)));
                    i++;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (ConnectionManager.isUseWebAPI()) {
            checkErrorCode(jsonObject);
            JSONArray jSONArray2 = jsonObject.getJSONObject("data").getJSONArray("items");
            int length2 = jSONArray2.length();
            while (i < length2) {
                SongItem songItemFromApiJson = SongItem.fromApiJson(jSONArray2.getJSONObject(i));
                if (songItemFromApiJson != null && songItemFromApiJson.isFile()) {
                    arrayList.add(songItemFromApiJson);
                }
                i++;
            }
        } else {
            JSONArray jSONArray3 = jsonObject.getJSONArray("items");
            int length3 = jSONArray3.length();
            while (i < length3) {
                SongItem songItemFromCgiJson = SongItem.fromCgiJson(jSONArray3.getJSONObject(i));
                if (songItemFromCgiJson != null && songItemFromCgiJson.isFile()) {
                    arrayList.add(songItemFromCgiJson);
                }
                i++;
            }
        }
        return new ItemSet<>(arrayList.size(), arrayList);
    }

    public ItemSet<PlaylistItem> doEnumPlaylist(boolean isOnline, int pageNum, boolean doRefresh) throws Exception {
        int validTotal;
        SynoLog.d(LOG, "doEnumPlaylist " + isOnline);
        File file = new File(getCachePrefixFolder(String.format("%b_%s_%s", isOnline, Common.ContainerType.PLAYLIST_MODE.name(), AbstractNetManager.getPersonalLibraryValue())));
        if (doRefresh) {
            clearFolder(file);
        }
        File file2 = new File(file, pageNum + ".cache");
        if (file2.exists()) {
            return (ItemSet<PlaylistItem>) ObjFile.getObjectFromFile(file2, new TypeToken<ItemSet<PlaylistItem>>() {
            }.getType());
        }
        LinkedList<PlaylistItem> linkedList = new LinkedList<>();
        BasePlaylistResponseVo basePlaylistResponseVoDoEnumPlaylist = ConnectionManager.doEnumPlaylist(isOnline, pageNum);
        if (basePlaylistResponseVoDoEnumPlaylist != null) {
            for (BasePlaylistResponseVo.BasePlaylistVo basePlaylistVo : basePlaylistResponseVoDoEnumPlaylist.getPlaylists()) {
                linkedList.add(PlaylistItem.generateByPlaylistVo(basePlaylistVo));
            }
            validTotal = basePlaylistResponseVoDoEnumPlaylist.getValidTotal();
        } else {
            validTotal = 0;
        }
        ItemSet<PlaylistItem> itemSet = new ItemSet<>(validTotal, linkedList);
        if (isOnline) {
            ObjFile.saveObjectToFile(itemSet, file2, ItemSet.class);
        }
        return itemSet;
    }

    private String getCachePrefixPlaylist(boolean isOnline, PlaylistItem playlistItem) {
        return ((("" + isOnline) + "_" + playlistItem.getType().name()) + "_" + Utilities.getMD5Code(playlistItem.getBundle().toString())) + "_" + AbstractNetManager.getPersonalLibraryValue();
    }

    private JSONObject doEnumPlaylistSongs(boolean isOnline, PlaylistItem playlistItem, int pageNum, boolean doRefresh, boolean[] isForcedLoaded) throws JSONException, IOException {
        SynoLog.d(LOG, "doEnumContainerSongs " + isOnline);
        String cachePrefixFolder = getCachePrefixFolder(getCachePrefixPlaylist(isOnline, playlistItem));
        if (doRefresh) {
            clearFolder(new File(cachePrefixFolder));
        }
        // RemoteSettings.FORWARD_SLASH_STRING
        String str = cachePrefixFolder + "/" + pageNum + ".cache";
        File file = new File(str);
        if (!file.exists()) {
            ConnectionManager.doEnumPlaylistSongs(isOnline, playlistItem, str, pageNum);
            isForcedLoaded[0] = true;
        }
        JSONObject jsonObjectFromFile = getJsonObjectFromFile(file);
        if (!isOnline) {
            file.delete();
        }
        return jsonObjectFromFile;
    }

    public ItemSet<SongItem> doEnumPlaylistSongsForPlaylist(boolean isOnline, PlaylistItem playlistItem, int pageNum, boolean doRefresh) throws WebAPIErrorException {
        ItemSet<SongItem> itemSet = new ItemSet<>();
        try {
            boolean[] zArr = {false};
            itemSet = parseJsonToPlaylistSongList(isOnline, doEnumPlaylistSongs(isOnline, playlistItem, pageNum, doRefresh, zArr));
            if (zArr[0]) {
                recordRatingFromDS(itemSet.getItemList());
            } else {
                adjustRating(itemSet.getItemList());
            }
        } catch (WebAPIErrorException e) {
            throw e;
        } catch (IOException | JSONException e2) {
            e2.printStackTrace();
        }
        return itemSet;
    }

    private ItemSet<SongItem> parseJsonToPlaylistSongList(boolean online, JSONObject jsonObject) throws JSONException, WebAPIErrorException {
        int i;
        ArrayList<SongItem> arrayList = new ArrayList<>();
        int i2 = 0;
        if (!online) {
            try {
                JSONArray jSONArray = jsonObject.getJSONArray("items");
                int length = jSONArray.length();
                for (int i3 = 0; i3 < length; i3++) {
                    arrayList.add(SongItem.fromJsonString(jSONArray.getString(i3)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            if (ConnectionManager.isUseWebAPI()) {
                checkErrorCode(jsonObject);
                JSONObject jSONObject = jsonObject.getJSONObject("data");
                new JSONArray();
                if (jSONObject.has(PLAYLISTS)) {
                    jSONObject = jSONObject.getJSONArray(PLAYLISTS).getJSONObject(0).getJSONObject("additional");
                }
                if (jSONObject.has(SONGS_TOTAL)) {
                    i = jSONObject.getInt(SONGS_TOTAL);
                } else {
                    i = jSONObject.has(TOTAL) ? jSONObject.getInt(TOTAL) : 0;
                }
                JSONArray jSONArray2 = jSONObject.getJSONArray(SONGS);
                int length2 = jSONArray2.length();
                while (i2 < length2) {
                    SongItem songItemFromApiJson = SongItem.fromApiJson(jSONArray2.getJSONObject(i2));
                    if (songItemFromApiJson != null) {
                        arrayList.add(songItemFromApiJson);
                    }
                    i2++;
                }
            } else {
                JSONArray jSONArray3 = jsonObject.getJSONArray("items");
                i = jsonObject.getInt(TOTAL);
                int length3 = jSONArray3.length();
                while (i2 < length3) {
                    SongItem songItemFromCgiJson = SongItem.fromCgiJson(jSONArray3.getJSONObject(i2));
                    if (songItemFromCgiJson != null) {
                        arrayList.add(songItemFromCgiJson);
                    }
                    i2++;
                }
            }
            i2 = i;
        }
        return new ItemSet<>(i2, arrayList);
    }

    private void checkErrorCode(JSONObject jObject) throws WebAPIErrorException {
        BaseVo baseVo = (BaseVo) new Gson().fromJson(jObject.toString(), BaseVo.class);
        if (baseVo.getError() != null && baseVo.getError().getCode() == 105) {
            throw new WebAPIErrorException(105);
        }
    }

    private void recordRatingFromDS(List<SongItem> songList) {
        if (!isUpdatingRate() && !isRecordingRatingFromUser()) {
            for (SongItem songItem : songList) {
                this.mRatingMap.remove(getRatingKey(songItem));
            }
        }
        DatabaseAccesser.getInstance().updateSongRating(songList);
        notifyRatingChanged(songList);
    }

    public void adjustRating(SongItem song) {
        ArrayList<SongItem> arrayList = new ArrayList<>();
        arrayList.add(song);
        adjustRating(arrayList);
    }

    public void adjustRating(List<SongItem> songList) {
        for (SongItem songItem : songList) {
            String key = getRatingKey(songItem);

            if (this.mRatingMap.containsKey(key)) {
                float rating = this.mRatingMap.get(key).intValue();
                songItem.setRating(rating);
            }
        }
    }

    public boolean isRecordingRatingFromUser() {
        return this.mHandler.hasMessages(MESSAGE_RECORD_RATING_FROM_USER) || this.bRecordingRatingFromUser;
    }

    public void requestRecordRatingFromUser(final List<SongItem> songList) {
        Message message = new Message();
        message.what = MESSAGE_RECORD_RATING_FROM_USER;
        message.obj = songList;
        this.mHandler.removeMessages(MESSAGE_RECORD_RATING_FROM_USER);
        this.mHandler.sendMessageDelayed(message, 500L);
    }

    public void recordRatingMapFromUser(List<SongItem> songList) {
        for (SongItem songItem : songList) {
            this.mRatingMap.put(getRatingKey(songItem), songItem.getSongRating());
        }
    }

    public void recordRatingMapFromUser(List<SongItem> songList, int rating) {
        for (SongItem songItem : songList) {
            this.mRatingMap.put(getRatingKey(songItem), rating);
        }
    }

    private String getRatingKey(SongItem song) {
        return song.getDsId() + ":" + song.getID();
    }

    public void requestToGetRating(SongItem song) {
        if (song == null) {
            return;
        }
        SongItem songItemFromJsonString = SongItem.fromJsonString(song.toJsonString());
        try {
            songItemFromJsonString.setSongRating(
                    ConnectionManager.requestToGetRating(songItemFromJsonString)
            );
            ArrayList<SongItem> arrayList = new ArrayList<>();
            arrayList.add(songItemFromJsonString);
            recordRatingFromDS(arrayList);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

}
