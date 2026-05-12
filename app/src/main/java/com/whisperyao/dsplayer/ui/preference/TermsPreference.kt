package com.whisperyao.dsplayer.ui.preference

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference
import com.whisperyao.dsplayer.R

class TermsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        initialize()
    }

    private fun initialize() {

        intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                context.getString(R.string.terms_url)
            )
        )

        title = context.getString(
            R.string.lib_gdpr_eula_link
        )

        isVisible = false
    }
}