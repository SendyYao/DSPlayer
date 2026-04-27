package com.synology.sylib.syhttp3.requestBody;

import com.synology.sylib.util.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;


public abstract class SyRequestBody extends RequestBody {
    private static final int SEGMENT_SIZE = 2048;
    private static final int UPDATE_INTERVAL_MILLISECS = 200;

    public interface ProgressListener {
        void update(long j, long j2, boolean z);
    }

    void writeToInternal(BufferedSink bufferedSink, File file, ProgressListener progressListener) throws Throwable {
        writeToInternal(bufferedSink, new FileInputStream(file), file.length(), progressListener);
    }

    void writeToInternal(BufferedSink bufferedSink, InputStream inputStream, long j, ProgressListener progressListener) throws Throwable {
        Source source;
        try {
            source = Okio.source(inputStream);
            long j2 = 0;
            long j3 = 0;
            while (true) {
                try {
                    long j4 = source.read(bufferedSink.buffer(), 2048L);
                    if (j4 != -1) {
                        j3 += j4;
                        bufferedSink.flush();
                        long jCurrentTimeMillis = System.currentTimeMillis();
                        if (200 < jCurrentTimeMillis - j2) {
                            progressListener.update(j3, j, j3 < j);
                            j2 = jCurrentTimeMillis;
                        }
                    } else {
                        progressListener.update(j3, j, true);
                        IOUtils.closeSilently(source);
                        return;
                    }
                } catch (Throwable th) {
                    IOUtils.closeSilently(source);
                    throw th;
                }
            }
        } catch (Throwable th) {
            source = null;
        }
    }

    public static SyRequestBody synoCreate(MediaType mediaType, String str) {
        Charset charset = StandardCharsets.UTF_8;
        if (mediaType != null && (charset = mediaType.charset()) == null) {
            charset = StandardCharsets.UTF_8;
            mediaType = MediaType.parse(mediaType + "; charset=utf-8");
        }
        return synoCreate(mediaType, str.getBytes(charset));
    }

    public static SyRequestBody synoCreate(MediaType mediaType, byte[] bArr) {
        return synoCreate(mediaType, bArr, 0, bArr.length);
    }

    public static SyRequestBody synoCreate(final MediaType mediaType, final byte[] bArr, final int i, final int i2) {
        if (bArr == null) {
            throw new NullPointerException("content == null");
        }
        checkOffsetAndCount(bArr.length, i, i2);
        return new SyRequestBody() { // from class: com.synology.sylib.syhttp3.requestBody.SyRequestBody.1
            @Override // okhttp3.RequestBody
            public MediaType contentType() {
                return mediaType;
            }

            @Override // okhttp3.RequestBody
            public long contentLength() {
                return i2;
            }

            @Override // okhttp3.RequestBody
            public void writeTo(BufferedSink bufferedSink) throws IOException {
                bufferedSink.write(bArr, i, i2);
            }
        };
    }

    public static SyRequestBody synoCreate(final MediaType mediaType, final File file, final ProgressListener progressListener) {
        if (file == null) {
            throw new NullPointerException("content == null");
        }
        return new SyRequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(BufferedSink bufferedSink) throws IOException {
                try {
                    writeToInternal(bufferedSink, file, progressListener);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static SyRequestBody synoCreate(final MediaType mediaType, final InputStream inputStream, final long j, final ProgressListener progressListener) {
        if (inputStream == null) {
            throw new NullPointerException("content == null");
        }
        return new SyRequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                return j;
            }

            @Override
            public void writeTo(BufferedSink bufferedSink) throws IOException {
                try {
                    writeToInternal(bufferedSink, inputStream, j, progressListener);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static void checkOffsetAndCount(long j, long j2, long j3) {
        if ((j2 | j3) < 0 || j2 > j || j - j2 < j3) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
