package com.whisperyao.dsplayer.vos.api.pin

data class PinResponseVo(
    val success: Boolean = false,
    val error: ErrorCodeVo? = null
) {
    data class ErrorCodeVo(
        val code: Int = 0,
        val errors: List<Int> = emptyList()
    )
}