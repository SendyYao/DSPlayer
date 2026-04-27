package com.whisperyao.dsplayer

import com.whisperyao.dsplayer.activity.BaseActivity
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.mediasession.client.MediaBrowserHelper
import com.whisperyao.dsplayer.mediasession.notifications.MediaNotificationManager
import com.whisperyao.dsplayer.model.data.DataModelManager
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.playing.NowPlayingManager
import com.whisperyao.dsplayer.playing.PlayerControlHelper
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.SynoLog

object AppDependencies {

    private var initialized = false

    lateinit var playerStatusManager: PlayingStatusManager
        private set

    lateinit var playingQueueManager: PlayingQueueManager
        private set

    lateinit var nowPlayingManager: NowPlayingManager
        private set

    lateinit var mediaNotificationManager: MediaNotificationManager
        private set

    fun init() {
        if (initialized) return

        val dataModelManager = DataModelManager.instance

        playerStatusManager = dataModelManager.playingStatusManager

        playingQueueManager = dataModelManager.playingQueueManager

//        mediaNotificationManager = MediaNotificationManager()

        SynoLog.i("AppDependencies", "Dsid: ${Common.getDsId()}")

        nowPlayingManager = NowPlayingManager("YaoNAS")

        initialized = true
    }
}