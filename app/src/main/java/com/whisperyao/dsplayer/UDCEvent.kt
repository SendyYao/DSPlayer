package com.whisperyao.dsplayer

import com.whisperyao.dsplayer.homepage.PinManager

object UDCEvent {

    const val EVENT__ANDROID_AUTO_FEATURE = "syno_android_auto_feature"
    const val EVENT__DOWNLOAD_PLAYLIST_COUNT = "syno_downloaded_playlist_count"
    const val EVENT__EQUALIZER = "syno_equalizer"
    const val EVENT__LOGIN_PATH = "syno_login_path"
    const val EVENT__MANUAL_DOWNLOAD_SONG_COUNT = "syno_manual_download_songs"
    const val EVENT__OPERATION_MULTI_SELECT = "syno_operation_multiple_select"
    const val EVENT__OPERATION_SINGLE_SONG = "syno_operation_single_song"
    const val EVENT__OPERATION_SONG_LIST = "syno_operation_song_list"
    const val EVENT__PLAYBACK = "syno_playback_path"
    const val EVENT__PLAYLIST = "syno_playlist"
    const val EVENT__REMOTE_PLAYER = "syno_remote_player"
    const val EVENT__SEARCH = "syno_search"

    const val KEY_ARTIST = "artist"
    const val KEY_CATEGORY = "category"
    const val KEY_COMPOSER = "composer"
    const val KEY_COUNT = "count"
    const val KEY_DEVICE = "device"
    const val KEY_FEATURE = "feature"
    const val KEY_GENRE = "genre"
    const val KEY_HOME = "home"
    const val KEY_LIBRARY = "library"
    const val KEY_MANAGE = "manage"
    const val KEY_OPERATION = "operation"
    const val KEY_PATH = "path"
    const val KEY_PLAYBACK = "playback"
    const val KEY_SEARCH_ACTION = "search_action"
    const val KEY_SETTING = "setting"
    const val KEY_SONG_COUNT = "song_count"

    private const val PREFIX = "syno_"

    enum class ContainerValue(val value: String) {
        MY_PINS("my_pins"),
        GENRE("genre"),
        ALBUM("album"),
        ALBUMS("albums"),
        ARTIST("artist"),
        COMPOSER("composer"),
        FOLDER("folder"),
        SONG(PinManager.SONG),
        ALL_SONGS("all_songs"),
        ALL_ALBUMS("all_albums")
    }

    enum class CategoryValue(val value: String) {
        HOME(KEY_HOME),
        LIBRARY("library"),
        PLAYLISTS("playlists"),
        RADIO("radio"),
        SEARCH("search"),
        DOWNLOADED_SONGS("downloaded_songs")
    }

    enum class LoginPathValue(val value: String) {
        WEBAPI("webapi"),
        CGI("cgi")
    }

    enum class ToggleValue(val value: Boolean) {
        ENABLED(true),
        DISABLED(false)
    }

    fun enumToggleValue(value: Boolean): ToggleValue {
        return if (value) ToggleValue.ENABLED else ToggleValue.DISABLED
    }
}