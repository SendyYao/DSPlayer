package com.whisperyao.dsplayer.mediasession.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.lifecycle.lifecycleScope
import com.whisperyao.dsplayer.AndroidAuto.VoiceSearchParams
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.LocalEnumerator
import com.whisperyao.dsplayer.PlaylistAdapter
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.datasource.network.LoginInfoManager
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.item.PlaylistItem
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.playing.NowPlayingManager
import com.whisperyao.dsplayer.provider.AudioDatabaseUtils
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.extension.SongExtensionsKt.toMediaItem
import io.reactivex.rxjava3.disposables.Disposable
import jakarta.inject.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList
import javax.inject.Inject


abstract class AndroidAutoService: AbstractMediaBrowserService() {
    companion object {
        private const val API_FavoriteID = "Favorite"
        private const val CGI_FavoriteID = "inetradio_favorite"
        private const val LOG = "AndroidAutoService"
        private const val PLAYLIST_LIMIT = 20

        const val CAT_DOWNLOADED_PLAYLIST = "[__DOWNLOADED_PLAYLIST__]"
        const val CAT_FAVORITE_RADIO = "[__FAVORITE_RADIO__]"
        const val CAT_PERSONAL_PLAYLIST = "[__PERSONAL_PLAYLIST__]"
        const val CAT_RATING_4 = "[__RATING_4__]"
        const val CAT_RATING_5 = "[__RATING_5__]"
        const val CAT_SHARED_PLAYLIST = "[__SHARED_PLAYLIST__]"

        const val SONG_RATING_LEVEL = "song_rating_level"

        private var playbackWifiLock: WifiManager.WifiLock? = null
    }

    @Inject
    lateinit var audioDatabaseUtils: AudioDatabaseUtils

    @Inject
    lateinit var loginInfoManager: LoginInfoManager

    @Inject
    lateinit var nowPlayingManagerProvider: Provider<NowPlayingManager>

    protected val nowPlayingManager: NowPlayingManager by lazy {
        nowPlayingManagerProvider.get()
    }

    private var autoCheckedLogin = false
    private var queueValid = false
    private var reloadSuccessful = false

    protected var udcCurrentSupportMediaId: String? = null

    private var loginForCar: Disposable? = null

    private var enumPlaylists: Job? = null
    private var enumRootPlaylists: Job? = null
    private var enumSongs: Job? = null
    private var enumSongsAndPlay: Job? = null
    private var updateSessionQueueJob: Job? = null

    private val mGeneralPlayList = LinkedList<PlaylistAdapter.UiPlaylistItem>()

    private val mPersonalPlayList = LinkedList<PlaylistAdapter.UiPlaylistItem>()

    private val mSharedPlayList = LinkedList<PlaylistAdapter.UiPlaylistItem>()

    private var mLocalPlayList = LinkedList<PlaylistAdapter.UiPlaylistItem>()

    private val mPlaylistsBundleList = arrayListOf<Bundle>()

    protected val mBrowsedSongList = arrayListOf<SongItem>()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    protected abstract fun enqueueImpl(action: String)

    protected abstract fun doReloadAll(isFromCar: Boolean)

    protected fun saveQueue(list: List<SongItem>) {
        if (queueValid) {
            lifecycleScope.launch(Dispatchers.IO) {
                nowPlayingManager.saveQueue(list)
            }
        }
    }

    fun enqueue(action: String) {
        require(action.isNotBlank())

        serviceScope.launch(Dispatchers.IO) {
            enqueueImpl(action)
        }
    }

    protected fun getReloadSuccessful(): Boolean {
        return this.reloadSuccessful
    }

    protected fun setReloadSuccessful(z: Boolean) {
        this.reloadSuccessful = z
    }

    private fun reloadAll(isFromCar: Boolean) {
        if (playingStatusManager.isPlayModeStreaming) {
            val queue: List<MediaSessionCompat.QueueItem>? = mSession.controller.queue ?: null
            if (queue == null || queue.isEmpty()) {
                // nowPlayingManager = nowPlayingManagerProvider.get()
                doReloadAll(isFromCar)
                this.reloadSuccessful = true
            }
        }
    }

    protected fun generatePreparingItem(): MediaMetadataCompat {
        val mediaMetadataCompatBuild: MediaMetadataCompat = MediaMetadataCompat.Builder()
            .putString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                PlayingQueueManager.preparingMediaId
                )
            .putString(
                "android.media.metadata.title",
                getString(R.string.prepare_for_songs)
            )
            .build()
        return mediaMetadataCompatBuild
    }

    private fun generateBrowsableItem(mediaId: String, title: String, icon: Bitmap): MediaBrowserCompat.MediaItem {
        return MediaBrowserCompat.MediaItem(MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setIconBitmap(icon)
            .build(),
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        )
    }

    protected fun enumSongsAndPlay(type: String) {

        mBrowsedSongList.clear()

        loginForCar?.dispose()
        enumSongsAndPlay?.cancel()

        setMetadata(generatePreparingItem())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(8)
        }

        enumSongsAndPlay = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bundle = Bundle()

                val itemSet = when (type) {
                    "[__MOST_FREQUENT_LISTEN__]" -> {
                        val playlist = PlaylistItem.generatePredifinedPlaylist(
                            Item.ItemType.SHARED_SMART_NEW,
                            "[__MOST_FREQUENT_LISTEN__]",
                            "[__MOST_FREQUENT_LISTEN__]"
                        )

                        CacheManager.getInstance()
                            .doEnumPlaylistSongsForPlaylist(
                                false,
                                playlist,
                                1,
                                true
                            )
                    }

                    "[__MOST_RECENT_ADDED__]" -> {
                        val playlist = PlaylistItem.generatePredifinedPlaylist(
                            Item.ItemType.SHARED_SMART_NEW,
                            "[__MOST_RECENT_ADDED__]",
                            "[__MOST_RECENT_ADDED__]"
                        )

                        CacheManager.getInstance()
                            .doEnumPlaylistSongsForPlaylist(
                                false,
                                playlist,
                                1,
                                true
                            )
                    }

                    "[__RANDOM100__]" -> {
                        CacheManager.getInstance()
                            .doEnumContainerSongsForContainer(
                                false,
                                Common.ContainerType.RANDOM100_MODE,
                                bundle,
                                1,
                                true
                            )
                    }

                    else -> null
                }

                itemSet?.let {
                    mBrowsedSongList.addAll(it.itemList)
                }

                setQueue(mBrowsedSongList)
                updateSessionQueue()
                enqueue("com.synology.dsaudio.mediabrowserservice.action.play_now")

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessageMetadata(
                        if (playingQueueManager.getQueueSize() == 0)
                            R.string.use_no_caches
                        else
                            R.string.error
                    )
                }
            }
        }
    }

    private fun showMessageMetadata(message: String) {
        setMetadata(MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, PlayingQueueManager.messageMediaId)
            .putString("android.media.metadata.TITLE", message)
            .build()
        )
    }

    private fun getDefaultPlaylists(): ArrayList<MediaBrowserCompat.MediaItem> =
        arrayListOf<MediaBrowserCompat.MediaItem>().apply {

            val setting = AudioPreference.getAndroidAutoSetting()

            fun addPlaylist(enabled: Boolean, id: String, titleRes: Int, imageRes: Int) {
                if (enabled) {
                    add(generateBrowsableItem(
                            id,
                            getString(titleRes),
                            BitmapFactory.decodeResource(
                                resources,
                                imageRes
                            )
                        )
                    )
                }
            }

            addPlaylist(
                Common.isLogin() && setting.containsDefaultPlaylistFavoriteRadio(),
                CAT_FAVORITE_RADIO,
                R.string.str_my_favorate_radio,
                R.drawable.thumbnail_fav
            )

            addPlaylist(
                Common.isLogin() && setting.containsDefaultPlaylistRandom100(),
                Common.CAT_RANDOM100_ID,
                R.string.random_100,
                R.drawable.thumbnail_100
            )

            addPlaylist(
                setting.containsDefaultPlaylistMostOften(),
                LocalEnumerator.MOST_OFTEN_PLAYED,
                R.string.most_often_played,
                R.drawable.thumbnail_often
            )

            addPlaylist(
                setting.containsDefaultPlaylistMostRecent(),
                LocalEnumerator.MOST_RECENT_PLAYED,
                R.string.most_recent_played,
                R.drawable.thumbnail_recently
            )

            addPlaylist(
                setting.containsDefaultPlaylistRating4(),
                CAT_RATING_4,
                R.string.rating_4_star,
                R.drawable.thumbnail_rating4
            )

            addPlaylist(
                setting.containsDefaultPlaylistRating5(),
                CAT_RATING_5,
                R.string.rating_5_star,
                R.drawable.thumbnail_rating5
            )
        }

    protected fun searchSongsAndPlay(voiceSearchParams: VoiceSearchParams) {
        SynoLog.e(LOG, "$LOG searchSongsAndPlay. voiceSearchParams: $voiceSearchParams")
        setMetadata(generatePreparingItem())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setNewState(8)
        }
        enumSongsAndPlay = lifecycleScope.launch(Dispatchers.IO) {
            try {
                mBrowsedSongList.clear()

                val searchCategory = when {
                    voiceSearchParams.isAlbumFocus -> Common.SearchCategory.ALBUM to voiceSearchParams.album
                    voiceSearchParams.isGenreFocus -> Common.SearchCategory.GENRE to voiceSearchParams.genre
                    voiceSearchParams.isArtistFocus -> Common.SearchCategory.ARTIST to voiceSearchParams.artist
                    voiceSearchParams.isSongFocus -> Common.SearchCategory.TITLE to voiceSearchParams.song
                    else -> null
                }

                val resultList = mutableListOf<SongItem>()

                searchCategory?.let { (category, keyword) ->
                    resultList.addAll(ConnectionManager.doSearch(category, keyword))
                }

                if (voiceSearchParams.isUnstructured || resultList.isEmpty()) {
                    resultList.addAll(
                        ConnectionManager.doSearch(
                            Common.SearchCategory.ALL,
                            voiceSearchParams.query
                        )
                    )
                }

                mBrowsedSongList.addAll(resultList)

                setQueue(mBrowsedSongList)
                updateSessionQueue()
                enqueue(CUSTOM_ACTION_PLAY_NOW)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // handleSearchError()
                }
            }
        }
    }

    private fun showMessageMetadata(messageId: Int) {
        setMetadata(MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, PlayingQueueManager.messageMediaId)
            .putString("android.media.metadata.TITLE", getString(messageId))
            .build()
        )
    }

    private fun stopEnumChildTasks() {
        enumRootPlaylists?.cancel()
        enumPlaylists?.cancel()
        enumSongs?.cancel()
    }

    private fun enumPlaylists(
        result: Result<List<MediaBrowserCompat.MediaItem>>,
        parentMediaId: String
    ) {
        enumPlaylists?.cancel()

        result.detach()

        enumPlaylists = lifecycleScope.launch(Dispatchers.IO) {
            val source = when (parentMediaId) {
                CAT_SHARED_PLAYLIST -> mSharedPlayList
                CAT_PERSONAL_PLAYLIST -> mPersonalPlayList
                CAT_DOWNLOADED_PLAYLIST -> mLocalPlayList
                else -> emptyList()
            }

            val mediaItems = arrayListOf<MediaBrowserCompat.MediaItem>()
            addPlaylists(mediaItems, source)

            withContext(Dispatchers.Main) {
                result.sendResult(mediaItems)
            }
        }
    }

    override fun onLoadChildren(parentMediaId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {

        when (parentMediaId) {
            "[__FAVORITE_RADIO__]" -> {
                udcCurrentSupportMediaId = "favorite_radio"
            }

            "[__RATING_5__]" -> {
                udcCurrentSupportMediaId = "5stars"
            }

            "[__RATING_4__]" -> {
                udcCurrentSupportMediaId = "4stars"
            }

            "[__MOST_FREQUENT_LISTEN__]" -> {
                udcCurrentSupportMediaId = "most_often_played"
            }

            "[__RANDOM100__]" -> {
                udcCurrentSupportMediaId = "random100"
            }

            "[__MOST_RECENT_ADDED__]" -> {
                udcCurrentSupportMediaId = "recently_played"
            }

            else -> {
                when {
                    parentMediaId.contains("playlist_shared") -> {
                        udcCurrentSupportMediaId = "group_playlist"
                    }

                    parentMediaId.contains("playlist_personal") -> {
                        udcCurrentSupportMediaId = "personal_playlist"
                    }

                    parentMediaId != "now_playing" -> {
                        udcCurrentSupportMediaId = null
                    }
                }
            }
        }

        stopEnumChildTasks()

        when (parentMediaId) {

            "not_allowed_root" -> {
                result.sendResult(emptyList())
            }

            "root" -> {
                result.sendResult(emptyList())
                reloadAll(true)

                notifyUpdateUIState()
            }

            "car_root" -> {
                // mViewModel
                val linked = App.connectionManager.isLinked()
                // autoCheckedLogin
                if (linked) {
                    result.detach()
                    enumRootPlaylists(result)
                } else {
                    doBackgroundLogin(result)
                }

                notifyUpdateUIState()
            }

            "now_playing" -> {
                result.sendResult(emptyList())
                notifyUpdateUIState()
            }

            else -> {
                if (isSecondCategoryId(parentMediaId)) {
                    enumPlaylists(result, parentMediaId)
                } else {
                    enumSongs(result, parentMediaId)
                }
            }
        }
    }

    private fun enumRootPlaylists(result: Result<List<MediaBrowserCompat.MediaItem>>) {
        loginForCar?.dispose()

        enumRootPlaylists?.cancel()

        mGeneralPlayList.clear()
        mPersonalPlayList.clear()
        mSharedPlayList.clear()
        mLocalPlayList.clear()

        enumRootPlaylists = lifecycleScope.launch(Dispatchers.IO) {
            loadRootPlaylists(result)
        }
    }

    @Deprecated("Unused legacy Android Auto background login flow")
    private fun doBackgroundLogin(result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()

        // mViewModel.cancelLogin()

        loginForCar?.dispose()
        loginForCar = null

        // loginForCar = mViewModel.isBusyObservable().subscribe { status ->
//            when (status.stat) {
//                    RequestStatus.STATUS_SUCCESS -> {
//                        autoCheckedLogin = true
//                        enumRootPlaylists(result)
//                    }
//
//                    RequestStatus.STATUS_FAILED -> {
//                        status.throwable?.printStackTrace()
//                        enumRootPlaylists(result)
//                    }
//                }
//            }

//        mViewModel.login(
//            loginInfoManager.userInputAddress,
//            loginInfoManager.account,
//            loginInfoManager.password,
//            loginInfoManager.isHttps(),
//            "",
//            false
//        )
    }

    private fun addPlaylists(
        target: MutableList<MediaBrowserCompat.MediaItem>,
        playlists: List<PlaylistAdapter.UiPlaylistItem>?
    ) {
        mPlaylistsBundleList.clear()

        playlists
            ?.take(20)
            ?.forEach { item ->

                val bundle = item.dataItem.bundle

                target.add(
                    generateBrowsableItem(
                        bundle.toString(),
                        item.title,
                        BitmapFactory.decodeResource(
                            resources,
                            item.dataItem.getIconResId()
                        )
                    )
                )

                mPlaylistsBundleList.add(bundle)
            }
    }

    private fun enumSongs(result: Result<List<MediaBrowserCompat.MediaItem>>, parentMediaId: String) {
        enumSongs?.cancel()

        mBrowsedSongList.clear()

        result.detach()

        val mediaItems = arrayListOf<MediaBrowserCompat.MediaItem>()

        enumSongs = lifecycleScope.launch(Dispatchers.IO) {
            loadSongs(parentMediaId, mediaItems, result)
        }
    }

    private suspend fun loadSongs(
        parentMediaId: String,
        mediaItems: ArrayList<MediaBrowserCompat.MediaItem>,
        result: Result<List<MediaBrowserCompat.MediaItem>>)
    {
        try {
            val itemSet = when (parentMediaId) {

                Common.CAT_RANDOM100_ID -> {
                    CacheManager.getInstance()
                        .doEnumContainerSongsForContainer(
                            true,
                            Common.ContainerType.RANDOM100_MODE,
                            Bundle(),
                            1,
                            true
                        )
                }

                LocalEnumerator.MOST_OFTEN_PLAYED -> {
                    val item = PlaylistItem.generatePredifinedPlaylist(
                        Item.ItemType.SHARED_SMART_NEW,
                        LocalEnumerator.MOST_OFTEN_PLAYED,
                        getString(R.string.most_often_played)
                    )

                    CacheManager.getInstance()
                        .doEnumPlaylistSongsForPlaylist(
                            false,
                            item,
                            1,
                            false
                        )
                }

                LocalEnumerator.MOST_RECENT_PLAYED -> {
                    val item = PlaylistItem.generatePredifinedPlaylist(
                        Item.ItemType.SHARED_SMART_NEW,
                        LocalEnumerator.MOST_RECENT_PLAYED,
                        getString(R.string.most_recent_played)
                    )

                    CacheManager.getInstance()
                        .doEnumPlaylistSongsForPlaylist(
                            false,
                            item,
                            1,
                            false
                        )
                }

                CAT_RATING_4 -> {
                    CacheManager.getInstance()
                        .doEnumContainerSongsForContainer(
                            Common.isLogin(),
                            Common.ContainerType.RATING_MODE,
                            getRatingSongBundle(4),
                            1,
                            false
                        )
                }

                CAT_RATING_5 -> {
                    CacheManager.getInstance()
                        .doEnumContainerSongsForContainer(
                            Common.isLogin(),
                            Common.ContainerType.RATING_MODE,
                            getRatingSongBundle(5),
                            1,
                            false
                        )
                }

                CAT_FAVORITE_RADIO -> {
                    val radioId =
                        if (ConnectionManager.isUseWebAPI()) {
                            "Favorite"
                        } else {
                            "inetradio_favorite"
                        }

                    CacheManager.getInstance()
                        .doEnumRadiosForRadios(
                            radioId,
                            1,
                            false
                        )
                }

                else -> {
                    loadSongsFromPlaylist(parentMediaId)
                }
            }

            itemSet?.let {
                mBrowsedSongList.addAll(it.itemList)
            }

            mBrowsedSongList.forEach { song ->
                mediaItems.add(song.toMediaItem())
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        withContext(Dispatchers.Main) {
            result.sendResult(mediaItems)
        }
    }

    private fun getRatingSongBundle(stars: Int): Bundle {
        val bundle = Bundle()
        bundle.putInt(SONG_RATING_LEVEL, stars)
        return bundle
    }

    private fun isSecondCategoryId(parentMediaId: String?): Boolean {
        if (parentMediaId.isNullOrEmpty()) {
            return false
        }
        return parentMediaId == CAT_DOWNLOADED_PLAYLIST ||
                parentMediaId == CAT_PERSONAL_PLAYLIST ||
                parentMediaId == CAT_SHARED_PLAYLIST
    }

    private fun loadSongsFromPlaylist(parentMediaId: String): CacheManager.ItemSet<SongItem>? {

        for (bundle in mPlaylistsBundleList) {
            if (parentMediaId == bundle.toString()) {

                val playlist = PlaylistItem.fromBundle(bundle)

                return if (playlist.isLocal()) {
                    mBrowsedSongList.addAll(
                        audioDatabaseUtils.doEnumLocalPlaylistSongs(
                            playlist.getDsId(),
                            playlist.id,
                            playlist.title
                        )
                    )
                    null
                } else {
                    CacheManager.getInstance()
                        .doEnumPlaylistSongsForPlaylist(
                            true,
                            playlist,
                            1,
                            false
                        )
                }
            }
        }

        return null
    }

    private suspend fun loadRootPlaylists(result: Result<List<MediaBrowserCompat.MediaItem>>) {
        val mediaItems = arrayListOf<MediaBrowserCompat.MediaItem>()

        try {
            val remotePlaylists = LinkedList<PlaylistItem>()

            mLocalPlayList =
                audioDatabaseUtils.loadDownloadedPlaylists(null)

            if (true) {
                try {
                    val itemSet = CacheManager.getInstance()
                        .doEnumPlaylist(true, 1, false)

                    remotePlaylists.addAll(itemSet.itemList)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            mediaItems.addAll(getDefaultPlaylists())

            classifyPlaylists(remotePlaylists)

            appendPlaylistCategories(mediaItems)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        withContext(Dispatchers.Main) {
            result.sendResult(mediaItems)
        }
    }

    private fun classifyPlaylists(playlists: List<PlaylistItem>) {
        if (playlists.isEmpty()) return

        for (item in playlists) {
            if (item.isRandom()) continue

            val uiItem =
                PlaylistAdapter.UiPlaylistItem
                    .generatePlaylistItem(item)

            when {
                item.isSharedSong() -> {
                    mGeneralPlayList.add(uiItem)
                }

                item.isPersonal() -> {
                    mPersonalPlayList.add(uiItem)
                }

                else -> {
                    mSharedPlayList.add(uiItem)
                }
            }
        }
    }

    private fun appendPlaylistCategories(list: ArrayList<MediaBrowserCompat.MediaItem>) {
        val playlistIcon = BitmapFactory.decodeResource(
            resources,
            R.drawable.thumbnail_playlist
        )

        if (mLocalPlayList.isNotEmpty()) {
            list.add(
                generateBrowsableItem(
                    CAT_DOWNLOADED_PLAYLIST,
                    getString(R.string.local_playlist_catgory_title),
                    playlistIcon
                )
            )
        }

        if (mPersonalPlayList.isNotEmpty()) {
            list.add(
                generateBrowsableItem(
                    CAT_PERSONAL_PLAYLIST,
                    getString(R.string.personal_playlist),
                    playlistIcon
                )
            )
        }

        if (mSharedPlayList.isNotEmpty()) {
            list.add(
                generateBrowsableItem(
                    CAT_SHARED_PLAYLIST,
                    getString(R.string.shared_playlist),
                    playlistIcon
                )
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    protected open fun onSkipToQueueItem(id: Long) {
        playingQueueManager.setPlayIndex(id.toInt())
    }

    protected fun updateSessionQueue(queue: List<SongItem> = playingQueueManager.getQueue()) {
        updateSessionQueueJob?.cancel()

        updateSessionQueueJob = lifecycleScope.launch(Dispatchers.IO) {
            mSession.setQueue(generateQueueItemList(queue))
            updateSessionQueueJob = null
        }
    }

    private fun generateQueueItemList(songList: List<SongItem>): List<MediaSessionCompat.QueueItem> {
        return songList.mapIndexed { index, song ->
            MediaSessionCompat.QueueItem(
                song.toDescription(),
                index.toLong()
            )
        }
    }

    override fun provideWifiLock(): WifiManager.WifiLock {
        if (playbackWifiLock == null) {
            val wifiManager =
                applicationContext.getSystemService(
                    WIFI_SERVICE
                ) as WifiManager

            playbackWifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "DSAudio:PlaybackWifiLock"
            )
        }

        return requireNotNull(playbackWifiLock)
    }

}