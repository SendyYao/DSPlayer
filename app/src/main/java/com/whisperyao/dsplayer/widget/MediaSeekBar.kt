package com.whisperyao.dsplayer.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import java.util.concurrent.TimeUnit
import com.whisperyao.dsplayer.util.SynoLog

class MediaSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private var controllerCallback: ControllerCallback? = null
    private var isTracking = false
    private var latestSeekTimestamp = -1L
    private var mediaController: MediaControllerCompat? = null
    private var playingQueueManager: PlayingQueueManager? = null
    private var thumbDrawable: Drawable? = null

    private val internalSeekListener = object : OnSeekBarChangeListener {
        private var positionOverride = -1L

        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            if (fromUser && isTracking) {
                positionOverride = progress.toLong()
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            isTracking = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            if (positionOverride >= 0) {
                mediaController?.transportControls?.seekTo(positionOverride)
                latestSeekTimestamp = System.currentTimeMillis()
                positionOverride = -1L
            }

            isTracking = false
        }
    }

    init {
        super.setOnSeekBarChangeListener(internalSeekListener)
    }

    private val isWaitingForSeekCompleted: Boolean
        get() {
            if (latestSeekTimestamp == -1L) return false

            return if (System.currentTimeMillis() - latestSeekTimestamp <= 200) {
                true
            } else {
                latestSeekTimestamp = -1L
                false
            }
        }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        throw UnsupportedOperationException(
            "Cannot add listeners to a MediaSeekBar"
        )
    }

    fun setPlayingQueueManager(manager: PlayingQueueManager) {
        playingQueueManager = manager
    }

    fun setMediaController(controller: MediaControllerCompat?) {
        controller ?: return

        mediaController?.let { oldController ->
            controllerCallback?.let {
                oldController.unregisterCallback(it)
            }
        }

        val callback = ControllerCallback()
        controller.registerCallback(callback)

        controllerCallback = callback
        mediaController = controller

        SynoLog.d("MetaDebug", "seekbar controller token=${controller.sessionToken}")

        // 关键：主动同步一次当前状态
        callback.onMetadataChanged(controller.metadata)
        callback.onPlaybackStateChanged(controller.playbackState)

    }

    fun disconnectController() {
        mediaController?.let { controller ->
            controllerCallback?.let {
                controller.unregisterCallback(it)
            }
        }

        controllerCallback = null
        mediaController = null
    }

    override fun setProgress(progress: Int) {
        if (isTracking || isWaitingForSeekCompleted) return
        super.setProgress(progress)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isEnabled) {
            super.onTouchEvent(event)
        } else {
            false
        }
    }

    override fun getThumb(): Drawable? {
        return thumbDrawable
    }

    override fun setThumb(thumb: Drawable?) {
        thumbDrawable = thumb
        super.setThumb(thumb)
    }

    private inner class ControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            SynoLog.d(
                "SeekBar",
                "state=${state?.state}, max=$max, enabled=$isEnabled"
            )
            super.onPlaybackStateChanged(state)

            latestSeekTimestamp = -1L

            if (isTracking) return

            isEnabled = state?.state == PlaybackStateCompat.STATE_PLAYING && max > 0
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            latestSeekTimestamp = -1L

            val song = playingQueueManager?.getSongItem()

            if (song?.isRadio == true) {
                max = 0
                progress = 0
            } else {
                max = TimeUnit.MILLISECONDS.convert(
                    song?.duration?.toLong() ?: 0L,
                    TimeUnit.SECONDS
                ).toInt()
            }
            secondaryProgress = 0
        }
    }
}