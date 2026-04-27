package com.whisperyao.dsplayer.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.facebook.drawee.view.SimpleDraweeView
import com.whisperyao.dsplayer.BuildConfig
import com.whisperyao.dsplayer.CoverUriLoader
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.model.NASSong
import com.whisperyao.dsplayer.model.Song
import com.whisperyao.dsplayer.util.SessionManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

class SongListActivity : AppCompatActivity() {
    private lateinit var listView: ListView

    private val songs = listOf(
        Song("无言感激", "谭咏麟", R.raw.song, R.raw.lyric)
    )

    private var baseUrl: String = "http://${BuildConfig.NAS_ADDRESS}:${BuildConfig.NAS_PORT}"
    private var sid: String = SessionManager.getSid(this)

    private var songCount: Int = 0

    fun fetchSongs(callback: (List<NASSong>) -> Unit) {

        val url = "$baseUrl/webapi/AudioStation/folder.cgi?" +
                "api=SYNO.AudioStation.Folder" +
                "&version=2" +
                "&method=list" +
                "&library=shared" +
                "&id=dir_599" +
                "&limit=1000" +
                "&sort_by=title" +
                "&sort_direction=ASC" +
                "&additional=song_tag,song_audio,song_rating" +
                "&_sid=$sid"

        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                val songs = parseSongs(body)

                callback(songs)
            }
        })
    }

    fun parseSongs(jsonStr: String?): List<NASSong> {
        val list = mutableListOf<NASSong>()

        if (jsonStr == null) return list

        try {
            val json = JSONObject(jsonStr)

            Log.i("parseSongs", jsonStr)

            val songsArray = json
                .getJSONObject("data")
                .getJSONArray("items")

            songCount = json
                .getJSONObject("data")
                .getInt("total")

            for (i in 0 until songsArray.length()) {
                val item = songsArray.getJSONObject(i)

                val title = item.getString("title")
                val id = item.getString("id")

                val artist = item
                    .getJSONObject("additional")
                    .getJSONObject("song_tag")
                    .optString("artist", "Unknown")

                list.add(
                    NASSong(
                        title = title,
                        artist = artist,
                        songId = id
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_list)

        listView = findViewById(R.id.listView)

        fetchSongs { songList ->

            runOnUiThread {

                findViewById<TextView>(R.id.song_count_text).text =
                    getString(R.string.songs_count, songCount)

                val firstSongItem: SongItem = SongItem.generateNoneSong()

                firstSongItem.id = songList[0].songId

                CoverUriLoader()
                    .with(findViewById(R.id.top_cover_bg))
                    .placeHolder(R.raw.cover)
                    .blur(15)
                    .load(firstSongItem)

                CoverUriLoader()
                    .with(findViewById(R.id.top_cover))
                    .placeHolder(R.raw.cover)
                    .load(firstSongItem)

                val adapter = object : ArrayAdapter<NASSong>(
                    this,
                    R.layout.item_song,
                    songList
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = convertView ?: layoutInflater.inflate(R.layout.item_song, parent, false)

                        val songItem: SongItem = SongItem.generateNoneSong()
                        getItem(position)!!.apply {
                            songItem.title = title
                            songItem.id = songId
                            songItem.artist = artist
                        }

                        view.findViewById<TextView>(R.id.tvTitle).text = songItem.title
                        view.findViewById<TextView>(R.id.tvArtist).text = songItem.artist

                        // 👉 封面
                        val imgCover = view.findViewById<SimpleDraweeView>(R.id.imgCover)

                        CoverUriLoader()
                            .with(imgCover)
                            .placeHolder(R.drawable.icon_song)
                            .failureImage(R.drawable.icon_music)
                            .load(songItem)

                        return view
                    }
                }

                listView.adapter = adapter
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("song_index", 0)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}