package com.whisperyao.dsplayer.mediasession.players

import android.support.v4.media.session.PlaybackStateCompat

abstract class PlaybackInfoListener {
    fun onPlaybackCompleted() { }

    abstract fun onPlaybackStateChange(state: PlaybackStateCompat)
}