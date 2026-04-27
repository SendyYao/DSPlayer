package com.whisperyao.dsplayer.playing

import com.google.gson.annotations.SerializedName
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.util.FlatBufferUtil;
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.util.ObjFile
import com.whisperyao.dsplayer.util.SynoLog
import java.io.File
import java.io.FileOutputStream;
import java.io.IOException;

class NowPlayingManager(
    private val dsId: String
) {

    companion object {
        private const val LOG_TAG = "NowPlayingManager"
    }

    private fun getPath(): String {
        return Common.getDSaudioAppFolder() + "nowplayinglist-$dsId"
    }

    private fun getFlatBufferPath(): String {
        return "${getPath()}.bin"
    }

    private fun getNowPlayingFile(): File {
        return File(getPath())
    }

    private fun getNowPlayingFileFlatBuffer(): File {
        return File(getFlatBufferPath())
    }

    class NowPlayingList(
        val list: Array<SongItem>
    )

    @Synchronized
    fun saveQueue(songs: List<SongItem>) {
        try {
            saveQueueByFlatBuffer(
                getNowPlayingFileFlatBuffer(),
                ArrayList(songs)
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveQueueByFlatBuffer(
        file: File,
        songs: ArrayList<SongItem>
    ) {
        SynoLog.d(LOG_TAG, "saveQueueByFlatbuffer song size: ${songs.size}")

        if (file.exists()) {
            file.delete()
        }

        val buffer = FlatBufferUtil.getByteBufferOfSongItems(songs)

        SynoLog.d(LOG_TAG, "transfer to ByteBuffer finish")

        try {
            FileOutputStream(file).channel.use { channel ->
                SynoLog.d(LOG_TAG, "open FileOutputStream")
                channel.write(buffer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        SynoLog.d(LOG_TAG, "saveQueueByFlatbuffer finish")
    }

    @Synchronized
    fun loadQueue(): Array<SongItem> {
        val oldFile = getNowPlayingFile()

        if (oldFile.exists()) {
            SynoLog.d(LOG_TAG, "loadQueue, old json format file start")

            val oldList = ObjFile.getObjectFromFile(
                oldFile,
                NowPlayingList::class.java
            ) as? NowPlayingList

            val songs = oldList?.list ?: emptyArray()

            oldFile.delete()

            SynoLog.d(
                LOG_TAG,
                "loadQueue, old json format file finish, song size: ${songs.size}"
            )

            return songs
        }

        SynoLog.d(LOG_TAG, "loadQueue, flatbuffer format file start")

        val songs = FlatBufferUtil.loadQueueFromFile(
            getNowPlayingFileFlatBuffer()
        )

        SynoLog.d(
            LOG_TAG,
            "loadQueue, flatbuffer format file finish, song size: ${songs.size}"
        )

        return songs
    }

    @Synchronized
    fun clearQueue() {
        saveQueue(emptyList())
    }

    private fun getNowPlayingStatusFile(): File {
        return File(Common.getDSaudioAppFolder() + "nowplayingstatus-$dsId")
    }

    data class NowPlayingStatus(
        val isPlaying: Boolean,

        @SerializedName(
            value = "playPos",
            alternate = ["mPlayPos"]
        )
        val playPos: Int,

        @SerializedName(
            value = "preSeekTime",
            alternate = ["mPreSeekTime"]
        )
        val preSeekTime: Int,

        @SerializedName(
            value = "repeatmode",
            alternate = ["mRepeatMode"]
        )
        val repeatMode: Common.RepeatMode?,

        @SerializedName(
            value = "shufflemode",
            alternate = ["mShuffleMode"]
        )
        val shuffleMode: Common.ShuffleMode?
    )

    @Synchronized
    fun saveStatus(
        playing: Boolean,
        pos: Int,
        seek: Int,
        repeat: Common.RepeatMode,
        shuffle: Common.ShuffleMode
    ) {
        ObjFile.saveObjectToFile(
            NowPlayingStatus(
                playing,
                pos,
                seek,
                repeat,
                shuffle
            ),
            getNowPlayingStatusFile(),
            NowPlayingStatus::class.java
        )
    }

    @Synchronized
    fun getStatus(): NowPlayingStatus? {
        return ObjFile.getObjectFromFile(
            getNowPlayingStatusFile(),
            NowPlayingStatus::class.java
        )
    }
}