package com.whisperyao.dsplayer.activity

import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dirror.lyricviewx.LyricViewX
import com.dirror.lyricviewx.OnPlayClickListener
import com.dirror.lyricviewx.OnSingleClickListener
import com.facebook.drawee.view.SimpleDraweeView
import com.synology.ThreadWork
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.ConnectionManager
import com.whisperyao.dsplayer.CoverUriLoader
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.model.NASSong
import com.whisperyao.dsplayer.util.SynoLog

class PlayerActivity : AppCompatActivity() {

    private var showLyric = true
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var lyricView: LyricViewX
    private var hasLyric = false
    private var currentSong: NASSong? = null
    private var mLoadLyricThread: ThreadWork? = null
    private lateinit var currentSongItem: SongItem

    fun playMusic(url: String) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            setDataSource(url)

            setOnPreparedListener {
                it.start()
            }

            setOnCompletionListener {
                SynoLog.i("Music", "播放完成")
            }

            setOnErrorListener { _, what, extra ->
                SynoLog.e("Music", "播放失败 what=$what extra=$extra")
                true
            }

            prepareAsync()
        }
    }

    private fun loadLyric() {
        mLoadLyricThread = object : ThreadWork() {
            private var strLyric: String? = null

            override fun preWork() {
                currentSongItem = SongItem.generateNoneSong()
                currentSongItem.id = currentSong?.songId
                hasLyric = false
            }

            override fun onWorking() {
                try {
                    val rootJson = CacheManager.getInstance().doEnumLyrics(currentSongItem)
                    val dataJson = rootJson.optJSONObject("data") ?: rootJson

                    strLyric = dataJson.optJSONArray("lyrics")
                        ?.optJSONObject(0)
                        ?.optJSONObject("additional")
                        ?.optString("full_lyrics")
                        ?: dataJson.optString("lyrics", "")

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onComplete() {
                SynoLog.d("PhoneLyricFragment", "instance=${hashCode()} lyric loaded, lyric: $strLyric")
                lyricView.loadLyric(strLyric)
            }
        }

        mLoadLyricThread?.startWork()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        currentSong = intent.getParcelableExtra("current_song")

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvArtist = findViewById<TextView>(R.id.tvArtist)
        val imgCover = findViewById<SimpleDraweeView>(R.id.imgCover)

        imgCover.post {
            val width = imgCover.width
            imgCover.layoutParams.height = width
            imgCover.requestLayout()
        }
        val cover = BitmapFactory.decodeStream(resources.openRawResource(R.raw.cover))
        imgCover.setImageBitmap(cover)

        val btnPlay = findViewById<Button>(R.id.btnPlay)
        lyricView = findViewById(R.id.lyricView)

        tvTitle.text = currentSong?.title
        tvArtist.text = currentSong?.artist

        loadLyric()

        val playUrl = ConnectionManager.getPlayUrl(currentSongItem)

        SynoLog.d("PlayerActivity", "playUrl: $playUrl")

        playMusic(playUrl)

        lyricView.setNormalColor(ContextCompat.getColor(this, R.color.white))
        lyricView.setCurrentColor(ContextCompat.getColor(this, R.color.current_lyric_color))

        lyricView.setDraggable(
            true,
            object : OnPlayClickListener {
                override fun onPlayClick(time: Long): Boolean {
                    mediaPlayer.seekTo(time.toInt())
                    return true
                }
            }
        )

        lyricView.setOnSingerClickListener(object : OnSingleClickListener {
            override fun onClick() {
                lyricView.alpha = if (showLyric) 1f else 0f
                showLyric = !showLyric
            }
        })

        val handler = Handler(Looper.getMainLooper())
        handler.post( object : Runnable {
            override fun run() {
                try {
                    if (::mediaPlayer.isInitialized) {
                        lyricView.updateTime(mediaPlayer.currentPosition.toLong())
                    }
                handler.postDelayed(this, 500)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        btnPlay.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
        }

        // TODO: Add dynamic background | DONE
        CoverUriLoader()
            .with(findViewById(R.id.imgCover))
            .load(currentSongItem)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}