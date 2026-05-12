package com.whisperyao.dsplayer.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.synology.sylib.ui3.fragment.IfTitleFragment
import dagger.android.support.AndroidSupportInjection

abstract class BasePreferenceFragment :
    PreferenceFragmentCompat(),
    IfTitleFragment {

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }
}