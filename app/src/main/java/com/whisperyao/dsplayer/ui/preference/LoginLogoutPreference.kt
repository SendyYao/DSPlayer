package com.whisperyao.dsplayer.ui.preference

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.TwoStatePreference
import com.whisperyao.dsplayer.R

class LoginLogoutPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TwoStatePreference(context, attrs) {

    interface Callback {

        fun login()

        fun logout()
    }

    var callback: Callback? = null

    init {

        summaryOn =
            context.getString(R.string.login_title)

        summaryOff =
            context.getString(R.string.logout_title)

        onPreferenceClickListener =
            OnPreferenceClickListener {

                if (isChecked) {

                    callback?.login()

                } else {

                    callback?.logout()
                }

                true
            }
    }

    override fun onClick() {
        // handled by onPreferenceClickListener
    }

    override fun onBindViewHolder(
        holder: PreferenceViewHolder
    ) {

        super.onBindViewHolder(holder)

        val summaryTextView =
            holder.findViewById(android.R.id.summary) as? TextView

        if (isChecked) {

            val loginText = summaryOn

            if (!loginText.isNullOrEmpty()) {

                summaryTextView?.text = loginText

                summaryTextView?.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.blue
                    )
                )
            }

        } else {

            val logoutText = summaryOff

            if (!logoutText.isNullOrEmpty()) {

                summaryTextView?.text = logoutText

                summaryTextView?.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.logout_text_color
                    )
                )
            }
        }

        summaryTextView?.layoutParams?.let { params ->

            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            summaryTextView.layoutParams = params
        }

        summaryTextView?.apply {

            gravity = Gravity.CENTER

            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                18f
            )

            visibility = View.VISIBLE
        }
    }
}