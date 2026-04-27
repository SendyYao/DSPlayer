package com.whisperyao.dsplayer.homepage

import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.fragment.ContentFragment.Companion.DISPLAY_ARTIST
import com.whisperyao.dsplayer.fragment.ContentFragment.Companion.GENRE_FILTER
import com.whisperyao.dsplayer.fragment.ContentFragment.Companion.SONG
import com.whisperyao.dsplayer.item.HomePagePinItem
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.item.PlaylistItem
import com.whisperyao.dsplayer.net.WebAPIErrorException
import java.util.LinkedList


class PinManager {

    private var mTaskLoadPinList: AsyncTask<Void?, Void?, Void?>? = null
    protected var exception: WebAPIErrorException? = null
    private var bIsLoading = false

    private val mCallbacks: ArrayList<Any?> = ArrayList()
    private val mItems: MutableList<HomePagePinItem?> = LinkedList<HomePagePinItem?>()

    private var mTotal = 0

    companion object {
        const val MODE: String = "mode"
        const val DISPLAY_ARTIST = "display_artist"
        const val TYPE_RECENTLY_ADDED = "recently_added"
        const val TYPE_RANDOM_100: String = "random_100"
        const val NORMAL: String = "normal"
        const val GENRE_FILTER = "genre_filter"
        const val PERSONAL: String = "personal"
        const val SONG = "song"

        const val EXTRA_PLAYLIST: String = "extra_playlist"
        private var instance: PinManager? = null

        fun getContainerTypeByItem(item: HomePagePinItem): Common.ContainerType {
            val type = item.type
            val criteria = item.criteria
            type.hashCode()
            when (type) {
                "artist" -> {
                    if (criteria!!.containsKey("genre")) {
                        return Common.ContainerType.GENRE_ARTIST_MODE
                    }
                    return Common.ContainerType.ARTIST_MODE
                }

                "folder" -> return Common.ContainerType.FOLDER_MODE
                "composer" -> return Common.ContainerType.COMPOSER_MODE
                "album" -> {
                    if (criteria!!.containsKey("genre")) {
                        return Common.ContainerType.GENRE_ARTIST_ALBUM_MODE
                    }
                    if (criteria.containsKey("artist")) {
                        return Common.ContainerType.ARTIST_ALBUM_MODE
                    }
                    if (criteria.containsKey("composer")) {
                        return Common.ContainerType.COMPOSER_ALBUM_MODE
                    }
                    return Common.ContainerType.ALBUM_MODE
                }

                "genre" -> return Common.ContainerType.GENRE_MODE
                "random_100" -> return Common.ContainerType.RANDOM100_MODE
                "recently_added" -> return Common.ContainerType.LATEST_ALBUM_MODE
                "playlist" -> return Common.ContainerType.PLAYLIST_MODE
                else -> return Common.ContainerType.ALBUM_MODE
            }
        }

        fun getEnumSongsBundle(item: HomePagePinItem): Bundle {
            val result = Bundle()
            result.putBoolean(MODE, true)

            val type = item.type
            val criteria = item.criteria

            val uiInfo = Bundle()

            // --- ui_info 构建 ---
            if (criteria?.containsKey("album_artist") ?: false) {
                uiInfo.putString("album_artist", criteria.get("album_artist"))
                uiInfo.putString(DISPLAY_ARTIST, criteria.get("album_artist"))
            } else if (criteria?.containsKey("artist") ?: false) {
                uiInfo.putString(DISPLAY_ARTIST, criteria.get("artist"))
            }

            uiInfo.putString("title", item.title)
            result.putBundle("ui_info", uiInfo)

            // --- 主逻辑分支 ---
            when (type) {

                "artist" -> {
                    result.putString("type", "container")
                    result.putString("title", item.title)

                    if (criteria?.containsKey("artist") ?: false) {
                        result.putString("key", criteria["artist"])
                        result.putString("artist", criteria["artist"])
                    }

                    when {
                        criteria?.containsKey("genre") ?: false -> {
                            result.putString("genre", criteria["genre"])
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                        }

                        criteria?.containsKey(GENRE_FILTER) ?: false -> {
                            result.putString(GENRE_FILTER, criteria.get(GENRE_FILTER))
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                        }

                        else -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.ARTIST_ALBUM_MODE.name
                            )
                        }
                    }
                }

                "folder" -> {
                    result.putString("type", "container")
                    result.putString("title", item.title)
                    result.putString("key", criteria?.get("folder"))
                    result.putString("id", criteria?.get("folder"))
                    result.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.FOLDER_MODE.name
                    )
                }

                "composer" -> {
                    result.putString("type", "container")
                    result.putString("title", item.title)
                    result.putString("key", criteria?.get("composer"))
                    result.putString("composer", criteria?.get("composer"))
                    result.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.COMPOSER_ALBUM_MODE.name
                    )
                }

                "album" -> {
                    result.putString("type", SONG)
                    result.putString("title", item.title)

                    if (criteria?.containsKey("album") ?: false) {
                        result.putString("album", criteria["album"])
                        result.putString("key", criteria["album"])
                    }

                    if (criteria?.containsKey("album_artist") ?: false) {
                        result.putString("album_artist", criteria["album_artist"])
                    }

                    when {
                        criteria?.containsKey("genre") ?: false -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                            result.putString("genre", criteria["genre"])
                            if (criteria.containsKey("artist")) {
                                result.putString("artist", criteria["artist"])
                            }
                        }

                        criteria?.containsKey(GENRE_FILTER) ?: false -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                            result.putString(GENRE_FILTER, criteria[GENRE_FILTER])
                            if (criteria.containsKey("artist")) {
                                result.putString("artist", criteria["artist"])
                            }
                        }

                        criteria?.containsKey("artist") ?: false -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.ARTIST_ALBUM_MODE.name
                            )
                            result.putString("artist", criteria["artist"])
                        }

                        criteria?.containsKey("composer") ?: false -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.COMPOSER_ALBUM_MODE.name
                            )
                            result.putString("composer", criteria["composer"])
                        }

                        else -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.ALBUM_MODE.name
                            )
                        }
                    }
                }

                "genre" -> {
                    result.putString("type", "container")
                    result.putString("title", item.title)
                    result.putString("key", criteria?.get("genre"))
                    result.putString("genre", criteria?.get("genre"))
                    result.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.GENRE_ARTIST_MODE.name
                    )
                }

                TYPE_RANDOM_100 -> {
                    result.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.RANDOM100_MODE.name
                    )
                    result.putString("type", Item.ItemType.CONTAINER_MODE.name)
                    result.putString("key", Common.CAT_RANDOM100_ID)
                    result.putString("title", item.title)
                }

                TYPE_RECENTLY_ADDED -> {
                    result.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.LATEST_ALBUM_MODE.name
                    )
                    result.putString("type", "container")
                }

                "playlist" -> {
                    result.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.PERSONAL_PLAYLIST_MODE.name
                    )
                    result.putString("title", item.title)
                    result.putString("key", criteria?.get("playlist"))

                    val isNormal = NORMAL.equals(criteria?.get("type"), ignoreCase = true)
                    val isPersonal = PERSONAL.equals(criteria?.get("library"), ignoreCase = true)

                    val itemType = when {
                        isPersonal && isNormal -> Item.ItemType.PERSONAL_NORMAL_NEW
                        isPersonal && !isNormal -> Item.ItemType.PERSONAL_SMART_NEW
                        !isPersonal && isNormal -> Item.ItemType.SHARED_NORMAL_NEW
                        else -> Item.ItemType.SHARED_SMART_NEW
                    }

                    val bundle = PlaylistItem.generatePredifinedPlaylist(
                        itemType,
                        criteria?.get("playlist"),
                        item.title
                    ).bundle

                    result.putBundle(EXTRA_PLAYLIST, bundle)
                }
            }

            return result
        }

        fun getInstance(): PinManager {
            println("create PinManager instance")
            return instance ?: PinManager()
        }

        fun putKeyValue(key: String, bundle: Bundle, criteria: HashMap<String, String>) {
            if (bundle.containsKey(key)) {
                criteria[key] = bundle.getString(key) ?: ""
            }
        }

        fun getPinCriteria(type: Common.ContainerType, bundle: Bundle): HashMap<String, String> {

            val map = hashMapOf<String, String>()

            when (type) {
                Common.ContainerType.GENRE_ARTIST_MODE -> {
                    putKeyValue("artist", bundle, map)
                    putKeyValue("genre", bundle, map)
                    putKeyValue(GENRE_FILTER, bundle, map)
                }

                Common.ContainerType.COMPOSER_ALBUM_MODE -> {
                    putKeyValue("album", bundle, map)
                    putKeyValue("composer", bundle, map)
                }

                Common.ContainerType.ARTIST_ALBUM_MODE -> {
                    putKeyValue("album", bundle, map)
                    putKeyValue("artist", bundle, map)
                }

                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE -> {
                    putKeyValue("album", bundle, map)
                    putKeyValue("genre", bundle, map)
                    putKeyValue(GENRE_FILTER, bundle, map)
                    putKeyValue("artist", bundle, map)
                }

                Common.ContainerType.ALBUM_MODE,
                Common.ContainerType.LATEST_ALBUM_MODE,
                Common.ContainerType.SEARCH_ALBUM_MODE -> {
                    putKeyValue("album", bundle, map)
                }

                Common.ContainerType.ARTIST_MODE,
                Common.ContainerType.SEARCH_ARTIST_MODE -> {
                    putKeyValue("artist", bundle, map)
                }

                Common.ContainerType.COMPOSER_MODE -> {
                    putKeyValue("composer", bundle, map)
                }

                Common.ContainerType.FOLDER_MODE -> {
                    when {
                        bundle.containsKey("folder") ->
                            map["folder"] = bundle.getString("folder").orEmpty()

                        bundle.containsKey("key") ->
                            map["folder"] = bundle.getString("key").orEmpty()
                    }
                }

                Common.ContainerType.PLAYLIST_MODE,
                Common.ContainerType.SMARTPLAYLIST_MODE,
                Common.ContainerType.PERSONAL_PLAYLIST_MODE,
                Common.ContainerType.PERSONAL_SMART_PLAYLIST_MODE,
                Common.ContainerType.SHARED_PLAYLIST_MODE,
                Common.ContainerType.SHARED_SMART_PLAYLIST_MODE -> {
                    when {
                        bundle.containsKey("playlist") ->
                            map["playlist"] =
                                bundle.getString("playlist").orEmpty()

                        bundle.containsKey("key") ->
                            map["playlist"] =
                                bundle.getString("key").orEmpty()
                    }
                }

                Common.ContainerType.GENRE_MODE -> {
                    putKeyValue("genre", bundle, map)
                }

                else -> {}
            }

            putKeyValue("album_artist", bundle, map)

            return map
        }
    }

    fun isLoading(): Boolean {
        return this.bIsLoading;
    }

    fun addCallback(callback: Callback) {
        synchronized(this.mCallbacks) {
            if (!this.mCallbacks.contains(callback)) {
                this.mCallbacks.add(callback)
            }
        }
    }

    fun removeCallback(callback: Callback?) {
        synchronized(this.mCallbacks) {
            if (this.mCallbacks.contains(callback)) {
                this.mCallbacks.remove(callback)
            }
        }
    }

    fun loadPinList(refresh: Boolean) {
        val task = mTaskLoadPinList
        if (task != null && task.status == AsyncTask.Status.RUNNING) {
            task.cancel(true)
        }

        val newTask = object : AsyncTask<Void, Void, Void>() {

            var retItems: List<HomePagePinItem>? = null

            override fun onPreExecute() {
                this@PinManager.exception = null
                this@PinManager.bIsLoading = true
                this@PinManager.mItems.clear()

                if (this@PinManager.mCallbacks.isEmpty()) return

                for (cb in this@PinManager.mCallbacks) {
                    (cb as Callback).onPinPreLoading()
                }
            }

            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    val itemSet = CacheManager.getInstance().doEnumPins(refresh)
                    retItems = itemSet.itemList
                    println(retItems)
                    this@PinManager.mTotal = itemSet.total
                } catch (e: WebAPIErrorException) {
                    this@PinManager.exception = e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }

            override fun onPostExecute(result: Void?) {
                this@PinManager.bIsLoading = false

                val list = retItems
                if (list != null && list.isNotEmpty()) {
                    this@PinManager.mItems.addAll(list)
                }

                if (this@PinManager.mCallbacks.isEmpty()) return

                for (cb in this@PinManager.mCallbacks) {
                    (cb as Callback).onPinLoadFinish()
                }
            }
        }

        mTaskLoadPinList = newTask as AsyncTask<Void?, Void?, Void?>?
        newTask.execute()
    }

    fun getWebAPIErrorException(): WebAPIErrorException? {
        return this.exception
    }

    fun getItems(): MutableList<HomePagePinItem?> {
        return this.mItems
    }

    fun alreadyPin(type: String, criteria: Map<String, String>): Boolean {
        for (homePagePinItem in mItems) {
            val type2 = homePagePinItem?.type
            val criteria2 = homePagePinItem?.criteria
            if (type == TYPE_RANDOM_100 && type == type2) {
                return true
            }
            if (type == TYPE_RECENTLY_ADDED && type == type2) {
                return true
            }
            if (type == type2) {
                if (type == "playlist" && criteria["playlist"] != null) {
                    if (criteria["playlist"] == criteria2?.get("playlist")) {
                        return true
                    }
                } else if (type == "folder" && criteria["folder"] != null && criteria["folder"] == criteria2?.get("folder")) {
                    return true
                }
                if (criteria2 == criteria) {
                    return true
                }
            }
        }
        return false
    }

    fun addQuickAction(pin: MenuItem, unpin: MenuItem, playlistItem: PlaylistItem) {
        if (ConnectionManager.canSupportPin()) {
            if (playlistItem.isRandom()) {
                if (alreadyPin(TYPE_RANDOM_100, HashMap<String, String>())) {
                    unpin.isVisible = true
                    return
                } else {
                    pin.isVisible = true
                    return
                }
            }
            if (playlistItem.isRecentlyAdded()) {
                if (alreadyPin(TYPE_RECENTLY_ADDED, HashMap<String, String>())) {
                    unpin.isVisible = true
                    return
                } else {
                    pin.isVisible = true
                    return
                }
            }
            if (playlistItem.isCanPinnedPlaylist()) {
                val bundle: Bundle = Bundle()
                bundle.putString("playlist", playlistItem.id)
                if (alreadyPin("playlist", getPinCriteria(Common.ContainerType.PLAYLIST_MODE, bundle))) {
                    unpin.isVisible = true
                } else {
                    pin.isVisible = true
                }
            }
        }
    }
    interface Callback {
        fun onPinErrorOccur()

        fun onPinLoadFinish()

        fun onPinPreLoading()

    }
}