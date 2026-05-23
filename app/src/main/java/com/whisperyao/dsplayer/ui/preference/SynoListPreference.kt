package com.whisperyao.dsplayer.ui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import com.whisperyao.dsplayer.R
import androidx.core.content.withStyledAttributes

class SynoListPreference(
    context: Context,
    attrs: AttributeSet?
) : ListPreference(context, attrs) {

    private var summaryFormat: String? = null

    init {
        context.withStyledAttributes(
            attrs,
            R.styleable.SynoMultiSelectListPreference,
            0,
            0
        ) {
            summaryFormat = getString(0)
        }
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
        super.onAttachedToHierarchy(preferenceManager)

        value?.let {
            updateSummary(it)
        }
    }

    override fun callChangeListener(newValue: Any?): Boolean {

        (newValue as? String)?.let {
            updateSummary(it)
        }

        return super.callChangeListener(newValue)
    }

    private fun updateSummary(value: String) {

        val selectedIndex = findIndexOfValue(value)

        if (selectedIndex >= 0) {
            summary = entries[selectedIndex]
        }
    }
}