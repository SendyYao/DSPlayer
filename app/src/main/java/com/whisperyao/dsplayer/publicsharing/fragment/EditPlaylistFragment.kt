package com.whisperyao.dsplayer.publicsharing.fragment

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.whisperyao.dsplayer.item.SongItem

class EditPlaylistFragment: DialogFragment() {

    interface Callbacks {
        fun onUpdatePlaylist()
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_SHARED = "shared"
        const val EXTRA_ID_LIST = "id_list"
        const val EXTRA_CREATE_BY_SHARE = "create_by_share"
        const val VALUE_EXTRA_MODE_CREATE = "create"

        @JvmStatic
        fun newInstance(bundlePlaylist: Bundle): EditPlaylistFragment {
            val editPlaylistFragment = EditPlaylistFragment()
            editPlaylistFragment.arguments = bundlePlaylist
            return editPlaylistFragment
        }

        @JvmStatic
        fun generateArgumentsForCreate(
            shared: Boolean,
            items: List<SongItem>,
            createByShare: Boolean
        ): Bundle {
            val idList = ArrayList(items.map { it.id })

            return Bundle().apply {
                putString(EXTRA_MODE, VALUE_EXTRA_MODE_CREATE)
                putBoolean(EXTRA_SHARED, shared)
                putStringArrayList(EXTRA_ID_LIST, idList)
                putBoolean(EXTRA_CREATE_BY_SHARE, createByShare)
            }
        }
    }

}