package com.whisperyao.dsplayer.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RAFOutputStream extends OutputStream {
    private final RandomAccessFile mRaf;

    public RAFOutputStream(RandomAccessFile raf) {
        this.mRaf = raf;
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        this.mRaf.write(buffer, offset, count);
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        this.mRaf.write(buffer);
    }

    @Override
    public void write(int oneByte) throws IOException {
        this.mRaf.write(oneByte);
    }

    @Override
    public void close() throws IOException {
        this.mRaf.close();
    }
}
