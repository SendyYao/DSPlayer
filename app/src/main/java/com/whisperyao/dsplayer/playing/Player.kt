package com.whisperyao.dsplayer.playing


import com.whisperyao.dsplayer.Common
import androidx.mediarouter.media.MediaRouter;
import com.whisperyao.dsplayer.item.RendererItem
import com.whisperyao.dsplayer.vos.PlayingInfo;
import com.whisperyao.dsplayer.vos.base.BaseRemotePlayerResponseVo;
import kotlin.jvm.JvmStatic;


class Player private constructor() {

    enum class PlayerType {
        LOCAL,
        USB,
        BLUETOOTH,
        UPNP,
        AIRPLAY,
        CHROMECAST,
        UNKNONWN
    }

    var hasPassword: Boolean = false
        private set

    var isGroupPlayer: Boolean = false
        private set

    var name: String = ""
        private set

    var playMode: PlayingStatusManager.PLAY_MODE =
        PlayingStatusManager.PLAY_MODE.STREAMING
        private set

    var playerType: PlayerType = PlayerType.LOCAL
        private set

    var playingInfo: PlayingInfo? = null

    var renderer: RendererItem? = null
        private set

    var routeInfo: MediaRouter.RouteInfo? = null
        private set

    val subPlayers = mutableListOf<Player>()

    var uniqueId: String = ""
        private set

    fun getModeName(): String = playMode.name

    fun getUniqueIdForPreference(): String {
        return if (isPlayModeStreaming()) {
            PlayingStatusManager.PLAY_MODE.STREAMING.name
        } else {
            uniqueId
        }
    }

    fun isPlayModeStreaming(): Boolean {
        return playMode.isStreaming
    }

    fun isPlayModeRenderer(): Boolean {
        return playMode.isRenderer
    }

    fun isPlayModeChromeCast(): Boolean {
        return playMode.isChromeCast
    }

    fun isLocalPlayer(): Boolean {
        return isPlayModeStreaming()
    }

    fun isRemotePlayer(): Boolean {
        return isPlayModeRenderer() || isPlayModeChromeCast()
    }

    fun isWithPlayingStatus(): Boolean {
        return playingInfo != null
    }

    override fun toString(): String {
        return "($playMode${Common.SZ_DATABASE_SEPARATOR}$uniqueId)"
    }

    override fun equals(other: Any?): Boolean {
        return other is Player &&
                other.playMode == playMode &&
                other.uniqueId == uniqueId
    }

    override fun hashCode(): Int {
        return playMode.hashCode() xor uniqueId.hashCode()
    }

    companion object {

        @JvmStatic
        fun generateLocalInstance(): Player {
            return Player().apply {
                playMode = PlayingStatusManager.PLAY_MODE.STREAMING
                name = Common.getDeviceName()
                uniqueId = Common.getDeviceName()
                playerType = PlayerType.LOCAL
            }
        }

        @JvmStatic
        fun generateRendererInstance(item: RendererItem): Player {
            return Player().apply {
                playMode = PlayingStatusManager.PLAY_MODE.RENDERER
                renderer = item
                name = item.name
                uniqueId = item.uniqueId
                hasPassword = item.hasPassword()
                isGroupPlayer = item.isGroupPlayer

                item.subPlayers.forEach { subItem ->
                    subPlayers.add(generateRendererInstance(subItem))
                }

                playerType = when (item.playerType) {
                    BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.airplay ->
                        PlayerType.AIRPLAY

                    BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.bluetooth ->
                        PlayerType.BLUETOOTH

                    BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.upnp ->
                        PlayerType.UPNP

                    BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.usb ->
                        PlayerType.USB

                    BaseRemotePlayerResponseVo.BaseRemotePlayerVo.BaseRemotePlayerType.unknown,
                    null -> PlayerType.UNKNONWN
                }
            }
        }

        @JvmStatic
        fun generateChromeCastInstance(
            routeInfo: MediaRouter.RouteInfo
        ): Player {
            return Player().apply {
                playMode = PlayingStatusManager.PLAY_MODE.CHROMECAST
                this.routeInfo = routeInfo
                name = routeInfo.name.toString()
                uniqueId = routeInfo.id
                playerType = PlayerType.CHROMECAST
            }
        }

        @JvmStatic
        fun generateUnknownInstance(
            playMode: PlayingStatusManager.PLAY_MODE,
            playerId: String
        ): Player {
            return Player().apply {
                this.playMode = playMode
                name = playerId
                uniqueId = playerId
                playerType = PlayerType.UNKNONWN
            }
        }
    }
}