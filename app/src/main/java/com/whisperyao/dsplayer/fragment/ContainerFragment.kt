package com.whisperyao.dsplayer.fragment

import android.app.ProgressDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.whisperyao.dsplayer.R
import com.synology.ThreadWork
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.ServiceOperator
import com.whisperyao.dsplayer.StateManager
import com.whisperyao.dsplayer.adapters.ContainerAdapter
import com.whisperyao.dsplayer.homepage.PinManager
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.net.WebAPIErrorException
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utilities
import java.util.ArrayList
import java.util.List
import java.util.Stack
import com.google.android.gms.cast.MediaTrack


class ContainerFragment : ContentFragment, ContentFragment.ContentCallback {

    companion object {
        private const val LOG = "ContainerFragment"
    }

    private lateinit var containerListAdapter: ContainerAdapter
    private var contentBundle: Bundle? = null
    private var enumSongsWork: ThreadWork? = null

    private var isLeft = true
    private var mContentFrag: ContentFragment? = null
    private var mItems = arrayListOf<Item>()
    private var mViewMode = Common.PrefViewMode.LIST

    private var page = 0
    private var scrollPos = -1
    private var selPos = -1

    private var pinManager = PinManager.getInstance()

    constructor() : super()

    constructor(callback: ContentCallback, load: Boolean) : super(callback) {
        blLoadContent = load
    }

    constructor(callback: ContentCallback, load: Boolean, doRefresh: Boolean) : super(callback) {
        blLoadContent = load
        blDoRefresh = doRefresh
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        isOnline = mArgument.getBoolean(PinManager.MODE) == true
        mType = Common.ContainerType.valueOf(
            mArgument.getString(Common.CONTAINER_TYPE)!!
        )

        mTitle = mArgument.getString("title")
        mSubtitle = mArgument.getString("subtitle")

        isLeft = mArgument.getBoolean("left_pane", true)

        selPos = mArgument.getInt("position", 0)
        scrollPos = mArgument.getInt("scroll_to_position", 0)

        initSpecialAllSongsItem()

        containerListAdapter = ContainerAdapter(this).apply {
            setIsLeft(isLeft)

            if (mArgument.containsKey("genre")) {
                setKey(mArgument.getString("genre") as String)
            }

            if (mArgument.containsKey("composer")) {
                setKey(mArgument.getString("composer") as String)
            }
        }
    }

    private fun initSpecialAllSongsItem() {
        when (mType) {
            Common.ContainerType.ARTIST_ALBUM_MODE,
            Common.ContainerType.GENRE_ALBUM_MODE,
            Common.ContainerType.GENRE_ARTIST_MODE,
            Common.ContainerType.GENRE_ARTIST_ALBUM_MODE,
            Common.ContainerType.COMPOSER_ALBUM_MODE -> {
                val item = Item(
                    Item.ItemType.CONTAINER_MODE,
                    "",
                    getString(
                        if (mType == Common.ContainerType.GENRE_ARTIST_MODE)
                            R.string.all_albums
                        else
                            R.string.all_songs
                    )
                )
                item.isAllSongs = true
                mItems.add(item)
            }

            else -> {}
        }
    }

    override fun onDetach() {
        enumSongsWork?.let {
            if (it.isWorking) { it.endThread() }
        }
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mContentView = if (StateManager.getInstance().isMobileLayout || !isLeft) {
            inflater.inflate(R.layout.content_fragment, null)
        } else {
            inflater.inflate(R.layout.tablet_content_fragment_recycler, null)
        }

        return mContentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        if (blLoadContent) {
            onPageSelected()
        }

        mContainerClickCallback.onUpdateTitle()
    }

    fun getEnumSongsBundle(item: Item): Bundle {
        return Bundle().apply {
            putString("title", item.title)

            if (item.isWithRating) {
                putFloat(SongItem.SQL_RATING, item.rating)
            }

            putBoolean(PinManager.MODE, isOnline)

            putBundle(
                "ui_info",
                Bundle().apply {
                    putString("album_artist", item.albumArtist)
                    putString(PinManager.DISPLAY_ARTIST, item.displayArtist)
                    putString("title", item.title)
                    putString("id", item.id)
                }
            )

            when (mType) {
                Common.ContainerType.ARTIST_MODE,
                Common.ContainerType.SEARCH_ARTIST_MODE -> {
                    putString("key", item.id)
                    putString("artist", item.id)
                    putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.ARTIST_ALBUM_MODE.name
                    )
                }

                Common.ContainerType.GENRE_MODE -> {
                    putString("genre", item.id)
                    putString(
                        Common.CONTAINER_TYPE,
                        if (ConnectionManager.canSupportGenreArtist(isOnline)) {
                            Common.ContainerType.GENRE_ARTIST_MODE.name
                        } else {
                            Common.ContainerType.GENRE_ALBUM_MODE.name
                        }
                    )
                }

                Common.ContainerType.COMPOSER_MODE -> {
                    putString("key", item.id)
                    putString("composer", item.id)
                    putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.COMPOSER_ALBUM_MODE.name
                    )
                }

                Common.ContainerType.GENRE_ARTIST_MODE -> {
                    putString("title", "$mTitle > ${item.title}")
                    putString(MediaTrack.ROLE_SUBTITLE, item.title)

                    if (mArgument.containsKey("genre")) {
                        putString("genre", mArgument.getString("genre"))
                    }

                    if (!item.isAllSongs) {
                        putString("artist", item.id)
                    }

                    putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                    )
                }

                Common.ContainerType.ALBUM_MODE,
                Common.ContainerType.LATEST_ALBUM_MODE,
                Common.ContainerType.SEARCH_ALBUM_MODE -> {
                    putString("key", item.id)
                    putString("album", item.id)
                    putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.ALBUM_MODE.name
                    )
                    putString("album_artist", item.albumArtist)
                }

                Common.ContainerType.ARTIST_ALBUM_MODE -> {
                    putString("artist", mArgument.getString("artist"))
                    putString("title", mTitle)
                    putString(MediaTrack.ROLE_SUBTITLE, item.title)

                    if (!item.isAllSongs) {
                        putString("album", item.id)
                        putString(
                            Common.CONTAINER_TYPE,
                            Common.ContainerType.ARTIST_ALBUM_MODE.name
                        )
                        putString("album_artist", item.albumArtist)
                    } else {
                        putString(
                            Common.CONTAINER_TYPE,
                            Common.ContainerType.ARTIST_MODE.name
                        )
                    }
                }

                Common.ContainerType.GENRE_ALBUM_MODE -> {
                    putString("genre", mArgument.getString("genre"))
                    putString("title", mTitle)
                    putString(MediaTrack.ROLE_SUBTITLE, item.title)

                    if (!item.isAllSongs) {
                        putString("album", item.id)
                        putString(
                            Common.CONTAINER_TYPE,
                            Common.ContainerType.GENRE_ALBUM_MODE.name
                        )
                        putString("album_artist", item.albumArtist)
                    } else {
                        putString(
                            Common.CONTAINER_TYPE,
                            Common.ContainerType.GENRE_MODE.name
                        )
                    }
                }

                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE -> {
                    putString("title", "$mTitle > ${item.title}")
                    putString(MediaTrack.ROLE_SUBTITLE, item.title)

                    mArgument.getString("genre")?.let {
                        putString("genre", it)
                    }

                    mArgument.getString("artist")?.let {
                        putString("artist", it)
                    }

                    if (!item.isAllSongs) {
                        putString("album", item.id)
                        putString("album_artist", item.albumArtist)
                    }

                    putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                    )
                }

                Common.ContainerType.COMPOSER_ALBUM_MODE -> {
                    putString("composer", mArgument.getString("composer"))
                    putString("title", mTitle)
                    putString(MediaTrack.ROLE_SUBTITLE, item.title)

                    if (!item.isAllSongs) {
                        putString("album", item.id)
                        putString(
                            Common.CONTAINER_TYPE,
                            Common.ContainerType.COMPOSER_ALBUM_MODE.name
                        )
                        putString("album_artist", item.albumArtist)
                    } else {
                        putString(
                            Common.CONTAINER_TYPE,
                            Common.ContainerType.COMPOSER_MODE.name
                        )
                    }
                }

                else -> {}
            }

            mArgument.getString(PinManager.GENRE_FILTER)?.let {
                putString(PinManager.GENRE_FILTER, it)
            }
        }
    }

    private fun onItemClick(pos: Int, doRefresh: Boolean) {
        SynoLog.d(LOG, "onItemClick : $pos, " +
                "mContentFrag: $mContentFrag, isEditMode: ${mContentFrag?.isEditMode()}")

        if (mContentFrag?.isEditMode() == true) return

        selPos = pos
        scrollPos = containerListAdapter.scrollToPosition

        mArgument.putInt("position", selPos)
        mArgument.putInt("scroll_to_position", scrollPos)

        val bundle = getEnumSongsBundle(
            containerListAdapter.data[pos]
        ).apply {
            putInt("position", pos)
            putInt("scroll_to_position", scrollPos)
        }

        when (mType) {
            Common.ContainerType.ARTIST_MODE,
            Common.ContainerType.GENRE_MODE,
            Common.ContainerType.COMPOSER_MODE,
            Common.ContainerType.SEARCH_ARTIST_MODE,
            Common.ContainerType.GENRE_ARTIST_MODE -> {
                bundle.putString("type", "container")
            }

            else -> {
                bundle.putString("type", PinManager.SONG)
            }
        }
        SynoLog.d(LOG, "isMobileLayout: ${StateManager.getInstance().isMobileLayout}, isLeft: $isLeft")
        if (StateManager.getInstance().isMobileLayout || !isLeft) {
            mContainerClickCallback.onContainerItemClick(bundle)
        } else {
            contentBundle = Bundle(bundle)

            bundle.putBoolean("left_pane", false)

            mContentFrag = newInstance(bundle, this, doRefresh)
        }
    }

    private fun setupViews() {
        mRefresh = mContentView.findViewById(R.id.refresh)
        mRefresh.setOnRefreshListener {
            if (loadContentWork == null || !loadContentWork!!.isWorking) {
                scrollPos = 0
                doRefresh()
            }
        }

        mRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        mEmptyView = mContentView.findViewById(R.id.content_empty)
        mEmptyTextView = mContentView.findViewById(R.id.tv_no_data)
        mEmptyImageView = mContentView.findViewById(R.id.icon_no_data)
        mLoadingView = mContentView.findViewById(R.id.content_progress)
        mFastScroller = mContentView.findViewById(R.id.fast_scroller)

        mRecyclerView = mContentView.findViewById(R.id.recycler_view)

        mRecyclerView.layoutManager = GridLayoutManager(
            context,
            getSpan(),
            RecyclerView.VERTICAL,
            false
        )

        mRecyclerView.adapter = containerListAdapter

        containerListAdapter.addEmptyView(mEmptyView, false)
        containerListAdapter.setFastScroller(mFastScroller)

        containerListAdapter.setOnItemClickListener { view, obj, position ->
            when (view.id) {
                R.id.shortcut -> {
                    getQuickAction(view, obj).show()
                }

                R.id.checkbox -> {
                    containerListAdapter.checkItem(position)
                    mContainerClickCallback.onUpdateTitle()
                }

                else -> {
                    this@ContainerFragment.onItemClick(position, false)
                }
            }
        }

        mTitleView = mContentView.findViewById(R.id.content_title)

        if (
            StateManager.getInstance().isMobileLayout && !TextUtils.isEmpty(mTitle)) {
            mTitleView.visibility = View.VISIBLE
            mTitleView.text = mTitle
        }

        mViewMode =
            if (!StateManager.getInstance().isMobileLayout && isLeft) {
                Common.PrefViewMode.LIST
            } else {
                AudioPreference.getViewMode()
            }

        onConfigurationChanged(resources.configuration)
    }

    private fun getQuickAction(anchor: View, item: Item): PopupMenu {
        return PopupMenu(requireContext(), anchor).apply {

            inflate(R.menu.file_song_menu)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {

                    R.id.ItemAction_PIN -> {
                        PinManager.getInstance().pin(
                            pinManager.getQuickActionTypeParamName(mType),
                            PinManager.getPinCriteria(mType, getEnumSongsBundle(item)),
                            item.title
                        )
                    }

                    R.id.ItemAction_UNPIN -> {
                        PinManager.getInstance().unpin(
                            PinManager.getInstance().getPinId(
                                pinManager.getQuickActionTypeParamName(mType),
                                PinManager.getPinCriteria(mType, getEnumSongsBundle(item))
                            )
                        )
                    }

                    else -> {
                        enumSongs(menuItem.itemId, item)
                    }
                }

                false
            }
            val canSupportPin = ConnectionManager.canSupportPin()
            SynoLog.d(LOG, "isOnline: $isOnline, canSupportPin: $canSupportPin")
            if (isOnline && canSupportPin) {
                val bundle = getEnumSongsBundle(item)
                if (pinManager.alreadyPin(pinManager.getQuickActionTypeParamName(mType), PinManager.getPinCriteria(mType, bundle))) {
                    menu.findItem(R.id.ItemAction_UNPIN).isVisible = true
                } else {
                    menu.findItem(R.id.ItemAction_PIN).isVisible = true
                }
            }

            menu.findItem(R.id.ItemAction_PLAY).isVisible = true
            menu.findItem(R.id.ItemAction_ADD_ITEM).isVisible = true

            if (ConnectionManager.canSupportAddToNext()) {
                menu.findItem(R.id.ItemAction_ADD_NEXT).isVisible = true
            }

            if (isOnline && mType.isAlbumType()) {
                if (Common.createPersonalPlaylist()) {
                    menu.findItem(R.id.ItemAction_ADDTO_PLAYLIST).isVisible = true
                }

                menu.findItem(R.id.ItemAction_DOWNLOAD).isVisible = true
            }
        }
    }

    private fun enumSongs(itemAction: Int, item: Item) {
        val progress = ProgressDialog(requireActivity()).apply {
            setMessage(getString(R.string.processing))
            setCancelable(false)
        }

        enumSongsWork = object : ThreadWork() {

            private var songList = arrayListOf<SongItem>()

            override fun preWork() {
                progress.show()
            }

            override fun onWorking() {
                try {
                    songList.addAll(
                        cacheMgr.doEnumContainerSongsForContainer(
                            isOnline,
                            mType,
                            getEnumSongsBundle(item),
                            -1,
                            true
                        ).itemList
                    )

                    if (itemAction == R.id.ItemAction_DOWNLOAD) {
                        songList = ArrayList(
                            songList.filter {
                                it.isFile &&
                                        Utilities.shouldManualDownload(it) &&
                                        !ServiceOperator.isDownloading(it)
                            }
                        )
                    }

                } catch (e: WebAPIErrorException) {
                    exception = e
                }
            }

            override fun postWork() {
                progress.dismiss()
            }

            override fun onComplete() {
                exception?.let {
                    handleError(it)
                    return
                }

                when (itemAction) {
                    R.id.ItemAction_DOWNLOAD -> {
                        downloadRemote(songList)
                    }

                    R.id.ItemAction_PLAY -> {
                        enqueueAction(Common.PlaybackAction.PLAY_NOW, 0, songList)
                    }

                    R.id.ItemAction_ADDTO_PLAYLIST -> { listPlaylistOption(songList) }

                    R.id.ItemAction_ADD_ITEM -> {
                        enqueueAction(Common.PlaybackAction.ADD_ONLY, 0, songList)
                    }

                    R.id.ItemAction_ADD_NEXT -> {
                        enqueueAction(Common.PlaybackAction.ADD_NEXT, 0, songList)
                    }
                }
            }
        }

        enumSongsWork?.startWork()
    }

    override fun toggleView() {
        if (this.isLeft && this.mContentFrag != null) {
            this.mContentFrag?.toggleView()
        } else if (!this.isLeft || StateManager.getInstance().isMobileLayout) {
            this.mViewMode = AudioPreference.getViewMode()
            showView(true)
        }
    }

    override fun onPageSelected() {
        if (this.isInitialized) {
            return
        }
        loadContent(this.blDoRefresh)
        this.isInitialized = true
        this.blDoRefresh = false
    }

    override fun allItemPlayAction(action: Common.ItemAction?) {
        if (!this.isLeft || this.mContentFrag == null) {
            return
        }
        this.mContentFrag?.allItemPlayAction(action)
    }

    override fun canLoadMore(): Boolean {
        return this.mItems.size < this.total
    }

    override fun canMultiEdit(): Boolean {
        if (!this.isLeft || this.mContentFrag == null) {
            return false
        }
        return this.mContentFrag?.canMultiEdit() == true
    }

    override fun canSetView(): Boolean {
        if (!this.isLeft || this.mContentFrag == null) {
            return ((!this.isLeft || StateManager.getInstance().isMobileLayout) && this.mItems.isNotEmpty())
        }
        return this.mContentFrag?.canSetView() == true
    }

    override fun getSelectedItems(): kotlin.collections.ArrayList<SongItem>? {
        if (this.isLeft || this.mContentFrag != null) {
            return this.mContentFrag?.getSelectedItems()
        }
        return ArrayList()
    }

    override fun isEditMode(): Boolean {
        if (!isLeft || this.mContentFrag == null) {
            return false
        }
        return this.mContentFrag?.isEditMode() == true
    }

    override fun isPlayable(): Boolean {
        return this.mContentFrag?.isPlayable() ?: false
    }

    override fun setEditMode(edit: Boolean) {
        if (!isLeft || this.mContentFrag == null) {
            return
        }
        this.mContentFrag?.setEditMode(edit)
    }

    override fun loadContent(refresh: Boolean) {
        loadContentWork?.let {
            if (it.isWorking) {
                it.endThread()
            }
        }

        loadContentWork = object : ThreadWork() {

            var connectionInfo =
                Common.ConnectionInfo.ERROR_NETWORK

            lateinit var retItems: List<Item>

            override fun preWork() {
                if (refresh) {
                    page = 0
                    mItems.clear()
                    initSpecialAllSongsItem()
                }

                page++

                if (page == 1) {
                    setNoDataView()
                    setRefreshing(true)
                }
            }

            override fun onWorking() {
                try {
                    val itemSet =
                        cacheMgr.doEnumContainerForContainer(
                            isOnline,
                            mType,
                            mArgument,
                            page,
                            refresh
                        )

                    retItems = itemSet.itemList as List<Item>
                    total = itemSet.total

                    if (mType == Common.ContainerType.LATEST_ALBUM_MODE) {
                        total = 50
                    }

                    connectionInfo =
                        Common.ConnectionInfo.SUCCESS

                } catch (e: WebAPIErrorException) {
                    exception = e
                }
            }

            override fun postWork() {
                if (page == 1) {
                    setRefreshing(false)
                }
            }

            override fun onComplete() {
                if (exception != null) {
                    handleError(exception!!)
                    if (containerListAdapter.realItemCount == 0) {
                        containerListAdapter.setData(null)
                        return
                    }
                    return
                }
                handleLoadComplete(refresh, connectionInfo, retItems)
            }
        }

        loadContentWork?.startWork()
    }

    private fun handleLoadComplete(refresh: Boolean, connectionInfo: Common.ConnectionInfo, retItems: List<Item>) {
        if (connectionInfo != Common.ConnectionInfo.SUCCESS) return

        mItems.addAll(retItems)

        containerListAdapter.setIsOnline(isOnline)
        containerListAdapter.setContainerType(mType)
        containerListAdapter.setData(mItems)

        showView(true)

        mContainerClickCallback.onUpdateTitle()
        mContainerClickCallback.onFinishLoading(mType, total)

        if (!StateManager.getInstance().isMobileLayout
            && isLeft
            && mItems.isNotEmpty()
        ) {
            val target = selPos.coerceAtLeast(0)
                .coerceAtMost(mItems.lastIndex)

            onItemClick(target, refresh)
        }

        if (canLoadMore() && retItems.isNotEmpty()) {
            loadContent(false)
        }
    }

    override fun markAllItem(mark: Boolean) {
        mContentFrag?.markAllItem(mark)
    }

    override fun onScrollToBottom(view: AbsListView?) {
        if ((loadContentWork == null || loadContentWork?.isWorking == true) && canLoadMore()) {
            loadContent(false)
        }
    }

    private fun showView(show: Boolean) {
        containerListAdapter.setIsListMode(
            Common.PrefViewMode.LIST == mViewMode
        )

        mRecyclerView.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    override fun getBundleStack(): Stack<Bundle>? {
        return this.mContainerClickCallback.getBundleStack()
    }

    override fun onContainerItemClick(bundle: Bundle) {
        this.contentBundle?.putInt("position", bundle.getInt("position"))
        this.contentBundle?.putInt("scroll_to_position", bundle.getInt("scroll_to_position"))
        this.mContainerClickCallback.onContainerItemClick(this.contentBundle as Bundle)
    }

    override fun onUpdateTitle() {
        this.mContainerClickCallback.onUpdateTitle()
    }

    override fun onFinishLoading(type: Common.ContainerType, size: Int) { }
    override fun scrollToTop() {
        if (this.mRecyclerView != null) {
            val i: Int = if (StateManager.getInstance().isMobileLayout) 20 else 0
            if (getScrollToPosition() >= i) {
                this.mRecyclerView.scrollToPosition(i)
            }
            this.mRecyclerView.smoothScrollToPosition(0)
        }
    }
}