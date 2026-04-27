package com.whisperyao.dsplayer.mediasession.service

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.widget.Toast
import com.synology.ThreadWork
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.RemoteController
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.mediasession.PlayerAdapter
import com.whisperyao.dsplayer.net.WebAPI
import com.whisperyao.dsplayer.playing.Player
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utilities
import com.whisperyao.dsplayer.vos.PlayingInfo
import java.util.LinkedList
import kotlin.collections.emptyList


class RemoteControllerService : AbstractRemoteControllerService() {

    companion object {
        private const val LIST_RENDERER_DELAY = 1500L
        private const val LOG = "RemoteControllerService"

        private const val MSG_RELOAD_SONG = 1
        private const val MSG_OPEN_CURRENT = 2
        private const val MSG_REFRESH_RENDERER_LIST = 3
        private const val MSG_GET_PLAYING_QUEUE = 5

        private const val POLLING_DELAY = 1000L
        private const val REMOTESERVICE_STATUS = 6164

        @Volatile
        private var mContinuePolling = true
    }

    private var mPollingWork: ThreadWork? = null
    private var mUpdateRendererList = false

    private var mLastPollingTime = 0L
    private var mLastRefreshTime = 0L
    private var mLastPlayingQueueTime = 0L

    private var repeatRetryCount = 5
    private var shuffleRetryCount = 5

    private val mDelayedHandler =
        Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                MSG_RELOAD_SONG -> reloadSong()
                MSG_OPEN_CURRENT -> openCurrent(msg.arg1)
                MSG_REFRESH_RENDERER_LIST -> refreshRendererList()
                MSG_GET_PLAYING_QUEUE -> doGetPlayingQueue()
            }
            true
        }

    override fun onCreate() {
        super.onCreate()

        mContinuePolling = true

        audioFocusManager.setup(this, getPlaybackProxy())

        setPlayer(playingStatusManager.player)

        // CacheManager.getInstance().registerOnRatingChangeObserver(onRatingChangeObserver)

        Toast.makeText(this, R.string.login_as_usb, Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDestroy() {
        // CacheManager.getInstance().unregisterOnRatingChangeObserver(onRatingChangeObserver)

        mContinuePolling = false

        audioFocusManager.release()

        SynoLog.i(LOG, "onDestroy")

        super.onDestroy()
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(emptyList())
        if (this.mSession.controller != null) {
            notifyUpdateUIState()
        }
    }

    override fun startPollingRenderer() {
        mPollingWork?.let {
            it.endThread()
            mPollingWork = null
        }

        mPollingWork = object : ThreadWork() {

            private lateinit var playerUniqId: String
            private var newInfo: PlayingInfo = PlayingInfo.getDummyInfo()

            override fun preWork() {
                playerUniqId = Common.getPlayerUniqueId()
            }

            @Throws(InterruptedException::class)
            override fun onWorking() {
                setPlayingInfo(PlayingInfo.getDummyInfo())

                while (mContinuePolling) {
                    try {
                        Thread.sleep(POLLING_DELAY)
                    } catch (_: InterruptedException) {
                        setPlayingInfo(PlayingInfo.getDummyInfo())
                    }

                    if (!isNeedUpdate() || !mContinuePolling) {
                        continue
                    }

                    val pollingInfo = try {
                        RemoteController.doPollingStatus()
                    } catch (e: PlayingInfo.DeviceNotFoundException) {
                        e.printStackTrace()
                        getPlayingInfo()
                    } catch (e: PlayingInfo.NextworkException) {
                        e.printStackTrace()
                        getPlayingInfo()
                    }

                    newInfo = pollingInfo ?: getPlayingInfo()

                    val deviceChanged =
                        playerUniqId != Common.getPlayerUniqueId()

                    val deviceLost = pollingInfo == null

                    if (deviceLost && !deviceChanged) {
                        setPlayingInfo(PlayingInfo.getDummyInfo())
                        Common.gDeviceChanged = true
                    }

                    if (deviceLost || deviceChanged) {
                        stopSelf()
                        return
                    }

                    handlePollingResult(newInfo)
                }
            }
        }

        mPollingWork?.startWork()
    }

    private fun handlePollingResult(info: PlayingInfo) {
        if (info.volume != getVolume()) {
            playingStatusManager.setCurrentPlayingStatus(
                getPlayerVolume(info)
            )
        }

        mLastPollingTime = System.currentTimeMillis()

        if (!mContinuePolling) return

        val metadataChanged =
            !info.equalMetaData(getPlayingInfo())

        val playStateChanged =
            !info.equalPlayState(getPlayingInfo())

        val playInfoChanged =
            !info.equalPlayInfo(getPlayingInfo())

        if (info.needRefresh(mLastPlayingQueueTime)) {
            mLastPlayingQueueTime = info.timeStamp
            mDelayedHandler.removeMessages(MSG_GET_PLAYING_QUEUE)
            mDelayedHandler.sendEmptyMessage(MSG_GET_PLAYING_QUEUE)
        }

        setPlayingInfo(info)

        playingQueueManager.setPlayIndex(
            getPlayingInfo().playPos
        )

        if (!reloadingSong) {
            handleSessionStateSync(
                info,
                metadataChanged,
                playStateChanged,
                playInfoChanged
            )
        }
    }

    private fun handleSessionStateSync(info: PlayingInfo, metadataChanged: Boolean, playStateChanged: Boolean, playInfoChanged: Boolean) {
        val playerAdapter: PlayerAdapter = getPlayerAdapter()
        disablePollingEvenIfScreenOff()

        syncShuffleMode(info)
        syncRepeatMode(info)

        if ((playStateChanged || metadataChanged)
            && playerAdapter.currentMedia != null
        ) {
            setMetadata(playerAdapter.currentMedia)
        }

        when {
            info.isPlaying() -> {
                if (playerAdapter.currentMedia != null) {
                    setNewState(PlaybackStateCompat.STATE_PLAYING)
                }
            }

            info.isPause() -> {
                if (playerAdapter.currentMedia != null) {
                    setNewState(PlaybackStateCompat.STATE_PAUSED)
                }
            }

            info.isStop() -> {
                setMetadata(generateNotPlayingItem())
                setNewState(PlaybackStateCompat.STATE_STOPPED)
            }

            else -> {
                setMetadata(generateNotPlayingItem())
                setNewState(PlaybackStateCompat.STATE_NONE)
            }
        }

        if (playInfoChanged) {
            acquireWifiLock()
            audioFocusManager.tryToGetAudioFocus()

            mDelayedHandler.removeMessages(MSG_RELOAD_SONG)
            mDelayedHandler.sendEmptyMessage(MSG_RELOAD_SONG)
        }

        if (!mUpdateRendererList &&
            info.needRefresh(mLastRefreshTime)
        ) {
            mUpdateRendererList = true

            mDelayedHandler.removeMessages(
                MSG_REFRESH_RENDERER_LIST
            )

            mDelayedHandler.sendEmptyMessageDelayed(
                MSG_REFRESH_RENDERER_LIST,
                LIST_RENDERER_DELAY
            )
        }
    }

    private fun syncShuffleMode(info: PlayingInfo) {
        val currentMode =
            mSession.controller.shuffleMode

        val remoteMode =
            info.shuffleMode.ordinal

        if (remoteMode != currentMode) {
            shuffleRetryCount++

            if (shuffleRetryCount > 5) {
                mCallback.onSetShuffleMode(remoteMode)
                shuffleRetryCount = 0
            }
        }
    }

    private fun syncRepeatMode(info: PlayingInfo) {
        val currentMode =
            mSession.controller.repeatMode

        val remoteMode =
            info.repeatMode.ordinal

        if (remoteMode != currentMode) {
            repeatRetryCount++

            if (repeatRetryCount > 5) {
                mCallback.onSetRepeatMode(remoteMode)
                repeatRetryCount = 0
            }
        }
    }

    private fun refreshRendererList() {
        playingStatusManager.requestLoadPlayers(
            object : PlayingStatusManager.LoadPlayerCallback {

                private var mPlayer: Player? = null

                override fun onPreLoad() {
                    mPlayer = playingStatusManager.player
                }

                override fun onPostLoad() {
                    mLastRefreshTime =
                        getPlayingInfo().timeStamp

                    if (!playingStatusManager.players.contains(mPlayer)) {
                        setPlayingInfo(
                            PlayingInfo.getDummyInfo()
                        )

                        Common.gDeviceChanged = true
                        stopSelf()
                    }
                }
            }
        )
    }

    fun enqueue(action: Common.PlaybackAction?, position: Int) {
        var position = position
        val idList: String?
        val tempQueue = this.playingQueueManager.popTempQueue()
        val size = tempQueue.size
        var playImmediately = false

        if (ConnectionManager.isUseWebAPI()) {
            idList = Utilities.createIdList(tempQueue)
        } else {
            val separator: String = if (Common.haveInternetRadio())
                Common.SZ_STRING_SEPARATOR
            else
                Common.SZ_DATABASE_SEPARATOR

            val ids = arrayOfNulls<String>(size)
            for (i in 0..<size) {
                ids[i] = tempQueue[i].id
            }

            idList = TextUtils.join(separator, ids)
        }

        if (action === Common.PlaybackAction.PLAY_NOW ||
            action === Common.PlaybackAction.ADD_PLAY ||
            (action === Common.PlaybackAction.BY_SITUACTION && !isPlaying())) {
            playImmediately = true
        }

        // 保持原始反编译逻辑
        getPlayingInfo().isStop()

        if (action === Common.PlaybackAction.ADD_NEXT) {
            position = getPlayingInfo().getStopIndex() + 1
        }

        RemoteController.enqueue(
            idList,
            action,
            position,
            playImmediately
        )

        updateMetadata()
    }

    private fun removeTracks(songItems: ArrayList<SongItem?>, list: IntArray): Int {
        val length = list.size
        val ids = arrayOfNulls<Int>(length)

        val queuePosition = getQueuePosition()
        var newQueuePosition = queuePosition

        for (i in 0..<length) {
            ids[i] = list[i]

            if (newQueuePosition != -1) {
                val removedIndex = list[i]

                if (queuePosition == removedIndex) {
                    newQueuePosition = -1
                } else if (queuePosition > removedIndex) {
                    newQueuePosition--
                }
            }
        }

        RemoteController.removeTracks(
            LinkedList(songItems),
            ids,
            newQueuePosition
        )

        return length
    }

    private fun updateTracks(songItems: ArrayList<SongItem?>, start: Int, limit: Int, ids: IntArray) {
        var queuePosition = getQueuePosition()

        if (queuePosition >= start
            && queuePosition <= (start + limit) - 1
        ) {
            for (i in ids.indices) {
                if (queuePosition == ids[i]) {
                    queuePosition = start + i
                    break
                }
            }
        }

        RemoteController.updateTracks(
            LinkedList(songItems),
            start,
            limit,
            ids,
            queuePosition
        )

        this.reloadingSong = true
    }

    fun getQueuePosition(): Int {
        return getPlayingInfo().playPos
    }

    fun setQueuePosition(pos: Int) {
        openCurrent(pos)
    }

    private fun getPlayerVolume(info: PlayingInfo): PlayingStatusManager.PlayerVolume {
        val playerVolume: PlayingStatusManager.PlayerVolume = PlayingStatusManager.PlayerVolume()
        playerVolume.volume = info.volume
        playerVolume.subPlayersVolumes = info.subPlayerVolume
        return playerVolume
    }

    override fun duration(): Int {
        return getPlayingSong().duration * 1000
    }

    override fun getCurrentPosition(): Long {
        val delta =
            if (isPlaying() && mLastPollingTime > 0) {
                minOf(
                    System.currentTimeMillis() - mLastPollingTime,
                    1000L
                )
            } else {
                0L
            }

        if (!isPlaying() && !isPause()) {
            return 0L
        }

        val position =
            getPlayingInfo().position * 1000 + delta

        if (mExpectedSeekPosition != -1L &&
            kotlin.math.abs(
                mExpectedSeekPosition - position
            ) >= 5000
        ) {
            return mExpectedSeekPosition
        }

        mExpectedSeekPosition = -1L

        return position
    }

    override fun initMediaSessionCallback(): MediaSessionCallback {
        return SessionCallback()
    }

    private inner class SessionCallback : MediaSessionCallback() {

        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)

            setQueuePosition(
                playingQueueManager.getPlayIndex()
            )
        }

        override fun onClearQueue() {
            super.onClearQueue()
            setQueue(arrayListOf(), -1)
        }

        override fun onCustomAction(action: String, extras: Bundle) {
            super.onCustomAction(action, extras)

            object : ThreadWork() {
                override fun onWorking() {
                    if (TextUtils.equals(CUSTOM_ACTION_ADD_NEXT, action) ||
                        TextUtils.equals(CUSTOM_ACTION_PLAY_NOW, action) ||
                        TextUtils.equals(CUSTOM_ACTION_ADD_ONLY, action) ||
                        TextUtils.equals(CUSTOM_ACTION_ADD_PLAY, action) ||
                        TextUtils.equals(CUSTOM_ACTION_BY_SITUATION, action))
                    {
                        this@RemoteControllerService.enqueue(
                            Common.PlaybackAction.fromId(
                                extras.getInt("playbackAction")
                            ),
                            extras.getInt("position")
                        )
                        return
                    }

                    if (TextUtils.equals(
                            CUSTOM_ACTION_REMOVE_BYID,
                            action
                        )
                    ) {
                        val ids = extras.getIntArray("ids")

                        this@RemoteControllerService.removeTracks(
                            ArrayList(
                                this@RemoteControllerService
                                    .playingQueueManager
                                    .getQueue()
                            ),
                            ids!!
                        )
                    } else if (TextUtils.equals(
                            CUSTOM_ACTION_REORDER,
                            action
                        )
                    ) {
                        val start = extras.getInt("start")
                        val limit = extras.getInt(WebAPI.WebApiPin.LIMIT)

                        val list =
                            extras.getIntegerArrayList("ids")

                        val ids = IntArray(list!!.size)

                        for (i in list.indices) {
                            ids[i] = list[i]!!
                        }

                        this@RemoteControllerService.updateTracks(
                            ArrayList(
                                this@RemoteControllerService
                                    .playingQueueManager
                                    .getQueue()
                            ),
                            start,
                            limit,
                            ids
                        )
                    }
                }
            }.startWork()
        }

        override fun onSetRating(rating: RatingCompat?) {}

        override fun onSetRating(rating: RatingCompat?, extras: Bundle?) { }
    }
}