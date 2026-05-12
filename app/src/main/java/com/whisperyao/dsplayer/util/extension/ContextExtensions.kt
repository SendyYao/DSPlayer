package com.whisperyao.dsplayer.util.extension

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.ui.settings.DisplayPreferenceActivity

fun Fragment.openSettings(typeResId: Int) {
    activity?.openSettings(typeResId)
}

fun Context.openSettings(typeResId: Int) {
    val intent = Intent(this, DisplayPreferenceActivity::class.java).apply {
        putExtra(
            getString(R.string.fragment_type),
            getString(typeResId)
        )
    }

    startActivity(intent)
}
