package com.whisperyao.dsplayer.datasource.network.api

class SynoApiInfo: BaseWebApi() {
    companion object {
        const val API = "SYNO.API.INFO"
        val INSTANCE: SynoApiInfo = SynoApiInfo()
        const val KEY_QUERY = "query"
        const val METHOD_QUERY = "query"
        const val VALUE_ALL = "all"
        const val VERSION = 1
    }
    override fun name(): String {
        return "SYNO.API.INFO"
    }
}