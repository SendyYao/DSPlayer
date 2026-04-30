package com.whisperyao.dsplayer.fragment

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.synology.ThreadWork
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.RecyclerViewCallback
import com.whisperyao.dsplayer.UDCEvent
import com.whisperyao.dsplayer.activity.BaseActivity
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.datasource.network.api.BaseWebApi
import com.whisperyao.dsplayer.dialog.DialogHelper
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.net.WebAPI
import com.whisperyao.dsplayer.net.WebAPIErrorException
import com.whisperyao.dsplayer.playing.PlayingStatusManager.OnPlayerLocalityChangedObserver
import com.whisperyao.dsplayer.publicsharing.fragment.EditPlaylistFragment
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utilities
import com.whisperyao.dsplayer.util.Utils
import com.whisperyao.dsplayer.widget.ReSelectableSpinner
import com.whisperyao.dsplayer.widget.SynoFastScroller
import dagger.android.support.DaggerFragment
import java.util.Stack


abstract class ContentFragment() : DaggerFragment(), EditPlaylistFragment.Callbacks, RecyclerViewCallback {
    abstract fun toggleView()

    protected abstract fun onPageSelected()

    abstract fun allItemPlayAction(action: Common.ItemAction?)

    protected abstract fun canLoadMore(): Boolean

    abstract fun canMultiEdit(): Boolean

    abstract fun canSetView(): Boolean

    abstract fun getSelectedItems(): ArrayList<SongItem>?

    abstract fun isEditMode(): Boolean

    open fun isReorderable(): Boolean {
        return false
    }

    abstract fun isPlayable(): Boolean

    abstract fun setEditMode(edit: Boolean)

    protected open fun setToNeedRefresh() { }

    abstract fun loadContent(refresh: Boolean)

    abstract fun markAllItem(mark: Boolean)

    protected abstract fun onScrollToBottom(view: AbsListView?)

    protected inner class MyActionMode : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mActivity.menuInflater.inflate(R.menu.edit_menu, menu)

            val view = layoutInflater.inflate(R.layout.action_mode_spinner, null)
            val spinner = view.findViewById<ReSelectableSpinner>(R.id.spinner)

            mSelectedItemSum = getSelectedItems()?.size ?: 0

            mSelectModeAdapter = SelectModeAdapter(
                mActivity,
                R.layout.action_mode_spinner_item,
                arrayOf(
                    getString(R.string.select_all),
                    getString(R.string.deselect_all)
                )
            )

            mSelectModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = mSelectModeAdapter

            mode.customView = view

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    when (position) {
                        0 -> markAllItem(true)
                        1 -> markAllItem(false)
                    }
                    mSelectModeAdapter.notifyDataSetChanged()
                }
            }

            mRefresh?.isEnabled = false
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {

            if (isOnline) {
                if (!mType.isPlaylistType() ||
                    !mType.isNormalPlaylistType() ||
                    mType.isPersonalPlaylistType() && !Common.editPersonalPlaylist() ||
                    mType.isSharedPlaylistType() && !Common.editSharedPlaylist()) {
                    menu.findItem(R.id.editmenu_delete).isVisible = false
                }

                if (!Common.createPersonalPlaylist()) {
                    menu.findItem(R.id.editmenu_add_to_playlist).isVisible = false
                }

            } else {
                menu.findItem(R.id.editmenu_add_to_playlist).isVisible = false
                menu.findItem(R.id.editmenu_download).isVisible = false
            }

            // ConnectionManager.canSupportAddToNext()
            menu.findItem(R.id.editmenu_add_to_next).isVisible = true


            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {

            if (item.itemId == R.id.editmenu_group_play) return true

            val bundle = Bundle()
            val selectedItems = getSelectedItems()

            if (selectedItems?.isNotEmpty() ?: false) {
                when (item.itemId) {

                    R.id.editmenu_add -> {
//                        enqueueAction(Common.PlaybackAction.ADD_ONLY, 0, selectedItems)
                    }

                    R.id.editmenu_add_to_next -> {
//                        enqueueAction(Common.PlaybackAction.ADD_NEXT, 0, selectedItems)
                    }

                    R.id.editmenu_add_to_playlist -> {
                        listPlaylistOption(selectedItems)
                        bundle.putString(UDCEvent.KEY_OPERATION, "add_to_playlist")
                    }

                    R.id.editmenu_delete -> {
                        AlertDialog.Builder(mActivity)
                            .setTitle(R.string.delete)
                            .setMessage(R.string.remove_select)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                deleteSelected(selectedItems)
                            }
                            .setNegativeButton(R.string.no, null)
                            .show()
                    }

                    R.id.editmenu_download -> {
                        mDialog = ProgressDialog(activity).apply {
                            setMessage(getString(R.string.processing))
                            setCancelable(false)
                        }

                        object : ThreadWork() {

                            val downloadList = ArrayList<SongItem>()

                            override fun preWork() {
                                mDialog.show()
                            }

                            override fun onWorking() {
                                for (song in selectedItems) {
                                    if (song.isFile && Utilities.shouldManualDownload(song) // !ServiceOperator.isDownloading(song)
                                    ) {
                                        downloadList.add(song)
                                    }
                                }
                            }

                            override fun onComplete() {
                                mDialog.dismiss()
//                                downloadRemote(downloadList)
                            }

                        }.startWork()

                        bundle.putString(UDCEvent.KEY_OPERATION, "download")
                    }

                    R.id.editmenu_play -> {
//                        enqueueAction(Common.PlaybackAction.PLAY_NOW, 0, selectedItems)
                        bundle.putString(UDCEvent.KEY_OPERATION, "play")
                    }

                    R.id.editmenu_rating -> {
//                        rateSongs(selectedItems)
                        bundle.putString(UDCEvent.KEY_OPERATION, "rate")
                    }

                    R.id.editmenu_share -> {
//                        shareSongs(selectedItems)
                        bundle.putString(UDCEvent.KEY_OPERATION, "share")
                    }
                }
            }

            setEditMode(false)

            if (!bundle.isEmpty) {
//                firebaseAnalyticsUtil.logEvent(
//                    UDCEvent.EVENT__OPERATION_MULTI_SELECT,
//                    bundle
//                )
            }

            return true
        }


        override fun onDestroyActionMode(mode: ActionMode) {
            mMode = null

            mActionModeCallback.leaveActionMode()

            mRefresh.isEnabled = true

            setEditMode(false)
        }
    }

    protected inner class MyOnScrollListener() : AbsListView.OnScrollListener {
        override fun onScroll(
            view: AbsListView?,
            firstVisibleItem: Int,
            visibleItemCount: Int,
            totalItemCount: Int
        ) { }

        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            if (view.lastVisiblePosition == view.count - 1) {
                SynoLog.d(LOG, "scroll to bottom")
                onScrollToBottom(view)
            }
        }
    }


    var title: String = "Home"
    var initBundle: Bundle? = null

    protected var isInitialized: Boolean = false

    @JvmField
    protected var blLoadContent: Boolean = false

    @JvmField
    protected var blDoRefresh: Boolean = false

    protected var blLoadContentFirstTime: Boolean = false

    @JvmField
    protected var isOnline: Boolean = false

    protected lateinit var mActionModeCallback: ActionModeCallback

    @JvmField
    protected var mArgument: Bundle = Bundle()

    @JvmField
    protected var mMode: ActionMode? = null
    @JvmField
    protected var mSubtitle: String? = null
    @JvmField
    protected var mRating = 1.0f
    protected lateinit var mPopup: AlertDialog
    @JvmField
    protected var blEditMode: Boolean = false
    protected var mIsVisibleToUser: Boolean = false
    protected var mVisibleHintCalled: Boolean = false

    protected var mPlaylistChangedListener: BroadcastReceiver? = null

    @JvmField
    protected var mTitle: String? = null

    protected lateinit var cacheMgr: CacheManager

    protected lateinit var mTitleView: TextView

    protected lateinit var mContainerClickCallback: ContentCallback

    protected lateinit var mLoadingView: View
    protected lateinit var mRefresh: SwipeRefreshLayout
    protected lateinit var mSelectModeAdapter: SelectModeAdapter

    @JvmField
    protected var loadContentWork: ThreadWork? = null

    @JvmField
    protected var total: Int = 0

    @JvmField
    protected var mType: Common.ContainerType = Common.ContainerType.HOMEPAGE_PIN_MODE

    protected lateinit var mContentView: View
    protected lateinit var mEmptyView: View
    protected lateinit var mEmptyTextView: TextView
    protected lateinit var mEmptyImageView: ImageView
    protected lateinit var mFastScroller: SynoFastScroller
    protected lateinit var mRecyclerView: RecyclerView
    private var mOberserver: OnPlayerLocalityChangedObserver? = null

    protected lateinit var mDialog: ProgressDialog

    @JvmField
    protected var mScrollState: Parcelable? = null
    protected var mGScrollState: Parcelable? = null

    interface ActionModeCallback {
        fun enterActionMode(actionModeCallback: ActionMode.Callback?): ActionMode?

        fun leaveActionMode()
    }

    companion object {
        const val DISPLAY_ARTIST = "display_artist"
        const val GENRE_FILTER = "genre_filter"
        const val GSCROLL_STATE = "grid_scroll_state"
        private const val LOG = "ContentFragment"
        const val SCROLL_STATE = "scroll_state"
        const val SONG = "song"

        var sDummyCallback: ContentCallback = object : ContentCallback {
            override fun getBundleStack(): Stack<Bundle>? = null
            override fun onContainerItemClick(bundle: Bundle) {
                SynoLog.d(LOG, "onContainerItemClick from sDummyCallback, bundle: $bundle")
            }
            override fun onFinishLoading(type: Common.ContainerType, size: Int) {}
            override fun onUpdateTitle() {}
        }

        lateinit var mActivity: AppCompatActivity

        @JvmField
        var mSelectedItemSum: Int = 0

        fun newInstance(arguments: Bundle, callback: ContentCallback): ContentFragment? {
            return newInstance(arguments, callback, false)
        }

        fun newInstance(arguments: Bundle, callback: ContentCallback, doRefresh: Boolean): ContentFragment? {

            val containerType = arguments
                .getString(Common.CONTAINER_TYPE)
                ?.let { Common.ContainerType.valueOf(it) }
            // 或者抛异常 / 给默认值

            val type = arguments.getString("type")

            SynoLog.d(LOG, "newInstanceType: $type : $containerType")

            val fragment: ContentFragment? = when (containerType) {

                Common.ContainerType.FOLDER_MODE -> {
                    FileSongFragment(callback, true, doRefresh)
                }

                Common.ContainerType.ARTIST_MODE,
                Common.ContainerType.GENRE_MODE,
                Common.ContainerType.COMPOSER_MODE,
                Common.ContainerType.ALBUM_MODE,
                Common.ContainerType.ARTIST_ALBUM_MODE,
                Common.ContainerType.GENRE_ALBUM_MODE,
                Common.ContainerType.GENRE_ARTIST_MODE,
                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE,
                Common.ContainerType.COMPOSER_ALBUM_MODE,
                Common.ContainerType.LATEST_ALBUM_MODE,
                Common.ContainerType.SEARCH_ARTIST_MODE,
                Common.ContainerType.SEARCH_ALBUM_MODE -> {

                    if (type == "container") {
                        ContainerFragment(callback, true, doRefresh)
                    } else {
                        ContainerSongFragment(callback, true, doRefresh)
                    }
                }

                Common.ContainerType.RANDOM100_MODE,
                Common.ContainerType.SEARCH_SONG_MODE -> {
                    ContainerSongFragment(callback, true, doRefresh)
                }

                Common.ContainerType.RADIO_MODE -> {
                     RadioFragment(callback, doRefresh)
                }

                Common.ContainerType.RATING_MODE -> {
                    if (type == "container") {
                         RatingFragment(callback, doRefresh)
                    } else {
                        ContainerSongFragment(callback, true, doRefresh)
                    }
                }

                Common.ContainerType.PLAYLIST_MODE -> {
                    SynoLog.d(LOG, "PlaylistMode callback: $callback")
                    PlaylistFragment(callback, doRefresh)
                }

                Common.ContainerType.PERSONAL_PLAYLIST_MODE,
                Common.ContainerType.SHARED_PLAYLIST_MODE,
                Common.ContainerType.PERSONAL_SMART_PLAYLIST_MODE,
                Common.ContainerType.SHARED_SMART_PLAYLIST_MODE -> {
                     PlaylistSongFragment(callback, doRefresh)
                }

                Common.ContainerType.HOMEPAGE_PIN_MODE -> {
                    arguments.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.HOMEPAGE_PIN_MODE.name
                    )
                    arguments.putString("type", "container")
                    HomePagePinsFragment(callback, true, doRefresh)
                }

                Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE -> {
                    arguments.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE.name
                    )
                    arguments.putString("type", "container")
                     HomePageDefaultGenreFragment(callback, true, doRefresh)
                }

                else -> null
            }

            fragment?.arguments = arguments
            return fragment
        }

        fun newInstance(online: Boolean, type: Common.ContainerType, callback: ContentCallback): ContentFragment {
            return newInstance(online, type, callback, true)
        }

        fun newInstance(online: Boolean, type: Common.ContainerType, callback: ContentCallback, loadContent: Boolean): ContentFragment {
            return newInstance(online, type, null, callback, loadContent)
        }

        fun newInstance(online: Boolean, type: Common.ContainerType, filterKey: String?, callback: ContentCallback, loadContent: Boolean): ContentFragment {

            SynoLog.i(LOG, "newInstance ${type.name} $online, callback: $callback")

            val bundle = Bundle().apply {
                putBoolean("mode", online)
            }

            val fragment: ContentFragment = when (type) {

                Common.ContainerType.SEARCH_SONG_MODE -> {
                    bundle.putString(Common.CONTAINER_TYPE, type.name)
                    bundle.putString("key", filterKey)
                    ContainerSongFragment(callback, loadContent)
                }

                Common.ContainerType.RADIO_MODE -> {
                    bundle.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.RADIO_MODE.name
                    )

                    RadioFragment(callback)
                }

                Common.ContainerType.PLAYLIST_MODE -> {
                    bundle.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.PLAYLIST_MODE.name
                    )

                    PlaylistFragment(callback)
                }

                Common.ContainerType.HOMEPAGE_PIN_MODE -> {
                    bundle.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.HOMEPAGE_PIN_MODE.name
                    )
                    bundle.putString("type", "container")

                    HomePagePinsFragment(callback, loadContent)
                }

                Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE -> {
                    bundle.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE.name
                    )
                    bundle.putString("type", "container")

                    HomePageDefaultGenreFragment(callback, loadContent)
                }

                Common.ContainerType.HOMEPAGE_TEST_MODE -> {
                    bundle.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE.name
                    )
                    bundle.putString("type", "container")
                    TestFragment()
                }

                Common.ContainerType.FOLDER_MODE -> {
                    bundle.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.FOLDER_MODE.name
                    )

                    FileSongFragment(callback, loadContent)
                }

                Common.ContainerType.ARTIST_MODE,
                Common.ContainerType.GENRE_MODE,
                Common.ContainerType.COMPOSER_MODE,
                Common.ContainerType.ALBUM_MODE,
                Common.ContainerType.ARTIST_ALBUM_MODE,
                Common.ContainerType.GENRE_ALBUM_MODE,
                Common.ContainerType.GENRE_ARTIST_MODE,
                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE,
                Common.ContainerType.COMPOSER_ALBUM_MODE,
                Common.ContainerType.LATEST_ALBUM_MODE -> {
                    bundle.putString(
                        Common.CONTAINER_TYPE,
                        type.name
                    )
                    bundle.putString("type", "container")

                    ContainerFragment(callback, loadContent)
                }

                Common.ContainerType.SEARCH_ARTIST_MODE,
                Common.ContainerType.SEARCH_ALBUM_MODE -> {
                    bundle.putString(
                        Common.CONTAINER_TYPE,
                        type.name
                    )
                    bundle.putString("type", "container")
                    bundle.putString("key", filterKey)

                    ContainerFragment(callback, loadContent)
                }

                else -> {
                    SynoLog.e(LOG, "unsupported type, id = $type")
                    throw IllegalArgumentException(
                        "Unsupported type: $type"
                    )
                }
            }

            fragment.arguments = bundle

            return fragment
        }
    }

    fun getInitialBundle(): Bundle? {
        return initBundle
    }

    protected fun isProtract(): Boolean {
        return getOrientation() === 1
    }

    protected fun getOrientation(): Int {
        val appCompatActivity: AppCompatActivity = mActivity
        return appCompatActivity.getResources().configuration.orientation
    }
    override fun getScrollToPosition(): Int {
        return 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mArgument = arguments as Bundle
        this.isInitialized = false

        initBundle = arguments

        setHasOptionsMenu(true)

        cacheMgr = CacheManager.getInstance()

        if (this.mArgument.containsKey(SCROLL_STATE)) {
            this.mScrollState = this.mArgument.getParcelable(SCROLL_STATE)
        }
        if (this.mArgument.containsKey(GSCROLL_STATE)) {
            this.mGScrollState = this.mArgument.getParcelable(GSCROLL_STATE)
        }
    }

    protected fun enqueueAction(
        action: Common.PlaybackAction,
        position: Int,
        songItems: ArrayList<SongItem>
    ) {
        enqueueAction(action, position, songItems, true)
    }

    protected fun enqueueAction(
        action: Common.PlaybackAction,
        position: Int,
        songItems: ArrayList<SongItem>,
        isFromMenu: Boolean
    ) {
        enqueueSongs(action, position, songItems, isFromMenu)
    }

    protected fun enqueueSongs(
        action: Common.PlaybackAction,
        position: Int,
        songList: List<SongItem>,
        isFromMenu: Boolean
    ) {
        mDialog = ProgressDialog(mActivity)
        val progressDialog = mDialog

        if (!progressDialog.isShowing) {
            @SuppressLint("StaticFieldLeak")
            object : AsyncTask<Void, Void, String>() {

                override fun onPreExecute() {
                    mDialog = ProgressDialog(mActivity).apply {
                        setMessage(getString(R.string.loading))
                        setCancelable(false)
                        show()
                    }
                }

                override fun doInBackground(vararg params: Void?): String {
                    val containerPlayer: BaseActivity.ContainerPlayer = mActivity as BaseActivity.ContainerPlayer

                    return if (containerPlayer.checkSize(action, songList)) {
                        containerPlayer.playContainer(
                            action,
                            songList,
                            position,
                            isFromMenu
                        )

                        var size = songList.size

                        if (Common.getPlayerStatusManager().isPlayModeChromeCast) {
                            size = minOf(size, 1000)
                        }

                        var message = resources.getString(R.string.add_songs_success)

                        if (action == Common.PlaybackAction.ADD_NEXT) {
                            message = if (size > 1) {
                                resources.getString(
                                    R.string.add_multi_songs_to_next_success
                                )
                            } else {
                                resources.getString(
                                    R.string.add_single_song_to_next_success
                                )
                            }
                        }

                        message.replace(Common.NUMBER, size.toString())
                    } else {
                        val freeSize = containerPlayer.getQueueFreeSize(action)

                        val partialList = ArrayList(songList.subList(0, freeSize))

                        if (partialList.isNotEmpty()) {
                            containerPlayer.playContainer(
                                action,
                                partialList,
                                position,
                                isFromMenu
                            )
                        }

                        containerPlayer.getOutOfCapacityString(
                            resources.getString(R.string.too_many_songs)
                        )
                    }
                }

                override fun onPostExecute(msg: String) {
                    mDialog?.dismiss()

                    if (mActivity == null || songList.isEmpty()) {
                        return
                    }

                    Toast.makeText(
                        mActivity,
                        msg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    protected fun deleteSelected(song: SongItem) =
        deleteSelected(listOf(song))

    open fun deleteSelected(songs: List<SongItem>) {
        mDialog = ProgressDialog(activity).apply {
            setMessage(getString(R.string.processing))
            setCancelable(false)
        }

        object : DeleteThreadWork(songs) {

            override fun preWork() {
                mDialog.show()
            }

            override fun onComplete() {
                mDialog.dismiss()

                if (!isOnline) {
                    sendDeleteFileBroadCast()
                }

                doRefresh()
            }
        }.startWork()
    }

    private fun sendDeleteFileBroadCast() {
        val intent = Intent(Common.ACTION_LOCAL_SONG_DELETED)
        intent.putExtra(Common.FRAGMENT_HASH, hashCode())
        LocalBroadcastManager.getInstance(mActivity).sendBroadcast(intent)
    }

    override fun onAttach(context: Context) {
        SynoLog.d(LOG, "onAttach")
        super.onAttach(context)
        val appCompatActivity: AppCompatActivity = context as AppCompatActivity
        mActivity = appCompatActivity
        if (appCompatActivity is ActionModeCallback) {
            this.mActionModeCallback = context as ActionModeCallback
        }
        Common.getPlayerStatusManager().registerOnPlayerLocalityChangedObserver(this.mOberserver);
    }

    override fun onDetach() {
        SynoLog.d(LOG, "${mType.name} onDetach")

        Common.getPlayerStatusManager().unregisterOnPlayerLocalityChangedObserver(mOberserver)

        // mPopup.takeIf { it.isShowing }?.dismiss()
        // mDialog.takeIf { it.isShowing }?.dismiss()

        loadContentWork?.takeIf { it.isWorking }?.endThread()
        // mActivity = null

        super.onDetach()
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        // PinManager.getInstance().onPrepareOptionsMenu(menu, mArgument)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val log = LOG

        SynoLog.d(log, "onOptionsItemSelected : ${item.title}")

        if (mActivity == null) {
            return super.onOptionsItemSelected(item)
        }

        val bundle = Bundle()

        when (item.itemId) {
            R.id.menu_add_all -> {
                SynoLog.d(log, "process : ${item.title}")
                allItemPlayAction(Common.ItemAction.ADD_ITEM)
                bundle.putString(
                    UDCEvent.KEY_PLAYBACK,
                    "add_to_queue"
                )
            }

            R.id.menu_edit -> {
                setEditMode(true)
            }

            R.id.menu_pin -> {
//                if (mType == Common.ContainerType.LATEST_ALBUM_MODE) {
//                    PinManager.getInstance().pin(
//                        PinManager.TYPE_RECENTLY_ADDED,
//                        hashMapOf(),
//                        mActivity!!.getString(mType?.stringId)
//                    )
//                } else {
//                    PinManager.getInstance().pin(
//                        PinManager.getOptionsMenuTypeParamName(mType, mArgument?.getString("type")),
//                        PinManager.getPinCriteria(mType, mArgument),
//                        getTitle()
//                    )
//                }

                bundle.putString(UDCEvent.KEY_MANAGE, WebAPI.WebApiPin.PIN)
            }

            R.id.menu_play_all -> {
                SynoLog.d(log, "process : ${item.title}")
                allItemPlayAction(Common.ItemAction.PLAY)

                bundle.putString(
                    UDCEvent.KEY_PLAYBACK,
                    "android_play_all"
                )
            }

            R.id.menu_unpin -> {
                if (mType == Common.ContainerType.LATEST_ALBUM_MODE) {
//                    PinManager.getInstance().unpin(
//                        PinManager.getInstance().getPinId(
//                            PinManager.TYPE_RECENTLY_ADDED,
//                            hashMapOf()
//                        )
//                    )
                } else {
//                    PinManager.getInstance().unpin(
//                        PinManager.getInstance().getPinId(
//                            PinManager.getOptionsMenuTypeParamName(
//                                mType,
//                                mArgument?.getString("type")
//                            ),
//                            PinManager.getPinCriteria(
//                                mType,
//                                mArgument
//                            )
//                        )
//                    )
                }

                bundle.putString(
                    UDCEvent.KEY_MANAGE,
                    WebAPI.WebApiPin.UNPIN
                )
            }

            R.id.menu_view -> {
                AudioPreference.setViewMode(AudioPreference.getViewMode().toggle())
                toggleView()
            }
        }

//        firebaseAnalyticsUtil.logEvent(UDCEvent.EVENT__OPERATION_SONG_LIST, bundle)

        return super.onOptionsItemSelected(item)
    }

    protected open fun setNoDataView() {
        // StateManager.getInstance().isMobileLayout() && this.mEmptyTextView != null && this.mEmptyView.getVisibility() == 0
        if (this.mEmptyView.visibility == 0) {
            // isProtrait()
            this.mEmptyImageView.visibility = if (true) View.VISIBLE else View.GONE
        }
        val textView: TextView? = this.mEmptyTextView
        if (textView != null) {
            if (!this.isOnline) {
                textView.setText(R.string.no_data_local)
            }
            else if (AudioPreference.getPersonalPref().equals(Common.PrefPersonal.PERSONAL)) {
                this.mEmptyTextView.setText(R.string.no_data_personal)
            } else {
                this.mEmptyTextView.setText(R.string.no_data)
            }
        }
    }

    protected fun setRefreshing(refresh: Boolean) {
        if (this.blLoadContentFirstTime) {
            val view: View = this.mLoadingView
            if (view != null) {
                if (refresh) {
                    view.visibility = View.VISIBLE
                } else {
                    view.visibility = View.GONE
                    this.blLoadContentFirstTime = false
                }
                val swipeRefreshLayout: SwipeRefreshLayout = this.mRefresh
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setEnabled(!refresh)
                    return
                }
                return
            }
            return
        }
        val swipeRefreshLayout2: SwipeRefreshLayout = this.mRefresh
        swipeRefreshLayout2.isRefreshing = refresh
    }

    protected open fun doRefresh() {
        loadContent(true)
    }

    protected fun handleError(error: WebAPIErrorException) {
        if (error.error == 105) {
//            EventBus.getDefault().post(ForceLogoutEvent())
        }
        val swipeRefreshLayout: SwipeRefreshLayout = mRefresh
        swipeRefreshLayout.isRefreshing = false
    }

    protected fun getSpan(): Int {
        val activity = activity ?: return 2
        val windowWidth =
            ((Utils.getWindowWidth(activity) / BaseWebApi.WEBAPI_ERR_NO_APP_PRIVILEGE) / activity.resources.displayMetrics.density).toInt()
        if (windowWidth > 1) {
            return windowWidth
        }
        return 2
    }

    protected fun listPlaylistOption(item: SongItem) {
        val arrayList: ArrayList<SongItem> = ArrayList()
        arrayList.add(item)
        listPlaylistOption(arrayList)
    }

    protected fun listPlaylistOption(items: ArrayList<SongItem>) {
        DialogHelper.listPlaylistOption(mActivity, getChildFragmentManager(), items);
    }

    protected fun shareSongs(songList: List<SongItem>) {
        if (songList.isEmpty()) {
            return
        }
        if (songList.size == 1) {
            SynoLog.d(LOG, "ShareSingle")
        }
        if (songList.size == 2) {
            SynoLog.d(LOG, "ShareMultiple")
        }
    }

    interface ContentCallback {
        fun getBundleStack(): Stack<Bundle>?

        fun onContainerItemClick(bundle: Bundle)

        fun onFinishLoading(type: Common.ContainerType, size: Int)

        fun onUpdateTitle()
    }

    constructor(callback: ContentCallback?) : this() {
        mSubtitle = ""
        mRating = -1.0f
        blEditMode = false
        blLoadContent = false
        blLoadContentFirstTime = true
        blDoRefresh = false
        mSelectedItemSum = 0
        mScrollState = null
        mGScrollState = null
        mIsVisibleToUser = false
        mVisibleHintCalled = false

        mContainerClickCallback = sDummyCallback

        mOberserver = object : OnPlayerLocalityChangedObserver {
            override fun onPlayerLocalityChanged() {
                val activity = this@ContentFragment.activity
                activity?.runOnUiThread {
                    if (this@ContentFragment.isOnline) return@runOnUiThread
                    this@ContentFragment.doRefresh()
                }
            }
        }

        mPlaylistChangedListener = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                SynoLog.d(LOG, "${this@ContentFragment.mType} onReceive : $action")

                if (Common.ACTION_PLAYLIST_CHANGED == action) {
                    val id = intent.getStringExtra("id")

                    SynoLog.d(LOG, "onReceive id = $id")

                    if (TextUtils.isEmpty(id)) {
                        if (Common.ContainerType.PLAYLIST_MODE == this@ContentFragment.mType) {
                            Common.gIsCreatNewPlaylist = false
                            this@ContentFragment.doRefresh()
                        }
                        return
                    }

                    if (this@ContentFragment.mType == null ||
                        !this@ContentFragment.mType!!.isPlaylistType() ||
                        this@ContentFragment.mArgument == null ||
                        id != this@ContentFragment.mArgument!!.getString("key")
                    ) {
                        return
                    }

                    this@ContentFragment.doRefresh()
                }
            }
        }

        if (callback != null) {
            mContainerClickCallback = callback
        }

        SynoLog.d(LOG, "callback: $callback")
    }

    class SelectModeAdapter : ArrayAdapter<String> {
        constructor(context: Context, textViewResourceId: Int, objects: Array<String>) : super(
            context,
            textViewResourceId,
            objects
        ) {
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val textView: TextView = if (convertView == null) {
                ContentFragment.mActivity.layoutInflater.inflate(
                    R.layout.action_mode_spinner_item,
                    null
                ) as TextView
            } else {
                convertView as TextView
            }
            val i: Int = mSelectedItemSum
            when (i) {
                0 -> {
                    textView.text =
                        R.string.multi_items.toString().replace("[__DELETE_COUNT__]", "0")
                }

                1 -> {
                    textView.setText(R.string.one_item)
                }

                else -> {
                    textView.text = R.string.multi_items.toString()
                        .replace("[__DELETE_COUNT__]", "" + mSelectedItemSum)
                }
            }
            return textView;

        }
    }

    override fun onUpdatePlaylist() {
        DialogHelper.notifyPlaylistChanged(activity, "")
    }

    override fun onResume() {
        super.onResume()
        // (!this.mVisibleHintCalled || this.mIsVisibleToUser) && (getContext() instanceof MainActivity)
        if (true) {
            (context as HomeActivity).addCallback(this)
        }
    }

    private open inner class DeleteThreadWork(
        songItems: List<SongItem>
    ) : ThreadWork() {

        private val songsToBeDeleted: List<SongItem> =
            ArrayList(songItems)

        override fun onWorking() {
            songsToBeDeleted.forEach {
                cacheMgr.deleteSong(it)
            }
        }
    }
}

