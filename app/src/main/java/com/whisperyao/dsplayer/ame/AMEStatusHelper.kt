package com.whisperyao.dsplayer.ame

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.activity.BaseActivity
import com.whisperyao.dsplayer.util.Utils
import com.whisperyao.dsplayer.util.event.AMEDefectEvent
import com.whisperyao.dsplayer.vos.PlayingInfo
import org.greenrobot.eventbus.EventBus


object AMEStatusHelper {

    private const val LEARN_MORE_URL = "https://sy.to/dsadaactsf"

    private var isManager = false

    fun init(isManager: Boolean) {
        this.isManager = isManager
    }

    private fun getErrorMessageForRemotePlayerStreaming(
        context: Context
    ): String {
        return context.getString(
            if (isManager) {
                R.string.str_remote_streaming_can_not_play_aac
            } else {
                R.string.str_remote_streaming_can_not_play_aac_nonadmin
            }
        )
    }

    private fun getDialogMessageForRemotePlayerStreaming(
        context: Context
    ): String {
        return context.getString(
            if (isManager) {
                R.string.str_remote_streaming_can_not_play_aac_alert_msg
            } else {
                R.string.str_remote_streaming_can_not_play_aac_alert_msg_nonadmin
            }
        )
    }

    fun checkRemotePlayerAME(
        context: Context,
        playingInfo: PlayingInfo,
        newInfo: PlayingInfo
    ) {
        if (playingInfo.aacTimeStamp != newInfo.aacTimeStamp) {
            EventBus.getDefault().postSticky(
                AMEDefectEvent(
                    getErrorMessageForRemotePlayerStreaming(context)
                )
            )
        }
    }

    fun showLearnMoreDialog(activity: BaseActivity) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(
                activity.getString(
                    R.string.str_remote_streaming_can_not_play_aac_alert_title
                )
            )
            .setMessage(
                getDialogMessageForRemotePlayerStreaming(activity)
            )
            .setPositiveButton(R.string.str_ok, null)
            .create()

        dialog.show()

        val messageTextView =
            dialog.findViewById<TextView>(android.R.id.message)

        messageTextView?.let { textView ->
            val fullText = textView.text.toString()

            val learnMoreText = activity.getString(
                R.string.str_learn_more
            )

            textView.text = Utils.getUrlSpan(
                fullText,
                learnMoreText,
                LEARN_MORE_URL
            )

            textView.movementMethod =
                LinkMovementMethod.getInstance()
        }
    }
}