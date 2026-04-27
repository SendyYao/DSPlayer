package com.whisperyao.dsplayer.model.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.util.CoverUtil
import jakarta.inject.Inject
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock


class PlayingQueueManager @Inject constructor() {

    companion object {
        const val MAGIC_MINUS_ONE = -1

        const val PREFIX_BROWSABLE = ":browsable:"
        const val PREFIX_RADIO = ":radio:"
        const val PREFIX_ROOT = ":root:"
        const val PREFIX_SONG = ":song:"

        const val SEPARATOR = "_syno_separator_"

        const val carRootMediaId = "car_root"
        const val messageMediaId = "message"
        const val notAllowedRootMediaId = "not_allowed_root"
        const val notPlayingMediaId = "not_playing"
        const val nowPlayingMediaId = "now_playing"
        const val preparingMediaId = "preparing"
        const val rootMediaId = "root"
    }

    @Inject
    lateinit var coverUtil: CoverUtil

    private val queueChangedLiveData = MutableLiveData<Boolean>()
    private val currentSongLiveData = MutableLiveData<PlayingInfo>()
    private val internalMutableProgress = MutableLiveData<PlayProgress>()

    private val lock = ReentrantReadWriteLock()

    private val playIndexQueue = arrayListOf<Int>()
    private val playItemQueue = arrayListOf<SongItem>()
    private val tempItemQueue = arrayListOf<SongItem>()

    private var shuffleMode = Common.ShuffleMode.NONE
    private var repeatMode = Common.RepeatMode.NONE

    private val currentIndex = AtomicInteger(0)

    fun getProgress(): LiveData<PlayProgress> = internalMutableProgress

    fun setProgress(progress: Long, buffer: Long) {
        internalMutableProgress.postValue(
            PlayProgress(progress, buffer)
        )
    }

    fun getQueueSize(): Int = getQueue().size

    fun getQueue(): List<SongItem> {
        lock.readLock().lock()
        return try {
            playItemQueue.toList()
        } finally {
            lock.readLock().unlock()
        }
    }

    fun setQueue(playItemList: List<SongItem>, startIndex: Int = -1) {
        lock.writeLock().lock()
        try {
            currentIndex.set(
                if (startIndex in playItemList.indices) startIndex else 0
            )

            playItemQueue.clear()
            playIndexQueue.clear()

            playItemQueue.addAll(playItemList)
            playIndexQueue.addAll(playItemList.indices.toList())

            if (shuffleMode.isEnabled && !isRemotePlayer()) {
                shuffle()
            } else {
                updatePlayingInfo()
            }

            queueChangedLiveData.postValue(true)

        } finally {
            lock.writeLock().unlock()
        }
    }

    fun setQueueSilent(playItemList: List<SongItem>) {
        lock.writeLock().lock()
        try {
            playItemQueue.clear()
            playIndexQueue.clear()

            playItemQueue.addAll(playItemList)
            playIndexQueue.addAll(playItemList.indices.toList())

        } finally {
            lock.writeLock().unlock()
        }
    }

    fun appendQueue(newItems: List<SongItem>) {
        appendQueue(newItems, startIndex = -1, silent = false)
    }

    fun appendQueue(newItems: List<SongItem>, startIndex: Int = -1) {
        appendQueue(newItems, startIndex, silent = false)
    }

    fun appendQueue(newItems: List<SongItem>, startIndex: Int, silent: Boolean) {
        val writeLock = lock.writeLock()
        val readLock = lock.readLock()

        val readHoldCount =
            if (lock.writeHoldCount == 0) lock.readHoldCount else 0

        repeat(readHoldCount) {
            readLock.unlock()
        }

        writeLock.lock()
        var restoreCount = 0

        try {
            val insertIndex = startIndex + 1

            playIndexQueue.clear()
            playItemQueue.addAll(insertIndex, newItems)

            repeat(playItemQueue.size) {
                playIndexQueue.add(it)
            }

            if (!silent) {
                updatePlayingInfo()
            }

            queueChangedLiveData.postValue(true)

        } finally {
            repeat(readHoldCount) {
                readLock.lock()
                restoreCount++
            }
            writeLock.unlock()
        }
    }

    fun clearQueue() {
        lock.writeLock().lock()
        try {
            currentIndex.set(-1)
            playItemQueue.clear()
            playIndexQueue.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun setTempQueue(playItemList: List<SongItem>) {
        tempItemQueue.clear()
        tempItemQueue.addAll(playItemList)
    }

    fun popTempQueue(): List<SongItem> {
        val result = tempItemQueue.toList()
        tempItemQueue.clear()
        return result
    }

    fun setPlayIndex(index: Int) {
        when (index) {
            in 0 until getQueueSize() -> {
                currentIndex.set(
                    playIndexQueue.indexOf(index)
                )
            }
            -1 -> currentIndex.set(-1)
        }

        updatePlayingInfo()
    }

    fun castSetPlayIndex(index: Int) {
        currentIndex.set(index)
        updatePlayingInfo()
    }

    fun getPlayIndex(): Int {
        return playIndexQueue.getOrNull(
            currentIndex.get()
        ) ?: 0
    }

    fun addSongsToNext(
        newItems: List<SongItem>,
        isStop: Boolean
    ): Boolean {

        val writeLock = lock.writeLock()
        val readLock = lock.readLock()

        val readHoldCount =
            if (lock.writeHoldCount == 0) lock.readHoldCount else 0

        repeat(readHoldCount) {
            readLock.unlock()
        }

        writeLock.lock()

        try {
            val isQueueEmpty = playItemQueue.isEmpty()
            val playIndex = getPlayIndex()

            val insertBaseIndex = if (isQueueEmpty) playIndex else playIndex + 1

            playItemQueue.addAll(insertBaseIndex, newItems)

            val insertedSize = newItems.size

            // 修正 index queue
            for (i in playIndexQueue.indices) {
                val idx = playIndexQueue[i]
                if (idx > playIndex) {
                    playIndexQueue[i] = idx + insertedSize
                }
            }

            val currentIdx = currentIndex.get()

            val range = if (isQueueEmpty) {
                0 until insertedSize
            } else {
                1..insertedSize
            }

            for (offset in range) {
                playIndexQueue.add(
                    currentIdx + range.first + (offset - range.first),
                    playIndex + offset
                )
            }

            if (isStop && !isQueueEmpty) {
                next(false,true)
            }

            updatePlayingInfo()
            queueChangedLiveData.postValue(true)

            return isQueueEmpty

        } finally {
            repeat(readHoldCount) {
                readLock.lock()
            }
            writeLock.unlock()
        }
    }

    fun getSongItem(index: Int = currentIndex.get()): SongItem? {
        val realIndex = playIndexQueue.getOrNull(index) ?: return null
        return playItemQueue.getOrNull(realIndex)
    }

    fun getSongItem(): SongItem? {
        return getSongItem(currentIndex.get())
    }

    fun next(fromUser: Boolean = false, move: Boolean = true): SongItem? {
        var index = currentIndex.get()
        val size = getQueueSize()

        if (size == 0) return null

        if (repeatMode != Common.RepeatMode.ALL &&
            index + 1 >= size
        ) return null

        if (fromUser || repeatMode != Common.RepeatMode.ONE) {
            index = (index + 1) % size
        }

        if (move) {
            currentIndex.set(index)
            updatePlayingInfo()
        }

        return getSongItem(index)
    }

    fun previous(): SongItem? {
        val size = getQueueSize()
        if (size == 0) return null

        var index = currentIndex.get()

        if (repeatMode != Common.RepeatMode.ALL &&
            index == 0
        ) return null

        index = (index - 1 + size) % size

        currentIndex.set(index)
        updatePlayingInfo()

        return getSongItem(index)
    }

    fun shuffle() {
        lock.writeLock().lock()
        try {
            shuffleMode = Common.ShuffleMode.AUTO

            if (!isRemotePlayer() && playItemQueue.isNotEmpty()) {
                val current = currentIndex.get()

                playIndexQueue.clear()
                playIndexQueue.addAll(playItemQueue.indices)

                val currentSongIndex = playIndexQueue[current]
                playIndexQueue.removeAt(current)

                playIndexQueue.shuffle()
                playIndexQueue.add(current, currentSongIndex)

                updatePlayingInfo()
                queueChangedLiveData.postValue(true)
            }

        } finally {
            lock.writeLock().unlock()
        }
    }

    fun unShuffle() {
        lock.writeLock().lock()
        try {
            shuffleMode = Common.ShuffleMode.NONE

            if (!isRemotePlayer() && playItemQueue.isNotEmpty()) {
                currentIndex.set(getPlayIndex())

                rebuildPlayIndexQueue()

                updatePlayingInfo()
                queueChangedLiveData.postValue(true)
            }

        } finally {
            lock.writeLock().unlock()
        }
    }

    fun setRepeatMode(mode: Common.RepeatMode) {
        repeatMode = mode
    }

    fun updateCurrentSongRating(rating: Float) {
        lock.writeLock().lock()
        try {
            getSongItem()?.rating = rating
            updatePlayingInfo()
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun getAlbumBitmap(context: Context, mediaId: String): Bitmap? {
        if (mediaId.startsWith(PREFIX_RADIO)) {
            return BitmapFactory.decodeResource(context.resources, R.drawable.thumbnail_radio)
        }

        val songItem = getSongItem()

        val id = songItem?.id
        if (id != null) {
            val coverUtil = CoverUtil(context)

            val coverUrl = ConnectionManager.getCoverUrl(id)

            val coverFile = coverUtil.getCoverFileFromSong(songItem, coverUrl)

            val bitmap = coverFile?.path?.let {
                BitmapFactory.decodeFile(it)
            }

            if (bitmap != null) {
                return bitmap
            }
        }

        return BitmapFactory.decodeResource(context.resources, R.drawable.thumbnail_song)
    }

    fun observeQueueChanged(owner: LifecycleOwner, observer: Observer<Boolean>) {
        queueChangedLiveData.observe(owner, observer)
    }

    fun observeCurrentSong(
        owner: LifecycleOwner,
        observer: Observer<PlayingInfo>
    ) {
        currentSongLiveData.observe(owner, observer)
    }

    private fun rebuildPlayIndexQueue() {
        playIndexQueue.clear()
        playIndexQueue.addAll(playItemQueue.indices)
    }

    private fun updatePlayingInfo() {
        currentSongLiveData.postValue(
            PlayingInfo(getSongItem(), getPlayIndex())
        )
    }

    private fun isRemotePlayer(): Boolean {
        return DataModelManager.Companion.instance?.playingStatusManager?.isRemotePlayer ?: false
    }

    data class PlayProgress(
        val progress: Long,
        val buffer: Long
    )
}