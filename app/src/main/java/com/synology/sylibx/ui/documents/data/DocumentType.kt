package com.synology.sylibx.ui.documents.data

import android.content.Context
import com.whisperyao.dsplayer.R
import java.io.IOException

enum class DocumentType(
    val titleId: Int,
    private val urlTemplate: String
) {

    Unknown(
        0,
        "https://appassets.androidplatform.net/assets/"
    ),

    Help(
        R.string.str_help,
        "https://appassets.androidplatform.net/assets/data/[LOCALE]/help.html"
    );

    companion object {

        private const val LOCALE_PLACEHOLDER =
            "[LOCALE]"

        fun getType(typeName: String?): DocumentType {

            return when {
                Help.name.equals(typeName, ignoreCase = true) -> Help
                else -> Unknown
            }
        }

        @Throws(IOException::class)
        fun checkUrlExist(
            context: Context,
            path: String
        ): Boolean {

            val assetPath = path.substring(
                Unknown.urlTemplate.length
            )

            return try {

                context.assets.open(assetPath).close()

                true

            } catch (_: IOException) {

                false
            }
        }
    }

    fun getLocalUrlByLocale(locale: String): String {

        return urlTemplate.replace(
            LOCALE_PLACEHOLDER,
            locale
        )
    }
}