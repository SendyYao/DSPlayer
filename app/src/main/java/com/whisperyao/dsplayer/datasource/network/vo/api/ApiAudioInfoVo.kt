package com.whisperyao.dsplayer.datasource.network.vo.api

import com.google.gson.annotations.SerializedName;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.datasource.network.vo.BaseDataVo;
import com.whisperyao.dsplayer.datasource.network.vo.base.BaseAudioInfoVo;
import java.util.List;

class ApiAudioInfoVo :
    BaseDataVo<ApiAudioInfoVo.WebAPIData>(),
    BaseAudioInfoVo {

    override fun getMajorVer(): Int = 0

    override fun getMinorVer(): Int = 0

    override fun permitStream(): Boolean = true

    override fun getServerType(): ConnectionManager.ResourceType {
        return ConnectionManager.ResourceType.API
    }

    override fun getTranscode(): kotlin.collections.List<String?>? {
        return (data?.transcodeCapability ?: emptyList()) as kotlin.collections.List<String?>?
    }

    override fun isSuccess(): Boolean = success

    override fun haveRemotePlayer(): Boolean {
        return data?.privilege?.remotePlayer == true
    }

    override fun permitPlaylist(): Boolean {
        return data?.privilege?.playlistEdit == true
    }

    override fun permitPublicSharing(): Boolean {
        return data?.privilege?.sharing == true
    }

    override fun getBuildVer(): Int {
        return data?.version ?: 0
    }

    override fun getDSid(): String? {
        return data?.serialNumber
    }


    override fun getPlayingQueueMax(): Int {
        return data?.playingQueueMax ?: 0
    }

    fun getSID(): String? = data?.sid

    fun isManager(): Boolean = data?.isManager ?: false

    data class WebAPIData(
        @SerializedName("is_manager")
        val isManager: Boolean,

        @SerializedName("serial_number")
        val serialNumber: String?,

        @SerializedName("transcode_capability")
        val transcodeCapability: List<String>?,

        @SerializedName("version_string")
        val versionString: String?,

        val sid: String?,

        val version: Int,

        val privilege: Privilege?,

        @SerializedName("playing_queue_max")
        val playingQueueMax: Int
    )

    data class Privilege(
        @SerializedName("playlist_edit")
        var playlistEdit: Boolean,

        @SerializedName("remote_player")
        var remotePlayer: Boolean,

        var sharing: Boolean,

        @SerializedName("tag_edit")
        var tagEdit: Boolean,

        @SerializedName("upnp_browse")
        var upnpBrowse: Boolean
    )
}