package com.whisperyao.dsplayer.fragment


import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import dagger.android.support.DaggerFragment
import javax.inject.Inject

abstract class LyricFragment : DaggerFragment() {

    @Inject
    lateinit var playingQueueManager: PlayingQueueManager

    abstract fun setTimeLine(time: Long)

    abstract fun updateTrackInfo(songItem: SongItem?)

    companion object {
        const val ADDITIONAL: String = "additional"
        const val DATA: String = "data"
        const val FULL_LYRICS: String = "full_lyrics"
        const val LYRICS: String = "lyrics"
    }
}