package com.whisperyao.dsplayer.mediasession.service

import android.content.Intent
import android.content.IntentFilter
import android.media.metrics.PlaybackStateEvent.STATE_STOPPED
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.lifecycleScope
import com.synology.ScreenReceiver
import com.whisperyao.dsplayer.AudioFocusManager
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.RemoteController
import com.whisperyao.dsplayer.ame.AMEStatusHelper
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.mediasession.PlayerAdapter
import com.whisperyao.dsplayer.playing.Player
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.vos.PlayingInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class AbstractRemoteControllerService : AbstractMediaBrowserService() {

    companion object {
        protected const val QUEUE_LIMIT_PER_TIME = 1024
        private var remoteWifiLock: WifiManager.WifiLock? = null
    }

    private var mPollingEvenIfScreenOff = false
    private var player: Player? = null

    protected var reloadingSong = false
    private val screenReceiver = ScreenReceiver()

    private val mLock = ReentrantReadWriteLock()
    private val mSongLock = ReentrantReadWriteLock()
    private val mTempQueueLock = ReentrantReadWriteLock()

    private var playingInfo = PlayingInfo.getDummyInfo()
    private var playingSong = SongItem.generateNoneSong()

    protected var mExpectedSeekPosition = -1L

    private val mPlayingQueue = arrayListOf<SongItem>()

    private val playerAdapter = object : PlayerAdapter() {

        override fun getBufferingPercent(): Long = 0L

        override fun getCurrentMedia(): SongItem? {
            return if (TextUtils.isEmpty(getPlayingSong().id)) {
                null
            } else {
                getPlayingSong()
            }
        }

        override fun isPlaying(): Boolean {
            return getPlayingInfo().isPlaying
        }
    }

    protected val onRatingChangeObserver =
        CacheManager.OnRatingChangeObserver { list -> handleRatingChanged(list) }

    private val playbackProxy = object : AudioFocusManager.PlaybackProxy {

            override fun isPreparing(): Boolean = false

            override fun isRemotePlayer(): Boolean = true

            override fun toDuckVolume() {
                // remote player no-op
            }

            override fun toNormalVolume() {
                // remote player no-op
            }

            override fun isPlaying(): Boolean {
                return this@AbstractRemoteControllerService.isPlaying()
            }

            override fun hasAudioToPlay(): Boolean {
                return isPlaying() || this@AbstractRemoteControllerService.isPause()
            }

            override fun play() {
                this@AbstractRemoteControllerService.onPlay()
            }

            override fun pause() {
                this@AbstractRemoteControllerService.onPause()
            }

            override fun setRemoteVolume(newVolume: Int) {
                this@AbstractRemoteControllerService.setVolume(newVolume)
            }

            override fun getRemoteVolume(): Int {
                return this@AbstractRemoteControllerService.getVolume()
            }
        }

    protected abstract fun startPollingRenderer()

    override fun onCreate() {
        super.onCreate()

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )

        // CacheManager.getInstance().addOnRatingChangeObserver(onRatingChangeObserver)
    }

    override fun onDestroy() {
        // CacheManager.getInstance().removeOnRatingChangeObserver(onRatingChangeObserver)

        unregisterReceiver(screenReceiver)

        super.onDestroy()
    }

    protected fun getPlayer(): Player? = player

    protected fun setPlayer(player: Player?) {
        val oldPlayer = this.player
        this.player = player

        if (player != oldPlayer) {
            initializeRemotePlayer()
        }
    }

    protected fun setPlayingInfo(newInfo: PlayingInfo) {
        mLock.write {
            if (newInfo.aacTimeStamp > 0 && playingInfo.aacTimeStamp >= 0) {
                AMEStatusHelper.checkRemotePlayerAME(
                    this,
                    playingInfo,
                    newInfo
                )
            }
            playingInfo = newInfo
        }
    }

    protected fun getPlayingInfo(): PlayingInfo {
        return mLock.read { playingInfo }
    }

    private fun setPlayingSong(newSong: SongItem) {
        mSongLock.write {
            playingSong = newSong
        }
    }

    protected fun getPlayingSong(): SongItem {
        return mSongLock.read { playingSong }
    }

    fun isNeedUpdate(): Boolean {
        return mPollingEvenIfScreenOff || screenReceiver.isScreenOn
    }

    private fun enablePollingEvenIfScreenOff() {
        mPollingEvenIfScreenOff = true
    }

    fun disablePollingEvenIfScreenOff() {
        mPollingEvenIfScreenOff = false
    }

    protected fun reloadSong() {
        SynoLog.i(TAG, "reloadSong")

        lifecycleScope.launch(Dispatchers.IO) {
            val song = RemoteController.reloadSong(getPlayingInfo().playPos)

            playingQueueManager.setPlayIndex(getPlayingInfo().playPos)

            if (song != null) {
                setPlayingSong(song)
                setMetadata(playerAdapter.currentMedia)
            } else {
                setPlayingSong(SongItem.generateNoneSong())
            }

            if (getPlayingSong().artist.isEmpty() &&
                getPlayingSong().album.isEmpty()
            ) {
                getPlayingSong().artist = getPlayingInfo().artist
            }

            CacheManager.getInstance().adjustRating(getPlayingSong())
            requestToGetCurrentSongRating()
        }
    }

    private fun requestToGetCurrentSongRating() {
        val song = getPlayingSong()

        if (ConnectionManager.canEditRating(true, song)) {
            lifecycleScope.launch(Dispatchers.IO) {
                CacheManager.getInstance().requestToGetRating(song)
            }
        }
    }

    override fun onPlay() {
        lifecycleScope.launch(Dispatchers.IO) {
            acquireWifiLock()
            audioFocusManager.tryToGetAudioFocus()
            RemoteController.play()
        }
    }

    override fun onPause() {
        lifecycleScope.launch(Dispatchers.IO) {
            RemoteController.pause()
            releaseWifiLock()
            mExpectedSeekPosition = -1L
        }
    }

    override fun onStop() {
        lifecycleScope.launch(Dispatchers.IO) {
            setMetadata(generateNotPlayingItem())
            setNewState(STATE_STOPPED)

            audioFocusManager.giveUpAudioFocus()

            RemoteController.stop()

            releaseWifiLock()

            mExpectedSeekPosition = -1L
        }
    }

    override fun onSkipToNext() {
        lifecycleScope.launch(Dispatchers.IO) {
            RemoteController.next()
            mExpectedSeekPosition = -1L
        }
    }

    override fun onSkipToPrevious() {
        lifecycleScope.launch(Dispatchers.IO) {
            RemoteController.prev()
            mExpectedSeekPosition = -1L
        }
    }

    override fun onSeekTo(position: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            mExpectedSeekPosition = position
            audioFocusManager.tryToGetAudioFocus()
            RemoteController.seek(position)
        }
    }

    fun openCurrent(pos: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            acquireWifiLock()
            RemoteController.jumpPlay(pos)
        }
    }

    override fun setVolume(volume: Int) {
        super.setVolume(volume)

        lifecycleScope.launch(Dispatchers.IO) {
            RemoteController.setVolume(volume)
            enablePollingEvenIfScreenOff()
        }
    }

    override fun getVolume(): Int {
        return playingStatusManager.currentVolume
    }

    override fun setSubPlayersVolume(volumes: HashMap<String, Int>) {
        lifecycleScope.launch(Dispatchers.IO) {
            RemoteController.setMultiVolume(volumes)
            enablePollingEvenIfScreenOff()
        }
    }

    override fun setRepeatMode(repeatMode: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            RemoteController.setRepeatMode(
                Common.RepeatMode.fromId(repeatMode)
            )
            super.setRepeatMode(repeatMode)
        }
    }

    override fun setShuffleMode(shuffleMode: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            RemoteController.setShuffleMode(
                Common.ShuffleMode.fromId(shuffleMode)
            )
            super.setShuffleMode(shuffleMode)
        }
    }

    override fun doClearQueueIfNecessary() {
        lifecycleScope.launch(Dispatchers.IO) {
            RemoteController.clearQueue()
        }
    }

    protected fun doGetPlayingQueue() {
        lifecycleScope.launch(Dispatchers.IO) {
            val queue = if (ConnectionManager.isUseWebAPI()) {
                loadQueueByPage()
            } else {
                RemoteController.getPlayingQueue()
            }

            setQueue(queue)
            playingQueueManager.setPlayIndex(getPlayingInfo().playPos)
            updateMetadata()
        }
    }

    private fun loadQueueByPage(): List<SongItem> {
        val queueSize = RemoteController.getQueueSize()

        mTempQueueLock.write {
            mPlayingQueue.clear()

            var offset = 0
            while (offset < queueSize) {
                mPlayingQueue.addAll(
                    RemoteController.getPlayingQueue(
                        offset,
                        QUEUE_LIMIT_PER_TIME
                    )
                )
                offset += QUEUE_LIMIT_PER_TIME
            }
        }

        return mPlayingQueue
    }

    fun updateMetadata() {
        setMetadata(playingQueueManager.getSongItem())
    }

    private fun handleRatingChanged(list: List<SongItem>) {
        val currentSong = getPlayingSong()

        list.forEach { song ->
            if (
                song.dsId == currentSong.dsId &&
                song.id == currentSong.id
            ) {
                currentSong.songRating = song.songRating
            }
        }
    }

    fun isPlaying(): Boolean = getPlayingInfo().isPlaying

    fun isPause(): Boolean = getPlayingInfo().isPause

    private fun initializeRemotePlayer() {
        isPlayerSwitched = false
        setQueue(emptyList())
        startPollingRenderer()
        doGetPlayingQueue()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        setPlayer(playingStatusManager.player)
        return super.onGetRoot(clientPackageName, clientUid, rootHints)
    }

    override fun provideWifiLock(): WifiManager.WifiLock {
        if (remoteWifiLock == null) {
            remoteWifiLock = wifiLockProvider.get()
        }

        return requireNotNull(remoteWifiLock) {
            "WifiLock must not be null"
        }
    }

    override fun getPlayerAdapter(): PlayerAdapter {
        return playerAdapter
    }

    protected fun getPlaybackProxy(): AudioFocusManager.PlaybackProxy {
        return playbackProxy
    }
}