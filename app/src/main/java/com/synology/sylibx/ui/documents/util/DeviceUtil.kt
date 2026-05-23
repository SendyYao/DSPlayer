package com.synology.sylibx.ui.documents.util

import android.os.Build
import org.json.JSONException
import org.json.JSONObject

object DeviceUtil {

    private const val KEY_MANUFACTURER = "MANUFACTURER"
    private const val KEY_MODEL = "MODEL"
    private const val KEY_RELEASE = "RELEASE"
    private const val KEY_SDK_INT = "SDK_INT"

    @JvmStatic
    @Throws(JSONException::class)
    fun getDeviceInfo(): JSONObject {
        val jsonObject = JSONObject()

        try {
            jsonObject.put(KEY_RELEASE, Build.VERSION.RELEASE)
            jsonObject.put(KEY_SDK_INT, Build.VERSION.SDK_INT)
            jsonObject.put(KEY_MANUFACTURER, Build.MANUFACTURER)
            jsonObject.put(KEY_MODEL, Build.MODEL)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return jsonObject
    }
}