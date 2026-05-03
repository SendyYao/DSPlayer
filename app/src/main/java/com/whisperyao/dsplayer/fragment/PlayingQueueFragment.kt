package com.whisperyao.dsplayer.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
// import com.google.firebase.analytics.FirebaseAnalytics
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.adapters.AbsAdapter
import com.whisperyao.dsplayer.adapters.PlayingQueueAdapter
import com.whisperyao.dsplayer.adapters.SimpleItemTouchHelperCallback
import com.whisperyao.dsplayer.databinding.ActionModeSpinnerBinding
import com.whisperyao.dsplayer.databinding.PlayingqFragmentBinding
import com.whisperyao.dsplayer.dialog.DialogHelper
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.mediasession.service.AbstractMediaBrowserService
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.net.WebAPI
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.SynoLog
import dagger.android.support.DaggerFragment
import javax.inject.Inject


class PlayingQueueFragment : DaggerFragment(), AbsAdapter.Callback {

    companion object {
        private const val TAG = "PlayingQueueFragment"
    }

    private var binding: PlayingqFragmentBinding? = null
    private var controller: MediaControllerCompat? = null

    var injectContext: Context = App.getContext()

    @Inject
    lateinit var playingQueueManager: PlayingQueueManager

    @Inject
    lateinit var playingStatusManager: PlayingStatusManager

    private var layoutManager: LinearLayoutManager? = null
    private var actionMode: ActionMode? = null
    private var reorderMode: ActionMode? = null
    private var selectModeAdapter: SelectModeAdapter? = null

    private var selectedItemCount = 0
    private var firstLoad = true

    private val adapter = PlayingQueueAdapter(this)

    fun bindController(controller: MediaControllerCompat) {
        this.controller = controller
    }

    fun setInitialPlaybackState(state: PlaybackStateCompat) {
        adapter.setPlayingStatus(state)
    }

    private val mRefresh: SwipeRefreshLayout?
        get() = binding?.refresh

    private val mLoadingView: View?
        get() = binding?.contentProgress?.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        playingQueueManager.observeQueueChanged(this) { _ ->
            if (!adapter.isCheckMode &&
                !adapter.isDragMode
            ) {
                loadContent()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PlayingqFragmentBinding.inflate(
            inflater,
            container,
            false
        )
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        updateTrackInfo(playingQueueManager.getPlayIndex(), false)
    }

    override fun onPause() {
        actionMode?.finish()
        actionMode = null

        reorderMode?.finish()
        reorderMode = null

        super.onPause()
    }

    private fun setupViews() {
        binding?.refresh?.setOnRefreshListener {
            loadContent()
        }

        binding?.refresh?.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        val itemTouchHelper = ItemTouchHelper(
            SimpleItemTouchHelperCallback(adapter)
        )

        adapter.addEmptyView(binding?.playingqEmpty, false)
        adapter.setIsListMode(true)
        adapter.setTouchHelper(itemTouchHelper)

        adapter.setOnItemClickListener { view, item, position ->
            val song = item as SongItem

            if (view.id == R.id.SongItemShortCut) {
                getQuickAction(view, song, position).show()
            } else {
                onItemClick(position)
            }
        }

        layoutManager = LinearLayoutManager(
            injectContext,
            RecyclerView.VERTICAL,
            false
        )

        binding?.playlist?.apply {
            layoutManager = this@PlayingQueueFragment.layoutManager
            adapter = this@PlayingQueueFragment.adapter
        }

        binding?.fastScroller?.fastScroller?.let {
            adapter.setFastScroller(it)
        }

        itemTouchHelper.attachToRecyclerView(
            binding?.playlist
        )

        loadContent()
    }
    private fun setRefreshing(refresh: Boolean) {
        if (firstLoad) {

            mLoadingView?.let { view ->
                view.visibility = if (refresh) View.VISIBLE else View.GONE

                if (!refresh) {
                    firstLoad = false
                }

                mRefresh?.isEnabled = !refresh
                return
            }
        }

        mRefresh?.isRefreshing = refresh
    }

    private fun loadContent() {
        SynoLog.d(TAG, "loadContent")

        adapter.setData(playingQueueManager.getQueue())

        updateTrackInfo(playingQueueManager.getPlayIndex(), true)

        invalidateOptionsMenu()
        setRefreshing(false)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        invalidateOptionsMenu()
        super.onPrepareOptionsMenu(menu)
    }

    private fun invalidateOptionsMenu() {
        val playerFragment = parentFragment as? PlayerFragment ?: return
        val menu = playerFragment.menu

        val size = adapter.data.size

        val canSave = !playingStatusManager.player.isPlayModeChromeCast() &&
                Common.createPersonalPlaylist() &&
                size > 0

        val canReorder = playingStatusManager.isPlayModeStreaming ||
                ConnectionManager.isUseWebAPI()

        menu.findItem(R.id.menu_reoder)?.isVisible = canReorder && size > 0
        menu.findItem(R.id.menu_edit)?.isVisible = size > 0
        menu.findItem(R.id.menu_save)?.isVisible = canSave
        menu.findItem(R.id.menu_refresh)?.isVisible = false
        menu.findItem(R.id.menu_delete_all)?.isVisible = size > 0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.menu_delete_all -> {
                AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.playing_queue)
                    .setMessage(R.string.remove_all)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        controller?.transportControls?.sendCustomAction(
                            AbstractMediaBrowserService.CUSTOM_ACTION_REMOVE_ALL,
                            null
                        )
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
                true
            }

            R.id.menu_edit -> {
                setEditMode(true)
                true
            }

            R.id.menu_refresh -> {
                loadContent()
                true
            }

            R.id.menu_reoder -> {
                setReorderMode(true)
                true
            }

            R.id.menu_save -> {
                createPlaylist()
                loadContent()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startSupportActionMode(mode: ActionMode.Callback): ActionMode? {
        val parentFragment: Fragment? = parentFragment
        val playerFragment: PlayerFragment? = parentFragment as? PlayerFragment
        return playerFragment?.startSupportActionMode(mode)
    }

    fun updatePlaybackStatus(playbackState: PlaybackStateCompat?) {
        if (playbackState != null) {
            this.adapter.setPlayingStatus(playbackState)
        }
    }

    private fun doRemoveSong(ids: IntArray) {
        val bundle = Bundle().apply {
            putIntArray("ids", ids)
        }

        controller?.transportControls?.sendCustomAction(
            AbstractMediaBrowserService.CUSTOM_ACTION_REMOVE_BYID,
            bundle
        )
    }

    private fun onItemClick(index: Int) {
        if (adapter.isDragMode || adapter.isCheckMode ) return

        playingQueueManager.setPlayIndex(index)

        controller?.transportControls
            ?.skipToQueueItem(index.toLong())
    }

    private fun removeSongs(ids: IntArray) {
        val bundle = Bundle().apply {
            putIntArray("ids", ids)
        }

        controller?.transportControls
            ?.sendCustomAction(
                AbstractMediaBrowserService.CUSTOM_ACTION_REMOVE_BYID,
                bundle
            )
    }

    private fun getQuickAction(anchor: View, song: SongItem, position: Int): PopupMenu {
        return PopupMenu(injectContext, anchor).apply {
            inflate(R.menu.playing_queue_menu)

            if (ConnectionManager.canShareSong(true, song)) {
                menu.findItem(R.id.ItemAction_SHARING)?.isVisible = true
            }

            if (Common.getDsId() == song.dsId && Common.createPersonalPlaylist()) {
                menu.findItem(R.id.ItemAction_ADDTO_PLAYLIST)?.isVisible = true
            }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.ItemAction_ADDTO_PLAYLIST -> {
                        if (Common.getDsId() != song.dsId || !Common.createPersonalPlaylist()) {
                            return@setOnMenuItemClickListener false
                        }

                        val list = arrayListOf(song)
                        DialogHelper.listPlaylistOption(
                            injectContext,
                            childFragmentManager,
                            list
                        )
                        false
                    }

                    R.id.ItemAction_DELETE -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.delete)
                            .setMessage(R.string.remove_select)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                removeSongs(intArrayOf(position))
                            }
                            .setNegativeButton(
                                R.string.no,
                                null
                            )
                            .show()
                        false
                    }

                    R.id.ItemAction_SHARING -> {
                        if (Common.getDsId() != song.dsId || !Common.createPersonalPlaylist()) {
                            return@setOnMenuItemClickListener false
                        }

                        val list = arrayListOf(song)
                        shareSongs(list)
                        false
                    }

                    else -> true
                }
            }
        }
    }

    fun updateTrackInfo(position: Int, force: Boolean = false) {
        if (adapter.setNowPlayingQueueId(position) || force) {
            binding?.playlist?.post {
                layoutManager?.scrollToPosition(position)
            }
        }
    }

    private fun shareSongs(songList: List<SongItem>) {
        val size = songList.size
        if (size != 0) {
            if (size == 1) {
                shareSingle(songList[0])
            } else {
                shareMultiple(songList)
            }
        }
    }

    private fun shareSingle(song: SongItem) {
        DialogHelper.shareSong(childFragmentManager, song)
    }

    private fun shareMultiple(songList: List<SongItem>) {
        DialogHelper.createPlaylist(childFragmentManager, songList, false, true)
    }

    private fun setEditMode(set: Boolean) {
        if (set) {
            selectedItemCount = 0
            adapter.setIsCheckMode(true)
            actionMode = startSupportActionMode(MyActionMode())
            updateModeMenu()
        } else {
            selectedItemCount = 0
            adapter.setIsCheckMode(false)
            actionMode?.finish()
            actionMode = null
        }
        adapter.notifyDataSetChanged()
    }

    private fun setReorderMode(set: Boolean) {
        val actionModeStartSupportActionMode: ActionMode?
        if (set) {
            adapter.setIsDragMode(true)
            actionModeStartSupportActionMode = startSupportActionMode(ReorderMode())
        } else {
            adapter.setIsDragMode(false)
            reorderMode?.finish()
            actionModeStartSupportActionMode = null
        }
        reorderMode = actionModeStartSupportActionMode
        adapter.notifyDataSetChanged()
    }

    private fun createPlaylist() {
        val list = ArrayList(adapter.data.filter { it.dsId == Common.getDsId() })

        DialogHelper.createPlaylist(
            childFragmentManager,
            list,
            false
        )
    }

    private fun updateModeMenu() {
        val actionMode = actionMode ?: return
        val menu = actionMode.menu ?: return

        val arrayList = ArrayList(adapter.selectedItems)
        val hasSelection = selectedItemCount > 0

        val canShareAll = arrayList.all {
            ConnectionManager.canShareSong(true, it)
        }

        val isAllSameDs = arrayList.all {
            Common.getDsId() == it.dsId
        }

        menu.findItem(R.id.editmenu_sharing_share)?.apply {
            isVisible = ConnectionManager.canSharePlaylist(true)
            isEnabled = hasSelection && canShareAll
        }

        menu.findItem(R.id.editmenu_delete)?.apply {
            isEnabled = hasSelection
        }

        menu.findItem(R.id.editmenu_add_to_playlist)?.apply {
            isVisible = Common.createPersonalPlaylist()
            isEnabled = hasSelection && isAllSameDs
        }
    }

    override fun onItemSelected(count: Int) {
        selectedItemCount = count
        updateModeMenu()
        if (selectModeAdapter != null) {
            selectModeAdapter?.notifyDataSetChanged()
        }
    }
    override fun onTrackOrderChanged(playingPos: Int) {
        SynoLog.d(TAG, "onTrackOrderChanged")
    }

    inner class MyActionMode : ActionMode.Callback {

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return true
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val activity = this@PlayingQueueFragment.activity ?: return true

            activity.menuInflater.inflate(R.menu.playingq_edit_menu, menu)
            mRefresh?.isEnabled = false

            val binding = ActionModeSpinnerBinding.inflate(activity.layoutInflater)

            val spinner = binding.spinner

            val selectAll = getString(R.string.select_all)
            val deselectAll = getString(R.string.deselect_all)

            selectModeAdapter = SelectModeAdapter(
                activity,
                arrayOf(selectAll, deselectAll)
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            spinner.adapter = selectModeAdapter
            mode.customView = binding.root

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    when (position) {
                        0 -> {
                            adapter.checkAll()
                            selectModeAdapter?.notifyDataSetChanged()
                        }

                        1 -> {
                            adapter.unCheckAll()
                            selectModeAdapter?.notifyDataSetChanged()
                        }
                    }
                }
            }

            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {

                R.id.editmenu_add_to_playlist -> {
                    if (selectedItemCount > 0) {
                        DialogHelper.listPlaylistOption(
                            injectContext,
                            childFragmentManager,
                            ArrayList(adapter.selectedItems)
                        )
                    }
                }

                R.id.editmenu_delete -> {
                    if (selectedItemCount > 0) {
                        val selectedPositions = adapter.selectedPositions

                        AlertDialog.Builder(injectContext)
                            .setTitle(R.string.playing_queue)
                            .setMessage(R.string.remove_select)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                doRemoveSong(selectedPositions)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }

                R.id.editmenu_sharing_share -> {
                    if (selectedItemCount > 0) {
                        shareSongs(ArrayList(adapter.selectedItems))
                    }
                }
            }

            actionMode?.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            setEditMode(false)
            mRefresh?.isEnabled = true
            adapter.notifyDataSetChanged()
        }
    }

    inner class ReorderMode : ActionMode.Callback {

        private var orderNeedToBeReset = true

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            activity?.menuInflater?.inflate(R.menu.playingq_edit_menu_drag, menu)
            mRefresh?.isEnabled = false
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId == R.id.editmenu_ok) {
                orderNeedToBeReset = false

                val holdSongIndex = adapter.holdSongIndex
                if (holdSongIndex.isEmpty()) return true

                var start = 0
                var end = holdSongIndex.size - 1

                // 从前往后找第一个不一致的位置
                while (start < end) {
                    val value = holdSongIndex[start]
                    if (value == null || value != start) break
                    start++
                }

                // 从后往前找第一个不一致的位置
                while (start < end) {
                    val value = holdSongIndex[end]
                    if (value == null || value != end) break
                    end--
                }

                if (start < end) {
                    val count = end - start + 1

                    val subList = ArrayList<Int>(count).apply {
                        for (i in 0 until count) {
                            add(holdSongIndex[start + i])
                        }
                    }

                    val bundle = Bundle().apply {
                        putInt("start", start)
                        putInt(WebAPI.WebApiPin.LIMIT, count)
                        putIntegerArrayList("ids", subList)
                    }

                    controller?.transportControls?.sendCustomAction(
                        AbstractMediaBrowserService.CUSTOM_ACTION_REORDER,
                        bundle
                    )
                }

                SynoLog.d(
                    TAG,
                    "start = $start, end = $end, mHoldSongIndex = $holdSongIndex"
                )
            }

            reorderMode?.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            reorderMode = null

            if (orderNeedToBeReset) {
                adapter.undoOrder()
            }

            adapter.applyOrder()
            adapter.notifyDataSetChanged()

            mRefresh?.isEnabled = true

            setReorderMode(false)
            loadContent()
        }
    }

    inner class SelectModeAdapter(context: Context, objects: Array<String>) : ArrayAdapter<String>(context, R.layout.action_mode_spinner_item, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val textView = (convertView as? TextView)
                ?: layoutInflater.inflate(
                    R.layout.action_mode_spinner_item,
                    parent,
                    false
                ) as TextView

            val count = selectedItemCount

            textView.text = when (count) {
                0 -> getString(R.string.multi_items)
                    .replace("[__DELETE_COUNT__]", "0")

                1 -> getString(R.string.one_item)

                else -> getString(R.string.multi_items)
                    .replace("[__DELETE_COUNT__]", count.toString())
            }

            return textView
        }
    }
}