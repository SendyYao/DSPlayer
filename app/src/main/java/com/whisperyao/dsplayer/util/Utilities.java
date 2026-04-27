package com.whisperyao.dsplayer.util;

import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.widget.TextView;

import com.google.android.gms.cast.HlsSegmentFormat;
import com.synology.sylib.util.NetworkUtils;
import com.synology.sylibx.synofile.SynoFile;
import com.whisperyao.dsplayer.App;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.playing.EqualizerSettings;
import com.whisperyao.dsplayer.provider.DatabaseAccesser;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Utilities {
    public static String LOG = "Utilities";

    private static boolean isBitrateALAC(final long bitrate) {
        return 320000 < bitrate;
    }

    private static boolean isCodecALAC(final SongItem songItem) {
        return "alac".equalsIgnoreCase(songItem.getCodec());
    }

    public static String convertSecondsToTime(int totalSeconds) {
        int i = totalSeconds % 60;
        int i2 = totalSeconds / 60;
        int i3 = i2 % 60;
        int i4 = (i2 / 60) % 24;
        if (i4 == 0) {
            return String.format("%02d:%02d", i3, i);
        }
        return String.format("%d:%02d:%02d", i4, i3, i);
    }


    public static String escapeIdForUrl(String id) {
        return URLEncoder.encode(id);
    }

    public static String escapeId(String id) {
        if (TextUtils.isEmpty(id)) {
            return "";
        }
        return id.replace("\\", "\\\\").replace(",", "\\,");
    }

    public static String createJoinedEscapedIdList(List<String> idList) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (String s : idList) {
            arrayList.add(escapeId(s));
        }
        return StringUtils.join(arrayList, ",");
    }


    public static String getExt(final String filename) {
        int iLastIndexOf = filename.lastIndexOf(".");
        if (iLastIndexOf <= 0) {
            return "";
        }
        return filename.substring(iLastIndexOf + 1);
    }

    public static long getSongBitrate(SongItem song, String url) {
        String queryParameter = Uri.parse(url).getQueryParameter(SongItem.SQL_BITRATE);
        long bitrate = song.getBitrate();
        if (queryParameter == null) {
            return bitrate;
        }
        try {
            return Integer.parseInt(queryParameter);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return bitrate;
        }
    }

    public static boolean removeFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return true;
        }
        SynoFile synoFile = new SynoFile(path);
        if (synoFile.exists()) {
            if (synoFile.delete()) {
                SynoLog.d("removeFile", "removeFile : " + synoFile + " , file length = " + synoFile.length());
                return true;
            }
            SynoLog.e("removeFile", "fail to delete : " + synoFile);
            return false;
        }
        SynoLog.d("removeFile", "file doesn't exist : " + synoFile);
        return true;
    }

    public static void removeCachedFile(SongItem song) {
        if (song != null) {
            subCacheByte(song);
            removeFile(song.getCachePath());
            DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
            SongItem songItemQuerySong = databaseAccesser.querySong(song);
            if (songItemQuerySong != null && !TextUtils.isEmpty(songItemQuerySong.getCachePath())) {
                subCacheByte(songItemQuerySong);
                removeFile(songItemQuerySong.getCachePath());
            }
            databaseAccesser.close();
        }
    }


    public static boolean isSDCardFull() {
        if (!Environment.getExternalStorageState().equals("mounted")) {
            return true;
        }
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return 104857600 > ((long) statFs.getBlockSize()) * ((long) statFs.getAvailableBlocks());
    }

    public static boolean isStreamFormat(String format, int frequency, boolean isForChromecast) {
        if (TextUtils.isEmpty(format) || frequency > 48000) {
            return false;
        }
        if (format.equals(HlsSegmentFormat.MP3) || format.equals("mp4") || format.equals("wav")) {
            return true;
        }
        if (format.equals("flac")) {
            return !isForChromecast;
        }
        return false;
    }


    public static void subCacheByte(SongItem song) {
        String cachePath = song.getCachePath();
        if (TextUtils.isEmpty(cachePath)) {
            return;
        }
        if (song.getDownloadType() == 1) {
            AudioPreference.subAutoCacheByte(new File(cachePath).length());
        } else if (song.getDownloadType() == 2) {
            AudioPreference.subManualCacheByte(new File(cachePath).length());
        }
    }

    public static boolean supportGapless() {
        return !EqualizerSettings.getInstance().enableEqualizer();
    }

    public static TranscodeSetting.TranscodeForceFormat toStreamAudio(final String filename) {
        String lowerCase = filename.toLowerCase(Locale.getDefault());
        if (lowerCase.endsWith(".mp3")) {
            return TranscodeSetting.TranscodeForceFormat.MP3;
        }
        if (lowerCase.endsWith(".3gp") || lowerCase.endsWith(".mp4")) {
            return null;
        }
        if (lowerCase.endsWith(".m4a") || lowerCase.endsWith(".m4b") || lowerCase.endsWith(".aac")) {
            return TranscodeSetting.TranscodeForceFormat.AAC;
        }
        if (!lowerCase.endsWith(".ts")) {
            if (lowerCase.endsWith(".flac")) {
                return TranscodeSetting.TranscodeForceFormat.FLAC;
            }
            if (lowerCase.endsWith(".ogg")) {
                return TranscodeSetting.TranscodeForceFormat.OGG;
            }
            if (!lowerCase.endsWith(".mkv") && lowerCase.endsWith(".wav")) {
                return TranscodeSetting.TranscodeForceFormat.WAV;
            }
        }
        return null;
    }

    private static boolean isSupportCodec(final SongItem songItem) {
        return !TextUtils.isEmpty(songItem.getCodec());
    }


    public static boolean isAAC(final SongItem songItem) {
        TranscodeSetting.TranscodeForceFormat streamAudio = toStreamAudio(songItem.getFilePath());
        return streamAudio != null && streamAudio.isAac();
    }

    public static boolean isALAC(final SongItem songItem) {
        return isAAC(songItem) && (!isSupportCodec(songItem) ? !isBitrateALAC(songItem.getBitrate()) : !isCodecALAC(songItem));
    }


    public static boolean isStreamAudio(SongItem song, boolean isForChromecast) {
        String lowerCase = song.getFilePath().toLowerCase(Locale.getDefault());
        long frequency = song.getFrequency();
        if (lowerCase.endsWith(".aac") || lowerCase.endsWith(".m4a") || lowerCase.endsWith(".m4b")) {
            return !isALAC(song);
        }
        if (frequency > 48000) {
            return false;
        }
        if (lowerCase.endsWith(".mp3")) {
            return true;
        }
        if (!lowerCase.endsWith(".3gp") && !lowerCase.endsWith(".mp4") && !lowerCase.endsWith(".ts")) {
            if (lowerCase.endsWith(".flac")) {
                return !isForChromecast;
            }
            if (lowerCase.endsWith(".ogg")) {
                return true;
            }
            return !lowerCase.endsWith(".mkv") && lowerCase.endsWith(".wav");
        }
        return false;
    }


    public static String getMD5Code(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(input.getBytes());
            byte[] bArrDigest = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bArrDigest) {
                int i = b & 255;
                if (i < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(i));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return input;
        }
    }

    public static String makeTimeString(final long secs) {
        long j = secs / 3600;
        long j2 = (secs / 60) % 60;
        long j3 = secs % 60;
        String str = "";
        if (j > 0) {
            str = ("" + j) + ":";
            if (j2 < 10) {
                str = str + "0";
            }
        }
        String str2 = (str + j2) + ":";
        if (j3 < 10) {
            str2 = str2 + "0";
        }
        return str2 + j3;
    }

    public static boolean shouldManualDownload(SongItem song) {
//     isManualDownloadInDB(song) && checkHasSongCachedAndPassQuality(song)
        return false;
    }

    public static boolean checkPathAvailable(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        SynoFile synoFile = new SynoFile(path);
        return synoFile.exists() && synoFile.length() > 0;
    }

    public static boolean checkHasSongCachedAndPassQuality(SongItem song) {
        DatabaseAccesser databaseAccesser = DatabaseAccesser.getInstance();
        SongItem songItemQuerySong = databaseAccesser.querySong(song);
        boolean z = true;
        if (Common.isLogin() && NetworkUtils.isNetworkConnected(App.getContext())) {
            boolean z2 = false;
            if (songItemQuerySong == null || !checkPathAvailable(songItemQuerySong.getCachePath())) {
                z = false;
            } else {
                boolean zEquals = Common.getDsId().equals(song.getDsId());
                boolean zSupportMP3 = Common.getTranscodeType().supportMP3();
                boolean zIsUseWebAPI = ConnectionManager.isUseWebAPI();
                 TranscodeSetting transcodeSetting = AudioPreference.getTranscodeSetting();
                boolean z3 = zSupportMP3 && zIsUseWebAPI && transcodeSetting.isFormatMp3();
                boolean zEndsWith = songItemQuerySong.getCachePath().endsWith(".mp3");
                SynoLog.d(LOG, "isSongBelongToCurrentDS: " + zEquals + ", isToTranscodeToMP3:" + z3 + ", isCacheMP3:" + zEndsWith);
                if (zEquals && z3 && zEndsWith) {
                    long cacheBitrate = songItemQuerySong.getCacheBitrate();
                    long bitrate = songItemQuerySong.getBitrate();
                     TranscodeSetting.TranscodeDownloadQuality preferredDownloadQuality = Common.getPreferredDownloadQuality(song, transcodeSetting, false);
                    SynoLog.d(LOG, "bitrateReal: " + bitrate + ", bitrateCache:" + cacheBitrate + ", downloadQuality:" + "preferredDownloadQuality");
                    boolean z4 = preferredDownloadQuality.isOriginal() || ((long) preferredDownloadQuality.getBitrate()) > cacheBitrate;
                    String filePath = song.getFilePath();
                    boolean z5 = filePath != null && filePath.endsWith(".mp3");
                    SynoLog.d(LOG, "isOriginalFileIsMp3: " + z5);
                    if (z5) {
                        if (bitrate > cacheBitrate && z4) {
                            z2 = true;
                        }
                        z4 = z2;
                    }
                    z = !z4;
                }
            }
        }
        SynoLog.d(LOG, "isCacheSongPassQuality: " + z);
        databaseAccesser.close();
        return z;
    }

    public static String createIdList(final List<SongItem> songlist) {
        int size = songlist.size();
        if (size == 0) {
            return "";
        }
        String[] strArr = new String[size];
        for (int i = 0; i < size; i++) {
            strArr[i] = escapeId(songlist.get(i).getID());
        }
        return TextUtils.join(",", strArr);
    }

    public static float getAvgRating(List<SongItem> retItems) {
        float rating;
        if (!retItems.isEmpty()) {
            int i = 0;
            rating = 0.0f;
            for (SongItem songItem : retItems) {
                if (songItem.getRating() > 0.0f) {
                    rating += songItem.getRating();
                    i++;
                }
            }
            if (i > 0) {
                rating /= i;
            }
        } else {
            rating = 0.0f;
        }
        if (rating < 0.0f || Float.isNaN(rating)) {
            return -1.0f;
        }
        if (rating == 0.0f) {
            return rating;
        }
        if (0.0f < rating && rating < 1.25d) {
            return 1.0f;
        }
        float f = 1.25f;
        for (float f2 = 1.75f; rating >= f2; f2 = (float) (f2 + 0.5d)) {
            f = (float) (f + 0.5d);
        }
        return f + 0.25f;
    }

    public static void setTextIfNeeded(TextView tv, String title) {
        if (tv != null) {
            if (title != null) {
                if (title.equals(tv.getText().toString())) {
                    return;
                }
                tv.setText(title);
                return;
            }
            tv.setText(title);
        }
    }

    public static String getProperName(String targetPath) {
        String str;
        if (!new SynoFile(targetPath).exists()) {
            SynoLog.d("getProperName", targetPath);
            return targetPath;
        }
        int iLastIndexOf = targetPath.lastIndexOf(".");
        int i = 0;
        String strSubstring = targetPath.substring(0, iLastIndexOf);
        String strSubstring2 = targetPath.substring(iLastIndexOf);
        do {
            i++;
            str = strSubstring + i + strSubstring2;
            SynoLog.d("getProperName", str);
        } while (new SynoFile(str).exists());
        return str;
    }


}
