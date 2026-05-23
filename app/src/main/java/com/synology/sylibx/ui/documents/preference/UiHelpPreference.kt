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
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.UiHelpPreference,
            0,
            0
        )

        val enableAlgorithmicDarkening =
            typedArray.getBoolean(
                R.styleable.UiHelpPreference_algorithmicDarkening,
                true
            )

        val applyPrefersColorScheme =
            typedArray.getBoolean(
                R.styleable.UiHelpPreference_applyPrefersColorScheme,
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