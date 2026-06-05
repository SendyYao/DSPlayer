package com.whisperyao.dsplayer.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Point
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.URLSpan
import android.util.Size
import android.view.WindowManager
import com.synology.sylibx.synofile.Extensions.registerReceiverCompat

object Utils {

    private fun getWindowManager(
        context: Context
    ): WindowManager? {
        return context.getSystemService(
            Context.WINDOW_SERVICE
        ) as? WindowManager
    }

    fun getWindowSize(context: Context): Size {
        val windowManager = getWindowManager(context)
            ?: return Size(0, 0)

        return if (isSdk30()) {
            val bounds =
                windowManager.currentWindowMetrics.bounds

            Size(bounds.width(), bounds.height())
        } else {
            val point = Point()
            windowManager.defaultDisplay.getSize(point)
            Size(point.x, point.y)
        }
    }

    @JvmStatic
    fun getWindowWidth(context: Context): Int {
        return getWindowSize(context).width
    }

    private fun isSdkVersionAfter(
        versionCode: Int
    ): Boolean {
        return Build.VERSION.SDK_INT >= versionCode
    }

    @JvmStatic
    fun isSdk33() = isSdkVersionAfter(33)

    @JvmStatic
    fun isSdk31() = isSdkVersionAfter(31)

    @JvmStatic
    fun isSdk30() = isSdkVersionAfter(30)

    @JvmStatic
    fun isSdk29() = isSdkVersionAfter(29)

    @JvmStatic
    fun isSdk28() = isSdkVersionAfter(28)

    @JvmStatic
    fun isSdk27() = isSdkVersionAfter(27)

    @JvmStatic
    @JvmOverloads
    fun registerReceiver(
        context: Context,
        receiver: BroadcastReceiver,
        intentFilter: IntentFilter,
        export: Boolean = false
    ) {
        context.registerReceiverCompat(
            receiver,
            intentFilter,
            export
        )
    }

    fun getUrlSpan(
        message: String,
        urlLabel: String,
        url: String
    ): CharSequence {
        val urlSpan = URLSpan(url)

        var start = message.indexOf(urlLabel)

        val text =
            if (start < 0) {
                start = message.length + 1
                "$message $urlLabel"
            } else {
                message
            }

        val end = start + urlLabel.length

        return SpannableStringBuilder(text).apply {
            setSpan(
                urlSpan,
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}