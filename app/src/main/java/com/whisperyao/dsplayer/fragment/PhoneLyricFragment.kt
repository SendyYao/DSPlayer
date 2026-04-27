package com.whisperyao.dsplayer.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.whisperyao.dsplayer.R
import com.synology.ThreadWork
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.databinding.LyricFragmentBinding
import com.whisperyao.dsplayer.injection.Constants
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.playing.PlayingSongDetailHelper
import com.whisperyao.dsplayer.util.LyricUtils
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.widget.DynamicLyricListAdapter
import com.whisperyao.dsplayer.widget.DynamicLyricListView
import com.whisperyao.dsplayer.widget.RatingBar
import javax.inject.Inject
import javax.inject.Named
import androidx.core.content.edit

class PhoneLyricFragment : LyricFragment() {

    companion object {
        private const val KEY_SHOWLYRIC = "show_lyric"
        private const val LOG = "PhoneLyricFragment"
    }

    private var binding: LyricFragmentBinding? = null
    private var hasLyric = false

    @Inject
    @Named(Constants.PREF_LYRIC)
    lateinit var lyricPreference: SharedPreferences

    private var mDynamicListAdapter: DynamicLyricListAdapter? = null
    private var mLoadLyricThread: ThreadWork? = null
    private var mSongItem: SongItem? = null
    private var mStrLyric: String? = null
    private var modeShowDynamicLyric = false

    private var mSongDetailHelper = PlayingSongDetailHelper()
    private var mLastPlayingSongId = ""

    private val mLyricText
        get() = binding?.root?.findViewById<TextView>(R.id.tv_lyric)

    private val mScrollView
        get() = binding?.root?.findViewById<ScrollView>(R.id.sv_lyric)

    private val mDynamicListView
        get() = binding?.root?.findViewById<DynamicLyricListView>(
            R.id.dymanic_listview
        )

    private val mLayoutInfo
        get() = binding?.root?.findViewById<LinearLayout>(
            R.id.layout_info
        )

    private val mLayoutDynamicLyric
        get() = binding?.root?.findViewById<View>(
            R.id.layout_dynamiclyric
        )

    private val mScrollLayout
        get() = binding?.root?.findViewById<View>(
            R.id.layout_sv_lyric
        )

    private val mLyricFragmentView
        get() = binding?.root?.findViewById<View>(
            R.id.layout_lyric_fragment
        )

    private val ivCover: SimpleDraweeView?
        get() = binding?.root?.findViewById(R.id.PlayingControlPanel_ThumbImageView)

    private val tvAlbum: TextView?
        get() = binding?.root?.findViewById(R.id.PlayingControlPanel_TextAlbum)

    private val tvArtist: TextView?
        get() = binding?.root?.findViewById(R.id.PlayingControlPanel_TextArtist)

    private val tvTitle: TextView?
        get() = binding?.root?.findViewById(R.id.PlayingControlPanel_TextTitle)

    private val rbRating: RatingBar?
        get() = binding?.root?.findViewById(R.id.PlayingControlPanel_RatingBar)

    private val layoutDynamicLyric: View?
        get() = binding?.root?.findViewById(R.id.layout_dynamiclyric)

    private val dynamicListView: DynamicLyricListView?
        get() = binding?.root?.findViewById(R.id.dymanic_listview)

    private val layoutInfo: LinearLayout?
        get() = binding?.root?.findViewById(R.id.layout_info)

    private val scrollViewLyric: ScrollView?
        get() = binding?.root?.findViewById(R.id.sv_lyric)

    private fun getLyricPref(): Boolean {
        return lyricPreference.getBoolean(KEY_SHOWLYRIC, true)
    }

    private fun setLyricPref(value: Boolean) {
        lyricPreference.edit {
            putBoolean(KEY_SHOWLYRIC, value)
        }
    }

    private fun setUpViews() {
        mLyricFragmentView?.setOnClickListener {
            SynoLog.d(LOG, "(mLyricFragmentView) onClick")
            setLyricPref(true)
            showView()
        }

        mLayoutInfo?.setOnClickListener {
            SynoLog.d(LOG, "(mLayoutInfo) onClick")
            setLyricPref(true)
            showView()
        }

        mLyricText?.setOnClickListener {
            SynoLog.d(LOG, "(mLyric) onClick")
            setLyricPref(false)
            showView()
        }

        mScrollLayout?.setOnClickListener {
            SynoLog.d(LOG, "(mScrollLayout) onClick")
            setLyricPref(false)
            showView()
        }

        mLayoutDynamicLyric?.setOnClickListener {
            SynoLog.d(LOG, "(mLayoutDynamicLyric) onClick")
            setLyricPref(false)
            showView()
        }

        mDynamicListView?.setAdapter(mDynamicListAdapter)

        mDynamicListView?.setOnItemClickListener { _, _, _, _ ->
            SynoLog.d(LOG, "(mDynamicListView) onItemClick")
            setLyricPref(false)
            showView()
        }

        mSongDetailHelper = PlayingSongDetailHelper(
            activity,
            ivCover,
            tvAlbum,
            tvArtist,
            tvTitle,
            rbRating
        ).apply {
            setIsForPhone(true)
            setPlayingQueueManager(playingQueueManager)
        }

        updateTrackInfo(mSongItem)
    }

    private fun loadLyric(songItem: SongItem) {
        mLoadLyricThread = object : ThreadWork() {

            private val mRequestSongId = songItem.id
            private var strLyric: String? = null

            override fun preWork() {
                hasLyric = false
                showView()
            }

            override fun onWorking() {
                try {
                    var json = CacheManager.getInstance().doEnumLyrics(songItem)

                    if (json.has("data")) {
                        json = json.optJSONObject("data")
                    }

                    strLyric = json.optString(LYRICS, "")

                    val lyricsArray = json.optJSONArray(LYRICS)

                    if (lyricsArray != null) {
                        strLyric = lyricsArray
                                .optJSONObject(0)
                                ?.optJSONObject(ADDITIONAL)
                                ?.optString(FULL_LYRICS, "")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onComplete() {
                if (mSongItem?.id != mRequestSongId) {
                    SynoLog.d(LOG, "The song with lyric is not current")
                    return
                }

                mDynamicListAdapter = LyricUtils.extractDynamicLyric(strLyric)

                SynoLog.d("PhoneLyricFragment", "mDynamicListAdapter: $mDynamicListAdapter")

                mStrLyric = LyricUtils.extractLyric(strLyric)

                if (mDynamicListAdapter == null) {
                    modeShowDynamicLyric = false

                    if (mStrLyric.isNullOrEmpty()) {
                        mLyricText?.text = null
                        hasLyric = false
                    } else {
                        mLyricText?.text = mStrLyric
                        hasLyric = true
                    }

                    mScrollView?.scrollTo(0, 0)
                } else {
                    hasLyric = true
                    modeShowDynamicLyric = true
                    mDynamicListView?.setAdapter(mDynamicListAdapter)
                }

                showView()
                SynoLog.d("PhoneLyricFragment", "instance=${hashCode()} lyric loaded")
            }
        }

        mLoadLyricThread?.startWork()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LyricFragmentBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setUpViews()
        SynoLog.d("PhoneLyricFragment", "instance=${hashCode()} onViewCreated")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        lyricPreference =
            context.getSharedPreferences(
                Constants.PREF_LYRIC,
                Context.MODE_PRIVATE
            )
    }

    override fun onDetach() {
        SynoLog.d(LOG, "onDetach")
        val threadWork = this.mLoadLyricThread
        if (threadWork != null && threadWork.isWorking) {
            threadWork.endThread()
        }
        super.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun setTimeLine(time: Long) {
        // SynoLog.i("PhoneLyricFragment", "setTimeLine, time: $time, !modeShowDynamicLyric: ${!modeShowDynamicLyric}")
        // SynoLog.d("PhoneLyricFragment", "instance=${hashCode()} setTimeLine=$time")
        if (!modeShowDynamicLyric) {
            return
        }

        dynamicListView?.setSelection(time)
    }

    override fun updateTrackInfo(songItem: SongItem?) {
        mSongItem = songItem

        mSongDetailHelper.updateSongInfo(songItem)

        if (songItem == null) {
            hasLyric = false
            showView()
            return
        }

        if (mLastPlayingSongId == songItem.id) {
            return
        }

        mLastPlayingSongId = songItem.id ?: ""

        loadLyric(songItem)
    }

    private fun showView() {
        if (hasLyric && getLyricPref()) {

            if (modeShowDynamicLyric) {
                layoutDynamicLyric?.visibility = View.VISIBLE
                dynamicListView?.visibility = View.VISIBLE
                dynamicListView?.requestFocus()

                scrollViewLyric?.visibility = View.GONE
            } else {
                scrollViewLyric?.visibility = View.VISIBLE
                scrollViewLyric?.requestFocus()

                layoutDynamicLyric?.visibility = View.GONE
                dynamicListView?.visibility = View.GONE
            }

            val alphaAnimation = AlphaAnimation(0.15f, 0.15f).apply {
                duration = 0L
                fillAfter = true
            }

            layoutInfo?.startAnimation(alphaAnimation)

            return
        }

        scrollViewLyric?.visibility = View.GONE
        layoutDynamicLyric?.visibility = View.GONE
        dynamicListView?.visibility = View.GONE

        val alphaAnimation = AlphaAnimation(1.0f, 1.0f).apply {
            duration = 0L
            fillAfter = true
        }

        layoutInfo?.startAnimation(alphaAnimation)
    }
}