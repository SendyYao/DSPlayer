package com.whisperyao.dsplayer.model.flatbuffers;

import com.google.flatbuffers.BaseVector;
import com.google.flatbuffers.Constants;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;
import com.whisperyao.dsplayer.model.flatbuffers.Song;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public final class SongsList extends Table {
    public static void ValidateVersion() {
        Constants.FLATBUFFERS_25_2_10();
    }

    public static SongsList getRootAsSongsList(ByteBuffer _bb) {
        return getRootAsSongsList(_bb, new SongsList());
    }

    public static SongsList getRootAsSongsList(ByteBuffer _bb, SongsList obj) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        return obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb);
    }

    public void __init(int _i, ByteBuffer _bb) {
        __reset(_i, _bb);
    }

    public SongsList __assign(int _i, ByteBuffer _bb) {
        __init(_i, _bb);
        return this;
    }

    public Song songs(int j) {
        return songs(new Song(), j);
    }

    public Song songs(Song obj, int j) {
        int i__offset = __offset(4);
        if (i__offset != 0) {
            return obj.__assign(__indirect(__vector(i__offset) + (j * 4)), this.bb);
        }
        return null;
    }

    public int songsLength() {
        int i__offset = __offset(4);
        if (i__offset != 0) {
            return __vector_len(i__offset);
        }
        return 0;
    }

    public Song.Vector songsVector() {
        return songsVector(new Song.Vector());
    }

    public Song.Vector songsVector(Song.Vector obj) {
        int i__offset = __offset(4);
        if (i__offset != 0) {
            return obj.__assign(__vector(i__offset), 4, this.bb);
        }
        return null;
    }

    public static int createSongsList(FlatBufferBuilder builder, int songsOffset) {
        builder.startTable(1);
        addSongs(builder, songsOffset);
        return endSongsList(builder);
    }

    public static void startSongsList(FlatBufferBuilder builder) {
        builder.startTable(1);
    }

    public static void addSongs(FlatBufferBuilder builder, int songsOffset) {
        builder.addOffset(0, songsOffset, 0);
    }

    public static int createSongsVector(FlatBufferBuilder builder, int[] data) {
        builder.startVector(4, data.length, 4);
        for (int length = data.length - 1; length >= 0; length--) {
            builder.addOffset(data[length]);
        }
        return builder.endVector();
    }

    public static void startSongsVector(FlatBufferBuilder builder, int numElems) {
        builder.startVector(4, numElems, 4);
    }

    public static int endSongsList(FlatBufferBuilder builder) {
        return builder.endTable();
    }

    public static void finishSongsListBuffer(FlatBufferBuilder builder, int offset) {
        builder.finish(offset);
    }

    public static void finishSizePrefixedSongsListBuffer(FlatBufferBuilder builder, int offset) {
        builder.finishSizePrefixed(offset);
    }

    public static final class Vector extends BaseVector {
        public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) {
            __reset(_vector, _element_size, _bb);
            return this;
        }

        public SongsList get(int j) {
            return get(new SongsList(), j);
        }

        public SongsList get(SongsList obj, int j) {
            return obj.__assign(SongsList.__indirect(__element(j), this.bb), this.bb);
        }
    }
}
