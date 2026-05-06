package com.whisperyao.dsplayer.homepage


import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.datasource.network.vo.BaseVo
import com.whisperyao.dsplayer.item.HomePagePinItem
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.item.PlaylistItem
import com.whisperyao.dsplayer.net.WebAPIErrorException
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.vos.api.pin.PinResponseVo
import com.whisperyao.dsplayer.vos.api.pin.UnpinResponseVo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList


class PinManager {

    protected var exception: WebAPIErrorException? = null
    private var bIsLoading = false

    private val mCallbacks: ArrayList<Callback> = ArrayList()
    private val mItems: MutableList<HomePagePinItem> = LinkedList<HomePagePinItem>()

    private var mTotal = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var loadJob: Job? = null

    companion object {
        const val MODE: String = "mode"
        const val LOG: String = "PinManager"
        const val DISPLAY_ARTIST = "display_artist"
        const val TYPE_RECENTLY_ADDED = "recently_added"
        const val TYPE_RANDOM_100: String = "random_100"
        const val NORMAL: String = "normal"
        const val GENRE_FILTER = "genre_filter"
        const val PERSONAL: String = "personal"
        const val PLAYLIST_ID_SHARED_SONG = "playlist_personal_normal/__SYNO_AUDIO_SHARED_SONGS__"
        const val SONG = "song"

        const val EXTRA_PLAYLIST: String = "extra_playlist"
        private var instance: PinManager? = null

        @JvmStatic
        fun getContainerTypeByItem(item: HomePagePinItem): Common.ContainerType {
            val type = item.type
            val criteria = item.criteria
            type.hashCode()
            when (type) {
                "artist" -> {
                    if (criteria.containsKey("genre")) {
                        return Common.ContainerType.GENRE_ARTIST_MODE
                    }
                    return Common.ContainerType.ARTIST_MODE
                }

                "folder" -> return Common.ContainerType.FOLDER_MODE
                "composer" -> return Common.ContainerType.COMPOSER_MODE
                "album" -> {
                    if (criteria.containsKey("genre")) {
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

        @JvmStatic
        fun getEnumSongsBundle(item: HomePagePinItem): Bundle {
            val result = Bundle()
            result.putBoolean(MODE, true)

            val type = item.type
            val criteria = item.criteria

            val uiInfo = Bundle()

            // --- ui_info 构建 ---
            if (criteria.containsKey("album_artist")) {
                uiInfo.putString("album_artist", criteria.get("album_artist"))
                uiInfo.putString(DISPLAY_ARTIST, criteria.get("album_artist"))
            } else if (criteria.containsKey("artist")) {
                uiInfo.putString(DISPLAY_ARTIST, criteria.get("artist"))
            }

            uiInfo.putString("title", item.title)
            result.putBundle("ui_info", uiInfo)

            // --- 主逻辑分支 ---
            when (type) {

                "artist" -> {
                    result.putString("type", "container")
                    result.putString("title", item.title)

                    if (criteria.containsKey("artist")) {
                        result.putString("key", criteria["artist"])
                        result.putString("artist", criteria["artist"])
                    }

                    when {
                        criteria.containsKey("genre") -> {
                            result.putString("genre", criteria["genre"])
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                        }

                        criteria.containsKey(GENRE_FILTER) -> {
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
                    result.putString("key", criteria.get("folder"))
                    result.putString("id", criteria.get("folder"))
                    result.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.FOLDER_MODE.name
                    )
                }

                "composer" -> {
                    result.putString("type", "container")
                    result.putString("title", item.title)
                    result.putString("key", criteria.get("composer"))
                    result.putString("composer", criteria.get("composer"))
                    result.putString(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.COMPOSER_ALBUM_MODE.name
                    )
                }

                "album" -> {
                    result.putString("type", SONG)
                    result.putString("title", item.title)

                    if (criteria.containsKey("album")) {
                        result.putString("album", criteria["album"])
                        result.putString("key", criteria["album"])
                    }

                    if (criteria.containsKey("album_artist")) {
                        result.putString("album_artist", criteria["album_artist"])
                    }

                    when {
                        criteria.containsKey("genre") -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                            result.putString("genre", criteria["genre"])
                            if (criteria.containsKey("artist")) {
                                result.putString("artist", criteria["artist"])
                            }
                        }

                        criteria.containsKey(GENRE_FILTER) -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                            result.putString(GENRE_FILTER, criteria[GENRE_FILTER])
                            if (criteria.containsKey("artist")) {
                                result.putString("artist", criteria["artist"])
                            }
                        }

                        criteria.containsKey("artist") -> {
                            result.putString(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.ARTIST_ALBUM_MODE.name
                            )
                            result.putString("artist", criteria["artist"])
                        }

                        criteria.containsKey("composer") -> {
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
                    result.putString("key", criteria.get("genre"))
                    result.putString("genre", criteria.get("genre"))
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
                    result.putString("key", criteria.get("playlist"))

                    val isNormal = NORMAL.equals(criteria.get("type"), ignoreCase = true)
                    val isPersonal = PERSONAL.equals(criteria.get("library"), ignoreCase = true)

                    val itemType = when {
                        isPersonal && isNormal -> Item.ItemType.PERSONAL_NORMAL_NEW
                        isPersonal && !isNormal -> Item.ItemType.PERSONAL_SMART_NEW
                        !isPersonal && isNormal -> Item.ItemType.SHARED_NORMAL_NEW
                        else -> Item.ItemType.SHARED_SMART_NEW
                    }

                    val bundle = PlaylistItem.generatePredifinedPlaylist(
                        itemType,
                        criteria.get("playlist"),
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

        @JvmStatic
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

    fun pin(playlistItem: PlaylistItem, containerType: Common.ContainerType) {
        when {
            playlistItem.isRandom() -> {
                pin(TYPE_RANDOM_100, HashMap(), playlistItem.title)
            }

            playlistItem.isRecentlyAdded() -> {
                pin(TYPE_RECENTLY_ADDED, HashMap(), playlistItem.title)
            }

            else -> {
                val bundle = Bundle().apply {
                    putString("playlist", playlistItem.id)
                }

                pin(
                    "playlist",
                    getPinCriteria(containerType, bundle),
                    playlistItem.title
                )
            }
        }
    }

    fun pin(type: String, criteria: HashMap<String, String>, name: String) {
        exception = null
        bIsLoading = true
        mItems.clear()

        mCallbacks.forEach {
            it.onPinPreLoading()
        }

        scope.launch {
            val result: PinResponseVo? = try {
                withContext(Dispatchers.IO) {
                    ConnectionManager.pin(type, criteria, name)
                }
            } catch (e: WebAPIErrorException) {
                exception = e
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (result != null && result.success) {
                showMessageToast(R.string.pin_success)
            } else {
                handlePinResponseError(result)
            }

            loadPinList(true)
        }
    }

    fun unpin(playlistItem: PlaylistItem, containerType: Common.ContainerType) {
        when {
            playlistItem.isRandom() -> {
                unpin(getPinId(TYPE_RANDOM_100, hashMapOf()))
            }

            playlistItem.isRecentlyAdded() -> {
                unpin(getPinId(TYPE_RECENTLY_ADDED, hashMapOf()))
            }

            else -> {
                val bundle = Bundle().apply {
                    putString("playlist", playlistItem.id)
                }

                unpin(
                    getPinId(
                        "playlist",
                        getPinCriteria(containerType, bundle)
                    )
                )
            }
        }
    }

    fun unpin(id: String) {
        unpin(listOf(id))
    }

    fun unpin(idList: List<String>) {
        exception = null
        bIsLoading = true
        mItems.clear()

        mCallbacks.forEach {
            it.onPinPreLoading()
        }

        scope.launch {

            val result: UnpinResponseVo? = try {
                withContext(Dispatchers.IO) {
                    ConnectionManager.unPin(idList)
                }
            } catch (e: WebAPIErrorException) {
                exception = e
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (result?.success == true) {
                showMessageToast(R.string.unpin_success)
            } else {
                handleUnpinResponseError(result)
            }

            loadPinList(true)
        }
    }

    private fun handlePinResponseError(vo: PinResponseVo?) {
        if (vo?.error != null) {
            val errors = vo.error.errors
            val code = errors.firstOrNull() ?: -1

            when (code) {
                1006 -> {
                    showMessageToast(R.string.pin_success)
                }

                1008, 1009 -> {
                    showMessageToast(R.string.operation_failed)
                }

                1010, 1011 -> {
                    showMessageToast(R.string.error_pin_target_not_exist)
                    mCallbacks.forEach {
                        it.onPinErrorOccur()
                    }
                }

                else -> {
                    showMessageToast(R.string.operation_failed)
                }
            }
        } else {
            showMessageToast(R.string.pin_fail)
        }
    }

    private fun handleUnpinResponseError(vo: UnpinResponseVo?) {

        val errors = vo?.error?.errors

        if (!errors.isNullOrEmpty()) {

            val hasRealError = errors.any { it.error != 1007 }

            if (!hasRealError) {
                // 1007 = already unpinned / not exist → 当成功
                showMessageToast(R.string.unpin_success)
            } else {
                showMessageToast(R.string.unpin_fail)
            }

        } else {
            showMessageToast(R.string.unpin_fail)
        }
    }

    private fun showMessageToast(strId: Int) {
        Toast.makeText(App.getContext(), strId, Toast.LENGTH_SHORT)
            .show()
    }

    fun rename(id: String, name: String) {
        scope.launch {
            exception = null
            bIsLoading = true
            mItems.clear()

            if (mCallbacks.isNotEmpty()) {
                mCallbacks.forEach {
                    it.onPinPreLoading()
                }
            }

            val result: BaseVo? = withContext(Dispatchers.IO) {
                try {
                    ConnectionManager.rename(id, name)
                } catch (e: WebAPIErrorException) {
                    exception = e
                    null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (result?.success != true) {
                val errorCode = result?.error?.code

                when (errorCode) {
                    1007 -> showMessageToast(R.string.error_pin_item_not_exist)
                    else -> showMessageToast(R.string.operation_failed)
                }
            }

            loadPinList(true)
        }
    }

    fun reorder(idList: List<String>) {
        scope.launch {

            exception = null
            bIsLoading = true
            mItems.clear()

            mCallbacks.forEach { it.onPinPreLoading() }

            val result: BaseVo? = withContext(Dispatchers.IO) {
                try {
                    ConnectionManager.reorder(idList)
                } catch (e: WebAPIErrorException) {
                    exception = e
                    null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            result?.let { vo ->
                if (!vo.success) {
                    if (vo.error?.code == 1005) {
                        showMessageToast(R.string.pin_reorder_fail)
                    } else {
                        showMessageToast(R.string.operation_failed)
                    }
                }
            }

            loadPinList(true)
        }
    }

    fun unpinAndReorder(deleteIdList: List<String>, reorderIdList: List<String>) {
        scope.launch {

            exception = null
            bIsLoading = true
            mItems.clear()
            mCallbacks.forEach { it.onPinPreLoading() }

            val result = withContext(Dispatchers.IO) {
                try {
                    ConnectionManager.unPin(deleteIdList)
                } catch (e: WebAPIErrorException) {
                    exception = e
                    null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            result?.let {
                if (it.success) {
                    showMessageToast(R.string.unpin_success)
                } else {
                    handleUnpinResponseError(it)
                }
            }

            reorder(reorderIdList)
        }
    }

    fun isLoading(): Boolean {
        return this.bIsLoading
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

    fun getPinId(type: String, criteria: Map<String, String>): String {

        for (item in mItems) {

            val itemType = item.type
            val itemCriteria = item.criteria

            if ((type == TYPE_RANDOM_100 || type == TYPE_RECENTLY_ADDED) && type == itemType) {
                return item.id
            }

            if (type != itemType) continue

            when (type) {

                "playlist" -> {
                    val playlistId = criteria["playlist"]
                    if (playlistId != null &&
                        playlistId == itemCriteria["playlist"]
                    ) {
                        return item.id
                    }
                }

                "folder" -> {
                    val folderId = criteria["folder"]
                    if (folderId != null &&
                        folderId == itemCriteria["folder"]
                    ) {
                        return item.id
                    }
                }
            }

            if (itemCriteria == criteria) {
                return item.id
            }
        }

        return ""
    }

    fun loadPinList(refresh: Boolean) {

        loadJob?.cancel()

        loadJob = scope.launch {
            exception = null
            bIsLoading = true
            mItems.clear()

            mCallbacks.forEach {
                it.onPinPreLoading()
            }

            var retItems: List<HomePagePinItem>? = null

            withContext(Dispatchers.IO) {
                try {
                    val itemSet = CacheManager.getInstance().doEnumPins(refresh)
                    retItems = itemSet.itemList
                    mTotal = itemSet.total
                } catch (e: WebAPIErrorException) {
                    exception = e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            bIsLoading = false

            retItems?.takeIf { it.isNotEmpty() }?.let {
                mItems.addAll(it)
            }

            mCallbacks.forEach {
                it.onPinLoadFinish()
            }
        }
    }

    fun getWebAPIErrorException(): WebAPIErrorException? {
        return this.exception
    }

    fun getItems(): MutableList<HomePagePinItem> {
        return this.mItems
    }

    fun alreadyPin(type: String, criteria: Map<String, String>): Boolean {
        for (homePagePinItem in mItems) {
            val type2 = homePagePinItem.type
            val criteria2 = homePagePinItem.criteria
            if (type == TYPE_RANDOM_100 && type == type2) {
                return true
            }
            if (type == TYPE_RECENTLY_ADDED && type == type2) {
                return true
            }
            if (type == type2) {
                if (type == "playlist" && criteria["playlist"] != null) {
                    if (criteria["playlist"] == criteria2.get("playlist")) return true
                } else if (type == "folder" && criteria["folder"] != null
                    && criteria["folder"] == criteria2.get("folder")) return true
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
                if (alreadyPin(TYPE_RANDOM_100, HashMap())) {
                    unpin.isVisible = true
                    return
                } else {
                    pin.isVisible = true
                    return
                }
            }
            if (playlistItem.isRecentlyAdded()) {
                if (alreadyPin(TYPE_RECENTLY_ADDED, HashMap())) {
                    unpin.isVisible = true
                    return
                } else {
                    pin.isVisible = true
                    return
                }
            }
            if (playlistItem.isCanPinnedPlaylist()) {
                val bundle = Bundle()
                bundle.putString("playlist", playlistItem.id)
                if (alreadyPin(
                        "playlist",
                        getPinCriteria(Common.ContainerType.PLAYLIST_MODE, bundle)
                    )
                ) {
                    unpin.isVisible = true
                } else {
                    pin.isVisible = true
                }
            }
        }
    }

    fun onPrepareOptionsMenu(menu: Menu, bundle: Bundle) {
        val isOnline = bundle.getBoolean(MODE)
        val containerTypeValueOf = Common.ContainerType.valueOf(bundle.getString(Common.CONTAINER_TYPE)!!)
        val type: String = bundle.getString("type") ?: ""
        val key: String = bundle.getString("key") ?: ""
        SynoLog.d(LOG, " onPrepareOptionsMenu isOnline= $isOnline , containerType= $containerTypeValueOf , list_type= $type , key= $key")
        if (!ConnectionManager.canSupportPin() || !isOnline || !isCanPinType(containerTypeValueOf, type) ||
            ((containerTypeValueOf == Common.ContainerType.FOLDER_MODE && TextUtils.isEmpty(key)) ||
                    (containerTypeValueOf == Common.ContainerType.GENRE_ARTIST_MODE && !bundle.containsKey("genre")))) {
            menu.findItem(R.id.menu_pin).isVisible = false
            menu.findItem(R.id.menu_unpin).isVisible = false
            return
        }
        menu.findItem(R.id.menu_pin).isVisible = true
        menu.findItem(R.id.menu_unpin).isVisible = true
        if (alreadyPin(getOptionsMenuTypeParamName(containerTypeValueOf, type), getPinCriteria(containerTypeValueOf, bundle))) {
            menu.findItem(R.id.menu_pin).isVisible = false
        } else {
            menu.findItem(R.id.menu_unpin).isVisible = false
        }
    }

    fun getOptionsMenuTypeParamName(type: Common.ContainerType, listType: String): String {
        val name = type.optionsMenuParamName(listType)
        SynoLog.d(LOG, " getOptionsMenuTypeParamName ContainerType= $type , list_type = $listType , name- $name")
        return name
    }

    fun getQuickActionTypeParamName(type: Common.ContainerType): String {
        return type.quickActionParamName
    }

    private fun isCanPinType(containerType: Common.ContainerType, listType: String): Boolean {
        if ("container" == listType) {
            return containerType.isCanPinTypeInContainer()
        }
        return containerType.isCanPinTypeInContainerSong()
    }

    interface Callback {
        fun onPinErrorOccur()

        fun onPinLoadFinish()

        fun onPinPreLoading()

    }
}