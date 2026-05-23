package com.whisperyao.dsplayer.ui.preference

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.net.toUri
import androidx.preference.Preference
import com.whisperyao.dsplayer.R

class PrivacyPreference @JvmOverloads constructor(
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
            context.getString(R.string.privacy_url).toUri()
        )

        title = context.getString(
            R.string.privacy_statement
        )
    }
}