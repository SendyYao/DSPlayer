package com.whisperyao.dsplayer.activity

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dirror.lyricviewx.LyricViewX
import com.dirror.lyricviewx.OnPlayClickListener
import com.dirror.lyricviewx.OnSingleClickListener
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.model.Song

class PlayerActivity : AppCompatActivity() {

    private var showLyric = true
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var lyricView: LyricViewX

    private val songs = listOf(
        Song("无言感激", "谭咏麟", R.raw.song, R.raw.lyric)
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val index = intent.getIntExtra("song_index", 0)
        val song = songs[index]

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvArtist = findViewById<TextView>(R.id.tvArtist)
        val imgCover = findViewById<ImageView>(R.id.imgCover)

        imgCover.post {
            val width = imgCover.width
            imgCover.layoutParams.height = width
            imgCover.requestLayout()
        }
        val cover = BitmapFactory.decodeStream(resources.openRawResource(R.raw.cover))
        imgCover.setImageBitmap(cover)

        val btnPlay = findViewById<Button>(R.id.btnPlay)
        lyricView = findViewById(R.id.lyricView)

        tvTitle.text = song.title
        tvArtist.text = song.artists

        mediaPlayer = MediaPlayer.create(this, song.resId)
        mediaPlayer.start()

        val lyricText = resources.openRawResource(song.lyricResId)
            .bufferedReader()
            .use { it.readText() }

        lyricView.loadLyric(lyricText)

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
                // TODO: Add dynamic background
                lyricView.alpha = if (showLyric) 1f else 0f
                showLyric = !showLyric
            }
        })

        val handler = Handler(Looper.getMainLooper())
        handler.post( object : Runnable {
            override fun run() {
                if (::mediaPlayer.isInitialized) {
                    lyricView.updateTime(mediaPlayer.currentPosition.toLong())
                    // TODO judge to exit
                }
                handler.postDelayed(this, 500)
            }
        })

        btnPlay.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}