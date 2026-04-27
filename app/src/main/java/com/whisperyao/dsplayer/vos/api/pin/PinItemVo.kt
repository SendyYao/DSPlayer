package com.whisperyao.dsplayer.vos.api.pin

data class PinItemVo(
    val type: String?,
    val id: String?,
    val name: String?,
    val criteria: HashMap<String, String>
)
