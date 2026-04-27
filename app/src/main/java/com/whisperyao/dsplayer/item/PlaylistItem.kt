package com.whisperyao.dsplayer.item

import android.os.Bundle
import com.whisperyao.dsplayer.App.Companion.getContext
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.vos.base.BasePlaylistResponseVo.BasePlaylistVo
import com.whisperyao.dsplayer.vos.base.BaseSharingInfoVo
import com.whisperyao.dsplayer.vos.base.BaseSharingInfoVo.SharingStatusVo
import java.util.Date


class PlaylistItem private constructor(
    type: ItemType,
    id: String?,
    title: String?
) : Item(type, id, title) {

    companion object {
        const val DSID = "dsid"
        private const val PLAYLIST_ID_SHARED_SONG = "playlist_personal_normal/__SYNO_AUDIO_SHARED_SONGS__"
        private const val PREDEFINED_TYPE = "predefined_type"

        @JvmStatic
        fun generatePlaylistWithType(
            type: ItemType,
            id: String?,
            title: String?
        ): PlaylistItem {
            return generatePlaylistWithType(type, PredefinedType.none, id, title)
        }

        @JvmStatic
        fun generateByPlaylistVo(playlistVo: BasePlaylistVo): PlaylistItem {
            val id = playlistVo.getID()
            var name = playlistVo.getName()
            val zIsOldVersion = playlistVo.isOldVersion()
            val zIsPersonal = playlistVo.isPersonal()
            val zIsNormal = playlistVo.isNormal()
            val zIsSmart = playlistVo.isSmart()
            var itemType = ItemType.PERSONAL_NORMAL_NEW
            if (zIsOldVersion) {
                if (zIsPersonal) {
                    if (zIsNormal) {
                        itemType = ItemType.PERSONAL_NORMAL_OLD
                    } else if (zIsSmart) {
                        itemType = ItemType.PERSONAL_SMART_OLD
                    }
                } else if (zIsNormal) {
                    itemType = ItemType.SHARED_NORMAL_OLD
                } else if (zIsSmart) {
                    itemType = ItemType.SHARED_SMART_OLD
                }
            } else if (zIsPersonal) {
                if (zIsNormal) {
                    itemType = ItemType.PERSONAL_NORMAL_NEW
                } else if (zIsSmart) {
                    itemType = ItemType.PERSONAL_SMART_NEW
                }
            } else if (zIsNormal) {
                itemType = ItemType.SHARED_NORMAL_NEW
            } else if (zIsSmart) {
                itemType = ItemType.SHARED_SMART_NEW
            }
            var predefinedType: PredefinedType? = PredefinedType.none
            if (playlistVo.isRandom()) {
                predefinedType = PredefinedType.random
            }
            if (playlistVo.isSharedSongs()) {
                predefinedType = PredefinedType.shared_song
            }
            if (playlistVo.isSharedSongs()) {
                name = getContext().getString(R.string.share_shared_songs_playlist)
            }
            val playlistItem = PlaylistItem(itemType, id, name)
            playlistItem.setSharingInfo(playlistVo.sharingStatus, playlistVo.sharingInfo)
            playlistItem.mPredefinedType = predefinedType!!
            return playlistItem
        }

        private fun generatePlaylistWithType(
            type: Item.ItemType,
            predefinedType: PredefinedType,
            id: String?,
            title: String?
        ): PlaylistItem {
            val item = PlaylistItem(type, id, title)
            item.mPredefinedType = predefinedType
            return item
        }

        fun fromBundle(bundle: Bundle): PlaylistItem {
            val type = ItemType.valueOf(bundle.getString("type")!!)
            val id = bundle.getString("id")
            val title = bundle.getString("title")
            val predefinedStr = bundle.getString(PREDEFINED_TYPE)
            val dsid = bundle.getString("dsid")

            var predefined = PredefinedType.none
            if (predefinedStr != null) {
                try {
                    predefined = PredefinedType.valueOf(predefinedStr)
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }

            val item = generatePlaylistWithType(type, predefined, id, title)
            if (dsid != null) {
                item.setDsId(dsid)
            }
            return item
        }

        fun generatePredifinedPlaylist(type: ItemType?, id: String?, title: String?): PlaylistItem {
            val playlistItem = PlaylistItem(type!!, id, title)
            var predefinedType: PredefinedType? = PredefinedType.none
            if (Common.CAT_RANDOM100_ID == id) {
                predefinedType = PredefinedType.random
            }
            // LocalEnumerator.MOST_OFTEN_PLAYED
            else if ("[__MOST_RECENT_ADDED__]" == id) {
                predefinedType = PredefinedType.most_played
            }
            // LocalEnumerator.MOST_RECENT_PLAYED
            else if ("[__MOST_FREQUENT_LISTEN__]" == id) {
                predefinedType = PredefinedType.recently_played
            } else if (Common.CAT_RATING == id) {
                predefinedType = PredefinedType.rating
            } else if ("playlist_personal_normal/__SYNO_AUDIO_SHARED_SONGS__" == id) {
                predefinedType = PredefinedType.shared_song
            } else if (Common.CAT_RECENTLY_ADDED == id) {
                predefinedType = PredefinedType.recently_added
            }
            playlistItem.mPredefinedType = predefinedType!!
            return playlistItem
        }

    }

    private var mDsId: String = ""
    private var mPredefinedType: PredefinedType = PredefinedType.none
    private var mSharingStatus: SharingStatus? = null

    private var mSharingInfo: SharingInfo? = null

    private enum class PredefinedType {
        none,
        random,
        most_played,
        recently_played,
        rating,
        shared_song,
        recently_added
    }

    private enum class SharingStatus {
        none,
        invalid,
        valid,
        expired
    }

    fun setDsId(id: String?) {
        mDsId = id ?: ""
    }

    fun getDsId(): String = mDsId

    fun isLocal(): Boolean =
        type == ItemType.LOCAL_PLAYLIST_NORMAL

    fun isCanPinnedPlaylist(): Boolean =
        !(isRandom() || isMostPlayed() || isRecentPlayed() || isRating() || isLocal())

    fun isPredefined(): Boolean = isRandom() || isMostPlayed() || isRecentPlayed() ||
                isRating() || isSharedSong() || isRecentlyAdded()

    fun isRandom() = mPredefinedType == PredefinedType.random
    fun isRecentlyAdded() = mPredefinedType == PredefinedType.recently_added
    fun isMostPlayed() = mPredefinedType == PredefinedType.most_played
    fun isRecentPlayed() = mPredefinedType == PredefinedType.recently_played
    fun isRating() = mPredefinedType == PredefinedType.rating
    fun isSharedSong() = mPredefinedType == PredefinedType.shared_song

    fun isWithQuickAction(): Boolean =
        type.isPlayListItem || type.isContainer

    override fun isPersonal(): Boolean = type.isPersonal

    fun isNormal(): Boolean = type.isNormalPls

    fun hasNewPlaylistID(): Boolean = type.hasNewPlaylistID()

    private fun setSharingInfo(sharingStatusInfo: SharingStatusVo, sharingInfoVo: BaseSharingInfoVo?) {
        this.mSharingInfo = SharingInfo(sharingInfoVo)
        if (sharingStatusInfo.isNone()) {
            setAsNoneSharing()
        }
        if (sharingStatusInfo.isInvalid()) {
            setAsInvalidSharing()
        }
        if (sharingStatusInfo.isValid()) {
            setAsValidSharing()
        }
        if (sharingStatusInfo.isExpired()) {
            setAsExpiredSharing()
        }
    }

    class SharingInfo internal constructor(sharingInfoVo: BaseSharingInfoVo?) {
        private var mAvailableDate: Date? = null
        private var mExpiredDate: Date? = null
        private var mUrl: String? = null

        init {
            if (sharingInfoVo != null) {
                this.mUrl = sharingInfoVo.getUrl()
                this.mAvailableDate = sharingInfoVo.getAvailableDate()
                this.mExpiredDate = sharingInfoVo.getExpiredDate()
            }
        }
    }

    private fun setAsNoneSharing() {
        mSharingStatus = SharingStatus.none
    }

    private fun setAsInvalidSharing() {
        mSharingStatus = SharingStatus.invalid
    }

    private fun setAsValidSharing() {
        mSharingStatus = SharingStatus.valid
    }

    private fun setAsExpiredSharing() {
        mSharingStatus = SharingStatus.expired
    }

    fun isWithSharing(): Boolean =
        mSharingStatus != SharingStatus.none

    fun getIconResId(): Int {
        return when {
            isRandom() -> R.drawable.thumbnail_100
            isMostPlayed() -> R.drawable.thumbnail_often
            isRecentPlayed() -> R.drawable.thumbnail_recently
            isRating() -> R.drawable.thumbnail_rating
            isSharedSong() -> R.drawable.thumbnail_shared
            isRecentlyAdded() -> R.drawable.thumbnail_recently_add
            isLocal() -> R.drawable.thumbnail_playlist
            mSharingStatus == null -> {
                if (isNormal()) R.drawable.thumbnail_playlist
                else R.drawable.thumbnail_smart_playlist
            }
            else -> {
                if (isNormal()) {
                    when (mSharingStatus) {
                        SharingStatus.valid -> R.drawable.thumbnail_playlist_sharing
                        SharingStatus.invalid,
                        SharingStatus.expired -> R.drawable.thumbnail_playlist_shared_fail
                        else -> R.drawable.thumbnail_playlist
                    }
                } else {
                    when (mSharingStatus) {
                        SharingStatus.valid -> R.drawable.thumbnail_smart_playlist_sharing
                        SharingStatus.invalid,
                        SharingStatus.expired -> R.drawable.thumbnail_smart_playlist_shared_fail
                        else -> R.drawable.thumbnail_smart_playlist
                    }
                }
            }
        }
    }

    fun MyEqual(other: PlaylistItem): Boolean {
        if (isLocal() != other.isLocal()) return false
        if (isLocal()) return getDsId() == other.getDsId() && id.equals(other.id)
        return id.equals(other.id)
    }
}