package com.synology.sylibx.passcode


class PassCodeObserver {

    interface AppCallback {
        object DefaultImpls {
            fun onBackground(appCallback: AppCallback?) {
            }

            fun onForeground(appCallback: AppCallback?) {
            }
        }

        fun onBackground()

        fun onForeground()
    }

}