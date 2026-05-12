package com.synology.sylibx.ui.documents.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.synology.sylibx.ui.documents.activity.DocumentActivity
import com.whisperyao.dsplayer.R

class UiHelpPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    init {
        // R.styleable.UiHelpPreference
        val typedArray = context.obtainStyledAttributes(
            attrs,
            intArrayOf(R.attr.algorithmicDarkening, R.attr.applyPrefersColorScheme),
            0,
            0
        )

        // R.styleable.UiHelpPreference_algorithmicDarkening
        val enableAlgorithmicDarkening =
            typedArray.getBoolean(
                0,
                true
            )

        // R.styleable.UiHelpPreference_applyPrefersColorScheme
        val applyPrefersColorScheme =
            typedArray.getBoolean(
                1,
                true
            )

        typedArray.recycle()

        intent = DocumentActivity.generateHelpIntent(
            context,
            enableAlgorithmicDarkening,
            applyPrefersColorScheme
        )

        title = context.getString(R.string.str_help)
    }
}