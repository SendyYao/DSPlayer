package com.whisperyao.dsplayer.activity

import android.app.ProgressDialog
import android.content.Intent
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.datasource.network.ConnectionManager
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.mediasession.QueueMaxCount
import com.whisperyao.dsplayer.mediasession.client.MediaBrowserHelper
import com.whisperyao.dsplayer.mediasession.service.AbstractMediaBrowserService
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.ConnectionManagerProvider
import com.whisperyao.dsplayer.util.StoragePermissionHelper
import com.whisperyao.dsplayer.util.SynoLog
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject
import javax.inject.Provider


abstract class BaseActivity : DaggerAppCompatActivity(), ConnectionManagerProvider {

    @Inject
    lateinit var classProvider: Provider<Class<out AbstractMediaBrowserService>>

    var connectionManager: ConnectionManager = App.connectionManager

    // PlayingStatusManager(ChromeCastHelper(App.getContext()))
    // var playerStatusManager: PlayingStatusManager = DataModelManager.getMInstance().playingStatusManager

    @Inject
    lateinit var playerStatusManager: PlayingStatusManager

    @Inject
    lateinit var progressDialog: ProgressDialog

    var mMediaBrowserHelper: MediaBrowserHelper? = null

    interface ContainerPlayer {
        fun checkSize(
            action: Common.PlaybackAction,
            list: List<SongItem>
        ): Boolean

        fun getOutOfCapacityString(str: String): String

        fun getQueueFreeSize(
            action: Common.PlaybackAction
        ): Int

        fun playContainer(
            action: Common.PlaybackAction,
            list: List<SongItem>,
            position: Int,
            isFromMenu: Boolean
        )
    }

    override fun provideConnectionManager(): ConnectionManager {
        return connectionManager
    }

    protected fun setMediaBrowserHelper(mediaBrowserHelper: MediaBrowserHelper?) {
        this.mMediaBrowserHelper = mediaBrowserHelper!!
    }

    protected fun getMediaBrowserHelper(): MediaBrowserHelper? {
        return this.mMediaBrowserHelper
    }

    open fun playContainer(
        action: Common.PlaybackAction,
        list: List<SongItem>,
        position: Int,
        isFromMenu: Boolean = false
    ) {
        SynoLog.d("BaseActivity", "action: ${action.name}")
        mMediaBrowserHelper?.enqueue(action, list, position)
    }


    open fun checkSize(
        action: Common.PlaybackAction,
        list: List<SongItem>
    ): Boolean {
        val freeSize: Int = getQueueFreeSize(action)
        SynoLog.d("BaseActivity", "QueueFreeSize: $freeSize, listSize: ${list.size}")
        return freeSize >= list.size
    }

    private fun getQueueSize(): Int {
        return mMediaBrowserHelper?.mMediaController?.queue?.size ?: 0
    }

    fun getQueueFreeSize(
        action: Common.PlaybackAction
    ): Int {
        val maxQueueSize = getMaxQueueSize()

        return if (action != Common.PlaybackAction.PLAY_NOW) {
            maxQueueSize - getQueueSize()
        } else {
            maxQueueSize
        }
    }

    fun getMaxQueueSize(): Int {
        return QueueMaxCount.getQueueMaxByPlayer(playerStatusManager.playMode)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 9487) {
            StoragePermissionHelper.onActivityResult(this, resultCode, data)
        }
    }


    override fun onStart() {
        super.onStart()

        if (getNeedReConnect()) {
//            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

//        if (!EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().register(this)
//        }
    }

    override fun onPause() {
//        if (EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().unregister(this)
//        }

        super.onPause()
    }

    open fun getNeedReConnect(): Boolean {
        // AudioPreference.keepLogin() && Common.getCookieStore() == null && Common.getAudioInfo() != null
        return false
    }
}