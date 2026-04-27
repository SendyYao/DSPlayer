package com.whisperyao.dsplayer.util

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.jvm.JvmStatic;


object CrashlyticsUtil {

    private val mCrashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    class ExceptionNonFatal : Exception()

    class TestCrashException(message: String) : RuntimeException(message)

    fun init(context: Context) {
        mCrashlytics.isCrashlyticsCollectionEnabled = ShareAnalyticUtils.isEnableShareAnalytics(context)
    }

    fun logMessage(msg: String) {
        mCrashlytics.log(msg)
    }

    @JvmStatic
    fun logException(
        tag: String,
        msg: String,
        tr: Throwable?
    ) {
        mCrashlytics.setCustomKey("tag", tag)
        mCrashlytics.setCustomKey("message", msg)

        val throwable = tr ?: ExceptionNonFatal().also {
            adjustStackTrace(it)
        }

        if (tr != null) {
            mCrashlytics.setCustomKey(
                "throwable",
                tr.toString()
            )
        }

        mCrashlytics.recordException(throwable)
    }

    fun enableCrashlyticsDataCollection(isEnabled: Boolean) {
        if (isEnabled) {
            mCrashlytics.deleteUnsentReports()
        }

        mCrashlytics.isCrashlyticsCollectionEnabled = isEnabled
    }

    fun crash() {
        throw TestCrashException("Crash test.")
    }

    private fun adjustStackTrace(exception: Exception) {
        val stacktrace = exception.stackTrace

        for (i in stacktrace.indices) {
            if (stacktrace[i].className !=
                CrashlyticsUtil::class.java.name
            ) {
                exception.stackTrace =
                    stacktrace.drop(i).toTypedArray()
                return
            }
        }
    }
}