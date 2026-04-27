package com.whisperyao.dsplayer.playing

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.facebook.drawee.view.SimpleDraweeView
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.fragment.PlayerFragment
import com.whisperyao.dsplayer.mediasession.service.AbstractMediaBrowserService
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.ui.volume.VolumeDialog
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utilities
import com.whisperyao.dsplayer.widget.AlwaysMarqueeTextView
import com.whisperyao.dsplayer.widget.MediaSeekBar
import com.whisperyao.dsplayer.widget.RatingBar
import java.util.HashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PlayerControlHelper @Inject constructor(
    private val playingQueueManager: PlayingQueueManager,
    private val playingStatusManager: PlayingStatusManager,
    private val supportFragmentManager: FragmentManager
) {

    companion object {
        private const val LOG = "PlayerControlHelper"
        private const val REFRESH = 1
    }

    private var controller: MediaControllerCompat? = null
    private var mArtistText: AlwaysMarqueeTextView? = null
    private var mCoverImage: SimpleDraweeView? = null
    private var mCurrentTimeText: TextView? = null
    private var mDurationText: TextView? = null
    private var mLayoutInfo: LinearLayout? = null
    private var mNextButton: ImageView? = null
    private var mOnRefreshListener: OnRefreshListener? = null
    private var mPlayButton: ImageView? = null
    private var mPrevButton: ImageView? = null
    private var mRatingBar: RatingBar? = null
    private var mRatingBarLayout: View? = null
    private var mRepeatButton: ImageView? = null
    private var mSeekBar: MediaSeekBar? = null
    private var mShuffleButton: ImageView? = null
    private var mSleepTimerInfoTextView: TextView? = null
    private var mStopButton: ImageView? = null
    private var mTimerButton: ImageView? = null
    private var mTitleText: AlwaysMarqueeTextView? = null
    private var mVolumeButton: ImageView? = null

    private var playerFragment: PlayerFragment? = null

    private var mShuffle = Common.ShuffleMode.NONE
    private var mRepeat = Common.RepeatMode.NONE
    private var mPolling = false

    interface OnRefreshListener {
        fun onRefresh(pos: Long)
    }

    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == REFRESH) {
                queueNextRefresh(refreshNow())
            }
        }
    }

    private val mPlayListener = View.OnClickListener { view ->
        controller?.let { mediaController ->
            val queue = playingQueueManager.getQueue()
            if (queue.isEmpty()) {
                Toast.makeText(
                    view.context,
                    R.string.message_to_addsongs,
                    Toast.LENGTH_SHORT
                ).show()
            }

            mediaController.playbackState?.let { playbackState ->
                when (playbackState.state) {
                    PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.STATE_STOPPED -> {
                        mediaController.transportControls.play()
                        startPollingIfNeeded()
                    }

                    PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.STATE_BUFFERING -> {
                        mediaController.transportControls.pause()
                        stopPolling()
                    }
                }
            }
        }
    }

    private val mPrevListener = View.OnClickListener {
        controller?.transportControls?.skipToPrevious()
    }

    private val mNextListener = View.OnClickListener {
        controller?.transportControls?.skipToNext()
    }

    private val mPlayerListener = View.OnClickListener {
        if (playerFragment == null) {
            playerFragment = PlayerFragment()
        }

        if (playerFragment?.isAdded != true) {
            playerFragment = PlayerFragment()
            playerFragment?.show(
                HomeActivity.MSupportManager,
                PlayerFragment.TAG
            )
        }
    }

    private val mStopListener = View.OnClickListener {
        controller?.transportControls?.stop()
        stopPolling()
    }

    private val mVolumeListener = View.OnClickListener {
        val volumeDialog = VolumeDialog()

        volumeDialog.setVolumeChangedCallback(
            object : VolumeDialog.VolumeChangedCallback {

                override fun setVolume(volume: Int) {
                    SynoLog.i("setVolume", "volume: $volume")
                    controller?.transportControls?.sendCustomAction(
                        AbstractMediaBrowserService.CUSTOM_ACTION_SET_SINGLE_VOLUME,
                        Bundle().apply {
                            putInt("volume", volume)
                        }
                    )
                }

                override fun setSubPlayersVolume(volumes: HashMap<String, Int>) {
                    controller?.transportControls?.sendCustomAction(
                        AbstractMediaBrowserService.CUSTOM_ACTION_SET_MULTI_VOLUME,
                        Bundle().apply {
                            putSerializable("volume", volumes)
                        }
                    )
                }
            }
        )

        volumeDialog.show(
            supportFragmentManager,
            VolumeDialog.TAG
        )
    }

    private val mTimerListener = View.OnClickListener {
        showSleepTimerDialog(it.context)
    }

    private val mShuffleListener = View.OnClickListener {
        controller?.let { mediaController ->
            mShuffle = mShuffle.toggle()
            mediaController.transportControls?.setShuffleMode(
                mShuffle.ordinal
            )
        }
    }

    private val mRepeatListener = View.OnClickListener {
        controller?.let { mediaController ->
            mRepeat = mRepeat.toNext()
            mediaController.transportControls?.setRepeatMode(
                mRepeat.ordinal
            )
        }
    }

    private var mPlayingStatusChangedObserver = PlayingStatusManager.OnPlayerStatusChangedObserver {
            mSleepTimerInfoTextView?.let { textView ->
                val sleepTimerRestTime =
                    playingStatusManager.getSleepTimerRestTime().toInt()

                // ExtensionsKt.toVisibility(sleepTimerRestTime > 0, false)
                textView.visibility = View.VISIBLE

                if (sleepTimerRestTime > 0) {
                    textView.text = Utilities.convertSecondsToTime(sleepTimerRestTime)
                }
            }
        }

    fun getPlayerFragment(): PlayerFragment? {
        return playerFragment
    }

    fun setPlayerFragment() {
        playerFragment = null
    }

    fun getMPlayingStatusChangedObserver(): PlayingStatusManager.OnPlayerStatusChangedObserver {
        return mPlayingStatusChangedObserver
    }

    fun setMPlayingStatusChangedObserver(onPlayerStatusChangedObserver:
        PlayingStatusManager.OnPlayerStatusChangedObserver
    ) {
        mPlayingStatusChangedObserver = onPlayerStatusChangedObserver
    }

    fun setupController(mediaController: MediaControllerCompat?) {
        controller = mediaController
        mSeekBar?.setMediaController(mediaController)
        playerFragment?.bindController(mediaController as MediaControllerCompat)
    }

    fun onCreate() {
        playingStatusManager.registerOnPlayerStatusChangedObserver(mPlayingStatusChangedObserver)
    }

    fun setupControlPanel(mainView: View, panelPlayingControl: View) {
        mSleepTimerInfoTextView = mainView.findViewById(R.id.PlayingControlPanel_SleepTimerInfo)

        mLayoutInfo = panelPlayingControl.findViewById(R.id.PlayingControlPanel_LayoutInfo)

        mCoverImage = panelPlayingControl.findViewById(R.id.PlayingControlPanel_CoverImage)

        mTitleText = panelPlayingControl.findViewById(R.id.PlayingControlPanel_TitleText)

        mArtistText = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ArtistText)

        mRatingBar = panelPlayingControl.findViewById(R.id.PlayingControlPanel_RatingBar)

        mRatingBar?.setOnRatingChangeListener { ratingBar, rating, fromUser ->
            if (!fromUser) return@setOnRatingChangeListener

            val song = playingQueueManager.getSongItem() ?: return@setOnRatingChangeListener

            val songs = arrayListOf(song)
            val value = rating.toInt()

            CacheManager.getInstance().recordRatingMapFromUser(songs, value)
            CacheManager.getInstance().requestRatingSongs(songs, value)

            ratingBar.setRating(rating)
        }

        mRatingBarLayout = panelPlayingControl.findViewById(R.id.PlayingControlPanel_RatingBar_Layout)

        mPrevButton = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ButtonPrev)

        mPlayButton = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ButtonPlay)

        mNextButton = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ButtonNext)

        mStopButton = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ButtonStop)

        mVolumeButton = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ButtonVolume)

        mTimerButton = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ButtonTimer)

        mRepeatButton = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ButtonRepeat)

        mShuffleButton = panelPlayingControl.findViewById(R.id.PlayingControlPanel_ButtonShuffle)

        mCurrentTimeText = panelPlayingControl.findViewById(R.id.PlayingControlPanel_PlayingTime)

        mDurationText = panelPlayingControl.findViewById(R.id.PlayingControlPanel_Duration)

        val mediaSeekBar: MediaSeekBar? = panelPlayingControl.findViewById(R.id.PlayingControlPanel_SeekBar)
        this.mSeekBar = mediaSeekBar
        // SynoLog.d("SeekInit", "seekbar=$mSeekBar, mediaSeekBar: $mediaSeekBar seekbar init success")
        mediaSeekBar?.apply {
            max = 1000
            isEnabled = false
            setPlayingQueueManager(playingQueueManager)
        }
        mLayoutInfo?.setOnClickListener(mPlayerListener)
        mPlayButton?.setOnClickListener(mPlayListener)
        mNextButton?.setOnClickListener(mNextListener)
        mPrevButton?.setOnClickListener(mPrevListener)
        mStopButton?.setOnClickListener(mStopListener)
        mVolumeButton?.setOnClickListener(mVolumeListener)
        mTimerButton?.setOnClickListener(mTimerListener)
        mRepeatButton?.setOnClickListener(mRepeatListener)
        mShuffleButton?.setOnClickListener(mShuffleListener)

        updateCover(null)
    }

    private fun updateCover(mediaId: String?) {
        val id = mediaId ?: PlayingQueueManager.notPlayingMediaId
        mCoverImage?.setImageBitmap(
            playingQueueManager.getAlbumBitmap(
                mCoverImage!!.context,
                id
            )
        )
    }

    fun release() {
        mSeekBar?.disconnectController()
        playingStatusManager
            .unregisterOnPlayerStatusChangedObserver(
                mPlayingStatusChangedObserver
            )
        controller = null
    }

    fun setOnClickRatingIndicatorListener(listener: View.OnClickListener) {
        mRatingBar?.setOnClickListener(listener)
    }

    fun updateAll() {
        updateSongDetails()
        if (controller != null) {
            updateButtons()
            updateTimeline()
        }
    }

    fun updateButtons() {
        updatePlayButton()
        updateRepeatButton()
        updateShuffleButton()
        updateVolumeButton()
        updateVolumeTimerButton()
    }

    private fun updateSongDetails() {
        // SynoLog.d(LOG, "updateSongDetails")
        val song = playingQueueManager.getSongItem()

        if (song != null) {
            if (mTitleText?.text != song.title) {
                mTitleText?.text = song.title
            }

            if (mArtistText?.text != song.artist) {
                mArtistText?.text = song.artist
            }
        } else {
            mTitleText?.text = null
            mArtistText?.text = null
        }

        updateCover(song?.mediaId)

        val rating = song?.rating ?: 0f
        val isWithRating = song?.isWithRating ?: false
        val canEditRating =
            song?.let {
                ConnectionManager.canEditRating(true, it)
            } ?: false

        if (this.mRatingBar != null) {
            this.mRatingBar?.setRating(rating)
            this.mRatingBar?.setIsIndicator(!canEditRating)
        }

        mRatingBarLayout?.visibility =
            if (isWithRating) View.VISIBLE else View.GONE
    }

    fun updateTimeline() {
        val controller = controller ?: return
        val metadata = controller.metadata ?: return

        val songItem = playingQueueManager.getSongItem()
        val durationMs = TimeUnit.MILLISECONDS.convert(
            songItem?.duration?.toLong() ?: 0L,
            TimeUnit.SECONDS
        )

        val position = controller.playbackState.position

        updateDuration(durationMs)
        updateCurrentTime(position)
    }

    fun setOnRefreshListener(listener: OnRefreshListener) {
        mOnRefreshListener = listener
    }

    fun startPollingIfNeeded() {
        SynoLog.i(LOG, "startPollingIfNeeded")
        if (controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            startPolling()
        }
    }

    fun startPolling() {
        SynoLog.d(LOG, " startPolling")
        mPolling = true
        queueNextRefresh(1L)
    }

    fun stopPolling() {
        SynoLog.d(LOG, " stopPolling")
        mPolling = false
        mHandler.removeMessages(REFRESH)
    }

    private fun queueNextRefresh(delay: Long) {
        val state = controller?.playbackState?.state

        if (state == PlaybackStateCompat.STATE_PAUSED) {
            SynoLog.d(LOG, " The status is Paused now , stopPolling");
            stopPolling()
        } else if (state != PlaybackStateCompat.STATE_PLAYING) {
            SynoLog.d(LOG, " The status is not playing or preparing now , stopPolling");
            stopPolling()
        }

        if (mPolling) {
            val msg = mHandler.obtainMessage(REFRESH)
            mHandler.removeMessages(REFRESH)
            mHandler.sendMessageDelayed(msg, delay)
        }
    }

    private fun refreshNow(): Long {
        updateAll()

        val position =
            controller?.playbackState?.position ?: 0L

        val j = 200L
        val j2 = j - (position % j)

        mOnRefreshListener?.onRefresh(position)

        return j2
    }

    fun updateSeekBar(d: Long, b: Long) {
        // SynoLog.d("SeekDebug", "progress=${d} max=${mSeekBar?.max}")
        mSeekBar?.apply {
            progress = d.toInt()
            secondaryProgress = (b.toInt() * max) / 100
        }

        updateCurrentTime(d)
    }

    private fun updateCurrentTime(currentTime: Long) {
        val state = controller?.playbackState?.state

        if (currentTime >= 0 &&
            (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED)
        ) {
            mCurrentTimeText?.text = Utilities.makeTimeString(currentTime / 1000)
        } else {
            mCurrentTimeText?.text = null
        }
    }

    private fun updateDuration(duration: Long) {
        mDurationText?.text =
            if (duration > 0)
                Utilities.makeTimeString(duration / 1000)
            else null
    }

    private fun updatePlayButton() {
        val state = controller?.playbackState?.state

        mPlayButton?.setImageResource(
            if (
                state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_BUFFERING
            ) {
                R.drawable.player_btn_pause
            } else {
                R.drawable.player_btn_play
            }
        )
    }

    private fun updateRepeatButton() {
        val repeatMode =
            Common.RepeatMode.fromId(
                controller?.repeatMode ?: 0
            )

        mRepeat = repeatMode

        mRepeatButton?.setImageResource(
            when (repeatMode) {
                Common.RepeatMode.ONE ->
                    R.drawable.player_btn_repeat_one

                Common.RepeatMode.ALL ->
                    R.drawable.player_btn_repeat_all

                else ->
                    R.drawable.player_btn_repeat_none
            }
        )
    }

    private fun updateShuffleButton() {
        val shuffleMode = Common.ShuffleMode.fromId(controller?.shuffleMode ?: 0)

        mShuffle = shuffleMode

        mShuffleButton?.setImageResource(
            if (shuffleMode == Common.ShuffleMode.AUTO)
                R.drawable.player_btn_shuffle_on
            else
                R.drawable.player_btn_shuffle
        )
    }

    private fun updateVolumeButton() {
        val res =
            if (playingStatusManager.player.isGroupPlayer)
                R.drawable.player_btn_multi_volume
            else
                R.drawable.player_btn_volume

        mVolumeButton?.setImageResource(res)
    }

    private fun updateVolumeTimerButton() {
        val sleepTimerRestTime =
            playingStatusManager.getSleepTimerRestTime().toInt()

        mTimerButton?.setImageResource(
            if (sleepTimerRestTime > 0)
                R.drawable.player_btn_timer_on
            else
                R.drawable.player_btn_timer
        )
    }

    fun pressVolume() {
        mVolumeListener.onClick(null)
    }

    fun updateBufferingPercent() {
        val bufferedPosition =
            controller?.playbackState?.bufferedPosition ?: return

        mSeekBar?.secondaryProgress =
            bufferedPosition.toInt() * 10
    }

    fun showSleepTimerDialog(context: Context) {
        val iArr = intArrayOf(120, 90, 60, 45, 30, 20, 10, 0)

        val strArr = Array(8) { i ->
            val minute = iArr[i]
            if (minute == 0) {
                context.getString(R.string.none)
            } else {
                context.getString(
                    R.string.sleep_timer_time_minutes,
                    minute
                )
            }
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.sleep_timer)
            .setItems(strArr) { _, which ->
                playingStatusManager.setSleepTimer(
                    iArr[which] * 60,
                    controller
                )
            }
            .create()
            .show()
    }

    fun removeSeekBarThumb() {
        mSeekBar?.let { seekBar ->
            seekBar.thumb?.mutate()?.alpha = 0
            seekBar.setOnTouchListener { _, _ -> true }
        }
    }
}