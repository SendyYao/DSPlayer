package com.whisperyao.dsplayer.model.flatbuffers;

import com.google.flatbuffers.BaseVector;
import com.google.flatbuffers.Constants;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Song extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_25_2_10();
    }

    public static Song getRootAsSong(ByteBuffer _bb) {
        return getRootAsSong(_bb, new Song());
    }

    public static Song getRootAsSong(ByteBuffer _bb, Song obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb);
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public Song __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public String mId() {
        int i__offset = __offset(4);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mIdAsByteBuffer() {
        return __vector_as_bytebuffer(4, 1);
    }

    public ByteBuffer mIdInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 4, 1);
    }

    public String mTitle() {
        int i__offset = __offset(6);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mTitleAsByteBuffer() {
        return __vector_as_bytebuffer(6, 1);
    }

    public ByteBuffer mTitleInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 6, 1);
    }

    public float mRating() {
        int i__offset = __offset(8);
        if (i__offset != 0) {
            return this.bb.getFloat(i__offset + this.bb_pos);
        }
        return 0.0f;
    }

    public String mAlbumArtist() {
        int i__offset = __offset(10);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mAlbumArtistAsByteBuffer() {
        return __vector_as_bytebuffer(10, 1);
    }

    public ByteBuffer mAlbumArtistInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 10, 1);
    }

    public boolean mMarked() {
        int i__offset = __offset(12);
        return (i__offset == 0 || this.bb.get(i__offset + this.bb_pos) == 0) ? false : true;
    }

    public String mType() {
        int i__offset = __offset(14);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mTypeAsByteBuffer() {
        return __vector_as_bytebuffer(14, 1);
    }

    public ByteBuffer mTypeInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 14, 1);
    }

    public boolean mAllSongs() {
        int i__offset = __offset(16);
        return (i__offset == 0 || this.bb.get(i__offset + this.bb_pos) == 0) ? false : true;
    }

    public String mDsId() {
        int i__offset = __offset(18);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mDsIdAsByteBuffer() {
        return __vector_as_bytebuffer(18, 1);
    }

    public ByteBuffer mDsIdInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 18, 1);
    }

    public String mArtist() {
        int i__offset = __offset(20);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mArtistAsByteBuffer() {
        return __vector_as_bytebuffer(20, 1);
    }

    public ByteBuffer mArtistInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 20, 1);
    }

    public String mAlbum() {
        int i__offset = __offset(22);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mAlbumAsByteBuffer() {
        return __vector_as_bytebuffer(22, 1);
    }

    public ByteBuffer mAlbumInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 22, 1);
    }

    public String mComposer() {
        int i__offset = __offset(24);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mComposerAsByteBuffer() {
        return __vector_as_bytebuffer(24, 1);
    }

    public ByteBuffer mComposerInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 24, 1);
    }

    public String mGenre() {
        int i__offset = __offset(26);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mGenreAsByteBuffer() {
        return __vector_as_bytebuffer(26, 1);
    }

    public ByteBuffer mGenreInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 26, 1);
    }

    public String mFilePath() {
        int i__offset = __offset(28);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mFilePathAsByteBuffer() {
        return __vector_as_bytebuffer(28, 1);
    }

    public ByteBuffer mFilePathInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 28, 1);
    }

    public String mCachePath() {
        int i__offset = __offset(30);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mCachePathAsByteBuffer() {
        return __vector_as_bytebuffer(30, 1);
    }

    public ByteBuffer mCachePathInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 30, 1);
    }

    public String mCoverPath() {
        int i__offset = __offset(32);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mCoverPathAsByteBuffer() {
        return __vector_as_bytebuffer(32, 1);
    }

    public ByteBuffer mCoverPathInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 32, 1);
    }

    public String mLyricPath() {
        int i__offset = __offset(34);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mLyricPathAsByteBuffer() {
        return __vector_as_bytebuffer(34, 1);
    }

    public ByteBuffer mLyricPathInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 34, 1);
    }

    public int mDownloadType() {
        int i__offset = __offset(36);
        if (i__offset != 0) {
            return this.bb.getInt(i__offset + this.bb_pos);
        }
        return 0;
    }

    public String mComment() {
        int i__offset = __offset(38);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mCommentAsByteBuffer() {
        return __vector_as_bytebuffer(38, 1);
    }

    public ByteBuffer mCommentInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 38, 1);
    }

    public String mCoverUrl() {
        int i__offset = __offset(40);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mCoverUrlAsByteBuffer() {
        return __vector_as_bytebuffer(40, 1);
    }

    public ByteBuffer mCoverUrlInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 40, 1);
    }

    public String mSongUrl() {
        int i__offset = __offset(42);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mSongUrlAsByteBuffer() {
        return __vector_as_bytebuffer(42, 1);
    }

    public ByteBuffer mSongUrlInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 42, 1);
    }

    public int mDisc() {
        int i__offset = __offset(44);
        if (i__offset != 0) {
            return this.bb.getInt(i__offset + this.bb_pos);
        }
        return 0;
    }

    public int mTrack() {
        int i__offset = __offset(46);
        if (i__offset != 0) {
            return this.bb.getInt(i__offset + this.bb_pos);
        }
        return 0;
    }

    public int mYear() {
        int i__offset = __offset(48);
        if (i__offset != 0) {
            return this.bb.getInt(i__offset + this.bb_pos);
        }
        return 0;
    }

    public int mDuration() {
        int i__offset = __offset(50);
        if (i__offset != 0) {
            return this.bb.getInt(i__offset + this.bb_pos);
        }
        return 0;
    }

    public int mFrequency() {
        int i__offset = __offset(52);
        if (i__offset != 0) {
            return this.bb.getInt(i__offset + this.bb_pos);
        }
        return 0;
    }

    public int mChannel() {
        int i__offset = __offset(54);
        if (i__offset != 0) {
            return this.bb.getInt(i__offset + this.bb_pos);
        }
        return 0;
    }

    public int mHitCount() {
        int i__offset = __offset(56);
        if (i__offset != 0) {
            return this.bb.getInt(i__offset + this.bb_pos);
        }
        return 0;
    }

    public long mTimeStamp() {
        int i__offset = __offset(58);
        if (i__offset != 0) {
            return this.bb.getLong(i__offset + this.bb_pos);
        }
        return 0L;
    }

    public long mFileSize() {
        int i__offset = __offset(60);
        if (i__offset != 0) {
            return this.bb.getLong(i__offset + this.bb_pos);
        }
        return 0L;
    }

    public long mBitrate() {
        int i__offset = __offset(62);
        if (i__offset != 0) {
            return this.bb.getLong(i__offset + this.bb_pos);
        }
        return 0L;
    }

    public long mCacheBitrate() {
        int i__offset = __offset(64);
        if (i__offset != 0) {
            return this.bb.getLong(i__offset + this.bb_pos);
        }
        return 0L;
    }

    public String mStreamId() {
        int i__offset = __offset(66);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mStreamIdAsByteBuffer() {
        return __vector_as_bytebuffer(66, 1);
    }

    public ByteBuffer mStreamIdInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 66, 1);
    }

    public String mFormat() {
        int i__offset = __offset(68);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mFormatAsByteBuffer() {
        return __vector_as_bytebuffer(68, 1);
    }

    public ByteBuffer mFormatInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 68, 1);
    }

    public String mCodec() {
        int i__offset = __offset(70);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mCodecAsByteBuffer() {
        return __vector_as_bytebuffer(70, 1);
    }

    public ByteBuffer mCodecInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 70, 1);
    }

    public String mContainer() {
        int i__offset = __offset(72);
        if (i__offset != 0) {
            return __string(i__offset + this.bb_pos);
        }
        return null;
    }

    public ByteBuffer mContainerAsByteBuffer() {
        return __vector_as_bytebuffer(72, 1);
    }

    public ByteBuffer mContainerInByteBuffer(ByteBuffer _bb) {
        return __vector_in_bytebuffer(_bb, 72, 1);
    }

    public static int createSong(FlatBufferBuilder builder, int mIdOffset, int mTitleOffset, float mRating, int mAlbumArtistOffset, boolean mMarked, int mTypeOffset, boolean mAllSongs, int mDsIdOffset, int mArtistOffset, int mAlbumOffset, int mComposerOffset, int mGenreOffset, int mFilePathOffset, int mCachePathOffset, int mCoverPathOffset, int mLyricPathOffset, int mDownloadType, int mCommentOffset, int mCoverUrlOffset, int mSongUrlOffset, int mDisc, int mTrack, int mYear, int mDuration, int mFrequency, int mChannel, int mHitCount, long mTimeStamp, long mFileSize, long mBitrate, long mCacheBitrate, int mStreamIdOffset, int mFormatOffset, int mCodecOffset, int mContainerOffset) {
        builder.startTable(35);
        addMCacheBitrate(builder, mCacheBitrate);
        addMBitrate(builder, mBitrate);
        addMFileSize(builder, mFileSize);
        addMTimeStamp(builder, mTimeStamp);
        addMContainer(builder, mContainerOffset);
        addMCodec(builder, mCodecOffset);
        addMFormat(builder, mFormatOffset);
        addMStreamId(builder, mStreamIdOffset);
        addMHitCount(builder, mHitCount);
        addMChannel(builder, mChannel);
        addMFrequency(builder, mFrequency);
        addMDuration(builder, mDuration);
        addMYear(builder, mYear);
        addMTrack(builder, mTrack);
        addMDisc(builder, mDisc);
        addMSongUrl(builder, mSongUrlOffset);
        addMCoverUrl(builder, mCoverUrlOffset);
        addMComment(builder, mCommentOffset);
        addMDownloadType(builder, mDownloadType);
        addMLyricPath(builder, mLyricPathOffset);
        addMCoverPath(builder, mCoverPathOffset);
        addMCachePath(builder, mCachePathOffset);
        addMFilePath(builder, mFilePathOffset);
        addMGenre(builder, mGenreOffset);
        addMComposer(builder, mComposerOffset);
        addMAlbum(builder, mAlbumOffset);
        addMArtist(builder, mArtistOffset);
        addMDsId(builder, mDsIdOffset);
        addMType(builder, mTypeOffset);
        addMAlbumArtist(builder, mAlbumArtistOffset);
        addMRating(builder, mRating);
        addMTitle(builder, mTitleOffset);
        addMId(builder, mIdOffset);
        addMAllSongs(builder, mAllSongs);
        addMMarked(builder, mMarked);
        return endSong(builder);
    }

    public static void startSong(FlatBufferBuilder builder) {
        builder.startTable(35);
    }

    public static void addMId(FlatBufferBuilder builder, int mIdOffset) {
        builder.addOffset(0, mIdOffset, 0);
    }

    public static void addMTitle(FlatBufferBuilder builder, int mTitleOffset) {
        builder.addOffset(1, mTitleOffset, 0);
    }

    public static void addMRating(FlatBufferBuilder builder, float mRating) {
        builder.addFloat(2, mRating, 0.0d);
    }

    public static void addMAlbumArtist(FlatBufferBuilder builder, int mAlbumArtistOffset) {
        builder.addOffset(3, mAlbumArtistOffset, 0);
    }

    public static void addMMarked(FlatBufferBuilder builder, boolean mMarked) {
        builder.addBoolean(4, mMarked, false);
    }

    public static void addMType(FlatBufferBuilder builder, int mTypeOffset) {
        builder.addOffset(5, mTypeOffset, 0);
    }

    public static void addMAllSongs(FlatBufferBuilder builder, boolean mAllSongs) {
        builder.addBoolean(6, mAllSongs, false);
    }

    public static void addMDsId(FlatBufferBuilder builder, int mDsIdOffset) {
        builder.addOffset(7, mDsIdOffset, 0);
    }

    public static void addMArtist(FlatBufferBuilder builder, int mArtistOffset) {
        builder.addOffset(8, mArtistOffset, 0);
    }

    public static void addMAlbum(FlatBufferBuilder builder, int mAlbumOffset) {
        builder.addOffset(9, mAlbumOffset, 0);
    }

    public static void addMComposer(FlatBufferBuilder builder, int mComposerOffset) {
        builder.addOffset(10, mComposerOffset, 0);
    }

    public static void addMGenre(FlatBufferBuilder builder, int mGenreOffset) {
        builder.addOffset(11, mGenreOffset, 0);
    }

    public static void addMFilePath(FlatBufferBuilder builder, int mFilePathOffset) {
        builder.addOffset(12, mFilePathOffset, 0);
    }

    public static void addMCachePath(FlatBufferBuilder builder, int mCachePathOffset) {
        builder.addOffset(13, mCachePathOffset, 0);
    }

    public static void addMCoverPath(FlatBufferBuilder builder, int mCoverPathOffset) {
        builder.addOffset(14, mCoverPathOffset, 0);
    }

    public static void addMLyricPath(FlatBufferBuilder builder, int mLyricPathOffset) {
        builder.addOffset(15, mLyricPathOffset, 0);
    }

    public static void addMDownloadType(FlatBufferBuilder builder, int mDownloadType) {
        builder.addInt(16, mDownloadType, 0);
    }

    public static void addMComment(FlatBufferBuilder builder, int mCommentOffset) {
        builder.addOffset(17, mCommentOffset, 0);
    }

    public static void addMCoverUrl(FlatBufferBuilder builder, int mCoverUrlOffset) {
        builder.addOffset(18, mCoverUrlOffset, 0);
    }

    public static void addMSongUrl(FlatBufferBuilder builder, int mSongUrlOffset) {
        builder.addOffset(19, mSongUrlOffset, 0);
    }

    public static void addMDisc(FlatBufferBuilder builder, int mDisc) {
        builder.addInt(20, mDisc, 0);
    }

    public static void addMTrack(FlatBufferBuilder builder, int mTrack) {
        builder.addInt(21, mTrack, 0);
    }

    public static void addMYear(FlatBufferBuilder builder, int mYear) {
        builder.addInt(22, mYear, 0);
    }

    public static void addMDuration(FlatBufferBuilder builder, int mDuration) {
        builder.addInt(23, mDuration, 0);
    }

    public static void addMFrequency(FlatBufferBuilder builder, int mFrequency) {
        builder.addInt(24, mFrequency, 0);
    }

    public static void addMChannel(FlatBufferBuilder builder, int mChannel) {
        builder.addInt(25, mChannel, 0);
    }

    public static void addMHitCount(FlatBufferBuilder builder, int mHitCount) {
        builder.addInt(26, mHitCount, 0);
    }

    public static void addMTimeStamp(FlatBufferBuilder builder, long mTimeStamp) {
        builder.addLong(27, mTimeStamp, 0L);
    }

    public static void addMFileSize(FlatBufferBuilder builder, long mFileSize) {
        builder.addLong(28, mFileSize, 0L);
    }

    public static void addMBitrate(FlatBufferBuilder builder, long mBitrate) {
        builder.addLong(29, mBitrate, 0L);
    }

    public static void addMCacheBitrate(FlatBufferBuilder builder, long mCacheBitrate) {
        builder.addLong(30, mCacheBitrate, 0L);
    }

    public static void addMStreamId(FlatBufferBuilder builder, int mStreamIdOffset) {
        builder.addOffset(31, mStreamIdOffset, 0);
    }

    public static void addMFormat(FlatBufferBuilder builder, int mFormatOffset) {
        builder.addOffset(32, mFormatOffset, 0);
    }

    public static void addMCodec(FlatBufferBuilder builder, int mCodecOffset) {
        builder.addOffset(33, mCodecOffset, 0);
    }

    public static void addMContainer(FlatBufferBuilder builder, int mContainerOffset) {
        builder.addOffset(34, mContainerOffset, 0);
    }

    public static int endSong(FlatBufferBuilder builder) {
        return builder.endTable();
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public Song get(int j) {
            return get(new Song(), j);
        }

        public Song get(Song obj, int j) {
            return obj.__assign(Song.__indirect(__element(j), this.bb), this.bb);
        }
    }
}