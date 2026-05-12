package com.whisperyao.dsplayer.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayout
import com.synology.sylib.data.SynoURL
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.PlayerChooserActivity
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.RecyclerViewCallback
import com.whisperyao.dsplayer.ServiceOperator
import com.whisperyao.dsplayer.UDCEvent
import com.whisperyao.dsplayer.databinding.MainDrawerBinding
import com.whisperyao.dsplayer.fragment.ContentFragment
import com.whisperyao.dsplayer.fragment.DrawerFragment
import com.whisperyao.dsplayer.fragment.HomePageFragment
import com.whisperyao.dsplayer.fragment.PagerFragment
import com.whisperyao.dsplayer.homepage.PinManager
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.mediasession.client.MediaBrowserHelper
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.playing.Player
import com.whisperyao.dsplayer.playing.PlayerControlHelper
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.extension.ExtensionsKt.extensionRegisterReceiver
import com.whisperyao.dsplayer.util.extension.ExtensionsKt.toVisibility
import com.whisperyao.dsplayer.util.extension.openSettings
import com.whisperyao.dsplayer.widget.RatingBar
import io.reactivex.rxjava3.disposables.Disposable
import java.util.Stack
import javax.inject.Inject


class HomeActivity : BaseActivity(), ContentFragment.ContentCallback, BaseActivity.ContainerPlayer,
    DrawerFragment.NavigationDrawerCallbacks, PlayerChooser {

    companion object {
        const val TAG: String = "MainActivityKT"
    }

    private var NAVI_MODE = -1
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tab: TabLayout
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private var containerBundle: Bundle? = null
    private lateinit var mHomePageFragment: HomePageFragment
    private lateinit var mOfflineFrag: PagerFragment
    private lateinit var mOnlineFrag: PagerFragment
    private var mPlaylistFrag: ContentFragment? = null
    private var mRadioFrag: ContentFragment? = null
    private lateinit var mPlaylistStack: Stack<Bundle>
    private lateinit var mRadioStack: Stack<Bundle>

    private var disposableSwitchPlayer: Disposable? = null

    private var mCallback: RecyclerViewCallback? = null

    @Inject
    lateinit var playingQueueManager: PlayingQueueManager
    private var listener: MediaBrowserListener? = null

    @Inject
    lateinit var mPlayerControlHelper: PlayerControlHelper

    private var bShouldReloadPlaylist = false

    private val mObserver = PlayingStatusManager.OnPlayerLocalityChangedObserver {
        bShouldReloadPlaylist = true
    }

    private val mExploreListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Common.ACTION_INNER_LOGIN_FROM_EXPLORE) {
                handleExplorerIntent(intent)
            }
        }
    }

    private val playerChooserLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var binding: MainDrawerBinding

    private val statusListener by lazy {
        object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return

                when (action) {
                    "com.synology.dsaudio.metachanged",
                    "com.synology.dsaudio.playstatechanged" -> {
                        mPlayerControlHelper.let {
                            it.updateAll()
                            it.startPollingIfNeeded()
                        }
                    }

                    "com.synology.dsaudio.ERR_DEVICE_NOT_FOUND",
                    "com.synology.dsaudio.chromecast.SESSION_ENDED" -> {
                        SynoLog.d("MainActivityKT", "onReceive : $action")

                        if (Common.isLogin()) {
                            showPlayerChooser()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mPlayerControlHelper.onCreate()
        ServiceOperator.MainPageClosed = false
        binding = MainDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initDrawer()
        tab = findViewById(R.id.tab)
        this.playerStatusManager.registerOnPlayerLocalityChangedObserver(this.mObserver)
        this.mPlaylistStack = Stack()
        this.mRadioStack = Stack()
        init()
        disposableSwitchPlayer = playerStatusManager.switchPlayer.subscribe {
            startMediaBrowserConnection()
        }
        startMediaBrowserConnection()
        setupControlPanel()
        playingQueueManager.observeQueueChanged(this) {
            determineMiniPlayerVisibility(playingQueueManager.getQueueSize() > 0)
        }
        playingQueueManager.getProgress().observe(this) { progress ->
            mPlayerControlHelper.updateSeekBar(
                progress.progress,
                progress.buffer
            )
        }
        val drawerFragment = supportFragmentManager.findFragmentById(R.id.navigation_drawer) as DrawerFragment?
        drawerFragment?.onNavigationItemSelected(2)
        LocalBroadcastManager.getInstance(this).registerReceiver(
            this.mExploreListener,
            IntentFilter(Common.ACTION_INNER_LOGIN_FROM_EXPLORE)
        )
    }

    protected fun determineMiniPlayerVisibility(show: Boolean) {
        // isMobile().get() == true
        if (true) {
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            layoutParams.setMargins(0, 0, 0,
                if (show) {
                    resources.getDimension(R.dimen.mini_player_visual_height).toInt()
                } else {
                    0
                }
            )

            findViewById<View>(R.id.coordinatorLayout).layoutParams = layoutParams

            findViewById<View>(R.id.Main_ControlPanel).visibility = show.toVisibility()
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ServiceOperator.PLAYSTATE_CHANGED);
        intentFilter.addAction(ServiceOperator.META_CHANGED);
        intentFilter.addAction(ServiceOperator.PREPARE_CHANGED);
        intentFilter.addAction(ServiceOperator.ERR_DEVICE_NOT_FOUND);
        intentFilter.addAction(ServiceOperator.ERR_SESSION_ENDED);
        this.extensionRegisterReceiver(this.statusListener, IntentFilter(intentFilter), false)
        mPlayerControlHelper.updateAll()
        mPlayerControlHelper.startPollingIfNeeded()
    }

    override fun onStop() {
        unregisterReceiver(this.statusListener)
        mPlayerControlHelper.stopPolling()
        super.onStop()
    }

    private fun initView() {

        drawerLayout = findViewById(R.id.drawer_layout)

        toolbar = findViewById(R.id.toolbar) // 你的toolbar id

        // 设置 Toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = "Home"
        }

        toolbar.setOnClickListener {
            val recycleViewCallback: RecyclerViewCallback? = this.mCallback
            recycleViewCallback?.scrollToTop()
        }

    }

    private fun init() {
        if (!Common.isLogin()) {
            this.playerStatusManager.setPlayerInfoStreaming()
            return
        }
        this.playerStatusManager.loadLastPlayer()
        val player: Player = this.playerStatusManager.player
        if (player.isPlayModeStreaming()) {
            this.playerStatusManager.setPlayerInfoStreaming()
        } else if (player.isPlayModeRenderer() && !Common.haveRenderer()) {
            this.playerStatusManager.setPlayerInfoStreaming()
        } else {
            playerStatusManager.requestLoadPlayers(
                object : PlayingStatusManager.LoadPlayerCallback {

                    override fun onPreLoad() {
                        progressDialog.show()
                    }

                    override fun onPostLoad() {
                        if (isFinishing) return

                        progressDialog.dismiss()
                        onComplete()
                    }

                    private fun onComplete() {
                        for (playerItem in playerStatusManager.players) {
                            if (playerItem == player) {
                                playerStatusManager.player = playerItem
                                return
                            }
                        }

                        if (Common.isLogin()) {
                            showPlayerChooser()
                        }
                    }
                }
            )
        }
    }

    override fun showPlayerChooser() {
        if (this.playerChooserLauncher == null) return
        this.playerChooserLauncher.launch(
            Intent(
                this,
                PlayerChooserActivity::class.java as Class<*>
            )
        )
    }

    private fun setupControlPanel() {
        this.mPlayerControlHelper.setupControlPanel(
            binding.MainContainer,
            binding.MainControlPanel.root
        )
        this.mPlayerControlHelper.setOnClickRatingIndicatorListener {
            val songItem: SongItem = this.playingQueueManager.getSongItem() ?: return@setOnClickRatingIndicatorListener
            val arrayList: ArrayList<SongItem> = ArrayList()
            arrayList.add(songItem)
            rateSongs(arrayList)
        }
        this.mPlayerControlHelper.removeSeekBarThumb()
        if (getMediaBrowserHelper()?.connected == true) {
            this.mPlayerControlHelper.setupController(getMediaBrowserHelper()?.mediaController)
            getMediaBrowserHelper()?.sendUpdatePlaybackStatusCommand()
        }
    }

    @Throws(Resources.NotFoundException::class)
    protected fun rateSongs(songList: List<SongItem>) {
        if (songList.isEmpty()) return

        val (title, rating) = if (songList.size == 1) {
            val song = songList[0]
            song.title to song.rating
        } else {
            getString(R.string.songs_count, songList.size) to 0f
        }

        val ratingBar = LayoutInflater.from(this)
            .inflate(R.layout.rating_bar_quick_action, null) as RatingBar

        ratingBar.setOnRatingChangeListener { bar, newRating, fromUser ->
            if (!fromUser) return@setOnRatingChangeListener

            val value = newRating.toInt()

            CacheManager.getInstance().recordRatingMapFromUser(songList, value)

            CacheManager.getInstance().requestRatingSongs(songList, value)

            bar.setRating(newRating)
        }

        ratingBar.setRating(rating)

        val customTitleView = LayoutInflater.from(this).inflate(R.layout.rating_alert_dialog_title, null)

        customTitleView.findViewById<TextView>(R.id.title).text = title

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setCustomTitle(customTitleView)
            .setView(ratingBar)
            .show()

        dialog.window?.setLayout(
            resources.getDimensionPixelOffset(
                R.dimen.main_detail_editable_rating_bar_width
            ),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun startMediaBrowserConnection() {
        val mediaBrowserHelper: MediaBrowserHelper? = this.mMediaBrowserHelper
        mediaBrowserHelper?.onStop()
        val mediaBrowserConnection: MediaBrowserHelper = MediaBrowserConnection(this)
        setMediaBrowserHelper(mediaBrowserConnection)
        this.mMediaBrowserHelper = mediaBrowserConnection
        val callback: MediaControllerCompat.Callback? = this.listener
        if (callback != null) {
            mediaBrowserHelper?.unregisterCallback(callback)
        }
        val mediaBrowserListener = MediaBrowserListener()
        this.listener = mediaBrowserListener
        mediaBrowserConnection.registerCallback(mediaBrowserListener)
        mediaBrowserConnection.onStart()
    }

    private inner class MediaBrowserConnection(mContext: Context) : MediaBrowserHelper(
        mContext,
        this@HomeActivity.playerStatusManager,
        this@HomeActivity.playingQueueManager,
        this@HomeActivity.classProvider.get()
    ) {

        override fun onConnected(mediaController: MediaControllerCompat) {
            SynoLog.i("HomeActivity", "MediaBrowserConnection onConnected")
            mMediaController = mediaController
            this@HomeActivity.mPlayerControlHelper.setupController(mediaController)
        }
    }

    private inner class MediaBrowserListener : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat?) {
            mPlayerControlHelper.updateAll()
            mPlayerControlHelper.getPlayerFragment()?.onPlaybackStateChanged(playbackState)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (metadata != null) {
                mPlayerControlHelper.updateAll()
                if (mPlayerControlHelper.getPlayerFragment() != null) {
                    mPlayerControlHelper.getPlayerFragment()?.updateTrackInfo()
                }
            }
        }

        override fun onSessionReady() {
            mPlayerControlHelper.updateAll()
        }

    }

    fun provideController(): MediaControllerCompat {
        return getMediaBrowserHelper()?.mediaController as MediaControllerCompat
    }

    fun isConnected(): Boolean {
        return getMediaBrowserHelper()?.connected == true
    }

    fun sendUpdatePlaybackStatusCommand() {
        getMediaBrowserHelper()?.sendUpdatePlaybackStatusCommand()
    }

    private fun initDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    fun handleBack(): Boolean {
        if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }

        val showCheckToLeave = when (NAVI_MODE) {
            1 -> mOfflineFrag.handleBack()

            2 -> mOnlineFrag.handleBack()

            3 -> {
                if (mPlaylistStack.isEmpty()) false
                else {
                    mPlaylistFrag = ContentFragment.newInstance(mPlaylistStack.pop(), this)

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content, mPlaylistFrag as Fragment)
                        .commit()

                    updateTitle()
                    true
                }
            }

            else -> false
        }
        if (showCheckToLeave) { return true }
        if (NAVI_MODE != 4) {
            if (NAVI_MODE != 5) {
                return false
            }
            return this.mHomePageFragment.handleBack()
        }
        if (this.mRadioStack.isEmpty()) {
            return false
        }
        this.mRadioFrag = ContentFragment.newInstance(this.mRadioStack.pop(), this)
        val fragmentTransactionBeginTransaction2 = supportFragmentManager.beginTransaction()
        fragmentTransactionBeginTransaction2.replace(R.id.content, this.mRadioFrag as Fragment)
        fragmentTransactionBeginTransaction2.commit()
        updateTitle()
        return true
    }

    fun getCategoryTitle(): CharSequence? {
        val categoryTitle = when (this.NAVI_MODE) {
            1 -> getString(R.string.local)
            2 -> getString((R.string.music_library))
            3 -> getString(R.string.category_playlist)
            4 -> getString(R.string.category_radio)
            5 -> getString(R.string.category_homepage)
            else -> null
        }
        return categoryTitle
    }

    private fun hasBackStack(): Boolean {
        return when (NAVI_MODE) {
            1 -> mOfflineFrag.hasBackStack()
            2 -> mOnlineFrag.hasBackStack()
            3 -> mPlaylistStack.isNotEmpty()
            4 -> mRadioStack.isNotEmpty()
            5 -> mHomePageFragment.hasBackStack()
            else -> false
        }
    }

    private fun updateTitle() {
        supportInvalidateOptionsMenu()

        val categoryTitle = getCategoryTitle()

        supportActionBar?.title = categoryTitle

        toggle.syncState()

        if (hasBackStack()) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            toolbar.setNavigationOnClickListener {
                handleBack()
            }
        } else {
            toolbar.setNavigationOnClickListener {
                if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }
        }
    }

    private fun calculateStack(stack: Stack<Bundle>, udc: Bundle) {
        var libraryType: String? = null

        stack.forEach { item ->
            if (libraryType == null) {
                libraryType = when (item.getString(Common.CONTAINER_TYPE, "")) {
                    "FOLDER_MODE" -> "folder"
                    "ARTIST_ALBUM_MODE" -> "artist"
                    "GENRE_MODE" -> "genre"
                    "ALBUM_MODE" -> "album"
                    "COMPOSER_ALBUM_MODE" -> "composer"
                    else -> null
                }

                libraryType?.let {
                    udc.putString("library", it)
                }
            }

            val uiInfo = item.getBundle("ui_info") ?: return@forEach
            val containerType =
                item.getString(Common.CONTAINER_TYPE, "").lowercase()
            val title =
                uiInfo.getString("title", "").lowercase()

            if (
                item.getString("type") == "container" &&
                "genre" in containerType
            ) {
                val value =
                    if (title == "all albums")
                        UDCEvent.ContainerValue.ALL_ALBUMS.value
                    else
                        UDCEvent.ContainerValue.ARTIST.value

                udc.putString("genre", value)
            }

            if (
                item.getString("type") == PinManager.SONG &&
                ("composer" in containerType || "artist" in containerType)
            ) {
                val key =
                    if ("composer" in containerType) "composer"
                    else "artist"

                val value =
                    if (title == "all songs")
                        UDCEvent.ContainerValue.ALL_SONGS.value
                    else
                        UDCEvent.ContainerValue.ALBUM.value

                udc.putString(key, value)
            }
        }
    }

    protected fun openSettings() {
        this.openSettings(R.string.type_settings)
    }

    @SuppressLint("GestureBackNavigation")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (handleBack()) return true

            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.check_to_leave)
                .setPositiveButton(R.string.yes) { _, _ ->
                    ServiceOperator.MainPageClosed = true
                    finish()
                }
                .setNegativeButton(R.string.no, null)
                .show()

            return true
        }

        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            if (!Common.isLogin()) {
                return false
            }
            // SearchActivity
            startActivity(Intent(this, SongListActivity::class.java))
            return true
        }

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            AudioPreference.enableRemoteController(this)) {
            mPlayerControlHelper.pressVolume()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        SynoLog.d(TAG, "onPrepareOptionsMenu")
        // isEditable()
        menu.findItem(R.id.menu_edit).isVisible = true
        // canSetView()
        menu.findItem(R.id.menu_view).isVisible = true
        menu.findItem(R.id.menu_sleep_timer).isVisible = true
        // isReorderable()
        menu.findItem(R.id.menu_reorder).isVisible = true
        // isPlayable()
        menu.findItem(R.id.menu_play_all).isVisible = true
        // isPlayable()
        menu.findItem(R.id.menu_add_all).isVisible = true

        // updatePlayerChooserAsset()

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                SynoLog.i("Menu Callback", "Home")
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    // 让 toggle 生效（重要！）
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    // 点击侧边栏 item 回调
    override fun onNavigationDrawerItemSelected(itemId: Int) {
        if (itemId == 0) {
            SynoLog.i("Drawer Callback", "Open Settings")
            openSettings()
        } else if (this.NAVI_MODE != itemId) {
            val fragmentTransactionBeginTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            when (itemId) {
                1 -> {
                    // Local
                    SynoLog.i("Drawer Callback", "Home")
                    val pagerFragmentNewInstance: PagerFragment = PagerFragment.newInstance(
                        PagerFragment.PagerParent.MAIN_ACTIVITY, null, false)
                    this.mOfflineFrag = pagerFragmentNewInstance
                    fragmentTransactionBeginTransaction.replace(R.id.content, pagerFragmentNewInstance)
                }

                2 -> {
                    // Library
                    SynoLog.i("Drawer Callback", "Library")
                    val pagerFragmentNewInstance: PagerFragment = PagerFragment.newInstance(
                        PagerFragment.PagerParent.MAIN_ACTIVITY, null, true)
                    this.mOnlineFrag = pagerFragmentNewInstance
                    fragmentTransactionBeginTransaction.replace(R.id.content, pagerFragmentNewInstance)
                }

                3 -> {
                    SynoLog.i("Drawer Callback", "Playlist")
                    tab.visibility = View.GONE
                    this.mPlaylistFrag = ContentFragment.newInstance(Common.isLogin(), Common.ContainerType.PLAYLIST_MODE, this)
                    this.mPlaylistStack.clear()
                    fragmentTransactionBeginTransaction.replace(R.id.content, mPlaylistFrag as Fragment)
                }

                4 -> {
                    SynoLog.i("Drawer Callback", "Radio")
                    tab.visibility = View.GONE
                    this.mRadioFrag = ContentFragment.newInstance(true, Common.ContainerType.RADIO_MODE, this);
                    fragmentTransactionBeginTransaction.replace(R.id.content, this.mRadioFrag as Fragment)
                }

                5 -> {
                    // Home
                    SynoLog.i("Drawer Callback", "Home")
                    val homePageFragment = HomePageFragment()
                    this.mHomePageFragment = homePageFragment
                    fragmentTransactionBeginTransaction.replace(R.id.content, homePageFragment)
                }

                6 -> {
                    // SongList
                    startActivity(Intent(this, SongListActivity::class.java))
                }
            }
            fragmentTransactionBeginTransaction.commit()
            this.NAVI_MODE = itemId
            if (Common.isLogin()) {
                AudioPreference.setNavigationPref(this.NAVI_MODE)
            }
            updateTitle()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun getOutOfCapacityString(str: String): String {
        return str.replace(Common.SONG_LIMITATION, getMaxQueueSize().toString())
    }

    override fun playContainer(action: Common.PlaybackAction, list: List<SongItem>, position: Int, isFromMenu: Boolean) {
        SynoLog.i("HomeActivity", "playContainer")
        super.playContainer(action, list, position, isFromMenu)
        val pathBundle = Bundle()
        when (NAVI_MODE) {
            1 -> {
                pathBundle.putString("category", UDCEvent.CategoryValue.DOWNLOADED_SONGS.value)

                val stack = mOfflineFrag.getBundleStack()
                calculateStack(stack as Stack<Bundle>, pathBundle)
            }

            2 -> {
                pathBundle.putString("category", UDCEvent.CategoryValue.LIBRARY.value)

                val stack = mOnlineFrag.getBundleStack()
                calculateStack(stack as Stack<Bundle>, pathBundle)
            }

            3 -> {
                pathBundle.putString("category", UDCEvent.CategoryValue.PLAYLISTS.value)

                val playbackBundle = Bundle()

                if (mPlaylistStack.size > 1) {
                    for (bundle in mPlaylistStack) {
                        if (bundle.getString("key") == "[__RECENTLY_ADDED__]") {
                            playbackBundle.putString("playback", "recently_added")
                        }
                    }
                }

                containerBundle?.let { container ->
                    val key = container.getString("key", "")
                    val containerType = container.getString("ContainerType", "")

                    when (key) {
                        "[__MOST_RECENT_ADDED__]" -> { playbackBundle.putString("playback", "recently_played") }

                        "[__RANDOM100__]" -> {
                            if (containerType == "RATING_MODE") {
                                playbackBundle.putString("playback", "ratings")
                            } else {
                                playbackBundle.putString("playback", "random100")
                            }
                        }

                        "[__MOST_FREQUENT_LISTEN__]" -> {
                            playbackBundle.putString("playback", "most_often_played")
                        }

                        else -> {
                            if (container.getString("title") == "Shared songs") {
                                playbackBundle.putString(
                                    "playback",
                                    "shared_songs"
                                )
                            } else {
                                val extraPlaylist =
                                    container.getBundle("extra_playlist")

                                if (extraPlaylist != null) {
                                    when (extraPlaylist.getString("type")) {
                                        "LOCAL_PLAYLIST_NORMAL" -> {
                                            playbackBundle.putString("playback", "download_playlist")
                                        }

                                        "PERSONAL_NORMAL_NEW" -> {
                                            playbackBundle.putString("playback", "personal_playlist")
                                        }

                                        "SHARED_NORMAL_NEW" -> {
                                            playbackBundle.putString("playback", "group_playlist")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

//                if (!playbackBundle.isEmpty) { firebaseAnalyticsUtil.logEvent("syno_playlist", playbackBundle) }
            }

            4 -> { pathBundle.putString("category", UDCEvent.CategoryValue.RADIO.value) }

        }

//        if (!isFromMenu) { firebaseAnalyticsUtil.logEvent("syno_playback_path", pathBundle) }
    }

    override fun getBundleStack(): Stack<Bundle>? { return null }

    override fun onContainerItemClick(bundle: Bundle) {
        this.containerBundle = null
        val i = this.NAVI_MODE
        if (i == 3) {
            this.containerBundle = bundle
            this.mPlaylistStack.push(this.mPlaylistFrag?.getInitialBundle())
            this.mPlaylistFrag = ContentFragment.newInstance(bundle, this)
            val fragmentTransactionBeginTransaction = supportFragmentManager.beginTransaction()
            fragmentTransactionBeginTransaction.replace(
                R.id.content,
                this.mPlaylistFrag as Fragment
            )
            fragmentTransactionBeginTransaction.commit()
        } else if (i == 4) {
            this.mRadioStack.push(this.mRadioFrag?.getInitialBundle())
            this.mRadioFrag = ContentFragment.newInstance(bundle, this)
            val fragmentTransaction2 = supportFragmentManager.beginTransaction()
            fragmentTransaction2.replace(R.id.content, this.mRadioFrag as Fragment)
            fragmentTransaction2.commit()
        }
        updateTitle()
    }

    override fun onFinishLoading(type: Common.ContainerType, size: Int) { }

    override fun onUpdateTitle() {
        updateTitle()
    }

    override fun onResume() {
        // App.isMainActivityFirstLaunched() && !permissionGranted() && AudioPreference.enableAutoDownload()
        if (!permissionGranted() && AudioPreference.enableAutoDownload()) {
            showHintsThenAskPermission();
        }
        if (Common.gDeviceChanged) {
            if (Common.isLogin()) {
                showPlayerChooser()
            }
            Common.gDeviceChanged = false
        }
        if (Common.gModeSwitchMode) {
            Common.gModeSwitchMode = false
        }
        // checkAndReloadPlaylist
        if (this.mPlayerControlHelper != null) {
            this.mPlayerControlHelper.updateAll()
        }
        super.onResume()
    }

    private fun equalToCurrentAccount(intent: Intent): Boolean {
        val currentAddress = AudioPreference.getUserInputAddress()
        val currentAccount = AudioPreference.getAccount()
        val currentHttps = AudioPreference.getHttpsPref()

        // LoginActivity.EXPLORE_ARG__ADDRESS
        val address = intent.getStringExtra("address")
        val account = intent.getStringExtra("account")

        if (address != null) {
            val currentUrl = SynoURL.composeValidURL(currentAddress, currentHttps, 5000, 5001)
            val targetUrl = SynoURL.composeValidURL(
                address,
                intent.getBooleanExtra("isHttps", false),
                5000,
                5001
            )

            if (!SynoURL.compareUrlIgnoreHttps(currentUrl, targetUrl)) {
                return false
            }
        }

        if (account != null && !account.equals(currentAccount, ignoreCase = true)) {
            return false
        }

        return true
    }

    private fun handleExplorerIntent(intent: Intent) {
        if (equalToCurrentAccount(intent)) return

        val newIntent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Common.ACTION_ASK_LOGOUT
            data = intent.data
            putExtras(intent)
        }

        startActivity(newIntent)
    }

    fun addCallback(callback: RecyclerViewCallback) {
        this.mCallback = callback
    }

}