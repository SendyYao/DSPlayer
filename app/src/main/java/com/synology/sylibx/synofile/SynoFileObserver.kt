package com.synology.sylibx.synofile


import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import androidx.annotation.RequiresApi

abstract class SynoFileObserver @JvmOverloads constructor(
    file: SynoFile,
    private val flag: Int = 4095
) : ObjectProvider.ObserverEvent {

    companion object {
        const val EVENT_MEDIA_CHANGE = 0
    }

    private val mContext =
        ObjectProvider.provideContext()

    @RequiresApi(Build.VERSION_CODES.Q)
    private val targetUri =
        SAFUtils.getMediaUri(
            mContext,
            file
        )

    private val isScopedStorage =
        SAFUtils.isScopedStoragePath(file.path)

    private val mFileObserver by lazy {
        ObjectProvider.provideFileObserver(
            file,
            flag,
            this
        )
    }

    private val mContentObserver =
        object : ContentObserver(null) {

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onChange(
                selfChange: Boolean,
                uri: Uri?
            ) {

                super.onChange(selfChange)

                if (uri == null) return

                if (targetUri == null) return

                if (uri.path != targetUri.path) return

                onFileChange(
                    EVENT_MEDIA_CHANGE,
                    uri.toString()
                )
            }
        }

    abstract fun onFileChange(
        event: Int,
        path: String?
    )

    fun getContentObserver(): ContentObserver =
        mContentObserver

    fun getFileObserver(): FileObserver =
        mFileObserver

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startWatching() {

        if (!isScopedStorage) {
            mFileObserver.startWatching()
            return
        }

        targetUri?.let {
            mContext.contentResolver.registerContentObserver(
                it,
                false,
                mContentObserver
            )
        }
    }

    fun stopWatching() {

        if (!isScopedStorage) {
            mFileObserver.stopWatching()
        } else {
            mContext.contentResolver.unregisterContentObserver(
                mContentObserver
            )
        }
    }

    override fun onEvent(
        event: Int,
        path: String?
    ) {
        onFileChange(event, path)
    }
}