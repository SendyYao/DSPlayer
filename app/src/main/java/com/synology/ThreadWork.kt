package com.synology

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.net.WebAPIErrorException

abstract class ThreadWork {

    protected var exception: WebAPIErrorException? = null

    private val msgHandler = MsgHandler(this)

    private var forceEnd = false

    var isWorking = false
        private set

    private var workThread: Thread? = null

    private var tag: Any? = null

    open fun onComplete() {}

    abstract fun onWorking()

    open fun postWork() {}

    open fun preWork() {}

    fun setTag(tag: Any?) {
        this.tag = tag
    }

    fun startWork() {
        preWork()
        isWorking = true

        workThread = Thread {
            onWorking()

            msgHandler.sendEmptyMessage(WORK_COMPLETED)
        }.also {
            it.start()
        }
    }

    fun endThread() {
        forceEnd = true

        tag?.let {
            Thread {
                ConnectionManager
                    .getHttpClient()
                    .cancel(it)
            }.start()
        }

        workThread?.interrupt()
    }

    private class MsgHandler(
        private val threadWork: ThreadWork
    ) : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            if (msg.what == WORK_COMPLETED) {
                threadWork.isWorking = false
                threadWork.postWork()

                if (!threadWork.forceEnd) {
                    threadWork.onComplete()
                }
            }
        }
    }

    companion object {
        private const val WORK_COMPLETED = 1
    }
}