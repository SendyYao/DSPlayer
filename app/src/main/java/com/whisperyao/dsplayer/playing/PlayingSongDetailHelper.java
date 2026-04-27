package com.whisperyao.dsplayer.playing;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.whisperyao.dsplayer.R;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.StateManager;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.model.data.PlayingQueueManager;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.widget.RatingBar;
import java.util.ArrayList;


public class PlayingSongDetailHelper {
    private TextView mAlbumText;
    private TextView mArtistText;
    private SimpleDraweeView mCoverImage;
    private boolean mIsForPhone;
    private RatingBar mRatingBar;
    private SongItem mSong;
    private TextView mTitleText;
    private PlayingQueueManager playingQueueManager;
    private String unknownAlbum;
    private String unknownArtist;

    public PlayingSongDetailHelper() {
    }

    public PlayingSongDetailHelper(Context context, SimpleDraweeView imCover, TextView tvAlbum, TextView tvArtist, TextView tvTitle, RatingBar rbRating) {
        this.unknownArtist = context.getString(R.string.unknown_artist);
        this.unknownAlbum = context.getString(R.string.unknown_album);
        this.mCoverImage = imCover;
        this.mAlbumText = tvAlbum;
        this.mArtistText = tvArtist;
        this.mTitleText = tvTitle;
        this.mRatingBar = rbRating;
        if (rbRating != null) {
            rbRating.setVisibility(4);
            this.mRatingBar.setOnRatingChangeListener((ratingBar, rating, fromUser) -> {
                if (!fromUser || PlayingSongDetailHelper.this.mSong == null) {
                    return;
                }
                int i = (int) rating;
                PlayingSongDetailHelper.this.mSong.setSongRating(i);
                ArrayList<SongItem> arrayList = new ArrayList<>();
                arrayList.add(PlayingSongDetailHelper.this.mSong);
                CacheManager.getInstance().recordRatingMapFromUser(arrayList);
                CacheManager.getInstance().requestRecordRatingFromUser(arrayList);
                PlayingSongDetailHelper playingSongDetailHelper = PlayingSongDetailHelper.this;
                playingSongDetailHelper.updateSongInfo(playingSongDetailHelper.mSong);
                CacheManager.getInstance().requestRatingSongs(arrayList, i);
            });
        }
        updateCover();
    }

    private void updateCover() {
        if (this.mCoverImage == null) {
            return;
        }
        PlayingQueueManager playingQueueManager = this.playingQueueManager;
        if (playingQueueManager == null || playingQueueManager.getSongItem() == null) {
            this.mCoverImage.setImageResource(R.drawable.thumbnail_song);
        } else {
            SimpleDraweeView simpleDraweeView = this.mCoverImage;
            simpleDraweeView.setImageBitmap(this.playingQueueManager.getAlbumBitmap(simpleDraweeView.getContext(), this.playingQueueManager.getSongItem().getMediaId()));
        }
    }

    public void setIsForPhone(boolean isForPhone) {
        this.mIsForPhone = isForPhone;
    }

    public void updateSongInfo(SongItem song) {
        this.mSong = song;
        if (this.mTitleText == null && this.mAlbumText == null && this.mArtistText == null && this.mCoverImage == null) {
            return;
        }
        if (song == null) {
            this.mArtistText.setText(null);
            this.mAlbumText.setText(null);
            this.mTitleText.setText(null);
            this.mRatingBar.setVisibility(4);
            this.mRatingBar.setRating(0.0f);
            return;
        }
        if (song.getArtist().isEmpty()) {
            Utilities.setTextIfNeeded(this.mArtistText, StateManager.getInstance().isMobileLayout() ? "" : this.unknownArtist);
        } else {
            String artist = song.getArtist();
            if (this.mIsForPhone) {
                String composer = song.getComposer();
                if (TextUtils.isEmpty(artist)) {
                    artist = composer;
                } else if (!TextUtils.isEmpty(composer)) {
                    artist = artist + " / " + composer;
                }
            }
            Utilities.setTextIfNeeded(this.mArtistText, artist);
        }
        if (song.getAlbum().isEmpty()) {
            Utilities.setTextIfNeeded(this.mAlbumText, StateManager.getInstance().isMobileLayout() ? "" : this.unknownAlbum);
        } else {
            Utilities.setTextIfNeeded(this.mAlbumText, song.getAlbum());
        }
        Utilities.setTextIfNeeded(this.mTitleText, song.getTitle());
        updateCover();
        boolean zIsWithRating = song.isWithRating();
        boolean zCanEditRating = ConnectionManager.canEditRating(Common.isLogin(), song);
        this.mRatingBar.setVisibility(zIsWithRating ? 0 : 4);
        this.mRatingBar.setIsIndicator(!zCanEditRating);
        this.mRatingBar.setRating(song.getRating());
    }

    public void setPlayingQueueManager(PlayingQueueManager playingQueueManager) {
        this.playingQueueManager = playingQueueManager;
    }
}