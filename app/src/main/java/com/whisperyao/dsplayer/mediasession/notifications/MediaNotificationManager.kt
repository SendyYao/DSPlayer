package com.whisperyao.dsplayer.mediasession.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat as NCompat
import androidx.media.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.injection.Constants
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import javax.inject.Inject
import javax.inject.Named


class MediaNotificationManager @Inject constructor(
    @Named(Constants.NOTIFICATION_CHANNEL_PLAYBACK)
    val channelId: String,
    val context: Context,
    val notificationManager: NotificationManager,
    val playingQueueManager: PlayingQueueManager
) {

    companion object {
        const val NOTIFICATION_ID = 412
        private const val REQUEST_CODE = 501
        private const val TAG = "MediaNotificationManager"
    }

    private val playAction by lazy {
        NCompat.Action(
            R.drawable.ic_play_arrow,
            "Play",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_PLAY
            )
        )
    }

    private val pauseAction by lazy {
        NCompat.Action(
            R.drawable.ic_pause,
            "Pause",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_PAUSE
            )
        )
    }

    private val nextAction by lazy {
        NCompat.Action(
            R.drawable.ic_skip_next,
            "Next",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        )
    }

    private val prevAction by lazy {
        NCompat.Action(
            R.drawable.ic_skip_previous,
            "Previous",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNotification(
        metadata: MediaMetadataCompat,
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token
    ): Notification {
        return buildNotification(
            state = state,
            token = token,
            isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING ||
                    state.state == PlaybackStateCompat.STATE_BUFFERING,
            metadata = metadata
        ).build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildNotification(
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token,
        isPlaying: Boolean,
        metadata: MediaMetadataCompat
    ): NCompat.Builder {
        createChannel()

        val description = metadata.description
        val mediaId = description.mediaId
            ?: throw NullPointerException("media id should not be null")

        var subText = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
        if (TextUtils.isEmpty(subText)) {
            subText = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        }

        val builder = NCompat.Builder(context, channelId)
            .setStyle(
                NotificationCompat.MediaStyle()
                    .setMediaSession(token)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
            .setSmallIcon(R.drawable.notification_icon)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setShowWhen(false)
            .setContentIntent(createContentIntent())
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setSubText(subText)
            .setOngoing(isPlaying)
            .setVisibility(NCompat.VISIBILITY_PUBLIC)

        val albumBitmap: Bitmap =
            playingQueueManager.getAlbumBitmap(context, mediaId)
                ?: BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.thumbnail_song
                )

        builder.setLargeIcon(albumBitmap)

        if ((state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0L) {
            builder.addAction(prevAction)
        }

        builder.addAction(if (isPlaying) pauseAction else playAction)

        if ((state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0L) {
            builder.addAction(nextAction)
        }

        return builder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val name = context.getString(R.string.app_name)

            val channel = NotificationChannel(
                channelId,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MediaSession and MediaPlayer"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "createChannel: New channel created")
        } else {
            Log.d(TAG, "createChannel: Existing channel reused")
        }
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }

    fun onDestroy() {
        Log.d(TAG, "onDestroy")
    }
}