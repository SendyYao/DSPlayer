package com.whisperyao.dsplayer.util.extension

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import java.util.concurrent.TimeUnit

object SongExtensionsKt {

    fun MediaMetadataCompat.addCover(bitmap: Bitmap?): MediaMetadataCompat {
        if (bitmap == null) return this

        return MediaMetadataCompat.Builder()
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            .putString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                getMediaId()
            )
            .putStringNotNull(
                MediaMetadataCompat.METADATA_KEY_ALBUM,
                getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            )
            .putStringNotNull(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            )
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            )
            .putStringNotNull(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            )
            .build()
    }

    fun SongItem.toMediaItem(): MediaBrowserCompat.MediaItem {
        val subtitle = if (artist.isNullOrEmpty()) {
            album ?: ""
        } else {
            "$artist/$album"
        }

        return MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setExtras(Bundle().apply {
                    putString("dsid", dsId)
                })
                .setMediaId(mediaId)
                .setTitle(title)
                .setSubtitle(subtitle)
                .build(),
            if (mediaId.startsWith(PlayingQueueManager.PREFIX_BROWSABLE)) {
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            } else {
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            }
        )
    }

    fun SongItem.toMetadata(): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
            .putStringNotNull(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putStringNotNull(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                TimeUnit.MILLISECONDS.convert(duration.toLong(), TimeUnit.SECONDS)
            )
            .putStringNotNull(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .build()
    }

    fun MediaMetadataCompat.getMediaId(): String {
        return getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            ?: error("Required value was null.")
    }

    private fun MediaMetadataCompat.Builder.putStringNotNull(
        key: String,
        value: String?
    ): MediaMetadataCompat.Builder {
        value?.let { putString(key, it) }
        return this
    }
}
