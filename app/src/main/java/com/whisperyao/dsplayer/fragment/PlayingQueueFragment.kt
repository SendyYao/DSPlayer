package com.whisperyao.dsplayer.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.whisperyao.dsplayer.App
// import com.google.firebase.analytics.FirebaseAnalytics
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.adapters.AbsAdapter
import com.whisperyao.dsplayer.adapters.PlayingQueueAdapter
import com.whisperyao.dsplayer.adapters.SimpleItemTouchHelperCallback
import com.whisperyao.dsplayer.databinding.PlayingqFragmentBinding
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.mediasession.service.AbstractMediaBrowserService
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.net.WebAPI
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.SynoLog
import dagger.android.support.DaggerFragment
import javax.inject.Inject;


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
    private var selectModeAdapter: ContentFragment.SelectModeAdapter? = null

    private var selectedItemCount = 0
    private var firstLoad = true

    private val adapter = PlayingQueueAdapter(this)

    fun bindController(controller: MediaControllerCompat) {
        this.controller = controller
    }

    fun setInitialPlaybackState(state: PlaybackStateCompat) {
        adapter.setPlayingStatus(state)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        playingQueueManager.observeQueueChanged(this) { changed ->
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



    private fun loadContent() {
        SynoLog.d(TAG, "loadContent")

        adapter.setData(playingQueueManager.getQueue())

        updateTrackInfo(playingQueueManager.getPlayIndex(), true)

//        invalidateOptionsMenuState()
    //        setRefreshing(false)
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

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
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
                        true
                    }

                    R.id.ItemAction_SHARING -> {
//                        shareSingle(song)
                        true
                    }

                    else -> false
                }
            }
        }
    }

    fun updateTrackInfo(
        position: Int,
        force: Boolean = false
    ) {
        if (adapter.setNowPlayingQueueId(position) || force) {
            binding?.playlist?.post {
                layoutManager?.scrollToPosition(position)
            }
        }
    }

    override fun onItemSelected(count: Int) {
        TODO("Not yet implemented")
    }

    override fun onTrackOrderChanged(playingPos: Int) {
        SynoLog.d(TAG, "onTrackOrderChanged");

    }
}