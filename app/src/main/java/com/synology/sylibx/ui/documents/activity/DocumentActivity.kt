package com.synology.sylibx.ui.documents.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.whisperyao.dsplayer.R
 import com.synology.sylibx.ui.documents.data.DocumentType
 import com.synology.sylibx.ui.documents.fragment.DocumentFragment

class DocumentActivity : AppCompatActivity() {

    companion object {

        const val KEY_TYPE = "type"

        const val KEY_ALGORITHMIC_DARKENING =
            "algorithmic_darkening"

        const val KEY_APPLY_PREFERS_COLOR_SCHEME =
            "apply_prefers_color_scheme"

        @JvmStatic
        @JvmOverloads
        fun generateHelpIntent(
            context: Context,
            algorithmicDarkening: Boolean = true,
            applyPrefersColorScheme: Boolean = true
        ): Intent {

            return Intent(
                context,
                DocumentActivity::class.java
            ).apply {
                // DocumentType.Help.name
                putExtra(
                    KEY_TYPE,
                    "help"
                )

                putExtra(
                    KEY_ALGORITHMIC_DARKENING,
                    algorithmicDarkening
                )

                putExtra(
                    KEY_APPLY_PREFERS_COLOR_SCHEME,
                    applyPrefersColorScheme
                )
            }
        }
    }

    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.ui_doc_activity_document)

        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val intent = intent

        val documentType = DocumentType.getType(
            intent?.getStringExtra(KEY_TYPE)
        )

        val enableAlgorithmicDarkening =
            intent?.getBooleanExtra(
                KEY_ALGORITHMIC_DARKENING,
                true
            ) ?: true

        val applyPrefersColorScheme =
            intent?.getBooleanExtra(
                KEY_APPLY_PREFERS_COLOR_SCHEME,
                true
            ) ?: true

        if (documentType.titleId != 0) {
            toolbar.setTitle(documentType.titleId)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.container,
                DocumentFragment.newInstance(
                    documentType.name,
                    enableAlgorithmicDarkening,
                    applyPrefersColorScheme
                )
            )
            .commit()
    }
}