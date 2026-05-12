package com.synology.sylibx.applog.ui.util

import android.view.View
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class EasterEggHandler @JvmOverloads constructor(
    private val triggerCount: Int,
    private val timeout: Long = DEFAULT_TIMEOUT,
    private val callback: ((remainingCount: Int) -> Unit)? = null
) {

    companion object {
        private const val DEFAULT_TIMEOUT = 2000L
    }

    interface IEasterEggCallback {
        fun onClicked(remainingCount: Int)
    }

    private val lock = ReentrantReadWriteLock()

    private var bindTarget: Any? = null

    private var clickCount = 0

    private var timer: Timer? = null

    constructor(
        triggerCount: Int,
        timeout: Long = DEFAULT_TIMEOUT,
        callback: IEasterEggCallback?
    ) : this(
        triggerCount,
        timeout,
        callback = { remain ->
            callback?.onClicked(remain)
        }
    )

    fun bind(preference: android.preference.Preference) {

        preference.setOnPreferenceClickListener {
            onClicked()
        }

        bindTarget = preference
    }

    fun bind(preference: androidx.preference.Preference) {

        preference.setOnPreferenceClickListener {
            onClicked()
        }

        bindTarget = preference
    }

    fun bind(view: View) {

        view.setOnClickListener {
            onClicked()
        }

        bindTarget = view
    }

    fun unbind() {

        when (val target = bindTarget) {

            is android.preference.Preference -> {
                target.onPreferenceClickListener = null
            }

            is androidx.preference.Preference -> {
                target.onPreferenceClickListener = null
            }

            is View -> {
                target.setOnClickListener(null)
            }
        }

        bindTarget = null

        timer?.cancel()
        timer = null

        lock.write {
            clickCount = 0
        }
    }

    private fun onClicked(): Boolean {

        timer?.cancel()

        val currentCount = lock.read {
            clickCount
        }

        val remainingCount: Int

        if (currentCount <= triggerCount) {

            lock.write {
                clickCount++
            }

            timer = Timer().apply {
                schedule(
                    TimeoutTask(),
                    timeout
                )
            }

            remainingCount =
                triggerCount - (currentCount + 1)

        } else {

            remainingCount = -1
        }

        callback?.invoke(remainingCount)

        return true
    }

    private inner class TimeoutTask : TimerTask() {

        override fun run() {

            lock.write {
                clickCount = 0
            }
        }
    }
}
