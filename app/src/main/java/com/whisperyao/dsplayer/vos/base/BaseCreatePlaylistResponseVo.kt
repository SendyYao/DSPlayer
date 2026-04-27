package com.whisperyao.dsplayer.vos.base

import com.whisperyao.dsplayer.datasource.network.vo.BaseVo

abstract class BaseCreatePlaylistResponseVo: BaseVo() {
    abstract fun getId(): String?

    abstract fun isErrorPlaylistExist(): Boolean
}