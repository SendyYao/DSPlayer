package com.whisperyao.dsplayer.publicsharing.fragment

import androidx.fragment.app.DialogFragment

class EditPlaylistFragment: DialogFragment() {

    interface Callbacks {
        fun onUpdatePlaylist()
    }

}