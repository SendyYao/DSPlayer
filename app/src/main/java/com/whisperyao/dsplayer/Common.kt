package com.whisperyao.dsplayer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import com.synology.sylib.syhttp3.cookieStore.CipherPersistentCookieStore
import com.synology.sylib.syhttp3.relay.utils.RelayUtil
import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair
import com.synology.sylib.util.NetworkUtils
import com.synology.sylib.util.FileUtils
import com.synology.sylibx.synofile.GrantStatus
import com.synology.sylibx.synofile.PermissionUtils
import com.synology.sylibx.synofile.SynoFile
import com.whisperyao.dsplayer.datasource.network.vo.BaseVo
import com.whisperyao.dsplayer.datasource.network.vo.base.BaseAudioInfoVo
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.model.data.DataModelManager
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.ObjFile
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.TranscodeSetting
import com.whisperyao.dsplayer.util.TranscodeType
import com.whisperyao.dsplayer.util.Utilities
import io.reactivex.rxjava3.annotations.SchedulerSupport
import org.apache.commons.lang3.CharEncoding
import java.io.File
import java.net.CookieStore
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import kotlin.Boolean


object Common {

    // ================== 全局状态 ==================

    @JvmStatic
    fun isLogin(): Boolean {
        return true
    }

    @JvmStatic
    fun haveInternetRadio(): Boolean {
        return true
    }

    @JvmStatic
    fun supportComposer(): Boolean {
        return true
    }

    @JvmStatic
    fun isPlayModeStreaming(): Boolean = false

    @JvmField
    var gIsCreatNewPlaylist: Boolean = false
    var mCookieStore: CookieStore? = null
    const val sBaseUrl = ""

    var sAudioInfo: BaseAudioInfoVo? = null
    var sTranscodeType: TranscodeType? = null


    @JvmField
    var gIsClearLocalCache = false
    @JvmField
    var gLibraryChanged = false

    @JvmField
    var gDeviceChanged = false
    @JvmField
    var gModeSwitchMode = false

    var mSkipTokenStr: String = ""


    // ================== 常量 ==================
    const val ACTION_ASK_LOGOUT = "com.synology.dsaudio.ASK_LOGOUT"
    const val ACTION_INNER_LOGIN_FROM_EXPLORE = "com.synology.dsaudio.INNER_LOGIN_FROM_EXPLORE";
    const val FRAGMENT_HASH = "fragment_hash"
    const val ACTION_LOCAL_SONG_DELETED = "com.synology.dsaudio.LOCAL_SONG_DELETED"
    const val ACTION_PLAYLIST_CHANGED = "com.synology.dsaudio.PLAYLIST_CHANGED"
    const val CONTAINER_TYPE = "ContainerType"
    const val CLIENT_AGENT = "client_agent"
    const val CLIENT_MODE = "client_mode"
    const val DEFAULT_WEBAPI_PATH = "/webapi"
    const val DEFAULT_PATH = "/audio"
    const val ENUMERATE_CGI = "iPhone/enumerate.cgi"
    const val PORXY_CGI: String = "iPhone/proxy.cgi"
    const val CAT_RANDOM100_ID = "[__RANDOM100__]"
    const val CAT_RATING = "[__RATING__]"
    const val CAT_RECENTLY_ADDED = "[__RECENTLY_ADDED__]"
    const val NUMBER = "[__NUMBER__]"
    const val USB_CONTROLLER_CGI = "iPhone/usb_controller.cgi"
    const val STREAM_CGI = "iPhone/stream.cgi"
    const val PROXY_CGI = "iPhone/proxy.cgi"
    const val TRANSCODER_CGI = "iPhone/transcoder.cgi"
    const val SONG_LIMITATION = "[__SONGLIMITATION__]"
    const val SZ_SHOUTCAST_ID = "inetradio_sc"
    const val SZ_RADIO_ID = "inetradio_rio"
    const val SZ_RADIO_USER_DEFINED_ID = "inetradio_userdefined"
    const val SZ_RADIO_FAVORITE_ID = "inetradio_favorite"
    const val SZ_STRING_SEPARATOR = "_SYNOSPTR_"
    const val SZ_DATABASE_SEPARATOR = ", "
    const val NOTIFY_SONGCACHE_COMPLETED = "com.synology.dsaudio.SONGCACHE_COMPLETED"
    const val SONGCACHE_PATH = "songcache_path"
    const val POLLING_STATUS_CGI = "syno_audio_ajax_handler.cgi"
    const val SHOUTCAST_URL = "http://www.shoutcast.com"

    private var folderCallCount = 0
    private var lastCallTime = 0L

    // ================== Context（关键修复）==================
    lateinit var context: Context

    @JvmStatic
    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    // ================== 工具方法 ==================

    private var appFolderCache: String? = null

    @JvmStatic
    fun getDSaudioAppFolder(): String {
        return appFolderCache ?: run {
            val currentTime = System.currentTimeMillis()
            folderCallCount++
            // 每秒打印一次调用统计，避免日志刷屏
            if (currentTime - lastCallTime > 1000) {
                println("getDSaudioAppFolder called $folderCallCount times in last second")
                Thread.currentThread().stackTrace.forEach {
                    println("  at $it")
                }
            // 如果调用次数异常，打印调用栈
            if (folderCallCount > 10) {
                println("WARNING: Excessive calls detected!")
            }

            folderCallCount = 0
            lastCallTime = currentTime
        }
            val folderPath = App.getContext().filesDir.absolutePath + "/"
            println("AppFolder: $folderPath")
            checkFolderExistLegacy(folderPath)
            appFolderCache = folderPath
            folderPath
        }
    }

    fun checkFolderExistLegacy(path: String): Boolean {
        val file = File(path)
        if (file.exists()) {
            return FileUtils.isWritableNormalOrSaf(App.getContext(), file) && file.canRead()
        }
        checkFolderExistLegacy(file.parent);
        if (!file.mkdir()) {
            SynoLog.e("checkFolderExist", "fail to create Directory : " + file.path);
            return false;
        }
        SynoLog.d("checkFolderExist", "create Directory : " + file.path);
        return true;
    }

    @JvmStatic
    fun getBaseUrl(): String {
        return if (TextUtils.isEmpty(sBaseUrl)) DEFAULT_PATH else sBaseUrl
    }

    fun composeUrl(apiUrl: String, apiName: String?, version: Int, method: String, param: ArrayList<BasicKeyValuePair>?): String {
        val sbAppend: StringBuilder = StringBuilder(256).append(apiUrl).append("?")
        if (apiName != null) {
            sbAppend.append("api=").append(apiName).append("&version=").append(version).append("&method=").append(method)
        } else {
            sbAppend.append("action=").append(method)
        }
        if (param != null) {
            val it: Iterator<BasicKeyValuePair> = param.iterator()
            while (it.hasNext()) {
                val next: BasicKeyValuePair = it.next()
                sbAppend.append("&").append(next.first).append("=").append(encode(next.second))
            }
        }
        return sbAppend.toString()
    }
    private fun encode(s: String?): String? {
        return try {
            URLEncoder.encode(s, CharEncoding.UTF_8).replace("+", "%20")
        } catch (_: Exception) {
            s
        }
    }

    fun getAudioInfo(): BaseAudioInfoVo? {
        if (sAudioInfo == null) {
            sAudioInfo = ObjFile.getAudioInfoFromFile()
        }
        return sAudioInfo
    }

    fun setAudioInfo(info: BaseAudioInfoVo) {
        sAudioInfo = info
        ObjFile.saveAudioInfoToFile(info)
    }

    @JvmStatic
    fun getDsId(): String? {
        val audioInfo: BaseAudioInfoVo? = getAudioInfo()
        if (audioInfo != null && audioInfo.getDSid() != null) {
            return audioInfo.getDSid()
        }
        return AudioPreference.getUserInputAddress()
    }

    @JvmStatic
    fun createPersonalPlaylist(): Boolean {
        val audioInfo: BaseAudioInfoVo = getAudioInfo() ?: return false
        return !audioInfo.serverType.equals(ConnectionManager.ResourceType.CGI) || 1528 <= audioInfo.buildVer
    }

    @JvmStatic
    fun createSharedPlaylist(): Boolean {
        val audioInfo: BaseAudioInfoVo = getAudioInfo() ?: return false
        return audioInfo.permitPlaylist()
    }

    @JvmStatic
    fun editPersonalPlaylist(): Boolean {
        val audioInfo: BaseAudioInfoVo? = getAudioInfo()
        return audioInfo != null && audioInfo.serverType.equals(ConnectionManager.ResourceType.API)
    }

    @JvmStatic
    fun editSharedPlaylist(): Boolean {
        val audioInfo: BaseAudioInfoVo? = getAudioInfo()
        if (audioInfo != null && audioInfo.serverType.equals(ConnectionManager.ResourceType.API)) {
            return audioInfo.permitPlaylist()
        }
        return false
    }

    @JvmStatic
    fun getIp() = BuildConfig.NAS_ADDRESS

    @JvmStatic
    fun getPort() = BuildConfig.NAS_PORT

    @JvmStatic
    fun getPlayerStatusManager(): PlayingStatusManager {
        return DataModelManager.instance.playingStatusManager
    }

    @JvmStatic
    fun getPlayerIndex(): Int {
        return getPlayerStatusManager().playerIndex
    }

    @JvmStatic
    fun getPlayerUniqueId(): String {
        return getPlayerStatusManager().playerUniqueId
    }

    @JvmStatic
    fun isRemotePlayer(): Boolean {
        return getPlayerStatusManager().isRemotePlayer
    }

    @JvmStatic
    fun isPlayModeRenderer(): Boolean {
//        return getPlayerStatusManager().isPlayModeRenderer
        return true
    }

    @JvmStatic
    fun makeAddress(baseUrl: String, path: String): String {
        return makeAddress(baseUrl, path, getIp(), getPort())
    }

    @JvmStatic
    fun makeAddress(baseUrl: String, path: String, ip: String, port: Int): String {
        return (if (AudioPreference.getHttpsPref()) "https://" else "http://") +
                "$ip:$port$baseUrl/$path"
    }
    @JvmStatic
    fun setCookieStore(cookieStore: CipherPersistentCookieStore) {
        mCookieStore = cookieStore
    }

    @JvmStatic
    fun getCookieStore(): CookieStore? {
        return mCookieStore
    }

    @JvmStatic
    fun getTranscodeType(): TranscodeType {
        return sTranscodeType ?: TranscodeType().apply {
            getAudioInfo()?.let { audioInfo ->
                parse(audioInfo.transcode)
            }
            sTranscodeType = this
        }
    }

    @JvmStatic
    fun getPreferredDownloadQuality(song: SongItem, transcodeSetting: TranscodeSetting, isForChromecast: Boolean): TranscodeSetting.TranscodeDownloadQuality {

        if (Utilities.isAAC(song) && !Utilities.isALAC(song)) {
            return TranscodeSetting.TranscodeDownloadQuality.ORIGINAL
        }

        var quality = TranscodeSetting.TranscodeDownloadQuality.LOW

        when {
            transcodeSetting.isQualityHigh() -> {
                quality = TranscodeSetting.TranscodeDownloadQuality.HIGH
            }

            transcodeSetting.isQualityMedium() -> {
                quality = TranscodeSetting.TranscodeDownloadQuality.MEDIUM
            }

            transcodeSetting.isQualityLow() -> {
                quality = TranscodeSetting.TranscodeDownloadQuality.LOW
            }

            transcodeSetting.isQualityAuto() -> {
                var ip = getIp()
                var isLanConnected = false

                if (RelayUtil.isQuickConnectId(ip)) {
                    try {
                        ip = RelayUtil
                            .getRealURL(URL("http", ip, ""), false)
                            .host
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                    }
                }

                try {
                    isLanConnected = NetworkUtils.isLANConnected(App.getContext())
                } catch (_: Exception) {
                }

                quality = when {
                    NetworkUtils.isLANAddress(ip) ->
                        TranscodeSetting.TranscodeDownloadQuality.HIGH

                    isLanConnected ->
                        TranscodeSetting.TranscodeDownloadQuality.MEDIUM

                    else ->
                        TranscodeSetting.TranscodeDownloadQuality.LOW
                }
            }
        }

        val isStreamAudio = Utilities.isStreamAudio(song, isForChromecast)
        val containsForceFormat = transcodeSetting.containsForceFormat(
            Utilities.toStreamAudio(song.filePath)
        )

        if (!isStreamAudio || containsForceFormat || song.isVirtualSong) {
            return quality
        }

        var finalQuality =
            if (transcodeSetting.isQualityAuto() &&
                !Utilities.isALAC(song) &&
                quality.isHigh()
            ) {
                TranscodeSetting.TranscodeDownloadQuality.ORIGINAL
            } else {
                quality
            }

        if (transcodeSetting.isQualityHigh() &&
            !Utilities.isALAC(song)
        ) {
            finalQuality = TranscodeSetting.TranscodeDownloadQuality.ORIGINAL
        }

        return if (song.bitrate < quality.getBitrate().toLong()) {
            TranscodeSetting.TranscodeDownloadQuality.ORIGINAL
        } else {
            finalQuality
        }
    }

    @JvmStatic
    fun getSongCacheFolder(): String? {
        val songCacheFolder: String = AudioPreference.getSongCacheFolder()
        if (folderAvailable(songCacheFolder)) {
            return songCacheFolder
        }
        return null
    }

    @JvmStatic
    fun getCoverFolder(): String {
        return File(App.getContext().externalCacheDir, "cover").path
    }

    @JvmStatic
    fun getLyricFolder(): String {
        return File(App.getContext().externalCacheDir, "lyric").path
    }

    fun folderAvailable(path: String): Boolean {
        val synoFile = SynoFile(path)
        if (synoFile.exists()) {
             return PermissionUtils.checkGrantStatus(synoFile) === GrantStatus.Granted
        }
        if (PermissionUtils.checkGrantStatus(synoFile.getParentSynoFile()) !== GrantStatus.Granted) {
            return false
        }
        if (!synoFile.mkdir()) {
            SynoLog.e("checkFolderExist", "fail to create Directory : " + synoFile.path)
            return false
        }
        SynoLog.d("checkFolderExist", "create Directory : " + synoFile.path)
        return true
    }

    fun notifySongCacheCompleted(context: Context, path: String?) {
        val intent: Intent = Intent(NOTIFY_SONGCACHE_COMPLETED)
        intent.putExtra(SONGCACHE_PATH, path)
        context.sendBroadcast(intent)
    }

    @JvmStatic
    fun getSID(): String {
        // AudioPreference.getSID()
        return BuildConfig.NAS_SID
    }

    fun getSkipTokenStr(): String {
        if (mSkipTokenStr == "") {
            val strArr = arrayOf<String?>(
                "og", "en", "et", "de", "en", "een", "eene", "de", "and",
                "a", "an", "the", "et", "un", "une", "des", "le", "la",
                "l'", "les", "und", "ein", "une", "des", "der", "das",
                "e", "é", "un", "uno", "una", "il", "la", "lo", "l'",
                "gli", "le", "og", "en", "ei", "et", "eit", "de", "dei",
                "e", "um", "uma", "uns", "umas", "o", "a", "os", "as",
                "y", "un", "unos", "una", "unas", "el", "la", "los",
                "las", "och", "en", "ett", "de"
            )
            val sb = java.lang.StringBuilder()
            for (i in 0..65) {
                sb.append("^(?i)" + strArr[i] + " |")
            }
            mSkipTokenStr = sb.toString()
        }
        return mSkipTokenStr
    }

    @JvmStatic
    fun sortTitle(title: String): String {
        return title.replace(getSkipTokenStr().toRegex(), "")
    }

    @JvmStatic
    fun getRadioTitleByID(radioID: String): String? {
        val resources = App.getContext().resources

        return when (radioID) {
            SZ_SHOUTCAST_ID -> resources.getString(R.string.str_shoutcast)
            SZ_RADIO_ID -> resources.getString(R.string.str_radioio)
            SZ_RADIO_USER_DEFINED_ID -> resources.getString(R.string.str_user_defined)
            SZ_RADIO_FAVORITE_ID -> resources.getString(R.string.str_my_favorate)
            else -> null
        }
    }

    @JvmStatic
    fun haveRenderer(): Boolean {
        val audioInfo: BaseAudioInfoVo? = getAudioInfo()
        if (audioInfo != null) {
            return false
        }
        return !audioInfo?.getServerType()?.equals(ConnectionManager.ResourceType.CGI)!! || 1707 <= audioInfo?.getBuildVer() ?: 1
    }

    @JvmStatic
    fun haveRemotePlayer(): Boolean {
        return getAudioInfo()?.haveRemotePlayer() ?: false
    }

    @JvmStatic
    fun getDeviceName(): String {
        return Build.MODEL
    }


    // ================== 枚举 ==================

    enum class PrefPersonal {
        ALL,
        SHARED,
        PERSONAL
    }

    enum class TapSongAction {
        ADD,
        REPLACE
    }

    enum class ContainerType(
        val stringId: Int,
        val isAlbum: Boolean = false,
        val isPlaylist: Boolean = false
    ) {

        ALBUM_MODE(R.string.category_album, isAlbum = true),
        FOLDER_MODE(R.string.category_folder),
        ARTIST_MODE(R.string.category_artist),
        COMPOSER_MODE(R.string.category_composer),
        GENRE_MODE(R.string.category_genre),
        PLAYLIST_MODE(R.string.category_playlist),
        SHARED_PLAYLIST_MODE(R.string.category_playlist, isPlaylist = true),
        PERSONAL_PLAYLIST_MODE(R.string.category_playlist, isPlaylist = true),
        SHARED_SMART_PLAYLIST_MODE(R.string.category_playlist),
        PERSONAL_SMART_PLAYLIST_MODE(R.string.category_playlist),
        RADIO_MODE(R.string.category_radio),
        ARTIST_ALBUM_MODE(R.string.category_album, isAlbum = true),
        GENRE_ALBUM_MODE(R.string.category_album),
        GENRE_ARTIST_MODE(R.string.category_artist),
        GENRE_ARTIST_ALBUM_MODE(R.string.category_album),
        COMPOSER_ALBUM_MODE(R.string.category_album),
        SMARTPLAYLIST_MODE(R.string.category_smartplaylist),
        SEARCH_MODE(R.string.category_search),
        RANDOM100_MODE(R.string.app_name),
        RATING_MODE(R.string.category_search),
        HOMEPAGE_PIN_MODE(R.string.category_homepage_pin),
        HOMEPAGE_DEFAULT_GENRE_MODE(R.string.category_homepage_default_genre),
        SEARCH_ARTIST_MODE(R.string.category_artist),
        SEARCH_ALBUM_MODE(R.string.category_album),
        SEARCH_SONG_MODE(R.string.category_song),
        LATEST_ALBUM_MODE(R.string.latest_album, isAlbum = true);

        fun isAlbumType() = isAlbum
        fun isPlaylistType() = isPlaylist

        fun isNormalPlaylistType(): Boolean {
            return equals(PERSONAL_PLAYLIST_MODE) || equals(SHARED_PLAYLIST_MODE)
        }

        fun isSharedPlaylistType(): Boolean {
            return equals(SHARED_PLAYLIST_MODE) || equals(SHARED_SMART_PLAYLIST_MODE)
        }

        fun isPersonalPlaylistType(): Boolean {
            return equals(PERSONAL_PLAYLIST_MODE) || equals(PERSONAL_SMART_PLAYLIST_MODE)
        }

        fun isShowRatingIcon(): Boolean {
            return equals(Item.ItemType.RATING_MODE)
        }
    }


    enum class ItemAction(
        val id: Int,
        private val strId: Int
    ) {
        PLAY(0, R.string.play),
        ADD_ITEM(1, R.string.add_item),
        ADD_PLAY(2, R.string.add_and_play_it),
        ADD_NEXT(14, R.string.add_to_next),
        BY_SITUATION(3, 0),
        DELETE(4, R.string.delete),
        DOWNLOAD(5, R.string.download),
        RENAME(6, R.string.rename),
        ADDTO_PLAYLIST(7, R.string.add_to_playlist),
        RATING(8, R.string.rating_action),
        EDIT(9, R.string.edit),
        SHARING(10, R.string.sharing_share),
        PIN(11, R.string.pin),
        UNPIN(12, R.string.unpin),
        PIN_EDIT(13, R.string.pin_edit);

        fun getString(context: Context): String {
            return context.getString(strId)
        }

        companion object {
            fun fromId(id: Int) = values().find { it.id == id }
        }
    }

    enum class ConnectionInfo(private val strId: Int) {

        SUCCESS(R.string.ok),
        FAILED_CONNECTION(R.string.connection_failed),
        ERROR_NETWORK(R.string.network_not_available),
        ERROR_ACCOUNT(R.string.login_error_account),
        NO_PRIVILEGE(R.string.error_noprivilege),
        NORUNNING_AUDIOSTATION(R.string.service_disabled),
        DS_IS_UNAVAILABLE(R.string.error_ds_is_unavailable),
        ERROR_FIRMWARE(R.string.error_firmware_version),
        NO_USB_SPEAKER(R.string.no_usb_speaker),
        AUDIO_NOT_FOUND(R.string.error_audio_package_not_found),
        ERROR_SSL(R.string.error_ssl),
        ERROR_CERT_FINGERPRINT(R.string.error_certificate_is_replaced),
        ERROR_PORTAL_PORT_INVALID(R.string.error_auth_port_invalid),
        ERR_OTP_REQUIRE(R.string.enter_otp_code),
        ERR_OTP_INVALID(R.string.error_otp_incorrect),
        ERR_OTP_ENFORCED(R.string.error_otp_enforced),
        ERR_AUTO_BLOCK_MAX_TRIES(R.string.error_max_tries),
        ERR_PWD_EXPIRED_CANT_CHANGE(R.string.error_pwd_expired),
        ERR_PWD_EXPIRED(R.string.error_pwd_expired),
        ERR_PWD_MUST_CHANGE(R.string.error_pwd_must_change),
        ERR_ACC_LOCKED(R.string.error_account_locked),
        ERR_RELAY_DISABLED(R.string.error_tunnel_disabled);

        var exception: Exception? = null
        var resultVo: BaseVo? = null
        var strResult: String? = null

        fun getStringId(): Int = strId

        fun getString(): String {
            return App.getContext()
                .getString(strId)
                .replace("[__VERSION__]", "3.0-1334")
        }
    }

    enum class PlaybackAction(val id: Int) {
        PLAY_NOW(0),
        ADD_ONLY(1),
        ADD_PLAY(2),
        BY_SITUACTION(3),
        ADD_NEXT(4);

        companion object {
            fun fromId(id: Int): PlaybackAction? {
                for (playbackAction in PlaybackAction.entries) {
                    if (playbackAction.id == id) {
                        return playbackAction
                    }
                }
                return null
            }
        }
    }

    enum class SearchCategory(val id: Int, val label: Int) {
        ALL(0, R.string.category_all),
        TITLE(1, R.string.category_title),
        ALBUM(2, R.string.category_album),
        ARTIST(3, R.string.category_artist),
        GENRE(4, R.string.category_genre),
        COMPOSER(5, R.string.category_composer);

        companion object {
            fun fromId(id: Int) = values().find { it.id == id }
        }
    }


    enum class PrefViewMode {
        LIST,
        THUMBNAIL;

        fun toggle(): PrefViewMode =
            if (this == LIST) THUMBNAIL else LIST
    }

    enum class RepeatMode(val id: Int) {
        NONE(0),
        ONE(1),
        ALL(2);

        fun toNext(): RepeatMode {
            val repeatMode = ALL
            if (repeatMode == this) {
                return ONE
            }
            return if (ONE == this) NONE else repeatMode
        }

        val castString: String
            get() {
                if (equals(ALL)) {
                    return "all"
                }
                if (equals(ONE)) {
                    return "one"
                }
                return SchedulerSupport.NONE
            }

        companion object {
            fun fromId(id: Int): RepeatMode {
                for (repeatMode in RepeatMode.entries) {
                    if (repeatMode.id == id) {
                        return repeatMode
                    }
                }
                return NONE
            }

            fun fromCastString(cast: String?): RepeatMode {
                if ("all".equals(cast, ignoreCase = true)) {
                    return ALL
                }
                if ("one".equals(cast, ignoreCase = true)) {
                    return ONE
                }
                return NONE
            }
        }
    }

    enum class ShuffleMode(private val id: Int) {
        NONE(0),
        AUTO(1);

        val isEnabled: Boolean
            get() = equals(AUTO)

        fun toggle(): ShuffleMode {
            val shuffleMode = AUTO
            return if (equals(shuffleMode)) NONE else shuffleMode
        }


        companion object {
            fun fromId(id: Int): ShuffleMode {
                for (shuffleMode in ShuffleMode.entries) {
                    if (shuffleMode.id == id) {
                        return shuffleMode
                    }
                }
                return NONE
            }
        }
    }

}