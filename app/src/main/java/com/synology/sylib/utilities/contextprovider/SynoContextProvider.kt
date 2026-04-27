package com.synology.sylib.utilities.contextprovider

import android.content.Context
import com.whisperyao.dsplayer.App
import java.util.concurrent.locks.ReentrantReadWriteLock

object SynoContextProvider {

    private val lock = ReentrantReadWriteLock()
    private var context: Context? = App.getContext()

    fun setContext(context: Context) {
        val readLock = lock.readLock()
        val writeLock = lock.writeLock()

        // 释放读锁（如果有）
        val readCount = if (lock.writeHoldCount == 0) lock.readHoldCount else 0
        repeat(readCount) { readLock.unlock() }

        writeLock.lock()
        try {
            this.context = context
        } finally {
            repeat(readCount) { readLock.lock() }
            writeLock.unlock()
        }
    }

    @JvmStatic
    fun getOrNull(): Context? {
        val readLock = lock.readLock()
        readLock.lock()

        try {
            val ctx = context?.applicationContext ?: context

            if (ctx != null) {
                val writeLock = lock.writeLock()
                val readCount = if (lock.writeHoldCount == 0) lock.readHoldCount else 0

                // 升级锁（读 → 写）
                repeat(readCount) { readLock.unlock() }

                writeLock.lock()
                try {
                    context = ctx
                } finally {
                    repeat(readCount) { readLock.lock() }
                    writeLock.unlock()
                }
            }

            return ctx
        } finally {
            readLock.unlock()
        }
    }

    @JvmStatic
    fun get(): Context {
        return getOrNull() ?: throw IllegalStateException("SynoContextProvider is NULL")
    }
}