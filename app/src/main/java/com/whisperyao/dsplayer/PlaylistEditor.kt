package com.whisperyao.dsplayer

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair
import com.whisperyao.dsplayer.Common.makeAddress
import com.whisperyao.dsplayer.ConnectionManager.GetHttpPost
import com.whisperyao.dsplayer.datasource.network.vo.BaseVo
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.item.PlaylistItem
import com.whisperyao.dsplayer.net.AudioStationAPI
import com.whisperyao.dsplayer.net.WebAPI
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.vos.api.ApiCreatePlaylistResponseVo
import com.whisperyao.dsplayer.vos.base.BaseCreatePlaylistResponseVo
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.LinkedList


object PlaylistEditor {

    private const val ADDITIONAL = "additional"
    private const val CONJ_RULE = "conj_rule"
    private const val COPY_TO_LIBRARY = "copytolibrary"
    private const val DATA = "data"
    private const val DELETE = "delete"
    private const val GET_INFO = "getinfo"
    private const val ID = "id"
    private const val LIBRARY = "library"
    private const val LIMIT = "limit"
    private const val LOG = "PlaylistEditor"
    private const val NAME = "name"
    private const val NEW_NAME = "new_name"
    private const val OFFSET = "offset"
    private const val PLAYLISTS = "playlists"
    private const val RENAME = "rename"
    private const val RULES = "rules"
    private const val RULES_CONJUNCTION = "rules_conjunction"
    private const val RULES_JSON = "rules_json"
    private const val SONGS = "songs"
    private const val UPDATE_SMART = "updatesmart"
    private const val UPDATE_SONGS = "updatesongs"

    const val PERSONAL = "personal"
    const val SHARED = "shared"

    fun doRenamePlaylist(playlistItem: PlaylistItem, newName: String, got: ConnectionManager.GetHttpPost): BaseCreatePlaylistResponseVo? {
        return if (playlistItem.isNormal()) {
            doRenameNormalPlaylist(playlistItem, newName, got)
        } else {
            doRenameSmartPlaylist(playlistItem, newName, got)
        }
    }

    private fun doRenameNormalPlaylist(playlistItem: PlaylistItem, newName: String, got: ConnectionManager.GetHttpPost): BaseCreatePlaylistResponseVo? {
        val webApi = WebAPI.getInstance()
        val api = webApi.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST)
            ?: return null.also {
                SynoLog.e(LOG, "Playlist API doesn't exist")
            }

        val url = Common.makeAddress(
            Common.DEFAULT_WEBAPI_PATH,
            api.path
        )

        got.onGetHttpPost(url)

        val params = arrayListOf(
            BasicKeyValuePair(ID, playlistItem.id),
            BasicKeyValuePair(NEW_NAME, newName)
        )

        return try {
            Gson().fromJson(
                JsonReader(
                    InputStreamReader(
                        webApi.doRequest(
                            url,
                            AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST,
                            RENAME,
                            params
                        ).body!!.byteStream()
                    )
                ),
                ApiCreatePlaylistResponseVo::class.java
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun doRenameSmartPlaylist(
        playlistItem: PlaylistItem,
        newName: String,
        got: ConnectionManager.GetHttpPost
    ): BaseCreatePlaylistResponseVo? {

        val rules = getSmartPlaylistRules(playlistItem) ?: return null

        val conjunction = rules.optString(RULES_CONJUNCTION)
        val rulesJson = rules.optJSONArray(RULES) ?: return null

        val webApi = WebAPI.getInstance()
        val api = webApi.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST)
            ?: return null

        val url = Common.makeAddress(
            Common.DEFAULT_WEBAPI_PATH,
            api.path
        )

        got.onGetHttpPost(url)

        val params = arrayListOf(
            BasicKeyValuePair(ID, playlistItem.id),
            BasicKeyValuePair(NEW_NAME, newName),
            BasicKeyValuePair(
                LIBRARY,
                if (playlistItem.isPersonal) PERSONAL else SHARED
            ),
            BasicKeyValuePair(CONJ_RULE, conjunction),
            BasicKeyValuePair(RULES_JSON, rulesJson.toString())
        )

        val response = webApi.doRequest(
            url,
            AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST,
            UPDATE_SMART,
            api.maxVersion,
            params
        )

        response.body!!.byteStream()
        val reader = JsonReader(InputStreamReader(response.body!!.byteStream()))

        val type = object : TypeToken<ApiCreatePlaylistResponseVo>() {}.type

        return try {
            Gson().fromJson(
                reader,
                type

            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun doUpdatePlaylist(
        id: String?,
        offset: Int,
        limit: Int,
        ids: String?,
        got: GetHttpPost?
    ): Common.ConnectionInfo {
        val webAPI = WebAPI.getInstance()
        val knownAPI = webAPI.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST)
        if (knownAPI == null) {
            SynoLog.e(LOG, "SYNO.AudioStation.Playlist api doesn't exist")
            throw IOException("SYNO.AudioStation.Playlist api doesn't exist")
        }
        val strMakeAddress = makeAddress(Common.DEFAULT_WEBAPI_PATH, knownAPI.path)
        SynoLog.d(LOG, "doUpdatePlaylist url = $strMakeAddress")
        got?.onGetHttpPost(strMakeAddress)
        val arrayList: ArrayList<BasicKeyValuePair> = ArrayList()
        arrayList.add(BasicKeyValuePair(ID, id))
        arrayList.add(BasicKeyValuePair(OFFSET, offset.toString()))
        arrayList.add(BasicKeyValuePair(LIMIT, limit.toString()))
        arrayList.add(BasicKeyValuePair(SONGS, ids))
        val responseDoRequest: Response = webAPI.doRequest(
            strMakeAddress,
            AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST,
            "updatesongs",
            arrayList
        )
        val connectionInfo = Common.ConnectionInfo.ERROR_NETWORK
        if (!responseDoRequest.isSuccessful) {
            return connectionInfo
        }
        val inputStreamByteStream: InputStream? = responseDoRequest.body?.byteStream()
        val gson = Gson()
        val jsonReader = JsonReader(InputStreamReader(inputStreamByteStream))
        val baseVo = gson.fromJson<Any?>(jsonReader, BaseVo::class.java) as BaseVo?
        jsonReader.close()
        val connectionInfo2 = Common.ConnectionInfo.SUCCESS
        connectionInfo2.resultVo = baseVo
        return connectionInfo2
    }


    private fun getSmartPlaylistRules(playlistItem: PlaylistItem): JSONObject? {
        val webApi = WebAPI.getInstance()
        val api = webApi.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST)
            ?: return null

        val url = Common.makeAddress(
            Common.DEFAULT_WEBAPI_PATH,
            api.path
        )

        val params = arrayListOf(
            BasicKeyValuePair(ID, playlistItem.id),
            BasicKeyValuePair(ADDITIONAL, RULES),
            BasicKeyValuePair(
                LIBRARY,
                if (playlistItem.isPersonal) PERSONAL else SHARED
            )
        )

        return try {
            JSONObject(
                webApi.doRequest(
                    url,
                    AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST,
                    GET_INFO,
                    params
                ).body!!.string()
            )
                .getJSONObject(DATA)
                .getJSONArray(PLAYLISTS)
                .getJSONObject(0)
                .getJSONObject(ADDITIONAL)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Throws(IOException::class)
    fun doDeletePlaylist(id: String, got: ConnectionManager.GetHttpPost) {
        val webApi = WebAPI.getInstance()
        val api = webApi.getKnownAPI(AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST)
            ?: throw IOException("Playlist API doesn't exist")

        val url = Common.makeAddress(
            Common.DEFAULT_WEBAPI_PATH,
            api.path
        )

        got.onGetHttpPost(url)

        val params = arrayListOf(
            BasicKeyValuePair(ID, id)
        )

        webApi.doRequest(
            url,
            AudioStationAPI.SYNO_AUDIOSTATION_PLAYLIST,
            DELETE,
            params
        )
    }

    fun parseApiJsonNormalPlayList(jsonObject: JSONObject): List<Item> {
        val result = LinkedList<Item>()

        try {
            val playlists =
                jsonObject.getJSONObject(DATA)
                    .getJSONArray(PLAYLISTS)

            for (i in 0 until playlists.length()) {
                val item = playlists.getJSONObject(i)

                val type = item.getString("type")
                val library = item.getString(LIBRARY)

                if (type.equals("normal", true)) {
                    result.add(
                        Item(
                            if (library.equals(PERSONAL, true))
                                Item.ItemType.PERSONAL_NORMAL_NEW
                            else
                                Item.ItemType.SHARED_NORMAL_NEW,
                            item.getString(ID),
                            item.getString(NAME)
                        )
                    )
                }
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return result
    }
}