package com.whisperyao.dsplayer.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.Map


object PermissionUtil {

    private const val LOG_TAG = "PermissionUtil"

    @JvmStatic
    fun requestPermission(
        fragment: Fragment,
        permissions: Array<String>,
        requestCode: RequestCode
    ) {
        val context = fragment.context ?: return

        if (hasPermission(context, permissions)) {
            fragment.onRequestPermissionsResult(
                requestCode.value,
                permissions,
                intArrayOf(PackageManager.PERMISSION_GRANTED)
            )
        } else {
            fragment.requestPermissions(permissions, requestCode.value)
        }
    }

    fun areAllGranted(grantResults: IntArray?): Boolean {
        if (grantResults == null || grantResults.isEmpty()) return false
        return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    fun hasPermission(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun areAllGranted(permissions: Map<String, Boolean>): Boolean {
        return permissions.values().all { it }
    }

    @JvmStatic
    fun getNotificationPermissionList(): Array<String> {
        return if (Utils.isSdk33()) {
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    enum class RequestCode(val value: Int) {
        NOTIFICATION_PERMISSION(0)
    }
}