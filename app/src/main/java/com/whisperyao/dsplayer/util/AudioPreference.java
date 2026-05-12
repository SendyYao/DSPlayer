package com.whisperyao.dsplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.whisperyao.dsplayer.AndroidAuto.AndroidAutoSetting;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.BuildConfig;
import com.whisperyao.dsplayer.Common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AudioPreference {

    private static final String LOG = "AudioPreference";

    private static final String DSAUDIO_INFO = "dsaudio_info";
    private static final String KEY_CURRENT_EQUALIZER_TYPE_NAME = "current_equalizer_type_name";
    private static final String KEY_COVER_VER = "cover_ver";
    private static final String KEY_HOME_PAGE = "home_page";
    private static final String KEY_VIEWMODE = "view_mode";
    private static final String KEY_LIBRARY_PAGE = "library_page";
    private static final String KEY_LOCAL_PAGE = "local_page";
    public static final String PREFERENCE_ENABLE_EQUALIZER = "enable_equalizer";
    public static final String PREFERENCE_CATEGORY_SOUND_EFFECT = "category_sound_effect";
    public static final String PREFERENCE_TAP_SONG = "pref_tap_song";
    public static final String PREFERENCE_PERSONAL = "pref_personal_library";
    public static final String PREFERENCE_ENABLE_REMOTE_CONTROLLER = "enable_remote_controller";
    public static final String PREFERENCE_FORCE_TRANSCODE = "pref_force_transcode";
    public static final String PREFERENCE_TRANSCODE = "pref_transcode_type";
    public static final String PREFERENCE_TRANSCODE_QUALITY = "pref_transcode_quality";
    public static final String PREFERENCE_SONG_CACHE_LIMIT = "song_cache_limit";
    public static final String PREFERENCE_ANDROIDAUTO_DEFAULT_PLAYLISTS = "pref_androidauto_default_playlists";
    public static final String PREFERENCE_ENABLE_REMOTE_CONTROLLER_DESCRIPTION = "enable_remote_controller_description";
    private static final String PREFERENCE_INFO = "preference_info";
    private static final String PREF_KEY_NAVIPREF = "navigation_preference";
    private static final String PREF_KEY_CATCHPATH = "catchpath";
    private static final String PREF_KEY_PLAYERID = "playerid";
    private static final String PREF_KEY_PLAYER_MODE = "playermode";
    private static final String KEY_USER_ADDR = "user_address";
    public static final String PREF_SONG_AUTO_CACHE_SIZE = "song_auto_cache_size";
    public static final String PREF_SONG_MANUAL_CACHE_SIZE = "song_manual_cache_size";

    private static String mCoverPath = null;
    private static int mCoverVer = -1;

    public static SharedPreferences getSharedPreferences() {
        return App.getContext().getSharedPreferences(DSAUDIO_INFO, 0);
    }

    public static boolean enableAutoDownload() {
        return getSongCacheLimit() != 0;
    }

    public static void setAutoCacheSize(long cacheByte) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putLong(PREF_SONG_AUTO_CACHE_SIZE, cacheByte).apply();
    }

    public static void setManualCacheSize(long cacheByte) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putLong(PREF_SONG_MANUAL_CACHE_SIZE, cacheByte).apply();
    }

    public static boolean enableEqualizer() {
        return PreferenceManager.getDefaultSharedPreferences(App.getContext()).getBoolean(PREFERENCE_ENABLE_EQUALIZER, false);
    }

    public static boolean enableRemoteController(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_ENABLE_REMOTE_CONTROLLER, true);
    }

    public static String getAccount() {
        return BuildConfig.NAS_ACCOUNT;
    }

    public static String getIp() {
        return BuildConfig.NAS_ADDRESS;
    }

    public static void setHomePage(Common.ContainerType pageType) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putString(KEY_HOME_PAGE, pageType.name()).apply();
    }

    public static Common.ContainerType getHomePage() {
        SharedPreferences sharedPreferences = App.getContext().getSharedPreferences(DSAUDIO_INFO, 0);
        Common.ContainerType containerType = Common.ContainerType.HOMEPAGE_PIN_MODE;
        try {
            return Common.ContainerType.valueOf(sharedPreferences.getString(KEY_HOME_PAGE, Common.ContainerType.HOMEPAGE_PIN_MODE.name()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return containerType;
        }
    }

    public static void setNavigationPref(int pos) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putInt(PREF_KEY_NAVIPREF, pos).apply();
    }

    public static int getNavigationPref() {
        return App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getInt(PREF_KEY_NAVIPREF, 0);
    }

    public static void setViewMode(final Common.PrefViewMode mode) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putString(KEY_VIEWMODE, mode.name()).apply();
    }

    public static Common.PrefViewMode getViewMode() {
        return Common.PrefViewMode.valueOf(App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getString(KEY_VIEWMODE, Common.PrefViewMode.LIST.name()));
    }

    public static Common.ContainerType getLibraryPage(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(DSAUDIO_INFO, 0);
        Common.ContainerType containerType = Common.ContainerType.ALBUM_MODE;
        try {
            return Common.ContainerType.valueOf(sharedPreferences.getString(KEY_LIBRARY_PAGE, Common.ContainerType.ALBUM_MODE.name()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return containerType;
        }
    }

    public static void setLibraryPage(Context context, Common.ContainerType pageType) {
        context.getSharedPreferences(DSAUDIO_INFO, 0).edit().putString(KEY_LIBRARY_PAGE, pageType.name()).apply();
    }


    public static Common.ContainerType getLocalPage(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(DSAUDIO_INFO, 0);
        Common.ContainerType containerType = Common.ContainerType.ALBUM_MODE;
        try {
            return Common.ContainerType.valueOf(sharedPreferences.getString(KEY_LOCAL_PAGE, Common.ContainerType.ALBUM_MODE.name()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return containerType;
        }
    }

    public static void setLocalPage(Context context, Common.ContainerType pageType) {
        context.getSharedPreferences(DSAUDIO_INFO, 0).edit().putString(KEY_LOCAL_PAGE, pageType.name()).apply();
    }


    public static boolean getHttpsPref() {
        return false;
    }

    public static Common.PrefPersonal getPersonalPref() {
        if (!Common.supportPersonalLibrary()) {
            return Common.PrefPersonal.ALL;
        }
        return Common.PrefPersonal.valueOf(PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(PREFERENCE_PERSONAL, Common.PrefPersonal.ALL.name()));
    }

    public static void setPlayerMode(String mode) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putString(PREF_KEY_PLAYER_MODE, mode).apply();
    }

    public static String getPlayerMode(String defaultMode) {
        return App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getString(PREF_KEY_PLAYER_MODE, defaultMode);
    }

    public static void setPlayerId(String id) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putString(PREF_KEY_PLAYERID, id).apply();
    }

    public static String getPlayerId(String defaultId) {
        return App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getString(PREF_KEY_PLAYERID, defaultId);
    }


    public static void setCoverPath(String path) {
        mCoverPath = path;
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putString("cover_path", path).apply();
    }

    public static String getCoverPath() {
        if (mCoverPath == null) {
            mCoverPath = App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getString("cover_path", "");
        }
        return mCoverPath;
    }

    public static void setCoverVer(int ver) {
        mCoverVer = ver;
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putInt(KEY_COVER_VER, ver).apply();
    }

    public static int getCoverVer() {
        if (-1 == mCoverVer) {
            mCoverVer = App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getInt(KEY_COVER_VER, 1);
        }
        return mCoverVer;
    }

    public static void setEqualizerType(final String name) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putString(KEY_CURRENT_EQUALIZER_TYPE_NAME, name).apply();
    }

    public static String getEqualizerType() {
        return App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getString(KEY_CURRENT_EQUALIZER_TYPE_NAME, "");
    }


    private static TranscodeSetting.TranscodeFormat getTranscodePref() {
        if (!Common.getTranscodeType().supportMP3()) {
            return TranscodeSetting.TranscodeFormat.WAV;
        }
        return TranscodeSetting.TranscodeFormat.valueOf(
                PreferenceManager.getDefaultSharedPreferences(App.getContext())
                        .getString(PREFERENCE_TRANSCODE, TranscodeSetting.TranscodeFormat.MP3.name()));
    }

    private static TranscodeSetting.TranscodeQuality getTranscodeQuality() {
        return TranscodeSetting.TranscodeQuality.valueOf(PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(PREFERENCE_TRANSCODE_QUALITY, TranscodeSetting.TranscodeQuality.AUTO.name()));
    }

    private static Set<TranscodeSetting.TranscodeForceFormat> getTranscodeForFormat() {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        migrateTranscodeForFormat(App.getContext());
        Set<String> stringSet = defaultSharedPreferences.getStringSet(PREFERENCE_FORCE_TRANSCODE, new HashSet());
        HashSet hashSet = new HashSet();
        Iterator<String> it = stringSet.iterator();
        while (it.hasNext()) {
            try {
                hashSet.add(TranscodeSetting.TranscodeForceFormat.valueOf(it.next()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return hashSet;
    }

    public static TranscodeSetting getTranscodeSetting() {
        TranscodeSetting transcodeSetting = new TranscodeSetting();
        transcodeSetting.setFormat(getTranscodePref());
        transcodeSetting.setQuality(getTranscodeQuality());
        transcodeSetting.setForceFormats(getTranscodeForFormat());
        return transcodeSetting;
    }

    public static void migrateTranscodeForFormat(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            String[] strArrSplit = defaultSharedPreferences.getString(PREFERENCE_FORCE_TRANSCODE, "").split(":");
            HashSet hashSet = new HashSet();
            for (String str : strArrSplit) {
                if (!TextUtils.isEmpty(str)) {
                    hashSet.add(str);
                }
            }
            defaultSharedPreferences.edit().remove(PREFERENCE_FORCE_TRANSCODE).apply();
            defaultSharedPreferences.edit().putStringSet(PREFERENCE_FORCE_TRANSCODE, hashSet).apply();
        } catch (ClassCastException unused) {
            SynoLog.d(LOG, "correct format. need no migrate");
        }
        try {
            HashSet hashSet2 = new HashSet(defaultSharedPreferences.getStringSet(PREFERENCE_FORCE_TRANSCODE, new HashSet()));
            if (hashSet2.remove("AAC")) {
                defaultSharedPreferences.edit().putStringSet(PREFERENCE_FORCE_TRANSCODE, hashSet2).apply();
            }
        } catch (ClassCastException unused2) {
            SynoLog.d(LOG, "Cannot migrate");
        }
    }

    private static Set<AndroidAutoSetting.DefaultPlaylist> getAndroidAutoDefaultPlaylists() {
        Set<String> stringSet = PreferenceManager.getDefaultSharedPreferences(App.getContext())
                .getStringSet(PREFERENCE_ANDROIDAUTO_DEFAULT_PLAYLISTS, Collections.emptySet());
        HashSet<AndroidAutoSetting.DefaultPlaylist> hashSet = new HashSet<>();
        Iterator<String> it = stringSet.iterator();
        while (it.hasNext()) {
            try {
                hashSet.add(AndroidAutoSetting.DefaultPlaylist.valueOf(it.next()));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return hashSet;
    }


    public static AndroidAutoSetting getAndroidAutoSetting() {
        AndroidAutoSetting androidAutoSetting = new AndroidAutoSetting();
        androidAutoSetting.setDefaultPlaylists(getAndroidAutoDefaultPlaylists());
        return androidAutoSetting;
    }

    public static long getSongCacheLimit() {
        return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(PREFERENCE_SONG_CACHE_LIMIT, "1000")) * PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED;
    }

    public static void addAutoCacheByte(long addByte) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putLong(PREF_SONG_AUTO_CACHE_SIZE, getAutoCacheSize() + addByte).apply();
    }

    public static void addManualCacheByte(long addByte) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putLong(PREF_SONG_MANUAL_CACHE_SIZE, getManualCacheSize() + addByte).apply();
    }


    public static long getAutoCacheSize() {
        return App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getLong(PREF_SONG_AUTO_CACHE_SIZE, 0L);
    }

    public static void subAutoCacheByte(long subByte) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putLong(PREF_SONG_AUTO_CACHE_SIZE, getAutoCacheSize() - subByte).apply();
    }

    public static long getManualCacheSize() {
        return App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).getLong(PREF_SONG_MANUAL_CACHE_SIZE, 0L);
    }

    public static void subManualCacheByte(long subByte) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putLong(PREF_SONG_MANUAL_CACHE_SIZE, getManualCacheSize() - subByte).apply();
    }


    public static void setSongCacheFolder(String path) {
        App.getContext().getSharedPreferences(DSAUDIO_INFO, 0).edit().putString(PREF_KEY_CATCHPATH, path).apply();
    }


    public static String getSongCacheFolder() {
        return App.getContext()
                .getSharedPreferences(DSAUDIO_INFO, 0)
                .getString(
                        PREF_KEY_CATCHPATH,
                        StoragePermissionHelper.INSTANCE.getDefaultFolderPath()
                );
    }


    public static void setUserInputAddress(String address) {
        App.getContext().getSharedPreferences(PREFERENCE_INFO, 0).edit().putString(KEY_USER_ADDR, address).apply();
    }


    public static String getUserInputAddress() {
        return App.getContext().getSharedPreferences(PREFERENCE_INFO, 0).getString(KEY_USER_ADDR, "");
    }

    public static Common.TapSongAction getTapSongPref() {
        return Common.TapSongAction.valueOf(PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(PREFERENCE_TAP_SONG, Common.TapSongAction.REPLACE.name()));
    }


}
