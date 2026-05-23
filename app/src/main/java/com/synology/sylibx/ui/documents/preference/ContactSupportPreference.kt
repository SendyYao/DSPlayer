package com.synology.sylibx.ui.documents.preference

import android.content.ActivityNotFoundException
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import com.synology.sylibx.ui.documents.util.ContactSupportUtil
import com.whisperyao.dsplayer.R

class ContactSupportPreference(
    context: Context,
    attributeSet: AttributeSet?
) : ListPreference(context, attributeSet) {

    init {
        title = context.getString(R.string.ui_doc_contact_support)
        negativeButtonText = ""
    }

    override fun onClick() {
        val ctx = context

        AlertDialog.Builder(ctx, R.style.UiDoc_ContactSupportDialogTheme)
            .setTitle(R.string.ui_doc_contact_support)
            .setMessage(R.string.ui_doc_contact_support_title)
            .setPositiveButton(R.string.ui_doc_location_global) { _, _ ->
                startSupportActivity(
                    ctx,
                    ContactSupportUtil.Region.GLOBAL
                )
            }
            .setNegativeButton(R.string.ui_doc_location_china) { _, _ ->
                startSupportActivity(
                    ctx,
                    ContactSupportUtil.Region.CHINA
                )
            }
            .show()
    }

    private fun startSupportActivity(
        context: Context,
        region: ContactSupportUtil.Region
    ) {
        try {
            context.startActivity(
                ContactSupportUtil.getContactSupportIntent(
                    context,
                    region
                )
            )
        } catch (_: ActivityNotFoundException) {
        }
    }
}