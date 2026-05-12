package com.whisperyao.dsplayer.ui.preference

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.util.Log
import androidx.preference.Preference
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.util.Utils
import com.synology.sylibx.applog.ui.util.EasterEggHandler

class VersionPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = VersionPreference::class.java.simpleName
    }

    interface EasterEggListener {
        fun onEasterEggActivated()
    }

    private var easterEggListener: EasterEggListener? = null

    private val easterEggHandler = EasterEggHandler(
        3,
        0L,
    ) { triggerState ->

        if (triggerState == 0) {
            easterEggListener?.onEasterEggActivated()
        }
    }

    init {
        initialize()
    }

    private fun initialize() {

        title = context.getString(R.string.version)

        summary = getVersionName(context)

        easterEggHandler.bind(this)
    }

    private fun getVersionName(context: Context): String {

        return try {

            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                0
            )

            val versionCode = getVersionCode(packageInfo)

            String.format(
                "%s-%03d",
                packageInfo.versionName,
                versionCode
            )

        } catch (e: PackageManager.NameNotFoundException) {

            Log.e(
                TAG,
                "Fetch Package information failed"
            )

            ""
        }
    }

    private fun getVersionCode(packageInfo: PackageInfo): Int {

        return if (Utils.isSdk28()) {
            packageInfo.longVersionCode.toInt()
        } else {
            packageInfo.versionCode
        }
    }

    fun setEasterEggListener(listener: EasterEggListener) {
        easterEggListener = listener
    }

    fun setEasterEggListener(action: () -> Unit) {

        easterEggListener = object : EasterEggListener {

            override fun onEasterEggActivated() {
                action.invoke()
            }
        }
    }
}
