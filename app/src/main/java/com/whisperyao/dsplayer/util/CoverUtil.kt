package com.whisperyao.dsplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.provider.DatabaseAccesser
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import androidx.core.net.toUri


class CoverUtil {
    private var context: Context
    private val TAG: String = CoverUtil::class.java.name
    constructor(context: Context) {
        this.context = context
    }
    companion object {
        lateinit var mCoverUpdatedMediaId: Subject<String?>
        var publishSubjectCreate: PublishSubject<*> = PublishSubject.create<Any?>()


        fun getCoverUpdatedMediaId(): Subject<String?> {
            mCoverUpdatedMediaId = publishSubjectCreate as Subject<String?>;

            return this.mCoverUpdatedMediaId
        }
    }

    fun getContext(): Context {
        return this.context
    }

    fun getCoverFolder(): File {
        val file = File(this.context.cacheDir, "cover")
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

    fun getFileNameFromMediaID(mediaId: String?): String {
        val local = Locale.getDefault()
        // PlayingQueueManager.SEPARATOR
        val lowerCase: String = "_syno_separator_".lowercase(local)
        // RemoteSettings.FORWARD_SLASH_STRING
        return mediaId?.replace(lowerCase, ":")?.replace("/", "_") ?: ""

    }

    fun getCoverFileFromSong(songItem: SongItem, url: String): File? {
        val mediaId: String? = songItem.mediaId
        val file = File(getCoverFolder().path)
        val file2 = File(file, getFileNameFromMediaID(mediaId) + ".jpg")
        if (file2.exists()) {
            return file2
        } else {
            downloadImage(songItem, url)
        }
        return null
    }

    fun getCoverFileFromSong(songItem: SongItem): File {
        val mediaId: String? = songItem.mediaId
        return getCoverFileFromSong(mediaId)
    }

    fun getCoverFileFromSong(mediaId: String?): File {
        val file = File(File(getCoverFolder().path), getFileNameFromMediaID(mediaId) + ".jpg")
        SynoLog.d("CoverUtil", "getCoverFileFromSong file path: " + file.path)
        return file
    }

    fun downloadImage(mediaId: String, url: String) {
        val uri: Uri = url.toUri()
        downloadImage(mediaId, null, uri, getFileNameFromMediaID(mediaId))
    }

    fun downloadImage(songItem: SongItem, url: String) {
        SynoLog.d("CoverUtil", "downloadImage")
        val mediaId: String? = songItem.mediaId
        val uri: Uri = url.toUri()
        doDownloadImage(mediaId, songItem, uri, getFileNameFromMediaID(mediaId))
    }

    fun downloadImage(mediaId: String?, songItem: SongItem?, uri: Uri, filename: String) {
        val imageRequestBuild: ImageRequest = ImageRequestBuilder.newBuilderWithSource(uri).build()
        val imagePipeline: ImagePipeline = Fresco.getImagePipeline()
        imagePipeline.evictFromCache(uri)
        imagePipeline.fetchDecodedImage(imageRequestBuild, this.context).subscribe(
            object : BaseBitmapDataSubscriber() {
                @Throws(java.io.IOException::class)
                override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage?>?>) {
                }
                override fun onNewResultImpl(bitmap: Bitmap?) {
                    if (bitmap == null) {
                        Log.d(TAG, "download failed")
                    }
                    if (bitmap != null) {
                        val str: String = filename
                        val coverUtil: CoverUtil = this@CoverUtil
                        val songItem2: SongItem? = songItem
                        val str2: String? = mediaId
                        val file = File(coverUtil.getCoverFolder(), "$str.jpg")
                        try {
                            file.delete()
                            val fileOutputStream = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                            fileOutputStream.flush()
                            fileOutputStream.close()
                            if (songItem2 != null) {
                                DatabaseAccesser.getInstance().addCover(songItem2, file.path)
                            }
                            getCoverUpdatedMediaId().onNext(str2)
                        } catch (_: okio.IOException) {
                            Integer.valueOf(Log.d(TAG, "$str2 compress failed"))
                        }
                    }
                }


            }, CallerThreadExecutor.getInstance())
    }

    fun doDownloadImage(mediaId: String?, songItem: SongItem, uri: Uri, filename: String) {
        val imageRequestBuild = ImageRequestBuilder.newBuilderWithSource(uri).build()
        val imagePipeline = Fresco.getImagePipeline()
        imagePipeline.evictFromCache(uri)
        imagePipeline.fetchDecodedImage(imageRequestBuild, this.context)
            .subscribe(object : BaseBitmapDataSubscriber() {
                @Throws(java.io.IOException::class)
                public override fun onNewResultImpl(bitmap: Bitmap?) {
                    if (bitmap == null) {
                        Log.d(TAG, "download failed")
                    }
                    if (bitmap != null) {
                        val str: String = filename
                        val coverUtil: CoverUtil = this@CoverUtil
                        val songItem2: SongItem = songItem
                        val str2: String? = mediaId
                        val file = File(coverUtil.getCoverFolder(), "$str.jpg")
                        try {
                            file.delete()
                            val fileOutputStream = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                            fileOutputStream.flush()
                            fileOutputStream.close()
                            if (songItem2 != null) {
                                DatabaseAccesser.getInstance().addCover(songItem2, file.path)
                            }
                            getCoverUpdatedMediaId().onNext(str2)
                            val unit = Unit
                        } catch (_: java.io.IOException) {
                            Log.d(TAG, "$str2 compress failed")
                        }
                    }
                }

                override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage?>?>) {

                }
            }, CallerThreadExecutor.getInstance())

    }

}