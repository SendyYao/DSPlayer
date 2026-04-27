package com.whisperyao.dsplayer.util

import android.content.Context
import com.whisperyao.dsplayer.Common

object SessionManager {

    private const val PREF_NAME = "app_session"
    private const val KEY_SID = "sid"

    fun saveSid(context: Context, sid: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SID, sid)
            .apply()
    }

    @JvmStatic
    fun getSid(context: Context?): String {
        return Common.getSID()
//        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//            .getString(KEY_SID, null)
    }
}