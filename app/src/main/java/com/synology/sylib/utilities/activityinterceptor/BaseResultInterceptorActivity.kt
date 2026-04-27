package com.synology.sylib.utilities.activityinterceptor

import android.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

abstract class BaseResultInterceptorActivity : Activity() {

    companion object {
        const val ARG_ARGUMENTS = "ADDITIONAL_ARGUMENTS"
        const val ARG_INTENT = "ORIGINAL_INTENT"
        const val ARG_OPTIONS = "ORIGINAL_REQUEST_OPTION"
        const val ARG_REQUEST_CODE = "ORIGINAL_REQUEST_CODE"
    }

    protected var mArguments: Bundle? = null

    private lateinit var originalIntent: Intent
    private var originalOptions: Bundle? = null
    private var originalRequestCode: Int = -1

    protected open fun getRequestCodeForStartActivity(): Int {
        return getRawRequestCode()
    }

    protected fun getRawIntent(): Intent {
        check(::originalIntent.isInitialized) {
            "Calling getRawIntent() too early, you can only call this function after super.onCreate()"
        }
        return originalIntent
    }

    protected fun getRawRequestCode(): Int {
        check(::originalIntent.isInitialized) {
            "Calling getRawRequestCode() too early, you can only call this function after super.onCreate()"
        }
        return originalRequestCode
    }

    protected fun getOriginalOptions(): Bundle? {
        return originalOptions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Translucent_NoTitleBar)
        super.onCreate(savedInstanceState)

        val state = savedInstanceState ?: intent.extras
        extractInfo(state)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        onStartIntercept()
    }

    open fun onStartIntercept() {
        startActivityForResult(
            originalIntent,
            originalRequestCode,
            originalOptions
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(ARG_INTENT, originalIntent)
        outState.putInt(ARG_REQUEST_CODE, originalRequestCode)
        outState.putBundle(ARG_OPTIONS, originalOptions)
        outState.putBundle(ARG_ARGUMENTS, mArguments)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            if (requestCode == getRequestCodeForStartActivity()) {
                val handled = onActivityResultCallback(
                    requestCode,
                    resultCode,
                    data
                )

                if (!handled) {
                    setResult(resultCode, data)
                }
            }
        } finally {
            finish()
        }
    }

    protected abstract fun onActivityResultCallback(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean

    private fun extractInfo(bundle: Bundle?) {
        val intent = bundle?.getParcelable<Intent>(ARG_INTENT)
        val requestCode = bundle?.getInt(ARG_REQUEST_CODE)

        originalOptions = bundle?.getBundle(ARG_OPTIONS)
        mArguments = bundle?.getBundle(ARG_ARGUMENTS)

        requireNotNull(intent) {
            "No original intent is found : ${javaClass.simpleName}.onCreate()"
        }

        requireNotNull(requestCode) {
            "No original request code is found : ${javaClass.simpleName}.onCreate()"
        }

        originalIntent = intent
        originalRequestCode = requestCode
    }
}