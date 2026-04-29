package com.whisperyao.dsplayer.fragment

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.StateManager
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.activity.PlayerChooser
import com.whisperyao.dsplayer.databinding.PlayerBinding
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.playing.Player
import com.whisperyao.dsplayer.playing.PlayerControlHelper
import com.whisperyao.dsplayer.playing.PlayingSongDetailHelper
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.SynoLog
import dagger.android.support.DaggerAppCompatActivity
import dagger.android.support.DaggerDialogFragment
import jakarta.inject.Provider
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerFragment @Inject constructor() : DaggerDialogFragment() {

    companion object {
        const val TAG = "PlayerFragment"
    }

    enum class Mode {
        LYRIC,
        QUEUE;

        fun toggle(): Mode {
            return if (this == QUEUE) LYRIC else QUEUE
        }
    }

    private var binding: PlayerBinding? = null
    private lateinit var delegateForActionMode: AppCompatDelegate

    private lateinit var rootViewContainer: FrameLayout
    private lateinit var titleView: TextView

    private var mSongDetailHelper = PlayingSongDetailHelper()
    private var viewMode = Mode.LYRIC

    private var lyricFragment: LyricFragment? = null
    @Inject
    lateinit var lyricFragmentProvider: Provider<LyricFragment>

    @Inject
    lateinit var mPlayerControlHelper: PlayerControlHelper

    @Inject
    lateinit var playerStatusManager: PlayingStatusManager

    @Inject
    lateinit var playingQueueManager: PlayingQueueManager
    private var queueFragment: PlayingQueueFragment? = null

    @Inject
    lateinit var queueFragmentProvider: Provider<PlayingQueueFragment>

    private val keyEventListener = DialogInterface.OnKeyListener { _, keyCode, event ->
        onDialogKeyEvent(keyCode, event)
    }

    private val mPlayerSetObserver = object : PlayingStatusManager.PlayerSetObserver {
            override fun onPlayerChange(player: Player) {
                lifecycleScope.launch(Dispatchers.Main) {
                    updateTitle()
                }
            }

            override fun onPlayerSetChanged() {
                lifecycleScope.launch(Dispatchers.Main) {
                    updateTitle()
                }
            }
        }

    private val toolbar: Toolbar
        get() = binding!!.toolbar

    private val menu: Menu
        get() = toolbar.menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_FRAME, R.style.Player_FullScreen)

        mPlayerControlHelper.onCreate()

        playingQueueManager.getProgress().observe(this) { progress ->
            mPlayerControlHelper.updateSeekBar(
                progress.progress,
                progress.buffer
            )

            lyricFragment?.setTimeLine(progress.progress)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.let { dialog ->
            dialog.setOnKeyListener(keyEventListener)

            val activity = activity
            if (activity is DaggerAppCompatActivity) {
                delegateForActionMode =
                    AppCompatDelegate.create(dialog, activity)
            }
        }

        rootViewContainer = FrameLayout(inflater.context)

        binding = PlayerBinding.inflate(
            inflater,
            container,
            false
        )

        rootViewContainer.addView(binding!!.root)

        return rootViewContainer
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        rootViewContainer.removeAllViews()

        binding = PlayerBinding.inflate(
            layoutInflater,
            null,
            false
        )

        rootViewContainer.addView(binding!!.root)

        viewCreatedImpl(rootViewContainer)
        setMode(viewMode)
    }

    fun onPlaybackStateChanged(playbackState: PlaybackStateCompat?) {
        SynoLog.d("PlayerFragment", "onPlaybackStateChanged, playbackState: ${playbackState.toString()}, queueFragment: $queueFragment")
        mPlayerControlHelper.updateAll()
        if (playbackState == null || this.queueFragment == null) {
            return
        }
        queueFragment!!.updatePlaybackStatus(playbackState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewCreatedImpl(view)

        setMode(Mode.LYRIC)

        playingQueueManager.observeCurrentSong(viewLifecycleOwner) {
            queueFragment?.updateTrackInfo(it.position)
            lyricFragment?.updateTrackInfo(it.songItem)
            mPlayerControlHelper.updateAll()
        }
    }

    private fun viewCreatedImpl(view: View) {
        initToolbar()
        setInitialSpinnerAdapter()
        loadPlayerChooser()
        setupViews(view)
        updateTrackInfo()
    }

    override fun onDestroyView() {
        dialog?.setOnKeyListener(null)
        super.onDestroyView()
    }

    private fun initToolbar() {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white)

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        toolbar.overflowIcon = ContextCompat.getDrawable(
            toolbar.context,
            R.drawable.ic_more_vert_white
        )

        toolbar.inflateMenu(R.menu.player_menu)

        val clickListener =
            MenuItem.OnMenuItemClickListener {
                onOptionsItemSelected(it)
            }

        val ids = listOf(
            R.id.menu_lyric_queue,
            R.id.menu_sleep_timer,
            R.id.menu_volume,
            R.id.menu_edit,
            R.id.menu_reoder,
            R.id.menu_save,
            R.id.menu_refresh,
            R.id.menu_delete_all,
            R.id.menu_setting
        )

        ids.forEach { id ->
            menu.findItem(id)?.setOnMenuItemClickListener(clickListener)
        }

        invalidateOptionsMenu()
    }

    @Deprecated("onPrepareOptionsMenu is deprecated")
    override fun onPrepareOptionsMenu(menu: Menu) {
        invalidateOptionsMenu()
    }

    private fun invalidateOptionsMenu() {
        val isQueue = viewMode == Mode.QUEUE

        menu.findItem(R.id.menu_lyric_queue).setIcon(
            if (isQueue) R.drawable.tool_lyrics
            else R.drawable.tool_playingq
        )

        menu.findItem(R.id.menu_sleep_timer).isVisible =
            StateManager.getInstance().isMobileLayout

        if (isQueue) {
            menu.findItem(R.id.menu_volume).isVisible = false
        } else {
            menu.findItem(R.id.menu_volume).apply {
                setIcon(
                    if (playerStatusManager.player.isGroupPlayer)
                        R.drawable.tool_volume2
                    else
                        R.drawable.tool_volume
                )

                isVisible = StateManager.getInstance().isMobileLayout && playerStatusManager.isRemotePlayer
            }
        }

        menu.findItem(R.id.menu_edit).isVisible = isQueue
        menu.findItem(R.id.menu_reoder).isVisible = isQueue
        menu.findItem(R.id.menu_save).isVisible = isQueue
        menu.findItem(R.id.menu_refresh).isVisible = isQueue
        menu.findItem(R.id.menu_delete_all).isVisible = isQueue
    }

    private fun setInitialSpinnerAdapter() {
        SynoLog.d(TAG, "setInitialSpinnerAdapter")

        playerStatusManager.unregisterPlayerSetChanged(
            mPlayerSetObserver
        )

        playerStatusManager.registerPlayerSetChanged(
            mPlayerSetObserver
        )

        titleView =
            toolbar.findViewById(R.id.parent_title)

        titleView.apply {
            visibility = View.VISIBLE

            setOnClickListener {
                (activity as? PlayerChooser)?.showPlayerChooser()
            }
        }

        updateTitle()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_lyric_queue -> toggleLyricQueue()

            R.id.menu_setting -> {
//                ContextExtensionsKt.openSettings(
//                    this,
//                    R.string.type_settings
//                )
            }

            R.id.menu_sleep_timer -> {
                activity?.let {
                    mPlayerControlHelper
                        .showSleepTimerDialog(it)
                }
            }

            R.id.menu_volume -> {
                mPlayerControlHelper.pressVolume()
            }

            else -> {
                queueFragment?.onOptionsItemSelected(item)
                lyricFragment?.onOptionsItemSelected(item)
            }
        }

        return true
    }

    private fun toggleLyricQueue() {
        setMode(viewMode.toggle())
    }


    private fun setMode(mode: Mode) {
        viewMode = mode

        val fragment: Fragment =
            when (mode) {
                Mode.LYRIC -> {
                    queueFragment = null
                    if (lyricFragment == null) {
                        lyricFragment = lyricFragmentProvider.get()
                        val homeActivity = activity as? HomeActivity ?: return
                        if (!homeActivity.isConnected()) return
                        lyricFragment?.bindController(homeActivity.provideController())
                    }
                    lyricFragment!!
                }

                Mode.QUEUE -> {
                    lyricFragment = null
                    queueFragment = queueFragmentProvider.get()
                    queueFragment!!
                }
            }

        updateTrackInfo()

        childFragmentManager.beginTransaction()
            .replace(R.id.content, fragment)
            .commit()

        invalidateOptionsMenu()
    }

    @Synchronized
    private fun updateTitle() {
        titleView.text =
            playerStatusManager.playerName

        invalidateOptionsMenu()
    }

    private fun setupViews(view: View) {
        val mainLayout = view.findViewById<View>(R.id.Player_container)

        val controlPanel = view.findViewById<View>(R.id.Player_ControlPanel)

        mPlayerControlHelper.setupControlPanel(
            mainLayout,
            controlPanel
        )

        mPlayerControlHelper.setOnRefreshListener(
            object : PlayerControlHelper.OnRefreshListener {
                override fun onRefresh(pos: Long) {
                    SynoLog.i("PlayerFragment", "onRefresh, pos: $pos")
                    updateTrackInfo()

                    if (pos >= 0) {
                        lyricFragment?.setTimeLine(pos / 100)
                    }
                }
            }
        )

        mSongDetailHelper = PlayingSongDetailHelper(
            view.context,
            view.findViewById(R.id.PlayingControlPanel_ThumbImageView),
            view.findViewById(R.id.PlayingControlPanel_TextAlbum),
            view.findViewById(R.id.PlayingControlPanel_TextArtist),
            view.findViewById(R.id.PlayingControlPanel_TextTitle),
            view.findViewById(R.id.PlayingControlPanel_RatingBar)
        )

        mSongDetailHelper.setIsForPhone(false)

        val homeActivity = activity as? HomeActivity ?: return
        if (!homeActivity.isConnected()) return

        bindController(homeActivity.provideController())
        homeActivity.sendUpdatePlaybackStatusCommand()
    }

    fun bindController(controller: MediaControllerCompat) {

        queueFragment?.bindController(controller)

        lyricFragment?.updateTrackInfo(playingQueueManager.getSongItem())
        SynoLog.d("PlayerFragment", "lyricFragment=${lyricFragment?.hashCode()}")

        mSongDetailHelper.setPlayingQueueManager(
            playingQueueManager
        )
        mPlayerControlHelper.setupController(controller)
    }

    private fun loadPlayerChooser() {
        playerStatusManager.requestLoadPlayers(
            object : PlayingStatusManager.LoadPlayerCallback {
                override fun onPreLoad() {}

                override fun onPostLoad() {
                    if (dialog?.isShowing == true) {
                        updateTitle()
                    }
                }
            }
        )
    }

    fun updateTrackInfo() {
        val songItem = playingQueueManager.getSongItem()

        mSongDetailHelper.updateSongInfo(songItem)

        lyricFragment?.updateTrackInfo(songItem)

        queueFragment?.updateTrackInfo(playingQueueManager.getPlayIndex())

        mPlayerControlHelper.updateAll()
    }

    private fun onDialogKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        if ((keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) ||
            !AudioPreference.enableRemoteController(requireContext())
        ) {
            return false
        }

        mPlayerControlHelper.pressVolume()
        return true
    }

    fun startSupportActionMode(actionMode: ActionMode.Callback): ActionMode? {
        return delegateForActionMode.startSupportActionMode(actionMode)
    }
}