package com.whisperyao.dsplayer.mediasession.client

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.mediasession.service.AbstractMediaBrowserService
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.SynoLog

open class MediaBrowserHelper(
    private val mContext: Context,
    private val playingStatusManager: PlayingStatusManager,
    private val playingQueueManager: PlayingQueueManager,
    private val mediaBrowserServiceClass: Class<out MediaBrowserServiceCompat>
) {

    companion object {
        private const val TAG = "MediaBrowserHelper"
    }

    private val mCallbackList = ArrayList<MediaControllerCompat.Callback>()

    private var mMediaBrowser: MediaBrowserCompat? = null

    var mMediaController: MediaControllerCompat? = null

    private val mMediaBrowserConnectionCallback = MediaBrowserConnectionCallback()

    private val mMediaBrowserSubscriptionCallback = MediaBrowserSubscriptionCallback()

    private val mMediaControllerCallback = MediaControllerCallback()

    private interface CallbackCommand {
        fun perform(callback: MediaControllerCompat.Callback)
    }

    protected open fun onChildrenLoaded(
        parentId: String,
        children: List<MediaBrowserCompat.MediaItem>
    ) {
    }

    val connected: Boolean
        get() = mMediaBrowser?.isConnected ?: false

    val mediaController: MediaControllerCompat
        get() = mMediaController
            ?: throw IllegalStateException("MediaController is null!")

    val transportControls: MediaControllerCompat.TransportControls
        get() {
            val controller = mMediaController
            if (controller != null) {
                return controller.transportControls
            }

            SynoLog.d(TAG, "getTransportControls: MediaController is null!")
            throw IllegalStateException("MediaController is null!")
        }

    fun onStart() {
        if (mMediaBrowser == null) {
            mMediaBrowser = MediaBrowserCompat(
                mContext,
                ComponentName(mContext, mediaBrowserServiceClass),
                mMediaBrowserConnectionCallback,
                null
            )
            mMediaBrowser?.connect()
        }

        SynoLog.d(TAG, "onStart: Creating MediaBrowser, and connecting")
    }

    fun onStop() {
        mMediaController?.unregisterCallback(
            mMediaControllerCallback
        )
        mMediaController = null

        if (connected) {
            mMediaBrowser?.disconnect()
            mMediaBrowser = null
        }

        resetState()

        SynoLog.d(TAG, "onStop: Releasing MediaController, Disconnecting from MediaBrowser")
    }

    fun sendUpdatePlaybackStatusCommand() {
        mMediaController?.transportControls?.sendCustomAction(
            AbstractMediaBrowserService.CUSTOM_ACTION_UPDATE_UI,
            null
        )
    }

    fun enqueue(action: Common.PlaybackAction, list: List<SongItem>, position: Int) {
        SynoLog.d("MediaBrowserHelper", "action: ${action.name}, first: ${list.first().title}")
        if (!connected) {
            SynoLog.i("MediaBrowserHelper", "Not connected")
            onStart()
        }
        SynoLog.d("MediaBrowserHelper", "mediaBrowser: $mMediaBrowser")
        mMediaBrowser?.subscribe(
            PlayingQueueManager.nowPlayingMediaId,
            object : MediaBrowserCompat.SubscriptionCallback() {

                override fun onChildrenLoaded(
                    parentId: String,
                    children: List<MediaBrowserCompat.MediaItem>
                ) {
                    val mediaController = mediaController

                    val customAction = when (action) {
                        Common.PlaybackAction.PLAY_NOW ->
                            AbstractMediaBrowserService.CUSTOM_ACTION_PLAY_NOW

                        Common.PlaybackAction.ADD_ONLY ->
                            AbstractMediaBrowserService.CUSTOM_ACTION_ADD_ONLY

                        Common.PlaybackAction.ADD_PLAY ->
                            AbstractMediaBrowserService.CUSTOM_ACTION_ADD_PLAY

                        Common.PlaybackAction.ADD_NEXT ->
                            AbstractMediaBrowserService.CUSTOM_ACTION_ADD_NEXT

                        Common.PlaybackAction.BY_SITUACTION ->
                            AbstractMediaBrowserService.CUSTOM_ACTION_BY_SITUATION
                    }

                    if (
                        customAction != AbstractMediaBrowserService.CUSTOM_ACTION_PLAY_NOW ||
                        playingStatusManager.isRemotePlayer
                    ) {
                        playingQueueManager.setTempQueue(list)
                    } else {
                        playingQueueManager.setQueue(
                            list,
                            position
                        )
                    }

                    SynoLog.i("playingQueueManager", "first: ${playingQueueManager.getQueue().first().title}")

                    val bundle = Bundle().apply {
                        putInt("position", position)
                        putInt(
                            "playbackAction",
                            action.ordinal
                        )
                    }
                    SynoLog.d("MediaBrowserHelper", "customAction: $customAction, bundle: $bundle, controller: $mMediaController")
                    mediaController.transportControls.sendCustomAction(customAction, bundle)
                }
            }
        )
    }

    protected open fun onConnected(mediaController: MediaControllerCompat) {
        SynoLog.d(TAG, "onConnected")
    }

    protected fun onDisconnected() {
        SynoLog.d(TAG, "onDisconnected")
    }

    private fun resetState() {
        performOnAllCallbacks(
            object : CallbackCommand {
                override fun perform(
                    callback: MediaControllerCompat.Callback
                ) {
                    callback.onPlaybackStateChanged(
                        PlaybackStateCompat.Builder()
                            .setState(
                                PlaybackStateCompat.STATE_NONE,
                                0L,
                                1.0f
                            )
                            .build()
                    )
                }
            }
        )

        SynoLog.d(TAG, "resetState")
    }

    fun unregisterCallback(
        callback: MediaControllerCompat.Callback?
    ) {
        callback?.let {
            mCallbackList.remove(it)
        }
    }

    fun registerCallback(
        callback: MediaControllerCompat.Callback?
    ) {
        callback?.let {
            mCallbackList.add(it)

            mMediaController?.metadata?.let { metadata ->
                it.onMetadataChanged(metadata)
            }

            mMediaController?.playbackState?.let { state ->
                it.onPlaybackStateChanged(state)
            }
        }
    }

    private fun performOnAllCallbacks(command: CallbackCommand) {
        mCallbackList.forEach {
            command.perform(it)
        }
    }

    private inner class MediaBrowserConnectionCallback :
        MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            try {
                val browser = mMediaBrowser ?: return

                mMediaController = MediaControllerCompat(mContext, browser.sessionToken)

                HomeActivity.mMediaController = mMediaController as MediaControllerCompat

                mMediaController?.let { controller ->
                    controller.registerCallback(
                        mMediaControllerCallback
                    )

                    mMediaControllerCallback.onMetadataChanged(controller.metadata)

                    mMediaControllerCallback.onPlaybackStateChanged(
                        controller.playbackState
                    )

                    this@MediaBrowserHelper.onConnected(controller)
                }

                browser.subscribe(
                    browser.root,
                    mMediaBrowserSubscriptionCallback
                )

            } catch (e: RemoteException) {
                SynoLog.d(TAG, "onConnected: Problem: $e")
                throw RuntimeException(e)
            }
        }
    }

    inner class MediaBrowserSubscriptionCallback :
        MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaBrowserCompat.MediaItem>
        ) {
            this@MediaBrowserHelper.onChildrenLoaded(
                parentId,
                children
            )
        }
    }

    private inner class MediaControllerCallback :
        MediaControllerCompat.Callback() {

        override fun onMetadataChanged(
            metadata: MediaMetadataCompat?
        ) {
            SynoLog.d("CALLBACK", "metadata changed: ${metadata?.description?.title}")
            performOnAllCallbacks(
                object : CallbackCommand {
                    override fun perform(
                        callback: MediaControllerCompat.Callback
                    ) {
                        callback.onMetadataChanged(metadata)
                    }
                }
            )
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            performOnAllCallbacks(object : CallbackCommand {
                    override fun perform(callback: MediaControllerCompat.Callback) {
                        callback.onPlaybackStateChanged(state)
                    }
                }
            )
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            performOnAllCallbacks(object : CallbackCommand {
                    override fun perform(callback: MediaControllerCompat.Callback) {
                        callback.onRepeatModeChanged(repeatMode)
                    }
                }
            )
        }

        override fun onShuffleModeChanged(
            shuffleMode: Int
        ) {
            performOnAllCallbacks(
                object : CallbackCommand {
                    override fun perform(
                        callback: MediaControllerCompat.Callback
                    ) {
                        callback.onShuffleModeChanged(shuffleMode)
                    }
                }
            )
        }

        override fun onSessionDestroyed() {
            resetState()
            onPlaybackStateChanged(null)
            onDisconnected()
        }
    }
}