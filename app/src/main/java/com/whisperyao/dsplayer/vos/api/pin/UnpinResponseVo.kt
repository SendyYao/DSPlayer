package com.whisperyao.dsplayer.vos.api.pin

data class UnpinResponseVo(
    val error: UnpinErrorsVo?,
    val success: Boolean
) {
    data class UnpinErrorsVo(
        val code: Int,
        val errors: List<UnpinErrorCodeVo>
    )

    data class UnpinErrorCodeVo(
        val error: Int,
        val id: String
    )
}
