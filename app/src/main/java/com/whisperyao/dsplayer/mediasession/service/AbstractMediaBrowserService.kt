package com.whisperyao.dsplayer.mediasession.service

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.animation.LinearInterpolator
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.media.MediaBrowserServiceCompat
import com.synology.sylibx.passcode.PassCodeObserver
import com.synology.sylibx.synofile.ExtensionsKt.registerReceiverCompat
import com.whisperyao.dsplayer.mediasession.MediaButtonReceiver
import com.whisperyao.dsplayer.AudioFocusManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.mediasession.PlayerAdapter
import com.whisperyao.dsplayer.mediasession.notifications.MediaNotificationManager
import com.whisperyao.dsplayer.mediasession.players.PlaybackInfoListener
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.CoverUtil
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utils
import com.whisperyao.dsplayer.util.extension.SongExtensionsKt.addCover
import com.whisperyao.dsplayer.util.extension.SongExtensionsKt.getMediaId
import com.whisperyao.dsplayer.util.extension.SongExtensionsKt.toMetadata
import dagger.android.AndroidInjection
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import kotlin.properties.Delegates


abstract class AbstractMediaBrowserService: MediaBrowserServiceCompat(), LifecycleOwner {
    companion object {
        const val TAG = "AbstractMediaBrowserService"
        const val CUSTOM_ACTION_ADD_NEXT: String = "com.synology.dsaudio.mediabrowserservice.action.add_next"
        const val CUSTOM_ACTION_ADD_ONLY: String = "com.synology.dsaudio.mediabrowserservice.action.add_only"
        const val CUSTOM_ACTION_ADD_PLAY: String = "com.synology.dsaudio.mediabrowserservice.action.add_play"
        const val CUSTOM_ACTION_BY_SITUATION: String = "com.synology.dsaudio.mediabrowserservice.action.by_situation"
        const val CUSTOM_ACTION_PLAY_NOW: String = "com.synology.dsaudio.mediabrowserservice.action.play_now"
        const val CUSTOM_ACTION_REMOVE_ALL: String = "com.synology.dsaudio.mediabrowserservice.action.remove_all"
        const val CUSTOM_ACTION_REMOVE_BYID: String = "com.synology.dsaudio.mediabrowserservice.action.remove_byid"
        const val CUSTOM_ACTION_REORDER: String = "com.synology.dsaudio.mediabrowserservice.action.reorder"
        const val CUSTOM_ACTION_SET_MULTI_VOLUME: String = "com.synology.dsaudio.mediabrowserservice.action.multi_vol"
        const val CUSTOM_ACTION_SET_SINGLE_VOLUME: String = "com.synology.dsaudio.mediabrowserservice.action.single_vol"
        const val CUSTOM_ACTION_SWITCH_PLAYER: String = "com.synology.dsaudio.mediabrowserservice.action.switch_player"
        const val CUSTOM_ACTION_UPDATE_UI: String = "com.synology.dsaudio.mediabrowserservice.action.update_ui"
        const val KEY_VOLUME: String = "volume"
        const val prefix: String = "com.synology.dsaudio.mediabrowserservice.action."
    }

    @Inject
    lateinit var audioManager: AudioManager

    @Inject
    lateinit var mMediaNotificationManager: MediaNotificationManager

    @Inject
    lateinit var playingQueueManager: PlayingQueueManager

    @Inject
    lateinit var playingStatusManager: PlayingStatusManager

    @Inject
    lateinit var wifiLockProvider: Provider<WifiLock>

    protected lateinit var mSession: MediaSessionCompat
    protected lateinit var mCallback: MediaSessionCallback

    private lateinit var mControllerCallback: ControllerCallback
    private var mPlaybackInfoListener: MediaPlayerListener? = null

    private var mDisposableProgress: Disposable? = null
    private var mDisposableUpdateCover: Disposable? = null

    private var receiver: BroadcastReceiver? = null

    protected var isPlayerSwitched: Boolean = false
    private var mServiceInStartedState: Boolean = false

    lateinit var audioFocusManager: AudioFocusManager

    private val mLifecycleDispatcher = ServiceLifecycleDispatcher(this)

    private var state by Delegates.notNull<Int>()

    private val fgBgObserver = object : PassCodeObserver.AppCallback {
        override fun onForeground() {
            audioFocusManager.resetVolumeDetector(
                AudioPreference.enableRemoteController(this@AbstractMediaBrowserService)
            )
        }

        override fun onBackground() {
            audioFocusManager.resetVolumeDetector(false)
        }
    }

    protected abstract fun duration(): Int

    protected abstract fun getCurrentPosition(): Long

    protected abstract fun getPlayerAdapter(): PlayerAdapter?

    protected abstract fun initMediaSessionCallback(): MediaSessionCallback

    protected abstract fun onPause()
    protected abstract fun onPlay()
    protected abstract fun onSeekTo(position: Long)
    protected abstract fun onSkipToNext()
    protected abstract fun onSkipToPrevious()
    protected abstract fun onStop()

    protected abstract fun getVolume(): Int

    abstract fun provideWifiLock(): WifiLock

    protected open fun getCurrentPlaybackSpeed(): Float = 1.0f

    protected open fun logStartForeground(throwable: Throwable) {}

    protected open fun doBeforeSwitchIfNecessary(listToSave: List<SongItem>) { }

    protected open fun doClearQueueIfNecessary() { }

    protected open fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) { }

    protected open fun setSubPlayersVolume(volumes: HashMap<String, Int>) { }

    fun getMState(): Int {
        return this.state
    }

    fun setQueue(queue: List<SongItem>, startIndex: Int = -1) {
        if (isPlayerSwitched) return
        playingQueueManager.setQueue(queue, startIndex)
    }

    fun appendQueue(queue: List<SongItem>, startIndex: Int, silent: Boolean = false) {
        if (isPlayerSwitched) return
        playingQueueManager.appendQueue(
            queue,
            startIndex,
            silent
        )
    }

    private val wifiLock: WifiLock
        get() = provideWifiLock()


    private val token: MediaSessionCompat.Token
        get() = sessionToken
            ?: throw NullPointerException(
                "session token should not be null"
            )

    protected fun getSession(): MediaSessionCompat {
        val mediaSessionCompat: MediaSessionCompat = this.mSession
        return mediaSessionCompat
    }

    protected fun getRepeatMode(): Common.RepeatMode {
        val repeatModeFromId: Common.RepeatMode =
            Common.RepeatMode.fromId(getSession().controller.repeatMode)

        return repeatModeFromId
    }

    override val lifecycle: Lifecycle
        get() = mLifecycleDispatcher.lifecycle

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        SynoLog.d("AbstractMediaBrowserService", "onCreate")
        this.mLifecycleDispatcher.onServicePreSuperOnCreate();

        AndroidInjection.inject(this)

        super.onCreate()

        registerForegroundListener()

        mMediaNotificationManager.cancelAll()

        mSession = MediaSessionCompat(this, javaClass.simpleName).apply {
            setQueue(ArrayList())
        }

        mCallback = initMediaSessionCallback()

        mSession.apply {
            setCallback(mCallback)
            setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }

        sessionToken = mSession.sessionToken

        SynoLog.d("AbstractMediaBrowserService", "sessionToken: $sessionToken")

        mControllerCallback = ControllerCallback()
        mSession.controller.registerCallback(mControllerCallback)

        registerReceiver()

        playingStatusManager.currentVolume = getVolume()

        mPlaybackInfoListener = MediaPlayerListener()

        audioFocusManager = AudioFocusManager()

        setMetadata(generateNotPlayingItem())

        setNewState(PlaybackStateCompat.STATE_NONE)

        mDisposableUpdateCover = CoverUtil.getCoverUpdatedMediaId()
            .doOnNext { mediaId ->
                val currentSong = playingQueueManager.getSongItem()

                if (currentSong?.mediaId == mediaId) {
                    setMetadata(currentSong)
                }
            }
            .subscribe({}, {})

        mDisposableProgress =
            Observable.interval(150, TimeUnit.MILLISECONDS)
                .subscribe({
                    playingQueueManager.setProgress(
                        getCurrentPosition(),
                        getPlayerAdapter()?.bufferingPercent ?: 0
                    )
                }, {})

        mSession.isActive = true

        SynoLog.d(TAG, "onCreate: PlaybackService creating MediaSession, and MediaNotificationManager")
    }

    protected fun generateNotPlayingItem(): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
            .putString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                PlayingQueueManager.notPlayingMediaId
            )
            .build()
    }

    protected fun setMetadata(songItem: SongItem?) {
        SynoLog.d(
            "MetaDebug",
            "service session token=${mSession.sessionToken}"
        )

        setMetadata(songItem?.toMetadata())
    }

    protected fun setMetadata(metadata: MediaMetadataCompat?) {
        mSession.setMetadata(
            metadata?.addCover(
                playingQueueManager.getAlbumBitmap(
                    this,
                    metadata.getMediaId()
                )
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return

                when (action) {
                    MediaButtonReceiver.ACTION_PLAY ->
                        mCallback.onPlay()

                    MediaButtonReceiver.ACTION_PAUSE ->
                        mCallback.onPause()

                    MediaButtonReceiver.ACTION_STOP ->
                        mCallback.onStop()

                    MediaButtonReceiver.ACTION_NEXT ->
                        mCallback.onSkipToNext()

                    MediaButtonReceiver.ACTION_PREV ->
                        mCallback.onSkipToPrevious()

                    MediaButtonReceiver.ACTION_SHUFFLE ->
                        onShuffleClick()

                    MediaButtonReceiver.ACTION_REPEAT ->
                        onRepeatClick()

                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        if (playingStatusManager.isPlayModeStreaming) {
                            onPause()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(MediaButtonReceiver.ACTION_PLAY)
            addAction(MediaButtonReceiver.ACTION_PAUSE)
            addAction(MediaButtonReceiver.ACTION_STOP)
            addAction(MediaButtonReceiver.ACTION_NEXT)
            addAction(MediaButtonReceiver.ACTION_PREV)
            addAction(MediaButtonReceiver.ACTION_SHUFFLE)
            addAction(MediaButtonReceiver.ACTION_REPEAT)
        }
        val abstractMediaBrowserService: AbstractMediaBrowserService = this
        abstractMediaBrowserService.registerReceiverCompat(
            receiver as BroadcastReceiver,
            filter,
            false
        )
    }

    protected fun onRepeatClick() {
        val nextMode = when (getRepeatMode()) {
            Common.RepeatMode.NONE -> Common.RepeatMode.ALL
            Common.RepeatMode.ALL -> Common.RepeatMode.ONE
            Common.RepeatMode.ONE -> Common.RepeatMode.NONE
        }

        mSession.setRepeatMode(nextMode.ordinal)
    }

    protected fun getShuffleMode(): Common.ShuffleMode {
        return Common.ShuffleMode.fromId(getSession().controller.shuffleMode)
    }

    protected fun onShuffleClick() {
        val nextMode = when (getShuffleMode()) {
            Common.ShuffleMode.NONE -> Common.ShuffleMode.AUTO
            Common.ShuffleMode.AUTO -> Common.ShuffleMode.NONE
        }

        mSession.setShuffleMode(nextMode.ordinal)
    }

    override fun onBind(intent: Intent): IBinder? {
        mLifecycleDispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    override fun onStart(intent: Intent, startId: Int) {
        this.mLifecycleDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        mLifecycleDispatcher.onServicePreSuperOnStart()
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected open fun setRepeatMode(repeatMode: Int) {
        setNewState(getSession().controller.playbackState.state)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected open fun setShuffleMode(shuffleMode: Int) {
        setNewState(getSession().controller.playbackState.state)
    }

    protected open fun setVolume(volume: Int) {
        playingStatusManager.currentVolume = volume
    }

    override fun onDestroy() {
        releaseWifiLock()

        mLifecycleDispatcher.onServicePreSuperOnDestroy()

        unregisterReceiver(receiver)

        mMediaNotificationManager.onDestroy()

        mDisposableUpdateCover?.dispose()
        mDisposableProgress?.dispose()

        setMetadata(generateNotPlayingItem())

        mSession.isActive = false
        mSession.controller.unregisterCallback(mControllerCallback)

        mSession.release()

        resetWidget()

        unregisterForegroundListener()

        SynoLog.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released")

        super.onDestroy()
    }

    private fun resetWidget() {
        val intent = Intent().apply {
            action = MediaButtonReceiver.ACTION_APPWIDGET_UPDATE
            setPackage(packageName)
        }

        sendBroadcast(intent)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return if (clientPackageName.startsWith(packageName)) {
            BrowserRoot(PlayingQueueManager.rootMediaId, rootHints)
        } else {
            BrowserRoot(PlayingQueueManager.notAllowedRootMediaId, null)
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    fun getAvailableActions(): Long {
        if (this.state == 1) {
            return 3126L
        }
        if (this.state != 2) {
            return if (this.state != 3) 3639L else 3379L
        }
        return 3125L
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setNewState(newPlayerState: Int) {
        setNewStateInternal(newPlayerState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setNewStateInternal(newPlayerState: Int) {
        state = newPlayerState

        val currentPosition = getCurrentPosition()

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(getAvailableActions())
            .setState(
                state,
                currentPosition,
                getCurrentPlaybackSpeed(),
                SystemClock.elapsedRealtime()
            )
            .setBufferedPosition(getPlayerAdapter()?.bufferingPercent ?: 0)

        if (playingQueueManager.getSongItem() != null) {
            stateBuilder.setActiveQueueItemId(
                playingQueueManager.getPlayIndex().toLong()
            )
        }

        setCustomAction(stateBuilder)

        mPlaybackInfoListener?.onPlaybackStateChange(
            stateBuilder.build()
        )
    }

    private inner class MediaPlayerListener : PlaybackInfoListener() {

        private val serviceManager = ServiceManager()

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPlaybackStateChange(state: PlaybackStateCompat) {
            mSession.setPlaybackState(state)

            when (state.state) {
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED -> {
                    serviceManager.moveServiceOutOfStartedState(state)
                }

                PlaybackStateCompat.STATE_PAUSED -> {
                    serviceManager.updateNotificationForPause(state)
                }

                PlaybackStateCompat.STATE_PLAYING -> {
                    serviceManager.moveServiceToStartedState(state)
                }
            }
        }
    }

    inner class ServiceManager {

        fun moveServiceToStartedState(state: PlaybackStateCompat) {
            val song = playingQueueManager.getSongItem() ?: return
            val metadata = song.toMetadata() ?: return

            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMediaNotificationManager.getNotification(
                    metadata,
                    state,
                    token
                )
            } else {
                TODO("VERSION.SDK_INT < O")
            }

            if (!mServiceInStartedState) {
                if (Utils.isSdk31()) {
                    startForeground(412, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(412, notification)
                }

                mServiceInStartedState = true
            } else {
                mMediaNotificationManager.notificationManager.notify(
                    412,
                    notification
                )
            }

            audioFocusManager.toggleDetect(true)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun updateNotificationForPause(state: PlaybackStateCompat) {
            if (!Utils.isSdk31()) {
                stopForeground(false)
                mServiceInStartedState = false
            }

            val song = playingQueueManager.getSongItem() ?: return
            val metadata = song.toMetadata() ?: return

            mMediaNotificationManager.notificationManager.notify(
                412,
                mMediaNotificationManager.getNotification(metadata, state, token)
            )

            audioFocusManager.toggleDetect(false)
        }

        fun moveServiceOutOfStartedState(state: PlaybackStateCompat) {
            stopForeground(true)

            mMediaNotificationManager.notificationManager.cancel(412)

            audioFocusManager.toggleDetect(false)

            stopSelf()

            mServiceInStartedState = false
        }
    }

    protected open inner class MediaSessionCallback: MediaSessionCompat.Callback() {
        override fun onSkipToQueueItem(id: Long) { }

        override fun onPlay() {
            this@AbstractMediaBrowserService.onPlay()
        }

        override fun onPause() {
            this@AbstractMediaBrowserService.onPause()
        }

        override fun onSkipToNext() {
            this@AbstractMediaBrowserService.onSkipToNext()
        }

        override fun onSkipToPrevious() {
            this@AbstractMediaBrowserService.onSkipToPrevious()
        }

        override fun onStop() {
            this@AbstractMediaBrowserService.onStop()
        }

        override fun onSeekTo(pos: Long) {
            this@AbstractMediaBrowserService.onSeekTo(pos)
        }

        open fun onClearQueue() {
            onStop()
            this@AbstractMediaBrowserService.getSession().setQueue(emptyList())
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat) {
            val queue = mSession.controller.queue?.toMutableList() ?: mutableListOf()

            val index = queue.indexOfFirst {
                it.description.hashCode() == description.hashCode()
            }

            if (index >= 0) {
                queue.removeAt(index)
            }

            mSession.setQueue(queue)
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            mSession.setRepeatMode(repeatMode)
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            mSession.setShuffleMode(shuffleMode)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCustomAction(action: String, extras: Bundle) {

            SynoLog.i(TAG, "onCustomAction")
            super.onCustomAction(action, extras)

            when (action) {

                CUSTOM_ACTION_UPDATE_UI -> {
                    val metadata = mSession.controller.metadata ?: return
                    setMetadata(metadata)

                    mSession.controller.playbackState?.let {
                        setNewState(it.state)
                    }
                }

                CUSTOM_ACTION_SET_MULTI_VOLUME -> {
                    val volumeMap =
                        extras.getSerializable("volume") as? HashMap<String, Int>
                            ?: return

                    setSubPlayersVolume(volumeMap)
                }

                CUSTOM_ACTION_SET_SINGLE_VOLUME -> {
                    SynoLog.d("AbstractMediaBrowserService", "volume: $extras")
                    val volume = extras.getInt("volume") ?: return
                    SynoLog.d("AbstractMediaBrowserService", "volume: $volume")
                    setVolume(volume)
                }

                CUSTOM_ACTION_REMOVE_ALL -> {
                    doClearQueueIfNecessary()
                    onClearQueue()
                }

                CUSTOM_ACTION_SWITCH_PLAYER -> {
                    isPlayerSwitched = true

                    doBeforeSwitchIfNecessary(ArrayList(playingQueueManager.getQueue()))

                    playingQueueManager.clearQueue()
                    stopSelf()
                }
            }
        }
    }

    protected inner class ControllerCallback : MediaControllerCompat.Callback(), ValueAnimator.AnimatorUpdateListener {

        private var progressAnimator: ValueAnimator? = null

        override fun onRepeatModeChanged(repeatMode: Int) {
            setRepeatMode(repeatMode)
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            setShuffleMode(shuffleMode)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)

            progressAnimator?.cancel()
            progressAnimator = null

            val position = state?.position?.toInt() ?: 0

            if (state?.state == PlaybackStateCompat.STATE_PLAYING) {
                val duration = getMax()
                val remain = ((duration - position) / state.playbackSpeed).toInt()

                if (remain >= 0) {
                    progressAnimator =
                        ValueAnimator.ofInt(position, duration).apply {
                            setDuration(remain.toLong())
                            interpolator = LinearInterpolator()
                            addUpdateListener(this@ControllerCallback)
                            start()
                        }
                }
            }

            updateWidgetPlayState()
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            if (metadata == null) return

            val controller = mSession.controller
            val currentMetadata = controller.metadata

            val mediaId =
                currentMetadata?.description?.mediaId

            if (mediaId == PlayingQueueManager.notPlayingMediaId) {
                updateWidgetPlayState()
                return
            }

            val state = controller.playbackState.state

            if (state != 0 && state != 1 && state != 8) {
                val playbackState = controller.playbackState

                mMediaNotificationManager.notificationManager.notify(
                    412,
                    mMediaNotificationManager.getNotification(
                        metadata,
                        playbackState,
                        token
                    )
                )
            }

            updateWidgetPlayState()
        }

        override fun onAnimationUpdate(animator: ValueAnimator) {
            val progress = animator.animatedValue as Int
        }

        private fun updateWidgetPlayState() {
            val controller = mSession.controller

            if (
                controller.metadata == null ||
                controller.playbackState == null
            ) {
                resetWidget()
                return
            }

            val intent = Intent().apply {
                action = MediaButtonReceiver.ACTION_APPWIDGET_UPDATE
                `package` = packageName
            }

            val bundle = Bundle().apply {
                putInt("state", controller.playbackState?.state ?: 0)

                putString("mediaId", controller.metadata?.description?.mediaId)

                putString("title", controller.metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE))

                putString("artist", controller.metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))

                putString("album", controller.metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))

                putInt("shuffle", controller.shuffleMode)
                putInt("repeat", controller.repeatMode)
            }

            intent.putExtras(bundle)

            sendBroadcast(intent)
        }

        private fun getMax(): Int = duration()
    }

    protected fun getRepeatResId(): Int {
        return when (
            Common.RepeatMode.fromId(
                mSession.controller.repeatMode
            )
        ) {
            Common.RepeatMode.ALL ->
                R.drawable.player_btn_repeat_all

            Common.RepeatMode.ONE ->
                R.drawable.player_btn_repeat_one

            else ->
                R.drawable.player_btn_repeat_none
        }
    }

    protected fun getShuffleResId(): Int {
        return when (
            Common.ShuffleMode.fromId(
                mSession.controller.shuffleMode
            )
        ) {
            Common.ShuffleMode.AUTO ->
                R.drawable.player_btn_shuffle_on

            else ->
                R.drawable.player_btn_shuffle
        }
    }

    protected fun notifyUpdateUIState() {
        setMetadata(playingQueueManager.getSongItem())
        mSession.setPlaybackState(mSession.controller.playbackState)
    }

    private fun registerForegroundListener() {
        // PassCodeObserver.registerAppCallback(fgBgObserver)
    }

    private fun unregisterForegroundListener() {
        // PassCodeObserver.unregisterAppCallback(fgBgObserver)
    }

    protected fun acquireWifiLock() {
        wifiLock.takeIf { !it.isHeld }?.acquire()
    }

    protected fun releaseWifiLock() {
        wifiLock.takeIf { it.isHeld }?.release()
    }
}