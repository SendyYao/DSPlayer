package com.whisperyao.dsplayer.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.homepage.PinManager
import com.whisperyao.dsplayer.item.HomePagePinItem
import com.whisperyao.dsplayer.util.SynoLog

class HomePagePinEditFragment : DialogFragment() {

    private lateinit var pinItem: HomePagePinItem

    private lateinit var nameInput: EditText
    private lateinit var descriptionText: TextView

    companion object {
        const val LOG = "HomePagePinEditFragment"
        fun newInstance(item: HomePagePinItem) =
            HomePagePinEditFragment().apply {
                arguments = item.toBundle()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, androidx.appcompat.R.style.Theme_AppCompat_Light_DialogWhenLarge)
        pinItem = HomePagePinItem.fromBundle(requireArguments())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        SynoLog.d(LOG, " onCreateView")
        val view = inflater.inflate(R.layout.fragment_pin_edit, container, false)
        setupViews(view)
        return view
    }

    private fun setupViews(view: View) {
        view.findViewById<Toolbar>(R.id.toolbar)
            .setTitle(R.string.pin_edit)

        nameInput = view.findViewById(R.id.pin_item_name)
        descriptionText = view.findViewById(R.id.pin_item_description)

        nameInput.setText(pinItem.title)
        descriptionText.text = buildDescription()

        view.findViewById<TextView>(R.id.btn_done).setOnClickListener {
            val newName = nameInput.text.toString().trim()

            when {
                newName.isEmpty() -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.pin_name)
                        .setMessage(R.string.pin_name_empty)
                        .setPositiveButton(R.string.str_ok, null)
                        .show()
                }

                newName == pinItem.title -> dismiss()

                else -> {
                    PinManager.getInstance().rename(pinItem.id, newName)
                    dismiss()
                }
            }
        }

        view.findViewById<View>(R.id.btn_cancel)
            .setOnClickListener { dismiss() }
    }

    override fun onResume() {
        super.onResume()
        nameInput.setSelection(nameInput.text.length)
    }

    /**
     * ✅ 重构后的描述生成逻辑
     */
    private fun buildDescription(): String {
        val type = pinItem.type
        val criteria = pinItem.criteria

        // === 特殊类型 ===
        when (type) {
            PinManager.TYPE_RANDOM_100 ->
                return getString(R.string.random_100)

            PinManager.TYPE_RECENTLY_ADDED ->
                return getString(R.string.latest_album)
        }

        if (type == "playlist") {
            val playlistId = criteria["playlist"].orEmpty()

            if (playlistId == PinManager.PLAYLIST_ID_SHARED_SONG) {
                return getString(R.string.share_shared_songs_playlist)
            }

            if ("path" in criteria) {
                return "${getString(R.string.path)}: ${criteria["path"]}\n"
            }

            val isNormal = PinManager.NORMAL.equals(criteria["type"], true)

            val prefix = if (isNormal) {
                getString(R.string.personal_playlist)
            } else {
                getString(R.string.smart_playlist)
            }

            return "$prefix: ${criteria["name"]}\n"
        }

        if (type == "folder") {
            return criteria["path"]?.let {
                "${getString(R.string.path)}: $it\n"
            }.orEmpty()
        }


        return buildString {

            fun appendLine(labelRes: Int, value: String?) {
                if (!value.isNullOrEmpty()) {
                    append(getString(labelRes))
                    append(": ")
                    append(value)
                    append("\n")
                }
            }

            fun appendLineWithFallback(
                labelRes: Int,
                value: String?,
                fallbackRes: Int
            ) {
                append(getString(labelRes))
                append(": ")
                append(
                    if (value.isNullOrEmpty())
                        getString(fallbackRes)
                    else
                        value
                )
                append("\n")
            }

            appendLine(R.string.album, criteria["album"])
            appendLine(R.string.album_artist, criteria["album_artist"])

            if ("artist" in criteria) {
                appendLineWithFallback(
                    R.string.artist,
                    criteria["artist"],
                    R.string.unknown_artist
                )
            }

            if ("composer" in criteria) {
                appendLineWithFallback(
                    R.string.composer,
                    criteria["composer"],
                    R.string.unknown_composer
                )
            }

            if ("genre" in criteria) {
                appendLineWithFallback(
                    R.string.genre,
                    criteria["genre"],
                    R.string.unknown_genre
                )
            }

            criteria[PinManager.GENRE_FILTER]?.let {
                append(getString(R.string.category_homepage_default_genre))
                append(": ")
                append(it)
                append("\n")
            }
        }
    }
}