package com.whisperyao.dsplayer.ui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceManager
import com.whisperyao.dsplayer.R
import androidx.core.content.withStyledAttributes

class SynoMultiSelectListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MultiSelectListPreference(context, attrs) {

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

    override fun onAttachedToHierarchy(
        preferenceManager: PreferenceManager
    ) {

        super.onAttachedToHierarchy(preferenceManager)

        updateSummary(values)
    }

    override fun callChangeListener(newValue: Any?): Boolean {

        updateSummary(newValue)

        return super.callChangeListener(newValue)
    }

    private fun updateSummary(value: Any?) {

        val format = summaryFormat ?: return

        val selectedValues =
            value as? Set<*>

        val selectedCount =
            selectedValues?.size ?: 0

        summary = String.format(
            format,
            selectedCount
        )
    }
}