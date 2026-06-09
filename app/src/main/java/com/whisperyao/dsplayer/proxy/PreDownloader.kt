package com.whisperyao.dsplayer.proxy

import android.content.Context
import com.synology.sylib.util.IOUtils
import com.synology.sylib.util.NetworkUtils
import com.synology.sylibx.synofile.SAFUtils
import com.synology.sylibx.synofile.SynoFile
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.provider.DatabaseAccesser
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utilities
import com.whisperyao.dsplayer.util.extension.Extensions.getProperFile
import okhttp3.Call
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class PreDownloader(
    private val context: Context,
    val songItem: SongItem,
    private val listener: OnDownloadCompletedListener?
) : Runnable {

    companion object {
        private const val BUFFER_SIZE = 65536
        private const val HTTP_PREFIX = "http"
        private const val TAG = "PreDownloader"
    }

    enum class DownloadStatus {
        IDLE,
        Error,
        PREPARING,
        HEADER_DONE,
        FILE_DONE
    }

    interface OnDownloadCompletedListener {
        fun onDownloadCompleted(songItem: SongItem)
    }

    class InputStreamNullException : Exception(
        "InputStream should not be null."
    )

    private var cachePath: String = ""
    private var call: Call? = null
    private var dstFile: SynoFile? = null
    var response: Response? = null
    private var status = DownloadStatus.IDLE
    private var tempFile: File? = null
    private var thread: Thread? = null
    private var writeBytes = 0

    fun getHeaders(): Headers? = response?.headers

    fun start() {
        SynoLog.d(TAG, "start : ${songItem.filePath}")
        status = DownloadStatus.PREPARING
        thread = Thread(this).also { it.start() }
    }

    fun stop() {
        SynoLog.d(TAG, "stop : ${songItem.filePath}")

        Thread {
            Utilities.removeFile(cachePath)

            if (!isCompleted() ||
                !CacheManager.getInstance().rotateSong(File(cachePath).length())
            ) {
                dstFile?.delete()
                cachePath = ""
                CacheManager.getInstance().deleteSong(songItem)
            }

            call?.cancel()

            val t = thread
            if (t == null) {
                SynoLog.e(
                    TAG,
                    "Cannot stop downloader. it has not been started."
                )
                return@Thread
            }

            t.interrupt()

            try {
                t.join(5000L)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun run() {
        val url = ConnectionManager.getOriginalPlayUrl(songItem)

        SynoLog.d(TAG, "url = $url")

        if (songItem.isRadio) {
            cachePath = url
            status = DownloadStatus.FILE_DONE
        } else {
            if (url.startsWith(HTTP_PREFIX)) {
                download(url, songItem.filePath)
            } else {
                cachePath = url
                status = DownloadStatus.FILE_DONE
            }
        }

        listener?.onDownloadCompleted(songItem)
    }

    fun getCachePath(): String {
        return cachePath
    }

    @Throws(Throwable::class)
    private fun download(url: String, filePath: String): Boolean {
        val ext = Utilities.getExt(url)

        var name = File(filePath).name
        val str = name

        if (str.lastIndexOf(".") > 0) {
            name = name.take(str.lastIndexOf("."))
        }

        val file = File(
            context.cacheDir,
            "${songItem.uniqueKey}.$ext"
        )

        this.tempFile = file

        val path = file.path
        this.cachePath = path

        val file2 = this.tempFile ?: throw UninitializedPropertyAccessException("tempFile")

        val fileOutputStream = FileOutputStream(file2)

        while (!isIdle()) {
            val file3 = this.tempFile ?: throw UninitializedPropertyAccessException("tempFile")

            val length = file3.length()

            if (NetworkUtils.isNetworkConnected(context)) {
                download(url, fileOutputStream, length)
            }

            if (isCompleted() || isError()) {
                break
            }

            try {
                Thread.sleep(3000L)
            } catch (_: InterruptedException) {
                status = DownloadStatus.Error
            }
        }

        IOUtils.closeSilently(fileOutputStream)

        if (!Utilities.checkPathAvailable(cachePath)) {
            status = DownloadStatus.Error
        }

        if (!isCompleted()) {
            status = DownloadStatus.Error

            if (Utilities.removeFile(cachePath)) {
                cachePath = ""
            }

            return false
        }

        try {
            if (AudioPreference.enableAutoDownload()) {
                val utilities = Utilities()

                val path2 = File(
                    AudioPreference.getSongCacheFolder(),
                    "$name.$ext"
                ).path
                val properFile = utilities.getProperFile(path2)
                SynoLog.d("PreDownloader", "path2: $path2, properFile: ${properFile.path}")
                this.dstFile = properFile

                if (properFile == null) {
                    throw IllegalArgumentException("Required value was null.")
                }

                val file4 = this.tempFile ?: throw UninitializedPropertyAccessException("tempFile")

                val path3 = file4.path

                SAFUtils.copyFile(
                    SynoFile(path3, null),
                    properFile,
                    null
                )

                val songBitrate = Utilities.getSongBitrate(songItem, url)

                songItem.cachePath = properFile.path
                songItem.cacheBitrate = songBitrate

                val databaseAccesser = DatabaseAccesser.getInstance()
                val songItemQuerySong = databaseAccesser.querySong(songItem)

                if (songItemQuerySong == null) {
                    SynoLog.i(TAG, "Finish : ${properFile.path}")
                    databaseAccesser.addSong(songItem)
                } else {
                    if (songItemQuerySong.cachePath != properFile.path) {
                        SynoLog.i(TAG, " cache file exist, delete it at first")

                        SynoFile(cachePath, null).delete()
                    }

                    databaseAccesser.updateSong(songItem)
                }

                databaseAccesser.close()

                val downloadType = songItem.downloadType

                if (downloadType == 1) {
                    AudioPreference.addAutoCacheByte(properFile.length())
                } else if (downloadType == 2) {
                    AudioPreference.addManualCacheByte(properFile.length())
                }
            }

            Common.notifySongCacheCompleted(
                context,
                songItem.filePath
            )
        } catch (_: Exception) {
        }

        return true
    }

    private fun download(url: String, outputStream: OutputStream?, offset: Long): Boolean {
        val buffer = ByteArray(BUFFER_SIZE)
        var appendBytes = 0L
        var inputStream: InputStream? = null

        try {
            SynoLog.d(
                TAG,
                "download offset $offset for ${songItem.filePath}"
            )

            val builder = Request.Builder()
                .url(url)
                .get()

            if (offset > 0L) {
                builder.addHeader("Range", "bytes=$offset-")
            }

            val request = builder.build()

            call = ConnectionManager
                .getHttpClient()
                .newCall(request)

            response = call?.execute()

            if (response == null) {
                status = DownloadStatus.Error
                return false
            }

            status = DownloadStatus.HEADER_DONE

            if (response?.body == null) {
                SynoLog.e(TAG, "Response error.")
                status = DownloadStatus.Error
                return false
            }

            val contentLength = response?.body?.contentLength() ?: 0L

            if (contentLength <= 0L) {
                SynoLog.e(
                    TAG,
                    "Content Length error : $contentLength"
                )
                status = DownloadStatus.Error
                return false
            }

            SynoLog.i(TAG, "Content Length : $contentLength")

            inputStream = response?.body?.byteStream() ?: throw InputStreamNullException()

            while (!isIdle()) {
                writeBytes = inputStream.read(buffer, 0, BUFFER_SIZE)

                when (writeBytes) {
                    -1 -> {
                        status = DownloadStatus.FILE_DONE

                        SynoLog.i(
                            TAG,
                            "DownloadStatus.FILE_DONE"
                        )

                        SynoLog.i(
                            TAG,
                            "append $appendBytes, total ${offset + appendBytes}"
                        )
                        break
                    }
                    0 -> {
                        continue
                    }
                    else -> {
                        outputStream?.write(
                            buffer,
                            0,
                            writeBytes
                        )

                        appendBytes += writeBytes.toLong()
                    }
                }
            }
        } catch (e: Exception) {
            SynoLog.e(TAG, "download failed", e)

            SynoLog.e(
                TAG,
                "append $appendBytes, total ${offset + appendBytes}"
            )
        } finally {
            IOUtils.closeSilently(inputStream)
        }

        return isCompleted()
    }

    fun setError() {
        status = DownloadStatus.Error
    }

    private fun isIdle() =
        status == DownloadStatus.IDLE

    fun isPrepared() =
        status > DownloadStatus.PREPARING

    fun isCompleted() =
        status == DownloadStatus.FILE_DONE

    fun isDownloading() =
        status == DownloadStatus.HEADER_DONE

    fun isError() =
        status == DownloadStatus.Error
}