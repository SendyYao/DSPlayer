package com.whisperyao.dsplayer.util.extension

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.view.MenuItem;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.util.Utils;
import com.synology.sylibx.synofile.SynoFile;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import kotlin.Unit;
import kotlin.math.log
import kotlin.math.max
import kotlin.math.pow


object ExtensionsKt {
    fun Int.setFlags(flags: Int, enabled: Boolean): Int {
        return if (enabled) {
            this or flags
        } else {
            this and flags.inv()
        }
    }

    fun Boolean.toInt(): Int = if (this) 1 else 0

    fun Boolean.toVisibility(gone: Boolean = true): Int {
        return if (this) {
            0   // View.VISIBLE
        } else {
            if (gone) 8 else 4   // View.GONE : View.INVISIBLE
        }
    }

    fun PreferenceGroup.removePreference(preference: Preference?) {
        preference?.let { removePreference(it) }
    }

    fun ProgressDialog.safeDismiss() {
        if (isShowing) {
            dismiss()
        }
    }

    fun Context.extensionRegisterReceiver(
        receiver: BroadcastReceiver,
        intentFilter: IntentFilter,
        exported: Boolean = false
    ) {
        if (Utils.isSdk33()) {
            registerReceiver(
                receiver,
                intentFilter,
                if (exported) 2 else 4
            )
        } else {
            registerReceiver(receiver, intentFilter)
        }
    }

    fun Utilities.getProperFile(targetPath: String): SynoFile {
        var file = SynoFile(targetPath, null)

        if (!file.exists()) {
            SynoLog.d("getProperName", targetPath)
            return file
        }

        val dotIndex = targetPath.lastIndexOf(".")
        val name = targetPath.substring(0, dotIndex)
        val ext = targetPath.substring(dotIndex)

        var index = 0

        do {
            index++
            val newPath = "$name$index$ext"
            SynoLog.d("getProperName", newPath)
            file = SynoFile(newPath, null)
        } while (file.exists())

        return file
    }

    fun Boolean?.isTrue(): Boolean = this == true

    fun Boolean?.isFalse(): Boolean = this == false

    fun Long.toHumanReadableMagnitude(
        si: Boolean,
        minUnit: Int = 1
    ): String {
        val unit = if (si) 1000 else 1024

        if (this < unit) return toString()

        val exp = max(
            minUnit,
            (log(this.toDouble(), unit.toDouble())).toInt()
        )

        val prefix = if (si) "kMGTPE" else "KMGTPE"
        val suffix = prefix[exp - 1]

        return "%.1f %s".format(
            this / unit.toDouble().pow(exp),
            suffix
        )
    }

    fun Long.toMinSec(): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this)

        return "%02d:%02d".format(
            minutes,
            seconds - TimeUnit.MINUTES.toSeconds(minutes)
        )
    }

    fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") {
            it.replaceFirstChar { ch ->
                ch.uppercase()
            }
        }
    }

    fun ArrayList<String>.addIfNotEmpty(value: String?) {
        if (!value.isNullOrEmpty()) {
            add(value)
        }
    }

    fun Lock.withLockAction(action: () -> Unit) {
        lock()
        try {
            action()
        } finally {
            unlock()
        }
    }

    fun ReentrantLock.unlockIfLocked() {
        if (isLocked) {
            unlock()
        }
    }

    fun MenuItem.setVisibleStatus(visible: Boolean) {
        if (isVisible) {
            isVisible = visible
        }
    }

    fun MenuItem.setEnableStatus(enabled: Boolean) {
        if (isEnabled) {
            isEnabled = enabled
        }
    }
}

