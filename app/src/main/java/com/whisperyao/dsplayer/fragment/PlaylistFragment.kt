package com.whisperyao.dsplayer.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import com.synology.ThreadWork
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.LocalEnumerator
import com.whisperyao.dsplayer.PlaylistAdapter
import com.whisperyao.dsplayer.PlaylistEditor
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.StateManager
import com.whisperyao.dsplayer.homepage.PinManager
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.item.PlaylistItem
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.net.WebAPIErrorException
import com.whisperyao.dsplayer.provider.AudioDatabaseUtils
import com.whisperyao.dsplayer.publicsharing.fragment.EditPlaylistFragment
import com.whisperyao.dsplayer.util.SynoLog
import java.io.IOException
import java.util.LinkedList
import java.util.Stack
import javax.inject.Inject


class PlaylistFragment() : ContentFragment(),
    ContentFragment.ContentCallback,
    PlaylistAdapter.PlaylistCallbacks,
    EditPlaylistFragment.Callbacks,
    PinManager.Callback {

    companion object {
        const val CAT_GENERAL = 0
        const val CAT_PERSONAL = 1
        const val CAT_SHARED = 2
        private const val TAG = "PlaylistFragment"
    }

    @Inject
    lateinit var audioDatabaseUtils: AudioDatabaseUtils

    private lateinit var contentBundle: Bundle

    private var enumSongsWork: ThreadWork? = null
    private var playlistEditWork: ThreadWork? = null

    private var isLeft = true
    private var page = 0
    private var selectedPosition = 0

    private var listView: ListView? = null
    private var adapter: PlaylistAdapter? = null

    private var currentContentFragment: ContentFragment? = null
    private var lastSelectedPlaylist: PlaylistItem? = null

    private val generalPlaylists = LinkedList<PlaylistAdapter.UiPlaylistItem>()
    private val personalPlaylists = LinkedList<PlaylistAdapter.UiPlaylistItem>()
    private val sharedPlaylists = LinkedList<PlaylistAdapter.UiPlaylistItem>()
    private var localPlaylists = LinkedList<PlaylistAdapter.UiPlaylistItem>()

    constructor(callback: ContentCallback) : this() {
        mContainerClickCallback = callback
    }

    constructor(callback: ContentCallback, doRefresh: Boolean) : this(callback) {
        blDoRefresh = doRefresh
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SynoLog.i("PlaylistFragment", "onCreate")
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        isOnline = mArgument.getBoolean(PinManager.MODE) ?: false
        mType = Common.ContainerType.PLAYLIST_MODE

        initAdapter()

        PinManager.getInstance().addCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        PinManager.getInstance().removeCallback(this)
    }

    override fun onResume() {
        super.onResume()

        if (Common.gIsCreatNewPlaylist) {
            Common.gIsCreatNewPlaylist = false
            doRefresh()
        }
    }

    override fun onDetach() {
        enumSongsWork?.takeIf { it.isWorking }?.endThread()
        playlistEditWork?.takeIf { it.isWorking }?.endThread()
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mContentView = inflater.inflate(
            if (StateManager.getInstance().isMobileLayout)
                R.layout.playlist_fragment
            else
                R.layout.tablet_content_fragment,
            null
        )

        setupViews()
        SynoLog.i("PlaylistFragment", "isInitialized: $isInitialized")
        if (!isInitialized) {
            // blDoRefresh
            loadContent(true)
            isInitialized = true
            blDoRefresh = false
        }

        return mContentView
    }

    private fun initAdapter() {
        adapter = PlaylistAdapter(mActivity, this)

        adapter?.addSection("General", generalPlaylists)
        adapter?.addSection(
            getString(R.string.local_playlist_catgory_title),
            localPlaylists
        )

        SynoLog.i("PlaylistFragment", "isOnline: $isOnline")

        if (isOnline) {
            adapter?.addSection(
                getString(R.string.personal_playlist),
                personalPlaylists
            )
            adapter?.addSection(
                getString(R.string.shared_playlist),
                sharedPlaylists
            )
        }
    }

    private fun setupViews() {
        mRefresh = mContentView.findViewById(R.id.refresh)

        mRefresh.setOnRefreshListener {
            loadContentWork?.isWorking?.let {
                if (!it) {
                    doRefresh()
                }
            }
        }

        listView = mContentView.findViewById(R.id.content_list)

        listView?.adapter = adapter

        listView?.setOnItemClickListener { _, _, position, _ ->
            onPlaylistItemClick(position, false)
        }

        listView?.setOnScrollListener(MyOnScrollListener())
    }

    private fun onPlaylistItemClick(position: Int, refresh: Boolean) {
        SynoLog.i("PlaylistFragment", "onPlaylistItemClick")
        val item = adapter?.getItem(position) ?: return
        if (item.isHeader) return

        if (currentContentFragment?.isEditMode() == true) return

        selectedPosition = position

        val playlist = item.dataItem
        lastSelectedPlaylist = playlist

        val bundle = getEnumSongsBundle(playlist)

        SynoLog.i("PlaylistFragment", bundle.toString())

        if (StateManager.getInstance().isMobileLayout) {
            mContainerClickCallback.onContainerItemClick(bundle)
            return
        }

        contentBundle = Bundle(bundle)

        bundle.putBoolean("left_pane", false)

        currentContentFragment = newInstance(bundle, this, refresh)
    }

    override fun toggleView() { }

    override fun onPageSelected() { }

    override fun allItemPlayAction(action: Common.ItemAction?) {
        currentContentFragment?.allItemPlayAction(action)
    }

    override fun canLoadMore(): Boolean {
         return (this.personalPlaylists.size + this.sharedPlaylists.size) + 1 < this.total
    }

    override fun canMultiEdit() = currentContentFragment?.canMultiEdit() ?: false

    override fun canSetView(): Boolean { return false }

    override fun getSelectedItems(): ArrayList<SongItem>? {
        if (this.currentContentFragment != null) {
            return currentContentFragment?.getSelectedItems()
        }
        return ArrayList()
    }

    override fun isEditMode() = currentContentFragment?.isEditMode() ?: false

    override fun isPlayable() = currentContentFragment?.isPlayable() ?: false

    override fun setEditMode(edit: Boolean) {
        currentContentFragment?.setEditMode(edit)
    }

    private fun resetPlaylists() {
        page = 0
        personalPlaylists.clear()
        sharedPlaylists.clear()
        generalPlaylists.clear()
        localPlaylists.clear()
    }

    private fun appendPlaylistItem(item: PlaylistItem) {
        val uiItem =
            PlaylistAdapter.UiPlaylistItem.generatePlaylistItem(item)

        when {
            item.isSharedSong() -> generalPlaylists.add(uiItem)
            item.isPersonal -> personalPlaylists.add(uiItem)
            else -> sharedPlaylists.add(uiItem)
        }
    }

    private fun refreshSections() {
        if (generalPlaylists.isNotEmpty()) {
            adapter?.addSection("General", generalPlaylists)
        }

        if (localPlaylists.isNotEmpty()) {
            adapter?.addSection(
                getString(R.string.local_playlist_catgory_title),
                localPlaylists
            )
        }

        if (personalPlaylists.isNotEmpty()) {
            adapter?.addSection(
                getString(R.string.personal_playlist),
                personalPlaylists
            )
        }

        if (sharedPlaylists.isNotEmpty()) {
            adapter?.addSection(
                getString(R.string.shared_playlist),
                sharedPlaylists
            )
        }

        adapter?.notifyDataSetChanged()
        mContainerClickCallback.onUpdateTitle()
    }

    private fun initGeneralPlaylists() {
        if (generalPlaylists.isNotEmpty()) return

        if (isOnline) {
            generalPlaylists.add(
                PlaylistAdapter.UiPlaylistItem.generatePlaylistItem(
                    PlaylistItem.generatePredifinedPlaylist(
                        Item.ItemType.CONTAINER_MODE,
                        Common.CAT_RANDOM100_ID,
                        getString(R.string.random_100)
                    )
                )
            )
            // Common.haveLatestAlbum()
            if (true) {
                generalPlaylists.add(
                    PlaylistAdapter.UiPlaylistItem.generatePlaylistItem(
                        PlaylistItem.generatePredifinedPlaylist(
                            Item.ItemType.CONTAINER_MODE,
                            Common.CAT_RECENTLY_ADDED,
                            getString(R.string.latest_album)
                        )
                    )
                )
            }
        }

        generalPlaylists.add(
            PlaylistAdapter.UiPlaylistItem.generatePlaylistItem(
                PlaylistItem.generatePredifinedPlaylist(
                    Item.ItemType.SHARED_SMART_NEW,
                    LocalEnumerator.MOST_OFTEN_PLAYED,
                    getString(R.string.most_often_played)
                )
            )
        )

        generalPlaylists.add(
            PlaylistAdapter.UiPlaylistItem.generatePlaylistItem(
                PlaylistItem.generatePredifinedPlaylist(
                    Item.ItemType.SHARED_SMART_NEW,
                    LocalEnumerator.MOST_RECENT_PLAYED,
                    getString(R.string.most_recent_played)
                )
            )
        )
        // ConnectionManager.isWithRating(isOnline)
        if (true) {
            generalPlaylists.add(
                PlaylistAdapter.UiPlaylistItem.generatePlaylistItem(
                    PlaylistItem.generatePredifinedPlaylist(
                        Item.ItemType.RATING_MODE,
                        Common.CAT_RATING,
                        getString(R.string.rating)
                    )
                )
            )
        }
    }

    override fun loadContent(refresh: Boolean) {
        SynoLog.d("PlaylistFragment", "loadContent($refresh), type=${mType?.name}")

        loadContentWork?.takeIf { it.isWorking }?.endThread()

        loadContentWork = object : ThreadWork() {

            private var connectionInfo =
                Common.ConnectionInfo.ERROR_NETWORK

            private val retItems = mutableListOf<PlaylistItem>()

            override fun preWork() {
                activity?.setProgressBarIndeterminateVisibility(true)

                if (refresh) resetPlaylists()

                initGeneralPlaylists()

                page++

                if (page == 1) setRefreshing(true)
            }

            override fun onWorking() {
                try {
                    val result = cacheMgr.doEnumPlaylist(
                        isOnline,
                        page,
                        refresh
                    )

                    retItems.clear()
                    retItems.addAll(result.itemList)

                    total = result.total

                    connectionInfo =
                        Common.ConnectionInfo.SUCCESS

                    val dsId = Common.getDsId()
                        .takeIf { Common.isRemotePlayer() }

//                    localPlaylists = AudioDatabaseUtils.loadDownloadedPlaylists(dsId)

                } catch (e: WebAPIErrorException) {
                    exception = e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun postWork() {
                if (page == 1) setRefreshing(false)
            }

            override fun onComplete() {
                activity?.setProgressBarIndeterminateVisibility(false)

                exception?.let {
                    handleError(it)
                    return
                }

                if (connectionInfo != Common.ConnectionInfo.SUCCESS) return

                retItems
                    .filterNot { it.isRandom() }
                    .forEach(::appendPlaylistItem)

                refreshSections()

                if (!StateManager.getInstance().isMobileLayout && isLeft) {
                    checkSelPos()
                    onPlaylistItemClick(selectedPosition, refresh)
                }

                if (canLoadMore() && retItems.isNotEmpty()) {
                    loadContent(false)
                }
            }
        }

        loadContentWork?.startWork()
    }

    private fun checkSelPos() {
        for (i in 0..<this.adapter?.count as Int) {
            val dataItem: PlaylistItem? = this.adapter?.getItem(i)?.dataItem
            val playlistItem: PlaylistItem? = this.lastSelectedPlaylist
            if (playlistItem != null && dataItem != null && playlistItem.MyEqual(dataItem)) {
                this.selectedPosition = i
                return
            }
        }
        if (this.selectedPosition >= this.adapter?.count as Int) {
            this.selectedPosition = this.adapter?.count?.minus(1) as Int
        }
        while (true) {
            if (this.selectedPosition <= 0 || !this.adapter?.getItem(this.selectedPosition)?.isHeader!!) {
                return
            } else {
                this.selectedPosition--
            }
        }

    }

    override fun markAllItem(mark: Boolean) {
        currentContentFragment?.markAllItem(mark)
    }

    override fun onScrollToBottom(view: AbsListView?) {
        if ((this.loadContentWork?.isWorking == true) && canLoadMore()) {
            loadContent(false);
        }
    }

    private fun enumSongs(action: Common.ItemAction, playlistItem: PlaylistItem) {
        enumSongsWork = object : ThreadWork() {

            private val songList = ArrayList<SongItem>()

            override fun onWorking() {
                try {
//                    songList.addAll(cacheMgr.doEnumPlaylistSongsForPlaylist(getEnumSongsBundle(playlistItem)
//                        .getBoolean(PinManager.MODE),
//                        playlistItem,
//                        -1,
//                        true
//                    ).itemList)
                } catch (e: Exception) {
                    exception = e as WebAPIErrorException
                }
            }

            override fun onComplete() {
                if (exception != null) {
                    handleError(exception as WebAPIErrorException)
                    return
                }

                when (action) {
                    Common.ItemAction.PLAY ->
                        enqueueAction(
                            Common.PlaybackAction.PLAY_NOW,
                            0,
                            songList
                        )

                    Common.ItemAction.ADD_ITEM ->
                        enqueueAction(
                            Common.PlaybackAction.ADD_ONLY,
                            0,
                            songList
                        )

                    Common.ItemAction.ADD_NEXT ->
                        enqueueAction(
                            Common.PlaybackAction.ADD_NEXT,
                            0,
                            songList
                        )

                    Common.ItemAction.DOWNLOAD -> {
//                        downloadRemote(songList)
                        doRefresh()
                    }

                    else -> {}
                }
            }
        }

        enumSongsWork?.startWork()
    }

    fun getEnumSongsBundle(item: PlaylistItem): Bundle =
        if (item.type.isPlayListItem) {
            getEnumSongsBundleForPlaylist(item)
        } else {
            getEnumSongsBundleForOthers(item)
        }
    private fun getEnumSongsBundleForPlaylist(item: PlaylistItem): Bundle = Bundle().apply {

        putString(
            Common.CONTAINER_TYPE,
            when {
                item.isPersonal() && item.isNormal() ->
                    Common.ContainerType.PERSONAL_PLAYLIST_MODE.name

                item.isPersonal() ->
                    Common.ContainerType.PERSONAL_SMART_PLAYLIST_MODE.name

                item.isNormal() ->
                    Common.ContainerType.SHARED_PLAYLIST_MODE.name

                else ->
                    Common.ContainerType.SHARED_SMART_PLAYLIST_MODE.name
            }
        )

        putString("type", item.type.name)
        putBoolean("left_pane", false)

        putBoolean(
            PinManager.MODE,
            !(
                    item.id == LocalEnumerator.MOST_OFTEN_PLAYED ||
                            item.id == LocalEnumerator.MOST_RECENT_PLAYED ||
                            item.isLocal()
                    )
        )

        putString("key", item.id)
        putString("title", item.title)
        putBundle(PinManager.EXTRA_PLAYLIST, item.bundle)
    }

    private fun getEnumSongsBundleForOthers(item: PlaylistItem): Bundle = Bundle().apply {

        putString(
            Common.CONTAINER_TYPE,
            when (item.id) {
                Common.CAT_RANDOM100_ID ->
                    Common.ContainerType.RANDOM100_MODE.name

                Common.CAT_RATING ->
                    Common.ContainerType.RATING_MODE.name

                Common.CAT_RECENTLY_ADDED ->
                    Common.ContainerType.LATEST_ALBUM_MODE.name

                else ->
                    error("Unknown item id: ${item.id}")
            }
        )

        when (item.id) {
            Common.CAT_RATING -> {
                putString("type", "container")
                putBoolean("left_pane", true)
            }

            Common.CAT_RECENTLY_ADDED -> {
                putString("type", "container")
            }

            else -> {
                putString("type", item.type.name)
                putBoolean("left_pane", false)
            }
        }

        putBoolean(
            PinManager.MODE,
            if (item.id == Common.CAT_RATING) isOnline else true
        )

        putString("key", item.id)
        putString("title", item.title)
    }

    override fun onPinLoadFinish() {}
    override fun onPinPreLoading() {}
    override fun onFinishLoading(type: Common.ContainerType, size: Int) { }

    override fun onActionClicked(actionId: Int, playlistItem: PlaylistItem) {
        when (actionId) {
            R.id.ItemAction_PLAY ->
                enumSongs(Common.ItemAction.PLAY, playlistItem)

            R.id.ItemAction_ADD_ITEM ->
                enumSongs(Common.ItemAction.ADD_ITEM, playlistItem)

            R.id.ItemAction_ADD_NEXT ->
                enumSongs(Common.ItemAction.ADD_NEXT, playlistItem)

            R.id.ItemAction_DOWNLOAD ->
                enumSongs(Common.ItemAction.DOWNLOAD, playlistItem)

            R.id.ItemAction_DELETE ->
                deletePlaylist(playlistItem)
        }
    }

    // TODO shwoSharePlaylist(), editPlaylist()

    private fun deletePlaylist(item: PlaylistItem) {
        playlistEditWork = object : ThreadWork() {

            var success = false

            override fun onWorking() {
                try {
                    if (item.isLocal()) {
                        audioDatabaseUtils.deleteLocalPlaylist(
                            item.getDsId(),
                            item.id
                        )
                    } else {
                        PlaylistEditor.doDeletePlaylist(item.id) {}
                    }

                    success = true

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun onComplete() {
                if (success) doRefresh()
            }
        }

        playlistEditWork?.startWork()
    }

    override fun onContainerItemClick(bundle: Bundle) {
        this.contentBundle.putInt("position", bundle.getInt("position"))
        this.mContainerClickCallback.onContainerItemClick(this.contentBundle)
    }
    override fun onUpdateTitle() {
        this.mContainerClickCallback.onUpdateTitle()
    }
    override fun getBundleStack(): Stack<Bundle> {
        val stack: Stack<Bundle> = Stack<Bundle>()
        stack.push(this.contentBundle)
        return stack
    }

    override fun onUpdatePlaylist() {
        doRefresh()
    }

    override fun onPinErrorOccur() {
        doRefresh()
    }

    override fun scrollToTop() {
        this.listView?.smoothScrollToPosition(0)
    }
}