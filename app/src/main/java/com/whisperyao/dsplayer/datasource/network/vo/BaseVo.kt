package com.whisperyao.dsplayer.datasource.network.vo

import com.google.gson.JsonElement

open class BaseVo {

    var error: ErrorCodeVo? = null
    var success: Boolean = false

    companion object {
        @JvmStatic
        fun getSuccessBaseVo(): BaseVo {
            return BaseVo().apply {
                success = true
            }
        }
    }

    class ErrorCodeVo {
        var code: Int = 1
        var errors: JsonElement? = null
    }
}