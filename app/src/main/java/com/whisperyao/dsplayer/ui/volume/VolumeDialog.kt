package com.whisperyao.dsplayer.ui.volume

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.playing.Player
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.SynoLog
import dagger.android.support.DaggerDialogFragment
import javax.inject.Inject

class VolumeDialog : DaggerDialogFragment() {

    companion object {
        private const val DISMISS_DELAY = 5000L
        private const val MAIN_PLAYER_INDEX = 0
        private const val UPDATE_UI_SKIP_COUNT = 2
        private const val UPDATE_VOLUME_INTERVAL = 500L

        const val TAG = "VolumeDialog"
    }

    interface VolumeChangedCallback {
        fun setSubPlayersVolume(volumes: HashMap<String, Int>)
        fun setVolume(volume: Int)
    }

    private var callback: VolumeChangedCallback? = null
    private var isDetachedFromWindow = false

    private lateinit var mListView: ListView
    private lateinit var mPlayerVolumeAdapter: PlayerVolumeAdapter
    private lateinit var mOnPlayerStatusChangedObserver: PlayingStatusManager.OnPlayerStatusChangedObserver

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var playingStatusManager: PlayingStatusManager

    private val mHandler = Handler(Looper.getMainLooper())
    private val msgHandler = Handler(Looper.getMainLooper())

    private val mDismissRunnable = Runnable {
        if (!isDetachedFromWindow) {
            dismissAllowingStateLoss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.VolumeDialog)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            val attributes = window.attributes
            attributes.dimAmount = 0f
            window.attributes = attributes
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }

            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    volumeUp()
                    true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    volumeDown()
                    true
                }

                else -> false
            }
        }

        dialog?.setCanceledOnTouchOutside(true)

        return inflater.inflate(R.layout.volume_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }

            val rect = Rect()
            v.findViewById<View>(R.id.VolumeDialog_RootView).getHitRect(rect)

            if (!rect.contains(event.x.toInt(), event.y.toInt())) {
                dismiss()
                return@setOnTouchListener true
            }

            false
        }

        mListView = view.findViewById(R.id.VolumeDialog_RootView)

        mListView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    msgHandler.removeCallbacks(mDismissRunnable)
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    // CoroutineLiveDataKt.DEFAULT_TIMEOUT
                    msgHandler.postDelayed(
                        mDismissRunnable,
                        5000
                    )
                }
            }
            false
        }

        mPlayerVolumeAdapter = if (player.isGroupPlayer) {
            val playerVolumeManager = PlayerVolumeManager(player)
            MultiplePlayerVolumeAdapter(view.context, playerVolumeManager)
        } else {
            SinglePlayerVolumeAdapter(view.context, player)
        }

        mPlayerVolumeAdapter.setOnTrackingTouchListener(
            object : PlayerVolumeAdapter.OnTrackingTouchListener {
                override fun onStartTrackingTouch(bar: SeekBar) {
                    msgHandler.removeCallbacks(mDismissRunnable)
                }

                override fun onStopTrackingTouch(bar: SeekBar) {
                    // CoroutineLiveDataKt.DEFAULT_TIMEOUT
                    msgHandler.postDelayed(
                        mDismissRunnable,
                        5000
                    )
                }

                override fun onProgressChanged(bar: SeekBar, progress: Int) {
                    updateViews()
                }
            }
        )

        mOnPlayerStatusChangedObserver = PlayingStatusManager.OnPlayerStatusChangedObserver {
            mHandler.post {
                mPlayerVolumeAdapter.updateData()
                updateViews()
            }
        }

        mListView.adapter = mPlayerVolumeAdapter

        setCurrentVolume(playingStatusManager.currentVolume)

        // CoroutineLiveDataKt.DEFAULT_TIMEOUT
        msgHandler.postDelayed(
            mDismissRunnable,
            5000
        )

        playingStatusManager.registerOnPlayerStatusChangedObserver(
            mOnPlayerStatusChangedObserver
        )
    }

    override fun onDestroyView() {
        isDetachedFromWindow = true

        playingStatusManager.unregisterOnPlayerStatusChangedObserver(
            mOnPlayerStatusChangedObserver
        )

        super.onDestroyView()
    }

    private fun volumeUp() {
        SynoLog.d("VolumeDialog", "volumeUp, mPlayerVolumeAdapter: $mPlayerVolumeAdapter")
        mPlayerVolumeAdapter.increaseMainVolume()
        updateViews()
        msgHandler.removeCallbacks(mDismissRunnable)
        // CoroutineLiveDataKt.DEFAULT_TIMEOUT
        msgHandler.postDelayed(
            mDismissRunnable,
            5000
        )
    }

    private fun volumeDown() {
        mPlayerVolumeAdapter.decreaseMainVolume()
        updateViews()

        msgHandler.removeCallbacks(mDismissRunnable)
        // CoroutineLiveDataKt.DEFAULT_TIMEOUT
        msgHandler.postDelayed(
            mDismissRunnable,
            5000
        )
    }

    private fun updateViews() {
        val firstVisiblePosition = mListView.firstVisiblePosition
        var lastVisiblePosition = mListView.lastVisiblePosition

        if (lastVisiblePosition < 0 && firstVisiblePosition >= 0) {
            lastVisiblePosition = firstVisiblePosition
        }

        if (firstVisiblePosition > lastVisiblePosition) return

        for (i in firstVisiblePosition..lastVisiblePosition) {
            val childIndex = i - firstVisiblePosition
            val child = mListView.getChildAt(childIndex)
            mPlayerVolumeAdapter.updateView(i, child)
        }
    }

    private fun setCurrentVolume(volume: Int) {
        if (mPlayerVolumeAdapter is SinglePlayerVolumeAdapter) {
            (mPlayerVolumeAdapter as SinglePlayerVolumeAdapter)
                .setVolume(volume)
        }
    }

    fun setVolumeChangedCallback(callback: VolumeChangedCallback) {
        this.callback = callback
    }

    private inner class PlayerVolumeManager(private val mainPlayer: Player) {
        private val mSubPlayerVolumeBase = HashMap<String, Int>()
        private var mSubPlayerVolumeCurrent = HashMap<String, Int>()

        private val mUpdateSubPlayersVolumeRunnable = Runnable {
            callback?.setSubPlayersVolume(mSubPlayerVolumeCurrent)
        }

        init {
            if (mainPlayer.isWithPlayingStatus()) {
                update()
            }
        }

        fun getMainPlayer(): Player = mainPlayer

        fun getMainPlayerVolume(): Int {
            return getMaxVolume(mSubPlayerVolumeCurrent)
        }

        fun setMainPlayerVolume(volume: Int) {
            if (mSubPlayerVolumeCurrent.isEmpty()) return

            val maxVolume = getMaxVolume(mSubPlayerVolumeBase)

            if (maxVolume == 0) {
                val key = mSubPlayerVolumeCurrent.keys.first()
                mSubPlayerVolumeCurrent[key] = volume
                mSubPlayerVolumeBase[key] = volume
                return
            }

            for (key in mSubPlayerVolumeCurrent.keys) {
                val base = mSubPlayerVolumeBase[key] ?: 0
                mSubPlayerVolumeCurrent[key] =
                    (base * volume) / maxVolume
            }

            updateVolumes()
        }

        fun getSubPlayerList(): List<String> {
            return ArrayList(mSubPlayerVolumeCurrent.keys)
        }

        fun update() {
            val currentPlayingStatus = playingStatusManager.currentPlayingStatus

            val subPlayersVolumes = currentPlayingStatus?.subPlayersVolumes ?: return

            mSubPlayerVolumeCurrent = HashMap(subPlayersVolumes)

            val baseKeys = mSubPlayerVolumeBase.keys.toSet()
            val currentKeys = mSubPlayerVolumeCurrent.keys.toSet()

            val removedKeys = baseKeys - currentKeys
            for (key in removedKeys) {
                mSubPlayerVolumeBase.remove(key)
            }

            val addedKeys = currentKeys - baseKeys
            for (key in addedKeys) {
                mSubPlayerVolumeBase[key] = mSubPlayerVolumeCurrent[key] ?: 0
            }

            if (getMaxVolume(mSubPlayerVolumeCurrent) > 0) {
                for (key in mSubPlayerVolumeBase.keys.toList()) {
                    mSubPlayerVolumeBase[key] = mSubPlayerVolumeCurrent[key] ?: 0
                }
            }
        }

        fun getSubPlayerVolume(playerId: String?): Int {
            return mSubPlayerVolumeCurrent[playerId] ?: 0
        }

        fun setSubPlayerVolume(playerId: String?, volume: Int) {
            if (playerId == null) return

            mSubPlayerVolumeCurrent[playerId] = volume

            if (getMaxVolume(mSubPlayerVolumeCurrent) != 0) {
                mSubPlayerVolumeBase.clear()
                mSubPlayerVolumeBase.putAll(mSubPlayerVolumeCurrent)
            }

            updateVolumes()
        }

        fun getPlayerName(playerId: String): String {
            return playingStatusManager.findPlayer(playerId)?.name ?: playerId
        }

        private fun getMaxVolume(volumes: Map<String, Int>): Int {
            var max = 0
            for (value in volumes.values) {
                max = kotlin.math.max(max, value)
            }
            return max
        }

        private fun updateVolumes() {
            mHandler.removeCallbacks(
                mUpdateSubPlayersVolumeRunnable
            )
            mHandler.postDelayed(
                mUpdateSubPlayersVolumeRunnable,
                UPDATE_VOLUME_INTERVAL
            )
        }
    }

    private abstract class PlayerVolumeAdapter(private val mContext: Context) : BaseAdapter() {

        interface OnTrackingTouchListener {
            fun onProgressChanged(bar: SeekBar, progress: Int)

            fun onStartTrackingTouch(bar: SeekBar)

            fun onStopTrackingTouch(bar: SeekBar)
        }

        private var isDragging = false
        private var mSkipUiCount = 0

        private var mOnTrackingTouchListener:
                OnTrackingTouchListener? = null

        protected abstract fun doSetPlayerVolume(position: Int, volume: Int)

        protected abstract fun doUpdateData()

        abstract override fun getItem(position: Int): PlayerVolumeItem

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        protected abstract fun getPlayerVolume(position: Int): Int

        fun setOnTrackingTouchListener(listener: OnTrackingTouchListener) {
            mOnTrackingTouchListener = listener
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(mContext).inflate(
                    R.layout.player_volume_item,
                    null
                )

                holder = ViewHolder(
                    view.findViewById(R.id.player_name),
                    view.findViewById(R.id.player_volume_icon),
                    view.findViewById(R.id.player_volume_progress)
                )

                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ViewHolder
            }

            val item = getItem(position)

            holder.mTextViewName.text = item.getPlayerName()

            holder.mImageViewIcon.setImageResource(
                if (item.getIsGroupPlayer())
                    R.drawable.player_btn_multi_volume
                else
                    R.drawable.player_btn_volume
            )

            holder.mSeekBarVolume.max = item.getMaxVolume()

            holder.mSeekBarVolume.progress = getPlayerVolume(position)

            holder.mSeekBarVolume.setOnSeekBarChangeListener(
                PlayerOnSeekBarChangeListener(position)
            )

            return view
        }

        fun increaseMainVolume() {
            setPlayerVolume(
                MAIN_PLAYER_INDEX,
                getItem(0)
                    .getUpVolume(getPlayerVolume(0))
            )
        }

        fun decreaseMainVolume() {
            setPlayerVolume(
                MAIN_PLAYER_INDEX,
                getItem(0).getDownVolume(
                    getPlayerVolume(0)
                )
            )
        }

        fun updateData() {
            if (isDragging) return

            if (mSkipUiCount > 0) {
                mSkipUiCount--
            } else {
                doUpdateData()
            }
        }

        fun updateView(position: Int, view: View?) {
            if (view == null || position >= count) return

            val holder = view.tag as ViewHolder

            holder.mSeekBarVolume.progress = getPlayerVolume(position)
        }

        protected fun setPlayerVolume(position: Int, volume: Int) {
            SynoLog.d("setPlayerVolume", "position: $position, volume: $volume")
            doSetPlayerVolume(position, volume)
            mSkipUiCount = UPDATE_UI_SKIP_COUNT
        }

        private inner class PlayerOnSeekBarChangeListener(private val mPosition: Int) : SeekBar.OnSeekBarChangeListener {

            override fun onStartTrackingTouch(bar: SeekBar) {
                isDragging = true
                mOnTrackingTouchListener?.onStartTrackingTouch(bar)
            }

            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setPlayerVolume(mPosition, progress)

                    mOnTrackingTouchListener?.onProgressChanged(bar, progress)
                }
            }

            override fun onStopTrackingTouch(bar: SeekBar) {
                isDragging = false
                mOnTrackingTouchListener?.onStopTrackingTouch(bar)
            }
        }

        protected data class ViewHolder(
            val mTextViewName: TextView,
            val mImageViewIcon: ImageView,
            val mSeekBarVolume: SeekBar
        )
    }

    private inner class SinglePlayerVolumeAdapter(context: Context, private var mPlayer: Player) : PlayerVolumeAdapter(context) {

        private var mPlayerVolumeItem = PlayerVolumeItem(context, mPlayer)

        private var mVolume = 0

        private val mUpdateSubPlayersVolumeRunnable =
            Runnable {
                callback?.setVolume(mVolume)
            }

        override fun getCount(): Int = 1

        override fun getItem(
            position: Int
        ): PlayerVolumeItem {
            return mPlayerVolumeItem
        }

        override fun getPlayerVolume(
            position: Int
        ): Int {
            return mVolume
        }

        override fun doSetPlayerVolume(position: Int, volume: Int) {
            mVolume = volume

            if (!mPlayer.isLocalPlayer()) {
                mHandler.removeCallbacks(
                    mUpdateSubPlayersVolumeRunnable
                )

                mHandler.postDelayed(
                    mUpdateSubPlayersVolumeRunnable,
                    UPDATE_VOLUME_INTERVAL
                )
            } else {
                mUpdateSubPlayersVolumeRunnable.run()
            }
        }

        override fun doUpdateData() {
        }

        fun setVolume(volume: Int) {
            mVolume = volume
            updateViews()
        }
    }

    private inner class MultiplePlayerVolumeAdapter(context: Context, private var mPlayerVolumeManager: PlayerVolumeManager) : PlayerVolumeAdapter(context) {

        private var mPlayerVolumeItem = PlayerVolumeItem(context, mPlayerVolumeManager.getMainPlayer())

        private val mPlayerVolumeItemList = mutableListOf<PlayerVolumeItem>()

        init {
            updateData()
        }

        override fun getCount(): Int {
            return mPlayerVolumeItemList.size
        }

        override fun getItem(position: Int): PlayerVolumeItem {
            return mPlayerVolumeItemList[position]
        }

        override fun getPlayerVolume(position: Int): Int {
            return if (position == 0) {
                mPlayerVolumeManager
                    .getMainPlayerVolume()
            } else {
                mPlayerVolumeManager.getSubPlayerVolume(getItem(position).getPlayerId())
            }
        }

        override fun doSetPlayerVolume(position: Int, volume: Int) {
            if (position == 0) {
                mPlayerVolumeManager
                    .setMainPlayerVolume(volume)
            } else {
                mPlayerVolumeManager
                    .setSubPlayerVolume(
                        getItem(position).getPlayerId(),
                        volume
                    )
            }
        }

        override fun doUpdateData() {
            mPlayerVolumeManager.update()

            mPlayerVolumeItemList.clear()

            mPlayerVolumeItemList.add(
                mPlayerVolumeItem
            )

            for (id in mPlayerVolumeManager.getSubPlayerList()) {
                mPlayerVolumeItemList.add(
                    PlayerVolumeItem(
                        id,
                        mPlayerVolumeManager
                            .getPlayerName(id)
                    )
                )
            }

            notifyDataSetChanged()
        }
    }
}