package com.whisperyao.dsplayer.util;

import com.google.flatbuffers.FlatBufferBuilder;
import com.whisperyao.dsplayer.item.Item;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.model.flatbuffers.Song;
import com.whisperyao.dsplayer.model.flatbuffers.SongsList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes2.dex */
public class FlatBufferUtil {
    private static final String LOG_TAG = "FlatBufferUtil";

    public static ByteBuffer getByteBufferOfSongItems(final ArrayList<SongItem> songs) {
        FlatBufferBuilder flatBufferBuilder = new FlatBufferBuilder(1024);
        ArrayList arrayList = new ArrayList(songs);
        int[] iArr = new int[arrayList.size()];
        Iterator it = arrayList.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = fullSongTransfer(flatBufferBuilder, (SongItem) it.next());
            i++;
        }
        int iCreateSongsVector = SongsList.createSongsVector(flatBufferBuilder, iArr);
        SongsList.startSongsList(flatBufferBuilder);
        SongsList.addSongs(flatBufferBuilder, iCreateSongsVector);
        flatBufferBuilder.finish(SongsList.endSongsList(flatBufferBuilder));
        return flatBufferBuilder.dataBuffer();
    }

    private static int fullSongTransfer(FlatBufferBuilder builder, SongItem songItem) {
        int i;
        String codec;
        int iCreateString = builder.createString(songItem.getID());
        int iCreateString2 = builder.createString(songItem.getTitle());
        float rating = songItem.getRating();
        int iCreateString3 = builder.createString(songItem.getAlbumArtist());
        int iCreateString4 = builder.createString(songItem.getType().toString());
        songItem.isAllSongs();
        int iCreateString5 = builder.createString(songItem.getDsId());
        int iCreateString6 = builder.createString(songItem.getArtist());
        int iCreateString7 = builder.createString(songItem.getAlbum());
        int iCreateString8 = builder.createString(songItem.getComposer());
        int iCreateString9 = builder.createString(songItem.getGenre());
        int iCreateString10 = builder.createString(songItem.getFilePath());
        int downloadType = songItem.getDownloadType();
        int iCreateString11 = builder.createString(songItem.getComment());
        int disc = songItem.getDisc();
        int track = songItem.getTrack();
        int year = songItem.getYear();
        int duration = songItem.getDuration();
        int frequency = songItem.getFrequency();
        int channel = songItem.getChannel();
        int hitCount = songItem.getHitCount();
        long timeStamp = songItem.getTimeStamp();
        long fileSize = songItem.getFileSize();
        long bitrate = songItem.getBitrate();
        long cacheBitrate = songItem.getCacheBitrate();
        int iCreateString12 = builder.createString(songItem.getStreamId());
        int iCreateString13 = builder.createString(songItem.getCachePath() == null ? "" : songItem.getCachePath());
        int iCreateString14 = builder.createString(songItem.getCoverPath());
        if (songItem.getCodec() == null) {
            i = iCreateString14;
            codec = "";
        } else {
            i = iCreateString14;
            codec = songItem.getCodec();
        }
        int iCreateString15 = builder.createString(codec);
        int iCreateString16 = builder.createString(songItem.getContainer() != null ? songItem.getContainer() : "");
        Song.startSong(builder);
        Song.addMId(builder, iCreateString);
        Song.addMTitle(builder, iCreateString2);
        Song.addMRating(builder, rating);
        Song.addMAlbumArtist(builder, iCreateString3);
        Song.addMType(builder, iCreateString4);
        Song.addMDsId(builder, iCreateString5);
        Song.addMArtist(builder, iCreateString6);
        Song.addMAlbum(builder, iCreateString7);
        Song.addMComposer(builder, iCreateString8);
        Song.addMGenre(builder, iCreateString9);
        Song.addMFilePath(builder, iCreateString10);
        Song.addMDownloadType(builder, downloadType);
        Song.addMComment(builder, iCreateString11);
        Song.addMDisc(builder, disc);
        Song.addMTrack(builder, track);
        Song.addMYear(builder, year);
        Song.addMDuration(builder, duration);
        Song.addMFrequency(builder, frequency);
        Song.addMChannel(builder, channel);
        Song.addMHitCount(builder, hitCount);
        Song.addMTimeStamp(builder, timeStamp);
        Song.addMFileSize(builder, fileSize);
        Song.addMBitrate(builder, bitrate);
        Song.addMCacheBitrate(builder, cacheBitrate);
        Song.addMStreamId(builder, iCreateString12);
        Song.addMCachePath(builder, iCreateString13);
        Song.addMCoverPath(builder, i);
        Song.addMCodec(builder, iCreateString15);
        Song.addMContainer(builder, iCreateString16);
        return Song.endSong(builder);
    }

    public static SongItem[] loadQueueFromFile(final File file) {
        SongItem[] songItemArr = new SongItem[0];
        try {
            SongsList rootAsSongsList = SongsList.getRootAsSongsList(ByteBuffer.wrap(loadBytesBlocking(file)));
            SongItem[] songItemArr2 = new SongItem[rootAsSongsList.songsLength()];
            for (int i = 0; i < rootAsSongsList.songsLength(); i++) {
                songItemArr2[i] = songFlatBufferToSongItem(rootAsSongsList.songs(i));
            }
            return songItemArr2;
        } catch (IOException e) {
            e.printStackTrace();
            return songItemArr;
        } catch (IndexOutOfBoundsException e2) {
            e2.printStackTrace();
            SynoLog.e(LOG_TAG, " IndexOutOfBoundsException e: " + e2.toString());
            return songItemArr;
        } catch (Exception e3) {
            e3.printStackTrace();
            SynoLog.e(LOG_TAG, " Exception e: " + e3.toString());
            return songItemArr;
        }
    }

    private static byte[] loadBytesBlocking(final File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bArrConvertStreamToByteArray = convertStreamToByteArray(fileInputStream);
        fileInputStream.close();
        return bArrConvertStreamToByteArray;
    }

    private static byte[] convertStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[10240];
        while (true) {
            int i = is.read(bArr, 0, 10240);
            if (i > 0) {
                byteArrayOutputStream.write(bArr, 0, i);
            } else {
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    private static SongItem songFlatBufferToSongItem(Song song) {
        SongItem songItem = new SongItem(Item.ItemType.valueOf(song.mType()), song.mId(), song.mTitle());
        songItem.setDsId(song.mDsId());
        songItem.setArtist(song.mArtist());
        songItem.setAlbum(song.mAlbum());
        songItem.setComposer(song.mComposer());
        songItem.setGenre(song.mGenre());
        songItem.setAlbumArtist(song.mAlbumArtist());
        songItem.setFilePath(song.mFilePath());
        songItem.setDownloadType(song.mDownloadType());
        songItem.setDuration(song.mDuration());
        songItem.setFrequency(song.mFrequency());
        songItem.setChannel(song.mChannel());
        songItem.setHitCount(song.mHitCount());
        songItem.setDisc(song.mDisc());
        songItem.setTrack(song.mTrack());
        songItem.setYear(song.mYear());
        songItem.setTimeStamp(song.mTimeStamp());
        songItem.setFileSize(song.mFileSize());
        songItem.setBitrate(song.mBitrate());
        songItem.setCodec(song.mCodec() == null ? "" : song.mCodec());
        songItem.setContainer(song.mContainer() != null ? song.mContainer() : "");
        songItem.setStreamId(song.mStreamId());
        songItem.setSongRating((int) song.mRating());
        songItem.setCachePath(song.mCachePath());
        songItem.setCoverPath(song.mCoverPath());
        return songItem;
    }
}
