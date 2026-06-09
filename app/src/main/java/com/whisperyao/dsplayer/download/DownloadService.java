package com.whisperyao.dsplayer.download;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.CoroutineLiveDataKt;
import com.google.common.net.HttpHeaders;
import com.synology.sylib.util.FileUtils;
import com.synology.sylib.util.IOUtils;
import com.synology.sylibx.synofile.SAFUtils;
import com.synology.sylibx.synofile.SynoFile;
import com.whisperyao.dsplayer.CacheManager;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.injection.Constants;
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.provider.DatabaseAccesser;
import com.whisperyao.dsplayer.util.AudioPreference;
import com.whisperyao.dsplayer.util.CoverUtil;
import com.whisperyao.dsplayer.util.NoStorageAccessPermissionException;
import com.whisperyao.dsplayer.util.RAFOutputStream;
import com.whisperyao.dsplayer.util.SynoLog;
import com.whisperyao.dsplayer.util.Utilities;
import com.whisperyao.dsplayer.util.Utils;
import dagger.android.DaggerService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;


public class DownloadService extends DaggerService {
    private static final int BUFFER_SIZE = 65536;
    private static final String LOG = "DownloadService";
    private static final int NOTIFICATION_ID = 1157;
    private static final int RETRY_LIMIT = 3;

    @Inject
    CoverUtil coverUtil;

    @Inject
    @ApplicationContext
    Context mApplicationContext;

    @Inject
    @Named(Constants.NOTIFICATION_CHANNEL_DOWNLOAD)
    NotificationCompat.Builder mBuilder;
    private DatabaseAccesser mDBHelper;

    @Inject
    NotificationManager mNotificationManager;
    private SongDownloader songDownloader;

    @Inject
    TaskManager taskMgr;
    private Call mCall = null;
    private Response mResponse = null;
    private SongItem mProcessingSongItem = null;
    private final IDownloadService.Stub mBinder = new IDownloadService.Stub() {
        @Override
        public void notifyDeleteTask() {
            if (DownloadService.this.mCall == null || DownloadService.this.mProcessingSongItem == null || DownloadService.this.mProcessingSongItem.equals(DownloadService.this.taskMgr.peek())) {
                return;
            }
            new Thread(() -> DownloadService.this.mCall.cancel()).start();
        }
    };
    private final BroadcastReceiver mSongCacheCompleteListener = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            SynoLog.i("mSongCacheCompleteListener", "onReceive : action = " + intent.getAction());
            String stringExtra = intent.getStringExtra(Common.SONGCACHE_PATH);
            if (DownloadService.this.mCall == null || DownloadService.this.mProcessingSongItem == null || !DownloadService.this.mProcessingSongItem.getFilePath().equals(stringExtra)) {
                return;
            }
            DownloadService.this.taskMgr.poll();
            new Thread(() -> DownloadService.this.mCall.cancel()).start();
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        String str = LOG;
        SynoLog.i(str, "onCreate");
        this.mDBHelper = DatabaseAccesser.getInstance();
        Utils.registerReceiver(this, this.mSongCacheCompleteListener, new IntentFilter(Common.NOTIFY_SONGCACHE_COMPLETED), false);
        SongDownloader songDownloader = new SongDownloader();
        this.songDownloader = songDownloader;
        songDownloader.start();
        SynoLog.i(str, "getState = " + this.songDownloader.getState());
    }

    @Override
    public void onDestroy() {
        String str = LOG;
        SynoLog.i(str, "onDestroy");
        unregisterReceiver(this.mSongCacheCompleteListener);
        this.taskMgr.clear();
        if (this.mCall != null) {
            new Thread(() -> mCall.cancel()).start();
        }
        this.mDBHelper.close();
        SynoLog.i(str, "songDownloader.isAlive() : " + this.songDownloader.isAlive());
        if (this.songDownloader.isAlive()) {
            this.songDownloader.interrupt();
            try {
                this.songDownloader.join(CoroutineLiveDataKt.DEFAULT_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.mNotificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private class SongDownloader extends Thread {
        public SongDownloader() {
            super(new DownloadRunnable());
        }
    }

    private class DownloadRunnable implements Runnable {

        private static final int MAX_RETRY_COUNT = 3;
        private static final long RETRY_DELAY_MS = 3000;
        private static final int NOTIFICATION_ID = 0x485;
        private static final int DOWNLOAD_TYPE_CACHE = 2;

        @Override
        public void run() {
            CacheManager cacheManager = CacheManager.getInstance();

            while (true) {
                // 打印任务队列大小
                SynoLog.i(LOG, "taskMgr size  = " + taskMgr.size());

                // 获取队首任务
                mProcessingSongItem = taskMgr.peek();

                // 队列为空，停止服务
                if (mProcessingSongItem == null) {
                    mNotificationManager.cancel(NOTIFICATION_ID);
                    stopSelf();
                    return;
                }

                // 获取文件名
                String filePath = mProcessingSongItem.getFilePath();
                String fileName = new File(filePath).getName();

                SynoLog.i(LOG, "process name = " + fileName);

                // 显示通知
                showNotification(fileName);

                // 尝试获取歌词
                try {
                    cacheManager.doEnumLyrics(mProcessingSongItem);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 检查任务是否已变更
                if (!mProcessingSongItem.equals(taskMgr.peek())) {
                    mProcessingSongItem = null;
                    continue;
                }

                // 删除旧缓存文件
                Utilities.removeCachedFile(mProcessingSongItem);

                // 获取下载URL和扩展名
                String playUrl = ConnectionManager.getOriginalPlayUrl(mProcessingSongItem);
                String extension = Utilities.getExt(playUrl);

                // 获取文件名（不含扩展名）
                String originalFileName = new File(mProcessingSongItem.getFilePath()).getName();
                String baseFileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));

                // 获取下载目录
                String cacheFolder = Common.getSongCacheFolder();
                if (cacheFolder == null) {
                    SynoLog.e(LOG, "no download folder permission");
                    return;
                }

                // 构建目标文件路径
                String targetFileName = baseFileName + "." + extension;
                String targetPath = Utilities.getProperName(
                        new File(cacheFolder, targetFileName).getPath()
                );

                SynoLog.i(LOG, "path : " + targetPath);

                // 重试下载最多3次
                boolean downloadSuccess = false;
                int retryCount = 0;

                while (retryCount < MAX_RETRY_COUNT && !downloadSuccess) {
                    try {
                        SynoFile synoFile = new SynoFile(targetPath);
                        long fileLength = synoFile.length();
                        OutputStream outputStream;

                        if (FileUtils.isWritable(synoFile)) {
                            // 可写，使用 RandomAccessFile 续传
                            RandomAccessFile raf = new RandomAccessFile(synoFile, "rws");
                            raf.seek(raf.length());
                            outputStream = new RAFOutputStream(raf);
                        } else {
                            // 不可写，尝试创建文件或使用 SAF
                            if (!synoFile.exists() && !synoFile.createNewFile()) {
                                throw new NoStorageAccessPermissionException("can't create file");
                            }

                            DocumentFile documentFile = SAFUtils.getDocumentFile(
                                    mApplicationContext,
                                    synoFile.getAbsolutePath()
                            );

                            if (documentFile == null) {
                                throw new NoStorageAccessPermissionException("can't create document file");
                            }

                            outputStream = mApplicationContext.getContentResolver()
                                    .openOutputStream(documentFile.getUri(), "wa");
                        }

                        // 执行下载
                        downloadSuccess = download(mProcessingSongItem, outputStream, fileLength);

                        // 关闭输出流
                        IOUtils.closeSilently(outputStream);

                    } catch (IOException e) {
                        e.printStackTrace();
                        downloadSuccess = false;
                    }

                    // 检查任务是否已变更
                    if (!mProcessingSongItem.equals(taskMgr.peek())) {
                        break;
                    }

                    // 下载失败，等待后重试
                    if (!downloadSuccess) {
                        SynoLog.i(LOG, "sleep 3000");
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        SynoLog.i(LOG, "finish sleep");
                    }

                    retryCount++;
                }

                // 下载完成后处理
                if (mProcessingSongItem.equals(taskMgr.peek())) {
                    taskMgr.poll();

                    if (downloadSuccess) {
                        // 获取歌曲比特率
                        long bitrate = Utilities.getSongBitrate(mProcessingSongItem, playUrl);

                        // 查询数据库中的歌曲
                        SongItem existingSong = mDBHelper.querySong(mProcessingSongItem);

                        SynoLog.i(LOG, "finish : " + targetPath);

                        if (existingSong == null) {
                            // 新歌曲，添加到数据库
                            mProcessingSongItem.setHitCount(0);
                            mProcessingSongItem.setCachePath(targetPath);
                            mProcessingSongItem.setCacheBitrate(bitrate);
                            mProcessingSongItem.setDownloadType(DOWNLOAD_TYPE_CACHE);
                            mDBHelper.addSong(mProcessingSongItem);
                        } else {
                            // 已存在，检查是否需要删除旧缓存
                            if (!existingSong.getCachePath().equals(targetPath)) {
                                SynoLog.i(LOG, "cachesong exist, delete it at first");
                                Utilities.removeCachedFile(existingSong);
                            }

                            // 更新数据库
                            mProcessingSongItem.setCachePath(targetPath);
                            mProcessingSongItem.setCacheBitrate(bitrate);
                            mProcessingSongItem.setDownloadType(DOWNLOAD_TYPE_CACHE);
                            mDBHelper.updateSong(mProcessingSongItem);
                        }

                        // 更新缓存统计
                        long fileSize = new File(targetPath).length();
                        AudioPreference.addManualCacheByte(fileSize);

                        // 下载封面图片
                        String coverUrl = ConnectionManager.getCoverUrl(mProcessingSongItem.getID());
                        coverUtil.downloadImage(mProcessingSongItem, coverUrl);

                    } else {
                        // 下载失败，删除文件
                        SynoLog.i(LOG, "download fail, delete file : " + targetPath);
                        Utilities.removeFile(targetPath);
                    }
                } else {
                    // 任务已变更，删除下载的文件
                    SynoLog.i(LOG, "download fail, delete file : " + targetPath);
                    Utilities.removeFile(targetPath);
                }

                // 重置处理中的歌曲
                mProcessingSongItem = null;
            }
        }
    }

    private boolean download(final SongItem song, OutputStream outputStream, long resumeBytes) throws IOException {
        byte[] bArr = new byte[65536];
        Request.Builder builder = new Request.Builder().url(ConnectionManager.getOriginalPlayUrl(song)).get();
        long j = 0;
        if (0 < resumeBytes) {
            builder.addHeader(HttpHeaders.RANGE, String.format(Locale.ENGLISH, "bytes=%d-", Long.valueOf(resumeBytes)));
        }
        SynoLog.d(LOG, "download offset " + resumeBytes);
        Call callNewCall = ConnectionManager.getHttpClient().newCall(builder.build());
        this.mCall = callNewCall;
        Response responseExecute = callNewCall.execute();
        this.mResponse = responseExecute;
        if (responseExecute == null || responseExecute.body().contentLength() <= 0) {
            return false;
        }
        InputStream inputStreamByteStream = this.mResponse.body().byteStream();
        while (true) {
            int i = inputStreamByteStream.read(bArr, 0, 65536);
            if (i != 0) {
                if (-1 == i) {
                    String str = LOG;
                    SynoLog.i(str, "DownloadStatus : FILE_DONE");
                    SynoLog.i(str, "append " + j + ", total " + (j + resumeBytes));
                    this.mCall.cancel();
                    this.mCall = null;
                    closeInputStream(inputStreamByteStream);
                    return true;
                }
                outputStream.write(bArr, 0, i);
                long j2 = j + i;
                setProgress(this.mResponse.body().contentLength(), song, resumeBytes + j2);
                j = j2;
            }
        }
    }

    private void showNotification(String name) {
        Intent intent = new Intent(this, TaskActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.mBuilder.setContentText(name).setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        this.mNotificationManager.notify(NOTIFICATION_ID, this.mBuilder.build());
    }

    private void setProgress(long total, SongItem song, long done) {
        if (0 >= total) {
            return;
        }
        this.taskMgr.setProgress(song.getUniqueKey(), (int) ((done * 100) / total));
    }

    private void closeInputStream(InputStream in) throws IOException {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                SynoLog.e(LOG, "failed to close stream", e);
                e.printStackTrace();
            }
        }
    }
}
