package com.whisperyao.dsplayer.proxy;

import androidx.annotation.NonNull;
import androidx.lifecycle.CoroutineLiveDataKt;

import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.util.SynoLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.StatusLine;

public class StreamProxy implements Runnable {
    private static final int BUFFER_SIZE = 65536;
    private static final String LOCAL_PROXY_URL = "http://127.0.0.1:%d/";
    private static final String LOG_TAG = "StreamProxy";
    private static final int TIMEOUT_ACCEPT = 5000;
    private static final int TIMEOUT_CONNECTION = 30000;
    private String httpToken;
    private int port = 0;
    private boolean isRunning = true;
    private boolean isStreaming = false;
    private ServerSocket socket = null;
    private Thread thread = null;
    private Call mCall = null;
    private PreDownloader mDownloader = null;

    private int getPort() {
        return this.port;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public boolean isStreaming() {
        return this.isStreaming;
    }

    public String getUrl() {
        return String.format(Locale.getDefault(), LOCAL_PROXY_URL, getPort());
    }

    public void init(PreDownloader downloader) {
        if (downloader == null) {
            throw new IllegalArgumentException("Downloader may not be null");
        }
        try {
            ServerSocket serverSocket = new ServerSocket(this.port, 0, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            this.socket = serverSocket;
            serverSocket.setSoTimeout(5000);
            this.port = this.socket.getLocalPort();
            this.mDownloader = downloader;
            SynoLog.d(LOG_TAG, "port " + this.port + " obtained");
        } catch (IOException e) {
            SynoLog.e(LOG_TAG, "Error initializing server", e);
        }
    }

    public void start() {
        SynoLog.d(LOG_TAG, "start");
        if (this.socket == null) {
            throw new IllegalStateException("Cannot start proxy; it has not been initialized.");
        }
        Thread thread = new Thread(this);
        this.thread = thread;
        thread.start();
    }

    public void stop() {
        SynoLog.d(LOG_TAG, "stop");
        new Thread(() -> {
            try {
                StreamProxy.this.isRunning = false;
                if (StreamProxy.this.mCall != null) {
                    StreamProxy.this.mCall.cancel();
                }
                if (StreamProxy.this.socket != null) {
                    StreamProxy.this.socket.close();
                }
                if (StreamProxy.this.thread == null) {
                    SynoLog.e(StreamProxy.LOG_TAG, "Cannot stop proxy; it has not been started.");
                } else {
                    StreamProxy.this.thread.interrupt();
                    StreamProxy.this.thread.join(CoroutineLiveDataKt.DEFAULT_TIMEOUT);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override // java.lang.Runnable
    public void run() {
        SynoLog.d(LOG_TAG, "start running...");
        while (this.isRunning) {
            try {
                Socket socketAccept = this.socket.accept();
                if (socketAccept != null) {
                    SynoLog.d(LOG_TAG, "client connected");
                    socketAccept.setSoTimeout(30000);
                    new Thread(new ProcessRunnable(socketAccept)).start();
                }
            } catch (SocketTimeoutException unused) {
            } catch (IOException e) {
                SynoLog.e(LOG_TAG, "Error connecting to client", e);
            }
        }
        SynoLog.d(LOG_TAG, "Proxy interrupted. Shutting down.");
    }

    private class ProcessRunnable implements Runnable {
        private Socket mClient;
        private long mSeekPosition = 0;

        public ProcessRunnable(Socket client) {
            this.mClient = client;
        }

        @Override
        public void run() {
            try {
                readSeek(this.mClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                processRequest(this.mClient);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        private void readSeek(Socket client) throws IOException {
            try {
                Pattern patternCompile = Pattern.compile("Range *: *bytes=(\\d+)-(\\d+)?");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()), 65536);
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null || line.length() <= 0) {
                        return;
                    }
                    Matcher matcher = patternCompile.matcher(line);
                    if (matcher.matches()) {
                        this.mSeekPosition = Long.parseLong(matcher.group(1));
                    }
                    SynoLog.d(StreamProxy.LOG_TAG, "line = " + line);
                }
            } catch (IOException e) {
                SynoLog.e(StreamProxy.LOG_TAG, "Error parsing request", e);
            }
        }

        private void processRequest(Socket client) throws IllegalStateException, IOException {

            SynoLog.d(LOG_TAG, "processing...");

            // 等待 header 准备完成
            while (isRunning && !mDownloader.isPrepared()) {

                if (mDownloader.isError()) {
                    client.close();
                    return;
                }

                try {
                    SynoLog.d(LOG_TAG, "waiting for response header...");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            String path = mDownloader.getSongItem().getCachePath();
            SynoLog.d(LOG_TAG, "path = " + path);

            byte[] buffer = new byte[65536];

            boolean needWaitForMoreData = !mDownloader.isCompleted();

            InputStream input = null;
            OutputStream output = null;

            int totalRead = 0;

            try {
                output = client.getOutputStream();

                // ===== 网络直流模式 =====
                if (path.startsWith("http")) {

                    isStreaming = true;

                    Request request = new Request.Builder()
                            .url(path)
                            .get()
                            .build();

                    mCall =
                            ConnectionManager.getHttpClient()
                                    .newCall(request);

                    Response response = mCall.execute();

                    String statusLine =
                            StatusLine.Companion.get(response).toString();

                    input = response.body().byteStream();

                    mSeekPosition = 0;

                    SynoLog.d(LOG_TAG, "RESPONSE " + statusLine);

                    output.write((statusLine + "\n\n").getBytes());

                }
                // ===== 本地缓存文件模式 =====
                else {

                    isStreaming = false;

                    input = new FileInputStream(new File(path));

                    String statusLine = "HTTP/1.1 200 OK";

                    if (mSeekPosition > 0) {
                        statusLine = "HTTP/1.1 206 Partial Content";
                        input.close();

                        input = getFileInputStream(path, mSeekPosition);
                    }

                    SynoLog.d(LOG_TAG, "RESPONSE\n " + statusLine);

                    output.write((statusLine + "\n").getBytes());

                    Headers headers = mDownloader.getHeaders();

                    StringBuilder headerBuilder = getStringBuilder(headers);

                    output.write(headerBuilder.toString().getBytes());

                    long contentLength = mDownloader.getResponse().body().contentLength();

                    SynoLog.d(LOG_TAG,
                            "contentLength: " + contentLength
                                    + ", seek: " + mSeekPosition);

                    // seek / range 支持
                    if (mSeekPosition > 0) {

                        long remain = contentLength - mSeekPosition;

                        output.write(String.format(
                                Locale.ENGLISH,
                                "Content-Length: %d\n",
                                remain
                        ).getBytes());

                        output.write(String.format(
                                Locale.ENGLISH,
                                "Content-Range: bytes %d-%d/%d\n",
                                mSeekPosition,
                                contentLength - 1,
                                contentLength
                        ).getBytes());

                    } else {

                        output.write(String.format(
                                Locale.ENGLISH,
                                "Content-Length: %d\n",
                                contentLength
                        ).getBytes());
                    }

                    output.write("Accept-Ranges: bytes\n".getBytes());

                    // header 结束
                    output.write("\n".getBytes());
                }

                SynoLog.d(LOG_TAG, "writing content ...");

                // ===== 数据转发主循环 =====
                while (isRunning) {

                    boolean downloading =
                            !mDownloader.isCompleted();

                    int read = input.read(buffer, 0, 65536);

                    // 文件读到末尾，但下载还没结束
                    if (read == -1 && needWaitForMoreData) {

                        input.close();

                        input = getFileInputStream(
                                path,
                                mSeekPosition + totalRead
                        );

                        read = input.read(buffer, 0, 65536);
                    }

                    // 仍然没数据
                    if (read == -1) {

                        // 下载彻底完成
                        if (!downloading) {
                            SynoLog.i(LOG_TAG, "finish read");
                            break;
                        }

                        // 等待继续下载
                        Thread.sleep(1000);
                        continue;
                    }

                    totalRead += read;

                    output.write(buffer, 0, read);

                    needWaitForMoreData = downloading;
                }

            } catch (Exception e) {
                e.printStackTrace();

            } finally {

                SynoLog.i(LOG_TAG, "total read = " + totalRead);

                if (input != null) {
                    input.close();
                }

                client.close();
            }
        }

        @NonNull
        private StringBuilder getStringBuilder(Headers headers) {
            StringBuilder headerBuilder = new StringBuilder();

            for (int i = 0; i < headers.size(); i++) {

                String name = headers.name(i);

                // 这些头后面自己重新写
                if (name.equalsIgnoreCase("Content-Length")
                        || name.equalsIgnoreCase("Content-Range")
                        || name.equalsIgnoreCase("Accept-Ranges")) {
                    continue;
                }

                headerBuilder.append(name)
                        .append(": ")
                        .append(headers.value(i))
                        .append("\n");
            }
            return headerBuilder;
        }

        private InputStream getFileInputStream(String path, long seek) throws InterruptedException, FileNotFoundException {
            File file = null;
            while (StreamProxy.this.isRunning) {
                file = new File(path);
                if (file.length() >= seek) {
                    break;
                }
                try {
                    SynoLog.i(StreamProxy.LOG_TAG, "file length wait " + file.length() + " / " + seek);
                    Thread.sleep(100L);
                } catch (InterruptedException unused) {
                }
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            skipInputStream(fileInputStream, seek);
            return fileInputStream;
        }

        private long skipInputStream(InputStream in, long seekByte) {
            long jSkip = 0;
            while (StreamProxy.this.isRunning && jSkip != seekByte) {
                try {
                    jSkip += in.skip(seekByte - jSkip);
                    if (seekByte != jSkip) {
                        SynoLog.i(StreamProxy.LOG_TAG, "seek wait " + jSkip + " / " + seekByte);
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException ignored) {
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            SynoLog.i(StreamProxy.LOG_TAG, "seek finished " + jSkip + " / " + seekByte);
            return jSkip;
        }
    }

}

