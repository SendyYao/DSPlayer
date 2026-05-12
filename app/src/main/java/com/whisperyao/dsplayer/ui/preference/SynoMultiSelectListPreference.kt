package com.whisperyao.dsplayer.ui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceManager
import com.whisperyao.dsplayer.R

class SynoMultiSelectListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MultiSelectListPreference(context, attrs) {

    private val summaryFormat: String?

    init {

        // R.styleable.SynoMultiSelectListPreference
        val typedArray = context.obtainStyledAttributes(
            attrs,
            intArrayOf(R.attr.summaryRes),
            0,
            0
        )

        summaryFormat =
            typedArray.getString(0)

        typedArray.recycle()
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