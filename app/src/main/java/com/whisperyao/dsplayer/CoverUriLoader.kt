package com.whisperyao.dsplayer


import android.net.Uri
import android.os.Bundle
import android.view.View
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.facebook.imagepipeline.request.Postprocessor
import com.synology.sylib.syhttp3.tuple.BasicKeyValuePair
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.net.AudioStationAPI
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.CoverUtil
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utilities
import java.lang.ref.WeakReference
import jp.wasabeef.fresco.processors.BlurPostprocessor;
import androidx.core.net.toUri
import com.whisperyao.dsplayer.item.HomePagePinItem
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.model.data.PlayingQueueManager


class CoverUriLoader {

    companion object {
        private const val LOG = "CoverUriLoader"

        private const val GET_COVER = "getcover"
        private const val GET_COVER_FOR_ALBUM = "get_cover_for_album"
        private const val GET_COVER_FOR_FOLDER = "get_cover_for_folder"
    }

    private var blur = false
    private var key: String? = null
    private var legacyUri: Uri? = null
    private var md5: String? = null
    private var type: Common.ContainerType? = null
    private var postprocessor: Postprocessor? = null
    private var view: SimpleDraweeView? = null

    private var params = ArrayList<BasicKeyValuePair>()

    private var failureRes = 0
    private val dsId = Common.getDsId()

    fun with(view: SimpleDraweeView): CoverUriLoader {
        this.view = view
        return this
    }

    fun blur(radius: Int): CoverUriLoader {
        blur = true
        postprocessor = BlurPostprocessor(view?.context, radius)
        return this
    }

    fun containerType(type: Common.ContainerType): CoverUriLoader {
        this.type = type
        return this
    }

    fun legacy(uri: Uri): CoverUriLoader {
        legacyUri = uri
        return this
    }

    fun failureImage(res: Int): CoverUriLoader {
        failureRes = res
        return this
    }

    fun failureImage(type: Common.ContainerType): CoverUriLoader {
        this.failureRes = getPlaceHolder(type)
        return this
    }

    fun placeHolder(type: Common.ContainerType): CoverUriLoader {
        this.type = type
        view?.hierarchy?.apply {
            setPlaceholderImage(getPlaceHolder(type))
            actualImageScaleType = ScalingUtils.ScaleType.CENTER_CROP
        }
        return this
    }

    fun placeHolder(res: Int): CoverUriLoader {
        view?.hierarchy?.apply {
            setPlaceholderImage(res)
            actualImageScaleType = ScalingUtils.ScaleType.CENTER_CROP
        }
        return this
    }

    private fun getPlaceHolder(type: Common.ContainerType): Int {
        return when (type) {

            Common.ContainerType.LATEST_ALBUM_MODE,
            Common.ContainerType.ALBUM_MODE,
            Common.ContainerType.ARTIST_ALBUM_MODE,
            Common.ContainerType.COMPOSER_ALBUM_MODE,
            Common.ContainerType.GENRE_ALBUM_MODE,
            Common.ContainerType.GENRE_ARTIST_ALBUM_MODE,
            Common.ContainerType.SEARCH_ALBUM_MODE,

            Common.ContainerType.GENRE_MODE,

            Common.ContainerType.ARTIST_MODE,
            Common.ContainerType.SEARCH_ARTIST_MODE,
            Common.ContainerType.GENRE_ARTIST_MODE,

            Common.ContainerType.COMPOSER_MODE,
            Common.ContainerType.FOLDER_MODE,
            Common.ContainerType.PLAYLIST_MODE,
            Common.ContainerType.RANDOM100_MODE,
            Common.ContainerType.SEARCH_SONG_MODE,
            Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE -> {
                // 原代码所有分支最终都 return 同一个值
                R.drawable.thumbnail_song
            }

            else -> {
                SynoLog.e(LOG, "unsupported type : ${type.name}")
                R.drawable.thumbnail_song
            }
        }
    }

    fun load(songItem: SongItem) {
        loadSong(songItem)
    }

    private fun loadSong(songItem: SongItem) {
        val draweeView = view ?: return

        val coverUrl = ConnectionManager.getCoverUrl(songItem.id)
        val localFile = CoverUtil(App.getContext()).getCoverFileFromSong(songItem)

        val sources = mutableListOf<Uri>()

        if (localFile.exists()) {
            SynoLog.d("CoverUriLoader", "localFile exists")
            sources.add(Uri.fromFile(localFile))
        } else {
            CoverUtil(App.getContext()).downloadImage(songItem, coverUrl)
        }

        sources.add(coverUrl.toUri())

        sources.forEach { uri ->
            val requestBuilder =
                ImageRequestBuilder.newBuilderWithSource(uri)

            if (blur) {
                requestBuilder.postprocessor = postprocessor
            }

            val controllerBuilder = Fresco.newDraweeControllerBuilder()
                .setImageRequest(requestBuilder.build())
                .setOldController(draweeView.controller)

            controllerBuilder.controllerListener =
                object : ControllerListenerWithView(draweeView) {

                    override fun onFailure(id: String?, throwable: Throwable?) {
                        val imageView = getView() as? SimpleDraweeView ?: return

                        if (failureRes != 0) {
                            Fresco.getImagePipeline().evictFromCache(uri)

                            val fallbackBuilder =
                                legacyUri?.let {
                                    ImageRequestBuilder.newBuilderWithSource(it)
                                } ?: ImageRequestBuilder.newBuilderWithResourceId(failureRes)

                            if (blur) {
                                fallbackBuilder.postprocessor = postprocessor
                            }

                            imageView.controller =
                                Fresco.newDraweeControllerBuilder()
                                    .setImageRequest(fallbackBuilder.build())
                                    .setOldController(imageView.controller)
                                    .build()
                        } else {
                            legacyUri?.let {
                                imageView.setImageURI(it)
                            }
                        }
                    }
                }

            draweeView.controller = controllerBuilder.build()
        }

    }

    fun load(bundle: Bundle) {
        md5 = Utilities.getMD5Code(bundle.toString())
        params = arrayListOf()

        SynoLog.d(LOG, "mType: $type, bundle: $bundle, id: ${bundle.getString("id")}")

        val method = when {
            ConnectionManager.loadCoverCgi() -> {
                if (type == Common.ContainerType.FOLDER_MODE) {
                    bundle.getString("id")?.let {
                        params.add(BasicKeyValuePair("album_name", it))
                    }
                    GET_COVER_FOR_FOLDER
                } else {
                    bundle.getString("album")?.let {
                        params.add(BasicKeyValuePair("album_name", it))
                    }

                    when {
                        bundle.containsKey("artist") -> {
                            bundle.getString("artist")?.let {
                                params.add(BasicKeyValuePair("artist_name", it))
                            }
                        }

                        bundle.containsKey("genre") -> {
                            bundle.getString("genre")?.let {
                                params.add(BasicKeyValuePair("genre_name", it))
                            }
                        }
                    }

                    GET_COVER_FOR_ALBUM
                }
            }

            type == Common.ContainerType.FOLDER_MODE -> {
                val id = bundle.getString("id")

                val folderId = try {
                    id?.toInt()
                    "dir_$id"
                } catch (_: Exception) {
                    id
                }

                folderId?.let {
                    params.add(BasicKeyValuePair("id", it))
                }

                "getfoldercover"
            }

            else -> {
                bundle.getString("album")?.let {
                    params.add(BasicKeyValuePair("album_name", it))
                }

                if (bundle.containsKey("artist") && type != Common.ContainerType.ARTIST_ALBUM_MODE) {
                    bundle.getString("artist")?.let {
                        params.add(BasicKeyValuePair("artist_name", it))
                    }
                }

                bundle.getString("genre")?.let {
                    params.add(BasicKeyValuePair("genre_name", it))
                }

                bundle.getString("composer")?.let {
                    params.add(BasicKeyValuePair("composer_name", it))
                }

                bundle.getString("album_artist")?.let {
                    params.add(BasicKeyValuePair("album_artist_name", it))
                }

                if (type == Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE) {
                    bundle.getString("genre_filter")?.let {
                        params.add(BasicKeyValuePair("default_genre_name", it))
                        params.add(BasicKeyValuePair("is_hr", "true"))
                    }
                }

                "getcover"
            }
        }

        params.add(BasicKeyValuePair("library", AbstractNetManager.getPersonalLibraryValue()))

        loadIt(AudioStationAPI.SYNO_AUDIOSTATION_COVER, method)
    }

    fun load(item: Item, apiName: String) {
        md5 = Utilities.getMD5Code(item.bundle.toString())

        when (type) {
            Common.ContainerType.LATEST_ALBUM_MODE,
            Common.ContainerType.ALBUM_MODE,
            Common.ContainerType.ARTIST_ALBUM_MODE -> {
                val albumName = if (!item.id.isNullOrEmpty()) {
                    item.title
                } else {
                    item.id
                }
                params.add(BasicKeyValuePair("album_name", albumName))
                params.add(BasicKeyValuePair("album_artist_name", item.albumArtist))
            }

            Common.ContainerType.COMPOSER_ALBUM_MODE -> {
                val albumName = if (!item.id.isNullOrEmpty()) {
                    item.title
                } else {
                    item.id
                }

                params.add(BasicKeyValuePair("album_name", albumName))
                params.add(BasicKeyValuePair("composer_name", key))
            }

            Common.ContainerType.GENRE_ALBUM_MODE,
            Common.ContainerType.GENRE_ARTIST_ALBUM_MODE -> {
                params.add(BasicKeyValuePair("genre_name", key))
                params.add(BasicKeyValuePair("album_artist_name", item.albumArtist))
                params.add(BasicKeyValuePair("album_name", item.title))
            }

            Common.ContainerType.SEARCH_ALBUM_MODE,
            Common.ContainerType.GENRE_MODE -> {
                params.add(BasicKeyValuePair("genre_name", item.title))
            }

            Common.ContainerType.ARTIST_MODE -> {
                params.add(BasicKeyValuePair("artist_name", item.title))
            }

            Common.ContainerType.SEARCH_ARTIST_MODE,
            Common.ContainerType.GENRE_ARTIST_MODE -> {
                params.add(BasicKeyValuePair("genre_name", key))
                params.add(BasicKeyValuePair("album_artist_name", item.id))
            }

            Common.ContainerType.COMPOSER_MODE -> {
                params.add(BasicKeyValuePair("composer_name", item.title))
            }

            Common.ContainerType.FOLDER_MODE,
            Common.ContainerType.PLAYLIST_MODE,
            Common.ContainerType.RANDOM100_MODE,
            Common.ContainerType.SEARCH_SONG_MODE -> {
                // no-op
            }

            Common.ContainerType.HOMEPAGE_DEFAULT_GENRE_MODE -> {
                params.add(
                    BasicKeyValuePair("library", AbstractNetManager.getPersonalLibraryValue())
                )
                params.add(BasicKeyValuePair("is_hr", "true"))
                params.add(BasicKeyValuePair("default_genre_name", item.title))
            }

            else -> {
                SynoLog.e(LOG, "unsupported type : ${type?.name}")
            }
        }

        loadIt(apiName)
    }

    fun load(item: HomePagePinItem, apiName: String) {
        md5 = Utilities.getMD5Code(item.criteria.toString())
        params.clear()

        val criteria = item.criteria

        when {
            criteria.containsKey("album_artist") -> {
                params.add(BasicKeyValuePair("album_artist", criteria["album_artist"]))
                params.add(BasicKeyValuePair("display_artist", criteria["album_artist"]))
            }

            criteria.containsKey("artist") -> {
                params.add(BasicKeyValuePair("display_artist", criteria["artist"]))
            }
        }

        params.add(BasicKeyValuePair("title", item.title))
        params.add(BasicKeyValuePair("id", item.id))

        when (item.type) {

            "artist" -> {
                params.add(BasicKeyValuePair("type", "container"))
                params.add(BasicKeyValuePair("title", item.title))

                criteria["artist"]?.let {
                    params.add(BasicKeyValuePair("key", it))
                    params.add(BasicKeyValuePair("artist", it))
                }

                when {
                    criteria.containsKey("genre") -> {
                        params.add(BasicKeyValuePair("genre", criteria["genre"]))
                        params.add(
                            BasicKeyValuePair(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                        )
                    }

                    criteria.containsKey("genre_filter") -> {
                        params.add(BasicKeyValuePair("genre_filter", criteria["genre_filter"]))
                        params.add(
                            BasicKeyValuePair(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                        )
                    }

                    else -> {
                        params.add(
                            BasicKeyValuePair(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.ARTIST_ALBUM_MODE.name
                            )
                        )
                    }
                }
            }

            "folder" -> {
                params.add(BasicKeyValuePair("type", "container"))
                params.add(BasicKeyValuePair("title", item.title))

                criteria["folder"]?.let {
                    params.add(BasicKeyValuePair("key", it))
                    params.add(BasicKeyValuePair("id", it))
                }

                params.add(
                    BasicKeyValuePair(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.FOLDER_MODE.name
                    )
                )
            }

            "composer" -> {
                params.add(BasicKeyValuePair("type", "container"))
                params.add(BasicKeyValuePair("title", item.title))

                criteria["composer"]?.let {
                    params.add(BasicKeyValuePair("key", it))
                    params.add(BasicKeyValuePair("composer", it))
                }

                params.add(
                    BasicKeyValuePair(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.COMPOSER_ALBUM_MODE.name
                    )
                )
            }

            "album" -> {
                params.add(BasicKeyValuePair("type", "song"))
                params.add(BasicKeyValuePair("title", item.title))

                criteria["album"]?.let {
                    params.add(BasicKeyValuePair("album", it))
                    params.add(BasicKeyValuePair("key", it))
                }

                criteria["album_artist"]?.let {
                    params.add(BasicKeyValuePair("album_artist", it))
                }

                when {
                    criteria.containsKey("genre") -> {
                        params.add(
                            BasicKeyValuePair(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                        )
                        params.add(BasicKeyValuePair("genre", criteria["genre"]))

                        criteria["artist"]?.let {
                            params.add(BasicKeyValuePair("artist", it))
                        }
                    }

                    criteria.containsKey("genre_filter") -> {
                        params.add(
                            BasicKeyValuePair(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.GENRE_ARTIST_ALBUM_MODE.name
                            )
                        )
                        params.add(BasicKeyValuePair("genre_filter", criteria["genre_filter"]))

                        criteria["artist"]?.let {
                            params.add(BasicKeyValuePair("artist", it))
                        }
                    }

                    criteria.containsKey("artist") -> {
                        params.add(
                            BasicKeyValuePair(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.ARTIST_ALBUM_MODE.name
                            )
                        )
                        params.add(BasicKeyValuePair("artist", criteria["artist"]))
                    }

                    criteria.containsKey("composer") -> {
                        params.add(
                            BasicKeyValuePair(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.COMPOSER_ALBUM_MODE.name
                            )
                        )
                        params.add(BasicKeyValuePair("composer", criteria["composer"]))
                    }

                    else -> {
                        params.add(
                            BasicKeyValuePair(
                                Common.CONTAINER_TYPE,
                                Common.ContainerType.ALBUM_MODE.name
                            )
                        )
                    }
                }
            }

            "genre" -> {
                params.add(BasicKeyValuePair("type", "container"))
                params.add(BasicKeyValuePair("title", item.title))

                criteria["genre"]?.let {
                    params.add(BasicKeyValuePair("key", it))
                    params.add(BasicKeyValuePair("genre", it))
                }

                params.add(
                    BasicKeyValuePair(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.GENRE_ARTIST_MODE.name
                    )
                )
            }

            "random_100" -> {
                params.add(
                    BasicKeyValuePair(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.RANDOM100_MODE.name
                    )
                )
                params.add(
                    BasicKeyValuePair(
                        "type",
                        Item.ItemType.CONTAINER_MODE.name
                    )
                )
                params.add(BasicKeyValuePair("key", Common.CAT_RANDOM100_ID))
                params.add(BasicKeyValuePair("title", item.title))
            }

            "recently_added" -> {
                params.add(
                    BasicKeyValuePair(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.LATEST_ALBUM_MODE.name
                    )
                )
                params.add(BasicKeyValuePair("type", "container"))
            }

            "playlist" -> {
                params.add(
                    BasicKeyValuePair(
                        Common.CONTAINER_TYPE,
                        Common.ContainerType.PERSONAL_PLAYLIST_MODE.name
                    )
                )
                params.add(BasicKeyValuePair("title", item.title))

                criteria["playlist"]?.let {
                    params.add(BasicKeyValuePair("key", it))
                }
            }
        }

        loadIt(apiName)
    }

    private fun generateMediaId(
        type: Common.ContainerType,
        params: List<BasicKeyValuePair>
    ): String {
        return generateMediaIdImpl(type, params).lowercase()
    }

    private fun generateMediaIdImpl(
        type: Common.ContainerType,
        params: List<BasicKeyValuePair>
    ): String {
        val map = params.associate {
            it.first as String to it.second as String
        }

        return when (type) {
            Common.ContainerType.ALBUM_MODE -> {
                PlayingQueueManager.PREFIX_BROWSABLE +
                        type.name +
                        PlayingQueueManager.SEPARATOR +
                        "${map["album_artist_name"]}" +
                        PlayingQueueManager.SEPARATOR +
                        "${map["album_name"]}"
            }

            Common.ContainerType.ARTIST_MODE -> {
                PlayingQueueManager.PREFIX_BROWSABLE +
                        type.name +
                        PlayingQueueManager.SEPARATOR +
                        "${map["artist_name"]}"
            }

            Common.ContainerType.GENRE_MODE -> {
                PlayingQueueManager.PREFIX_BROWSABLE +
                        type.name +
                        PlayingQueueManager.SEPARATOR +
                        "${map["genre_name"]}"
            }

            Common.ContainerType.FOLDER_MODE -> {
                PlayingQueueManager.PREFIX_BROWSABLE +
                        type.name +
                        PlayingQueueManager.SEPARATOR +
                        "${map["id"]}"
            }

            else -> {
                params.toString()
            }
        }
    }

    private fun loadIt(apiName: String, method: String = GET_COVER) {
        val draweeView = view ?: return
        val currentType = type ?: return

        val url = Common.composeUrl(
            AudioPreference.getCoverPath(),
            apiName,
            AudioPreference.getCoverVer(),
            method,
            params
        )

        val mediaId = generateMediaId(currentType, params)

        val localFile = CoverUtil(App.getContext()).getCoverFileFromSong(mediaId)

        val sourceUri = if (localFile.exists()) {
            SynoLog.d("CoverUriLoader", "localFile exists")
            Uri.fromFile(localFile)
        } else {
            CoverUtil(App.getContext()).downloadImage(mediaId, url)
            url.toUri()
        }

        val requestBuilder = ImageRequestBuilder.newBuilderWithSource(sourceUri)

        if (blur) {
            requestBuilder.postprocessor = postprocessor
        }

        val controllerBuilder = Fresco.newDraweeControllerBuilder()
            .setImageRequest(requestBuilder.build())
            .setOldController(draweeView.controller)

        controllerBuilder.controllerListener = object : ControllerListenerWithView(draweeView) {

                override fun onFailure(
                    id: String?,
                    throwable: Throwable?
                ) {
                    val imageView =
                        getView() as? SimpleDraweeView ?: return

                    if (failureRes != 0) {
                        Fresco.getImagePipeline()
                            .evictFromCache(sourceUri)

                        val fallbackBuilder =
                            legacyUri?.let {
                                ImageRequestBuilder.newBuilderWithSource(it)
                            } ?: ImageRequestBuilder
                                .newBuilderWithResourceId(failureRes)

                        if (blur) {
                            fallbackBuilder.postprocessor =
                                postprocessor
                        }

                        imageView.controller =
                            Fresco.newDraweeControllerBuilder()
                                .setImageRequest(
                                    fallbackBuilder.build()
                                )
                                .setOldController(
                                    imageView.controller
                                )
                                .build()
                    } else {
                        legacyUri?.let {
                            imageView.setImageURI(it)
                        }
                    }
                }
            }

        draweeView.controller = controllerBuilder.build()

    }

    open class ControllerListenerWithView(view: View) :
        BaseControllerListener<Any>() {

        private val viewRef = WeakReference(view)

        protected fun getView(): View? {
            return viewRef.get()
        }
    }
}