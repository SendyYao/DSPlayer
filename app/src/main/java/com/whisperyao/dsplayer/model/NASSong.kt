package com.whisperyao.dsplayer.model

import android.os.Parcelable

@kotlinx.parcelize.Parcelize
data class NASSong (
    val title: String,
    val artist: String,
    val songId: String
) : Parcelable