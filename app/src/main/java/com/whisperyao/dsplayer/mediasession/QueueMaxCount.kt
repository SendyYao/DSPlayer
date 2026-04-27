package com.whisperyao.dsplayer.mediasession

import com.whisperyao.dsplayer.playing.PlayingStatusManager;

object QueueMaxCount {

    private const val CHROMECAST_MAX_SONG_CAPACITY = 1000
    const val DEFAULT_REMOTE_MAX_SONG_CAPACITY = 4096
    private const val MAX_SONG_CAPACITY = Int.MAX_VALUE

    var remoteQueueMax: Int = DEFAULT_REMOTE_MAX_SONG_CAPACITY
        set(value) {
            field = if (value > 0) {
                value
            } else {
                DEFAULT_REMOTE_MAX_SONG_CAPACITY
            }
        }

    fun getQueueMaxByPlayer(
        player: PlayingStatusManager.PLAY_MODE?
    ): Int {
        return when (player) {
            PlayingStatusManager.PLAY_MODE.STREAMING ->
                MAX_SONG_CAPACITY

            PlayingStatusManager.PLAY_MODE.CHROMECAST ->
                CHROMECAST_MAX_SONG_CAPACITY

            PlayingStatusManager.PLAY_MODE.RENDERER ->
                remoteQueueMax

            else ->
                DEFAULT_REMOTE_MAX_SONG_CAPACITY
        }
    }
}