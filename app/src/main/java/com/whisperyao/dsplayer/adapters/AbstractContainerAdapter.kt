package com.whisperyao.dsplayer.adapters

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager;
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.LocalEnumerator
import com.whisperyao.dsplayer.fragment.ContainerFragment;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.util.CoverUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.HashMap;
import java.util.List;


abstract class AbstractContainerAdapter(
    @JvmField
    val mContainerFragment: ContainerFragment
) : AbsAdapter<Item>() {

    private var calMapJob: Job? = null

    @JvmField
    protected var mIsOnline: Boolean = true

    lateinit var mType: Common.ContainerType

    protected var mkey: String? = null

    private val offlineCoverMap = HashMap<String, String>()

    fun calCoverPath(data: List<Item>) {

        if (mIsOnline) return

        calMapJob?.cancel()

        val gridLayoutManager = mLayoutManager as? GridLayoutManager
        val firstVisible = gridLayoutManager?.findFirstVisibleItemPosition() ?: 0
        val lastVisible = gridLayoutManager?.findLastVisibleItemPosition() ?: 0

        calMapJob = mContainerFragment.lifecycleScope.launch(Dispatchers.IO) {
            calculateOfflineCoverMap(
                data,
                firstVisible,
                lastVisible
            )
        }
    }

    private suspend fun calculateOfflineCoverMap(data: List<Item>, firstVisible: Int, lastVisible: Int) {
        data.forEachIndexed { index, item ->

            val savedSongs = LocalEnumerator()
                .doEnumContainerSongList(
                    mType,
                    mContainerFragment.getEnumSongsBundle(item)
                )

            offlineCoverMap[item.id] = ""

            savedSongs?.forEach { song ->
                val coverFile = CoverUtil(
                    App.getContext()
                ).getCoverFileFromSong(song)

                if (coverFile != null) {
                    offlineCoverMap[item.id] = coverFile.path
                    return@forEach
                }
            }

            val coverPath = offlineCoverMap[item.id]

            if (!coverPath.isNullOrEmpty() && index in firstVisible..lastVisible) {
                withContext(Dispatchers.Main) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    fun getCoverPath(item: Item): String? {
        return offlineCoverMap[item.id]
    }

    fun setContainerType(type: Common.ContainerType) {
        mType = type
    }

    fun setKey(key: String) {
        mkey = key
    }

    fun setIsOnline(b: Boolean) {
        mIsOnline = b
    }
}