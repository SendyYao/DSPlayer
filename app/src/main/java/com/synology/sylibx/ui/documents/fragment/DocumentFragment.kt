package com.synology.sylibx.ui.documents.fragment

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.synology.sylibx.ui.documents.activity.DocumentActivity
import com.synology.sylibx.ui.documents.data.DocumentType
// import com.synology.sylibx.ui.documents.util.UrlUtil
// import com.synology.sylibx.ui.documents.util.WebViewUtil
import com.whisperyao.dsplayer.R
import java.util.Locale

class DocumentFragment : Fragment() {

    companion object {

        private const val SAVED_INSTANCE_WEBVIEW_STATE =
            "webview_state"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            type: String?,
            algorithmicDarkening: Boolean = true,
            applyPrefersColorScheme: Boolean = true
        ): DocumentFragment {

            return DocumentFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        DocumentActivity.KEY_TYPE,
                        type
                    )

                    putBoolean(
                        DocumentActivity.KEY_ALGORITHMIC_DARKENING,
                        algorithmicDarkening
                    )

                    putBoolean(
                        DocumentActivity.KEY_APPLY_PREFERS_COLOR_SCHEME,
                        applyPrefersColorScheme
                    )
                }
            }
        }
    }

    private var loadingView: View? = null

    private var webView: WebView? = null

    private var documentType = DocumentType.Help

    private var enableAlgorithmicDarkening = true

    private var applyPrefersColorScheme = true

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        arguments?.let { args ->

            documentType = DocumentType.getType(
                args.getString(DocumentActivity.KEY_TYPE)
            )

            enableAlgorithmicDarkening =
                args.getBoolean(
                    DocumentActivity.KEY_ALGORITHMIC_DARKENING,
                    true
                )

            applyPrefersColorScheme =
                args.getBoolean(
                    DocumentActivity.KEY_APPLY_PREFERS_COLOR_SCHEME,
                    true
                )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.ui_doc_fragment_document,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        loadingView =
            view.findViewById(R.id.progress_container)

        val currentWebView =
            view.findViewById<WebView>(R.id.webView)

        webView = currentWebView

        configureWebView(
            currentWebView,
            savedInstanceState
        )
    }

    fun getTitleResId(): Int {
        return documentType.titleId
    }

    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)

        val webViewState = Bundle()

        webView?.saveState(webViewState)

        outState.putBundle(
            SAVED_INSTANCE_WEBVIEW_STATE,
            webViewState
        )
    }

    private fun configureWebView(
        webView: WebView,
        savedState: Bundle?
    ): WebView {

        val context = webView.context

        val assetLoader =
            WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/assets/",
                    WebViewAssetLoader.AssetsPathHandler(context)
                )
                .build()

        webView.webViewClient = LocalContentWebViewClient(assetLoader)

        webView.settings.apply {

            allowFileAccess = false

            javaScriptEnabled = true
        }

        webView.isFocusable = true

        if (savedState != null) {

            savedState
                .getBundle(SAVED_INSTANCE_WEBVIEW_STATE)
                ?.let {
                    webView.restoreState(it)
                }

        } else {

            webView.loadUrl(
                getLocalUrl(
                    context,
                    documentType
                )
            )
        }

        // WebViewUtil.setAdaptiveDarkModeIfPossible(webView, enableAlgorithmicDarkening, applyPrefersColorScheme)

        return webView
    }

    private fun getLocalUrl(
        context: Context,
        type: DocumentType
    ): String {

        // ISOUtils.getLocaleString(context)
        val locale = Locale("en").toString()

        val localizedUrl = type.getLocalUrlByLocale(locale)

        return if (
            DocumentType.checkUrlExist(
                context,
                localizedUrl
            )
        ) {

            localizedUrl

        } else {

            type.getLocalUrlByLocale(
                Locale.ENGLISH.toString()
            )
        }
    }

    private inner class LocalContentWebViewClient(
        private val assetLoader: WebViewAssetLoader
    ) : WebViewClientCompat() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {

            return assetLoader.shouldInterceptRequest(
                request.url
            )
        }

        override fun shouldOverrideUrlLoading(
            webView: WebView,
            request: WebResourceRequest
        ): Boolean {

            return try {
                // TODO init intent
                val context = webView.context

                // val intent = UrlUtil.getWebPageIntent(UrlUtil.INSTANCE, context, request.url.toString(), false, 4, null)
                val intent = null
                if (intent != null) {

                    // context.startActivity(intent)

                    true

                } else {

                    false
                }

            } catch (_: Throwable) {

                false
            }
        }

        override fun onPageStarted(
            view: WebView,
            url: String,
            favicon: Bitmap?
        ) {

            super.onPageStarted(
                view,
                url,
                favicon
            )

            webView?.visibility = View.INVISIBLE

            loadingView?.visibility = View.VISIBLE
        }

        override fun onPageFinished(
            view: WebView,
            url: String
        ) {

            webView?.visibility = View.VISIBLE

            loadingView?.visibility = View.INVISIBLE

            super.onPageFinished(view, url)
        }
    }
}