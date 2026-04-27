package com.whisperyao.dsplayer.datasource.network

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext
import com.whisperyao.dsplayer.util.Utils


class PreferenceManager {
    companion object {
        const val HAS_SHOWN_NOTIFICATION_PERMISSION_REQUEST: String = "has_shown_notification_permission_request"
        const val PREF_KEY_VERIFY_CERTIFICATE: String = "verify_certificate"
    }

    private val context: Context
    private var pref: SharedPreferences

    constructor(@ApplicationContext context: Context) {
        this.context = context
        this.pref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);

    }

    fun isVerifyCertification(): Boolean {
        return this.pref.getBoolean(PREF_KEY_VERIFY_CERTIFICATE, false)
    }

    fun setVerifyCertification(z: Boolean) {
        this.pref.edit().putBoolean(PREF_KEY_VERIFY_CERTIFICATE, z).apply()
    }

    fun getHasShownNotificationPermissionRequest(): Boolean {
        var systemService: Any? = this.context.getSystemService("notification")
        var notificationManager: NotificationManager? =
            if (systemService is NotificationManager) systemService as NotificationManager? else null;
        if (notificationManager == null) {
            return !Utils.isSdk33() || this.pref.getBoolean(HAS_SHOWN_NOTIFICATION_PERMISSION_REQUEST, false)
        }
        return false
    }

    fun setHasShownNotificationPermissionRequest(z: Boolean) {
        this.pref.edit().putBoolean(HAS_SHOWN_NOTIFICATION_PERMISSION_REQUEST, z).apply()
    }

}