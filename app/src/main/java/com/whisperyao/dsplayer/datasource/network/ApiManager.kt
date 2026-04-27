package com.whisperyao.dsplayer.datasource.network

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.whisperyao.dsplayer.datasource.network.vo.ApiPath
import com.whisperyao.dsplayer.net.WebAPI
import javax.inject.Named
import java.util.HashMap
import java.util.Map


class ApiManager {
    companion object {
        const val ARG_WEBApi = "webApi"
    }
    private var gson: Gson
    private var knownApis: HashMap<String, ApiPath>
    private var sharedPreferences: SharedPreferences

    fun support(apiManager: ApiManager, str: String, i: Int, i2: Int, obj: Any?): Boolean {
        var i = i
        if ((i2 and 2) != 0) {
            i = 1
        }
        return apiManager.support(this, str, 0, 2, null)
    }

    constructor(@Named(LoginInfoManager.PREF_NAME) sharedPreferences: SharedPreferences) {
        this.sharedPreferences = sharedPreferences
        this.gson = Gson()
        var map: HashMap<String, ApiPath> = HashMap()
        this.knownApis = map
        resetApiMap()
        var apis: HashMap<String, ApiPath>? = getApis()
        if (apis != null) {
            map.putAll(apis)
            WebAPI.getInstance().setKnownAPIs(map)
        }
    }

    fun getSharedPreferences(): SharedPreferences {
        return this.sharedPreferences
    }

    fun resetApiMap() {
        this.knownApis.clear()
        this.knownApis["SYNO.API.INFO"] = ApiPath(1, 1, "query.cgi")
    }

    fun clearMap() {
        resetApiMap()
        this.sharedPreferences.edit().remove(ARG_WEBApi).apply()
    }

    fun support(name: String, version: Int): Boolean {
        var apiPath: ApiPath? = get(name)
        return apiPath != null && apiPath.maxVersion >= version
    }

    fun putAll(all: HashMap<String, ApiPath>?) {
        if (all != null) {
            this.knownApis.putAll(all)
        }
        saveApis(this.knownApis)
    }

    private fun saveApis(map: HashMap<String, ApiPath>) {
        var json: String = this.gson.toJson(map)
        this.sharedPreferences.edit().putString(ARG_WEBApi, json).apply()
    }

    private fun getApis(): HashMap<String, ApiPath> {
        val json = sharedPreferences.getString(ARG_WEBApi, "") ?: ""

        return try {
            val type = object : TypeToken<HashMap<String, ApiPath>>() {}.type
            gson.fromJson<HashMap<String, ApiPath>>(json, type) ?: hashMapOf()
        } catch (e: Exception) {
            hashMapOf()
        }
    }

    fun get(name: String): ApiPath? {
        if (this.knownApis.isEmpty()) {
            resetApiMap()
        }
        return this.knownApis[name]
    }

    fun retrieve(name: String): ApiPath? {
        return get(name)
    }

}