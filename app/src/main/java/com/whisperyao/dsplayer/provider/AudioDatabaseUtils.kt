package com.whisperyao.dsplayer.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import com.whisperyao.dsplayer.PlaylistAdapter
import com.whisperyao.dsplayer.item.Item
import com.whisperyao.dsplayer.item.LocalPlaylistSongItem
import com.whisperyao.dsplayer.item.PlaylistItem
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.util.SynoLog
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
class AudioDatabaseUtils @Inject constructor(
    private val contentResolver: ContentResolver
) {

    companion object {
        private const val LOG_TAG = "AudioDatabaseUtils"
    }

    fun saveDownloadedPlaylists(playlistDsid: String, playlistId: String, playlistTitle: String) {
        val projection = AudioProvider.Localplaylist_Table.getProjection()
        val selection = AudioProvider.Localplaylist_Table.getSelection()
        val selectionArgs =
            AudioProvider.Localplaylist_Table.getSelectionArgs(playlistDsid, playlistId)

        val values = ContentValues().apply {
            put("dsid", playlistDsid)
            put(
                AudioProvider.Localplaylist_Table.Localplaylist_Column.SQL_PLAYLISTID,
                playlistId
            )
            put("title", playlistTitle)
        }

        val cursor = contentResolver.query(
            AudioProvider.CONTENT_URI_PLAYLISTS,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.count > 0) {
                contentResolver.update(
                    AudioProvider.CONTENT_URI_PLAYLISTS,
                    values,
                    selection,
                    selectionArgs
                )
            } else {
                contentResolver.insert(
                    AudioProvider.CONTENT_URI_PLAYLISTS,
                    values
                )
            }
            return
        }

        SynoLog.d(LOG_TAG, "saveDownloadedPlaylists query cursor null")
    }

    @SuppressLint("Range")
    fun loadDownloadedPlaylists(dsid: String?): LinkedList<PlaylistAdapter.UiPlaylistItem> {
        val result = LinkedList<PlaylistAdapter.UiPlaylistItem>()

        val projection = AudioProvider.Localplaylist_Table.getProjection()
        val sortOrder = AudioProvider.Localplaylist_Table.getSortOrder()

        val selection = if (dsid != null) "dsid=?" else null
        val selectionArgs = if (dsid != null) arrayOf(dsid) else null

        contentResolver.query(
            AudioProvider.CONTENT_URI_PLAYLISTS,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->

            while (cursor.moveToNext()) {
                val title = cursor.getString(cursor.getColumnIndex("title"))
                val itemDsid = cursor.getString(cursor.getColumnIndex("dsid"))
                val playlistId = cursor.getString(
                    cursor.getColumnIndex(
                        AudioProvider.Localplaylist_Table.Localplaylist_Column.SQL_PLAYLISTID
                    )
                )

                val playlistItem = PlaylistItem.generatePlaylistWithType(
                    Item.ItemType.LOCAL_PLAYLIST_NORMAL,
                    playlistId,
                    title
                ).apply {
                    setDsId(itemDsid)
                }

                result.add(
                    PlaylistAdapter.UiPlaylistItem.generatePlaylistItem(playlistItem)
                )
            }
        }

        return result
    }

    fun loadDownloadedPlaylistCount(dsid: String?): Int {
        val projection = AudioProvider.Localplaylist_Table.getProjection()
        val sortOrder = AudioProvider.Localplaylist_Table.getSortOrder()

        val selection = if (dsid != null) "dsid=?" else null
        val selectionArgs = if (dsid != null) arrayOf(dsid) else null

        return contentResolver.query(
            AudioProvider.CONTENT_URI_PLAYLISTS,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { it.count } ?: 0
    }

    fun deleteLocalPlaylist(playlistDsid: String, playlistId: String) {
        contentResolver.delete(
            AudioProvider.CONTENT_URI_PLAYLISTS,
            AudioProvider.Localplaylist_Table.getSelection(),
            AudioProvider.Localplaylist_Table.getSelectionArgs(
                playlistDsid,
                playlistId
            )
        )
    }

    fun saveSongsInDownloadPlaylist(playlistItem: PlaylistItem, songList: ArrayList<SongItem>) {
        val valuesArray = songList.map { song ->
            ContentValues().apply {
                put(
                    AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.PLAYLIST_DSID,
                    playlistItem.getDsId()
                )
                put(
                    AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.PLAYLIST_ID,
                    playlistItem.id
                )
                put(
                    AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.SONG_DSID,
                    song.dsId
                )
                put(
                    AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.SONG_PATH,
                    song.filePath
                )
                put(
                    AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column.SONG_ID,
                    song.id
                )
            }
        }.toTypedArray()

        contentResolver.bulkInsert(
            AudioProvider.CONTENT_URI_PLAYLISTS_SONGS_RELATION,
            valuesArray
        )
    }

    fun deleteLocalPlaylistSongRelation(songs: List<SongItem>) {
        val selection =
            AudioProvider.Localplaylist_Song_Relation_Table.getIDSelection()

        songs.forEach { song ->
            val relationId = (song as LocalPlaylistSongItem).getRelationId()

            contentResolver.delete(
                AudioProvider.CONTENT_URI_PLAYLISTS_SONGS_RELATION,
                selection,
                AudioProvider.Localplaylist_Song_Relation_Table.getIDSelectionArgs(
                    relationId
                )
            )
        }
    }

    fun deleteLocalPlaylistSongRelation(song: SongItem) {
        val relationId = (song as LocalPlaylistSongItem).relationId

        contentResolver.delete(
            AudioProvider.CONTENT_URI_PLAYLISTS_SONGS_RELATION,
            AudioProvider.Localplaylist_Song_Relation_Table.getIDSelection(),
            AudioProvider.Localplaylist_Song_Relation_Table.getIDSelectionArgs(relationId)
        )
    }

    fun deleteAllLocalSongsRelationByPlaylist(playlistItem: PlaylistItem) {
        contentResolver.delete(
            AudioProvider.CONTENT_URI_PLAYLISTS_SONGS_RELATION,
            AudioProvider.Localplaylist_Song_Relation_Table.getPlaylistSelection(),
            AudioProvider.Localplaylist_Table.getSelectionArgs(
                playlistItem.getDsId(),
                playlistItem.id
            )
        )
    }

    @SuppressLint("Range")
    fun getLocalPlaylistSongItemFromQueryCursor(cursor: Cursor): LocalPlaylistSongItem {
        return LocalPlaylistSongItem(
            Item.ItemType.FILE_MODE,
            cursor.getString(cursor.getColumnIndex(SongItem.SQL_SONGID)),
            cursor.getString(cursor.getColumnIndex("title"))
        ).apply {
            dsId = cursor.getString(cursor.getColumnIndex("dsid"))
            artist = cursor.getString(cursor.getColumnIndex("artist"))
            album = cursor.getString(cursor.getColumnIndex("album"))
            composer = cursor.getString(cursor.getColumnIndex("composer"))
            genre = cursor.getString(cursor.getColumnIndex("genre"))
            albumArtist = cursor.getString(cursor.getColumnIndex("album_artist"))
            filePath = cursor.getString(cursor.getColumnIndex("path"))
            cachePath = cursor.getString(cursor.getColumnIndex(SongItem.SQL_CACHEPATH))
            cacheBitrate = cursor.getLong(cursor.getColumnIndex(SongItem.SQL_CACHEBITRATE))
            comment = cursor.getString(cursor.getColumnIndex(SongItem.SQL_COMMENT))
            timeStamp = cursor.getLong(cursor.getColumnIndex(SongItem.SQL_TIMESTAMP))
            hitCount = cursor.getInt(cursor.getColumnIndex(SongItem.SQL_HITCOUNT))
            coverPath = cursor.getString(cursor.getColumnIndex(SongItem.SQL_COVER_PATH))
            lyricPath = cursor.getString(cursor.getColumnIndex(SongItem.SQL_LYRIC_PATH))
            disc = cursor.getInt(cursor.getColumnIndex(SongItem.SQL_DISC))
            track = cursor.getInt(cursor.getColumnIndex(SongItem.SQL_TRACK))
            year = cursor.getInt(cursor.getColumnIndex(SongItem.SQL_YEAR))
            duration = cursor.getInt(cursor.getColumnIndex("duration"))
            frequency = cursor.getInt(cursor.getColumnIndex(SongItem.SQL_FREQUENCY))
            channel = cursor.getInt(cursor.getColumnIndex(SongItem.SQL_CHANNEL))
            fileSize = cursor.getLong(cursor.getColumnIndex(SongItem.SQL_FILESIZE))
            bitrate = cursor.getLong(cursor.getColumnIndex(SongItem.SQL_BITRATE))
            rating = cursor.getFloat(cursor.getColumnIndex(SongItem.SQL_RATING))
            relationId = cursor.getInt(
                cursor.getColumnIndex(
                    AudioProvider.Localplaylist_Song_Relation_Table.Local_Relation_Column._RELATION_ID
                )
            )
        }
    }

    fun doEnumLocalPlaylistSongs(playlistDsid: String, playlistId: String, playlistTitle: String): ArrayList<SongItem> {
        val result = ArrayList<SongItem>()

        val selectionArgs = arrayOf(playlistDsid, playlistId)

        contentResolver.query(
            AudioProvider.CONTENT_URI_ENUM_PLAYLIST_SONGS,
            null,
            null,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                result.add(getLocalPlaylistSongItemFromQueryCursor(cursor))
            }
        }

        return result
    }
}