package com.synology

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenReceiver : BroadcastReceiver() {

    var isScreenOn: Boolean = true
        private set

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                isScreenOn = false
            }
            Intent.ACTION_SCREEN_ON -> {
                isScreenOn = true
            }
        }
    }
}