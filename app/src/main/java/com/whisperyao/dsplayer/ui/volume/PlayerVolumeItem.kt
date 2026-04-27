package com.whisperyao.dsplayer.ui.volume

import android.content.Context
import android.media.AudioManager
import com.whisperyao.dsplayer.playing.Player
import com.whisperyao.dsplayer.util.SynoLog

class PlayerVolumeItem {

    companion object {
        private const val LOG = "DSaudio.PlayerVolumeItem"
        private const val RENDERER_MAX_VOLUME = 100
    }

    private var isGroupPlayer: Boolean
    private var isLocal: Boolean
    private var maxVolume: Int
    private var playerId: String
    private var playerName: String

    fun getPlayerId(): String {
        return playerId
    }

    fun getPlayerName(): String {
        return playerName
    }

    fun getMaxVolume(): Int {
        return maxVolume
    }

    fun getIsGroupPlayer(): Boolean {
        return isGroupPlayer
    }

    constructor(context: Context, player: Player) {
        playerId = player.uniqueId
        playerName = player.name

        isLocal = player.isPlayModeStreaming()
        isGroupPlayer = player.isGroupPlayer

        maxVolume =
            if (isLocal) {
                val audioManager =
                    context.getSystemService(
                        Context.AUDIO_SERVICE
                    ) as AudioManager

                audioManager.getStreamMaxVolume(
                    AudioManager.STREAM_MUSIC
                )
            } else {
                RENDERER_MAX_VOLUME
            }
    }

    constructor(playerId: String, playerName: String) {
        this.playerId = playerId
        this.playerName = playerName

        isLocal = false
        isGroupPlayer = false
        maxVolume = RENDERER_MAX_VOLUME
    }

    fun getUpVolume(volume: Int): Int {
        SynoLog.d(LOG, "getUpVolume : $volume")

        var candidate =
            if (isLocal) {
                volume + 1
            } else {
                ((volume / 10) * 10) + 10
            }

        SynoLog.d(LOG, "getUpVolume candidate : $candidate")

        if (candidate > maxVolume) {
            candidate = maxVolume
        }

        SynoLog.d(LOG, "getUpVolume return : $candidate")

        return candidate
    }

    fun getDownVolume(volume: Int): Int {
        SynoLog.d(LOG, "getDownVolume : $volume")

        var candidate =
            if (isLocal) {
                volume - 1
            } else {
                val floor = (volume / 10) * 10

                SynoLog.d(LOG, "getDownVolume : $volume, floor: $floor")

                if (floor < volume) { floor }
                else { floor - 10 }
            }

        SynoLog.d(LOG, "getDownVolume candidate : $candidate")

        if (candidate < 0) {
            candidate = 0
        }

        SynoLog.d(LOG, "getDownVolume return : $candidate")

        return candidate
    }
}