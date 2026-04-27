package com.whisperyao.dsplayer.datasource.network.vo.cgi

import com.google.gson.annotations.SerializedName
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.datasource.network.vo.BaseVo
import com.whisperyao.dsplayer.datasource.network.vo.base.BaseAudioInfoVo
import java.util.List


class CgiAudioInfoVo : BaseVo(), BaseAudioInfoVo {

    var build: Int = 0
    var major: Int = 0
    var minor: Int = 0

    var playlist: Boolean = false
    var stream: Boolean = false
    var speaker: Boolean = false
    var usb: Boolean = false

    var reason: String? = null
    var result: String? = null
    var serialNumber: String? = null
    var sid: String? = null

    @SerializedName("transcode_capability")
    var transcodeCapability: List<String>? = null

    @SerializedName("transcode_type")
    var transcodeType: String? = null

    override fun getPlayingQueueMax(): Int = 4096

    override fun permitPublicSharing(): Boolean = false

    override fun isSuccess(): Boolean {
        return result.equals("success", ignoreCase = true)
    }

    override fun getMajorVer(): Int = major

    override fun getMinorVer(): Int = minor

    override fun getBuildVer(): Int = build
    override fun getDSid(): String? = serialNumber

    fun setMajorVer(value: Int) {
        major = value
    }

    fun setMinorVer(value: Int) {
        minor = value
    }

    fun setBuildVer(value: Int) {
        build = value
    }

    override fun haveRemotePlayer(): Boolean {
        return speaker || usb
    }

    override fun permitStream(): Boolean = stream

    override fun permitPlaylist(): Boolean = playlist

    override fun getTranscode(): MutableList<String?> {
        return (transcodeCapability ?: listOfNotNull(transcodeType)) as MutableList<String?>
    }


    override fun getServerType(): ConnectionManager.ResourceType {
        return ConnectionManager.ResourceType.CGI
    }
}