package com.whisperyao.dsplayer.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.dirror.lyricviewx.LyricViewX
import com.dirror.lyricviewx.OnPlayClickListener
import com.dirror.lyricviewx.OnSingleClickListener
import com.facebook.drawee.view.SimpleDraweeView
import com.synology.ThreadWork
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.databinding.LyricviewxFragmentBinding
import com.whisperyao.dsplayer.injection.Constants
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.playing.PlayingSongDetailHelper
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.widget.RatingBar
import javax.inject.Inject
import javax.inject.Named

class PhoneLyricViewXFragment : LyricFragment() {

    companion object {
        const val LOG = "PhoneLyricViewXFragment"
        const val KEY_SHOWLYRIC = "show_lyric"
    }

    private var binding: LyricviewxFragmentBinding? = null
    private var hasLyric = false
    private var modeShowLyricViewX = false
    private var mSongItem: SongItem? = null

    private var mSongDetailHelper = PlayingSongDetailHelper()

    private var mLastPlayingSongId = ""
    @Inject
    @Named(Constants.PREF_LYRIC)
    lateinit var lyricPreference: SharedPreferences

    private var controller: MediaControllerCompat? = null

    private val mDynamicLyricViewX
        get() = binding?.root?.findViewById<LyricViewX>(
            R.id.dymanic_listview
        )

    private val mLayoutInfo
        get() = binding?.root?.findViewById<LinearLayout>(
            R.id.layout_info
        )

    private val mLyricFragmentViewX
        get() = binding?.root?.findViewById<View>(
            R.id.layout_lyric_fragment
        )

    private val mScrollLayout
        get() = binding?.root?.findViewById<View>(
            R.id.layout_sv_lyric
        )

    private val mLayoutDynamicLyricX
        get() = binding?.root?.findViewById<View>(
            R.id.layout_dynamiclyric
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

    private var mLoadLyricThread: ThreadWork? = null

    private fun getLyricPref(): Boolean {
        return lyricPreference.getBoolean(KEY_SHOWLYRIC, true)
    }

    private fun setLyricPref(value: Boolean) {
        lyricPreference.edit {
            putBoolean(KEY_SHOWLYRIC, value)
        }
    }

    private fun setupViews() {
        SynoLog.d(LOG, "mLyricFragmentViewX: $mLyricFragmentViewX, mLayoutInfo: $mLayoutInfo, mDynamicLyricViewX: $mDynamicLyricViewX")

        mDynamicLyricViewX?.setOnSingerClickListener(object : OnSingleClickListener {
            override fun onClick() {
                SynoLog.d(LOG, "(mLyricFragmentViewX) onClick")
                setLyricPref(!getLyricPref())
                showView()
            }
        })

        mLyricFragmentViewX?.setOnClickListener {
            SynoLog.d(LOG, "(mLyricFragmentView) onClick")
            setLyricPref(true)
            showView()
        }

        mLayoutInfo?.setOnClickListener {
            SynoLog.d(LOG, "(mLayoutInfo) onClick")
            setLyricPref(true)
            showView()
        }

        mScrollLayout?.setOnClickListener {
            SynoLog.d(LOG, "(mScrollLayout) onClick")
            setLyricPref(false)
            showView()
        }

        mLayoutDynamicLyricX?.setOnClickListener {
            SynoLog.d(LOG, "(mLayoutDynamicLyric) onClick")
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

        mDynamicLyricViewX?.setNormalColor(ContextCompat.getColor(App.getContext(), R.color.white))
        mDynamicLyricViewX?.setCurrentColor(ContextCompat.getColor(App.getContext(), R.color.current_lyric_color))

        mDynamicLyricViewX?.setDraggable(
            true,
            object : OnPlayClickListener {
                override fun onPlayClick(time: Long): Boolean {
                    SynoLog.d(LOG, "onPlayClick, time: $time, controller: $controller")
                    this@PhoneLyricViewXFragment.controller?.transportControls?.seekTo(time)
                    return true
                }
            }
        )
        updateTrackInfo(mSongItem)
    }

    override fun bindController(controller: MediaControllerCompat) {
        this.controller = controller
    }

    private fun loadLyric(songItem: SongItem) {
        mLoadLyricThread = object : ThreadWork() {
            private var mRequestSongId = songItem.id
            private var strLyric: String? = null

            override fun preWork() {
                hasLyric = false
                showView()
            }

            override fun onWorking() {
                try {
                    val rootJson = CacheManager.getInstance().doEnumLyrics(songItem)
                    val dataJson = rootJson.optJSONObject("data") ?: rootJson

                    strLyric = dataJson.optJSONArray(LYRICS)
                        ?.optJSONObject(0)
                        ?.optJSONObject(ADDITIONAL)
                        ?.optString(FULL_LYRICS)
                        ?: dataJson.optString(LYRICS, "")

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onComplete() {
                if (mSongItem?.id != mRequestSongId) {
                    SynoLog.d(LOG, "The song with lyric is not current")
                    return
                }

                if (strLyric.isNullOrEmpty()) {
                    modeShowLyricViewX = false
                    hasLyric = false
                    mDynamicLyricViewX?.setLabel("No lyrics")
                } else {
                    SynoLog.d(LOG, "load lyric")
                    modeShowLyricViewX = true
                    hasLyric = true
                    mDynamicLyricViewX?.loadLyric(strLyric)
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
        binding = LyricviewxFragmentBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViews()
        SynoLog.d(LOG, "instance=${hashCode()} onViewCreated")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        lyricPreference = context.getSharedPreferences(
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
        if (!modeShowLyricViewX) {
            return
        }
        try {
            mDynamicLyricViewX?.updateTime(time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        try {
            SynoLog.d(LOG, "hasLyric: $hasLyric, lyricPref: ${getLyricPref()}, modeShowLyricViewX: $modeShowLyricViewX")
        } catch (e: UninitializedPropertyAccessException) {
            e.printStackTrace()
        }
        if (hasLyric && getLyricPref()) {
            if (modeShowLyricViewX) {
                mLayoutDynamicLyricX?.visibility = View.VISIBLE
            } else {
                mLayoutDynamicLyricX?.visibility = View.GONE
            }

            val alphaAnimation = AlphaAnimation(0.15f, 0.15f).apply {
                duration = 0L
                fillAfter = true
            }

            mLayoutInfo?.startAnimation(alphaAnimation)

            return
        }

        mLayoutDynamicLyricX?.visibility = View.GONE

        val alphaAnimation = AlphaAnimation(1.0f, 1.0f).apply {
            duration = 0L
            fillAfter = true
        }

        mLayoutInfo?.startAnimation(alphaAnimation)
    }
}