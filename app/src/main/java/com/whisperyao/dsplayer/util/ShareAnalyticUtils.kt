package com.whisperyao.dsplayer.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

object ShareAnalyticUtils {

    private const val PREF_KEY_ENABLE_SHARE_ANALYTICS = "enable_share_analytics"

    private fun getPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun isEnableShareAnalytics(context: Context): Boolean {
        return getPreferences(context)
            .getBoolean(PREF_KEY_ENABLE_SHARE_ANALYTICS, false)
    }
}