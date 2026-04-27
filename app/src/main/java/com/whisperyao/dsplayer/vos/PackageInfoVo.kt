package com.whisperyao.dsplayer.vos

import com.whisperyao.dsplayer.datasource.network.vo.BaseVo

open class PackageInfoVo : BaseVo() {

    var data: Info? = null

    fun getPath(): String? {
        return data?.path
    }

    fun getAudioMajorVer(): Int {
        return data?.version?.major ?: 0
    }

    fun getAudioMinorVer(): Int {
        return data?.version?.minor ?: 0
    }

    fun getAudioBuildVer(): Int {
        return data?.version?.build ?: 0
    }

    class Info {
        var path: String? = null
        var version: VersionVo? = null
    }

    class VersionVo {
        var build: Int = 0
        var major: Int = 0
        var minor: Int = 0
    }
}