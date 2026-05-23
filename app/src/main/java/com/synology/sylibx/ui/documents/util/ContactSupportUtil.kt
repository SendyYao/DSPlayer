package com.synology.sylibx.ui.documents.util

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.synology.sylib.util.PackageUtils
import androidx.core.net.toUri
import com.synology.sylib.util.ISOUtils

object ContactSupportUtil {

    private const val ACCOUNT_PROTOCOL_VERSION = "1"

    private const val KEY_ACCOUNT_PROTOCOL_VERSION = "account_protocol_ver"
    private const val KEY_APP_NAME = "app_name"
    private const val KEY_APP_VERSION = "app_version"
    private const val KEY_DEV_INFO = "dev_info"
    private const val KEY_LANG = "lang"
    private const val KEY_PLATFORM = "platform"
    private const val KEY_PROTOCOL_VERSION = "protocol_ver"

    private const val PLATFORM_ANDROID = "Android"

    private const val PROTOCOL_VERSION = "1"

    private const val SUPPORT_URL_CHINA =
        "https://account.synology.cn/support"

    private const val SUPPORT_URL_GLOBAL =
        "https://account.synology.com/support"

    enum class Region(
        val url: String
    ) {
        CHINA(SUPPORT_URL_CHINA),
        GLOBAL(SUPPORT_URL_GLOBAL)
    }

    @JvmStatic
    fun composeURL(
        context: Context,
        region: Region
    ): String {

        var appName = PackageUtils.getSynoAppName(context)

        if (TextUtils.isEmpty(appName)) {
            appName = PackageUtils.getPackageLastName(context)
        }

        return region.url.toUri()
            .buildUpon()
            .appendQueryParameter(
                KEY_PROTOCOL_VERSION,
                PROTOCOL_VERSION
            )
            .appendQueryParameter(
                KEY_LANG,
                ISOUtils.getLanguageString(context)
            )
            .appendQueryParameter(
                KEY_APP_NAME,
                appName
            )
            .appendQueryParameter(
                KEY_PLATFORM,
                PLATFORM_ANDROID
            )
            .appendQueryParameter(
                KEY_APP_VERSION,
                PackageUtils.getVersionName(context)
            )
            .appendQueryParameter(
                KEY_ACCOUNT_PROTOCOL_VERSION,
                ACCOUNT_PROTOCOL_VERSION
            )
            .appendQueryParameter(
                KEY_DEV_INFO,
                DeviceUtil.getDeviceInfo().toString()
            )
            .build()
            .toString()
    }

    @JvmStatic
    fun getContactSupportIntent(
        context: Context,
        region: Region
    ): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = composeURL(context, region).toUri()
        }
    }
}