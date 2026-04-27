package com.synology.sylib.utilities.activityinterceptor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

object ActivityInterceptor {

    fun <T : BaseResultInterceptorActivity> activityForResult(
        cls: Class<T>,
        activity: Activity,
        intent: Intent,
        i: Int,
        bundle: Bundle? = null,
        bundle2: Bundle? = null
    ) {
        activity.startActivityForResult(
            activityForResultIntent(
                cls,
                activity,
                intent,
                i,
                bundle,
                bundle2
            ),
            i,
            bundle
        )
    }

    fun <T : BaseResultInterceptorActivity> activityForResultIntent(
        cls: Class<T>,
        context: Context,
        intent: Intent,
        i: Int,
        bundle: Bundle? = null,
        bundle2: Bundle? = null
    ): Intent {
        val intent2 = Intent(context, cls)

        intent2.putExtra(
            BaseResultInterceptorActivity.ARG_INTENT,
            intent
        )

        intent2.putExtra(
            BaseResultInterceptorActivity.ARG_REQUEST_CODE,
            i
        )

        intent2.putExtra(
            BaseResultInterceptorActivity.ARG_OPTIONS,
            bundle
        )

        intent2.putExtra(
            BaseResultInterceptorActivity.ARG_ARGUMENTS,
            bundle2
        )

        return intent2
    }

    inline fun <reified interceptor : BaseResultInterceptorActivity>
            Activity.activityForResult(
        intent: Intent,
        i: Int,
        bundle: Bundle? = null,
        bundle2: Bundle? = null
    ) {
        activityForResult(
            interceptor::class.java,
            this,
            intent,
            i,
            bundle,
            bundle2
        )
    }

    inline fun <reified interceptor : BaseResultInterceptorActivity>
            Context.activityForResultIntent(
        intent: Intent,
        i: Int,
        bundle: Bundle? = null,
        bundle2: Bundle? = null
    ): Intent {
        return activityForResultIntent(
            interceptor::class.java,
            this,
            intent,
            i,
            bundle,
            bundle2
        )
    }
}