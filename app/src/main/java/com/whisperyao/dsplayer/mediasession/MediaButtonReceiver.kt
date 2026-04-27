package com.whisperyao.dsplayer.mediasession

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent

class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // 已经是我们自定义的广播，直接转发给 Service / Receiver
        if (action.startsWith(ACTION_PREFIX)) {
            val forwardIntent = Intent().apply {
                this.action = action
                `package` = context.packageName
            }
            context.sendBroadcast(forwardIntent)
            return
        }

        // 系统媒体按键事件
        if (Intent.ACTION_MEDIA_BUTTON == action) {
            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }

            if (
                keyEvent != null &&
                keyEvent.action == KeyEvent.ACTION_DOWN &&
                processKey(context, keyEvent) &&
                isOrderedBroadcast
            ) {
                abortBroadcast()
            }
        }
    }

    private fun processKey(context: Context, event: KeyEvent?): Boolean {
        if (event == null) return false

        val intent = Intent()

        when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                intent.action = ACTION_PREV
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                intent.action = ACTION_NEXT
            }

            KeyEvent.KEYCODE_MEDIA_STOP -> {
                intent.action = ACTION_STOP
            }

            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                intent.action = ACTION_PAUSE
            }

            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                intent.action = ACTION_PLAY
            }

            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                val now = SystemClock.uptimeMillis()
                val diff = now - lastClickTime

                intent.action = if (diff < DOUBLE_CLICK_DELAY) {
                    ACTION_NEXT
                } else {
                    ACTION_PLAY
                }

                lastClickTime = now
            }

            else -> return false
        }

        intent.`package` = context.packageName
        context.sendBroadcast(intent)
        return true
    }

    companion object {
        const val ACTION_PREFIX = "com.synology.DSaudio.ACTION_"

        const val ACTION_PLAY = "com.synology.DSaudio.ACTION_PLAY"
        const val ACTION_PAUSE = "com.synology.DSaudio.ACTION_PAUSE"
        const val ACTION_STOP = "com.synology.DSaudio.ACTION_STOP"
        const val ACTION_NEXT = "com.synology.DSaudio.ACTION_NEXT"
        const val ACTION_PREV = "com.synology.DSaudio.ACTION_PREV"
        const val ACTION_REPEAT = "com.synology.DSaudio.ACTION_REPEAT"
        const val ACTION_SHUFFLE = "com.synology.DSaudio.ACTION_SHUFFLE"
        const val ACTION_APPWIDGET_UPDATE =
            "com.synology.DSaudio.ACTION_APPWIDGET_UPDATE"

        private const val DOUBLE_CLICK_DELAY = 600L

        @Volatile
        private var lastClickTime: Long = 0L

        /**
         * 用于 Notification / MediaSession 的 PendingIntent
         */
        fun buildBroadcastPendingIntent(
            context: Context,
            action: String,
            requestCode: Int = 0
        ): PendingIntent {
            val intent = Intent(context, MediaButtonReceiver::class.java).apply {
                this.action = action
                `package` = context.packageName
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE

            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flags
            )
        }
    }
}