package com.whisperyao.dsplayer.model.data

import com.whisperyao.dsplayer.item.SongItem

data class PlayingInfo(
    val songItem: SongItem?,
    val position: Int
)