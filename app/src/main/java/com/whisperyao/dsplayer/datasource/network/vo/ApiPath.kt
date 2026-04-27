package com.whisperyao.dsplayer.datasource.network.vo

import java.io.Serializable

data class ApiPath(
    val maxVersion: Int,
    val minVersion: Int,
    val path: String
) : Serializable {
    companion object {
        private const val serialVersionUID = -6911016844444078762L
    }
}
