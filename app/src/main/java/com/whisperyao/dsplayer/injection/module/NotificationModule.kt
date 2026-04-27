package com.whisperyao.dsplayer.injection.module

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.whisperyao.dsplayer.R
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class NotificationModule {

    companion object {
        const val CHANNEL_ID_DOWNLOAD = "ds_audio_download"
        const val CHANNEL_ID_PLAYBACK = "ds_audio_playback"
    }

    @Provides
    @Named(CHANNEL_ID_PLAYBACK)
    fun provideChannelId(
        context: Context,
        notificationManager: NotificationManager
    ): String {
        migrateDeprecatedChannel(context, notificationManager)
        return CHANNEL_ID_PLAYBACK
    }

    @Provides
    @Named(CHANNEL_ID_PLAYBACK)
    fun provideMediaNotificationCompatBuilder(
        context: Context,
        notificationManager: NotificationManager
    ): NotificationCompat.Builder {

        val channel = NotificationChannel(
            CHANNEL_ID_PLAYBACK,
            context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(
            context,
            CHANNEL_ID_PLAYBACK
        )
            .setSmallIcon(R.drawable.notification_icon)
            .setColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimary
                )
            )
            .setTicker(context.getString(R.string.app_name))
            .setContentTitle(context.getString(R.string.app_name))
            .setChannelId(CHANNEL_ID_PLAYBACK)
    }

    @Provides
    @Named(CHANNEL_ID_DOWNLOAD)
    fun provideDownloadNotificationCompatBuilder(
        context: Context,
        notificationManager: NotificationManager
    ): NotificationCompat.Builder {

        val channel = NotificationChannel(
            CHANNEL_ID_DOWNLOAD,
            context.getString(R.string.download),
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(
            context,
            CHANNEL_ID_DOWNLOAD
        )
            .setSmallIcon(R.drawable.notification_icon)
            .setColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimary
                )
            )
            .setTicker(context.getString(R.string.downloading))
            .setContentTitle(context.getString(R.string.downloading))
            .setChannelId(CHANNEL_ID_DOWNLOAD)
            .setWhen(0L)
            .setOngoing(true)
    }

    private fun migrateDeprecatedChannel(
        context: Context,
        notificationManager: NotificationManager
    ) {
        notificationManager
            .getNotificationChannel("ds_audio_channel")
            ?.let {
                notificationManager.deleteNotificationChannel(
                    "ds_audio_channel"
                )
            }

        val downloadChannel =
            notificationManager.getNotificationChannel(
                CHANNEL_ID_DOWNLOAD
            )

        if (
            downloadChannel != null &&
            !TextUtils.equals(
                downloadChannel.name,
                context.getString(R.string.download)
            )
        ) {
            notificationManager.deleteNotificationChannel(
                CHANNEL_ID_DOWNLOAD
            )
        }
    }
}