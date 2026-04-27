package com.synology.sylib.util;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    private static final BigDecimal KILO_DIVISOR = new BigDecimal(1024L);
    private static final String PRIMARY_VOLUME_NAME = "primary";
    private static final String TAG = "FileUtils";
    private static final int BUFFER_SIZE = 0x1000; // 4096 bytes

    enum SizeSuffix {
        B, KB, MB, GB, TB, PB, EB, ZB, YB
    }

    /**
     * 将字节数转换为人类可读的大小字符串
     */
    public static String byteCountToDisplaySize(BigInteger size, int maxLength) {
        BigDecimal bigDecimal = new BigDecimal(size);
        SizeSuffix sizeSuffix = SizeSuffix.B;

        for (SizeSuffix suffix : SizeSuffix.values()) {
            if (!suffix.equals(SizeSuffix.B)) {
                if (bigDecimal.setScale(0, RoundingMode.HALF_UP).toString().length() <= maxLength) {
                    break;
                }
                bigDecimal = bigDecimal.divide(KILO_DIVISOR);
                sizeSuffix = suffix;
            }
        }

        String result = bigDecimal.setScale(0, RoundingMode.HALF_UP).toString();
        int targetLength = maxLength - 1;
        if (result.length() < targetLength) {
            result = bigDecimal.setScale(targetLength - result.length(), RoundingMode.HALF_UP).toString();
        }

        return result + " " + sizeSuffix;
    }

    public static String byteCountToDisplaySize(long size, int maxLength) {
        return byteCountToDisplaySize(BigInteger.valueOf(size), maxLength);
    }

    /**
     * 获取默认相机文件夹路径
     */
    public static String getDefaultCameraFolder() {
        File dcimFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File cameraFolder;

        if (dcimFolder.exists()) {
            cameraFolder = new File(dcimFolder, "Camera/");
            if (!cameraFolder.exists()) {
                cameraFolder = new File(dcimFolder, "100ANDRO/");
                if (!cameraFolder.exists()) {
                    cameraFolder = new File(dcimFolder, "100MEDIA/");
                }
            }
        } else {
            cameraFolder = new File(dcimFolder, "Camera/");
        }

        return cameraFolder.getAbsolutePath();
    }

    /**
     * 复制文件，支持多种方式（标准IO、FileChannel、DocumentFile、SAF）
     */
    public static boolean copyFile(Context context, File sourceFile, File destFile) {
        FileInputStream fis = null;
        OutputStream os = null;
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        boolean success = false;

        try {
            fis = new FileInputStream(sourceFile);

            if (isWritable(destFile)) {
                // 方式1: 目标可写，使用 FileChannel 高性能复制
                FileOutputStream fos = new FileOutputStream(destFile);
                os = fos;
                try {
                    inputChannel = fis.getChannel();
                    outputChannel = fos.getChannel();
                    inputChannel.transferTo(0, inputChannel.size(), outputChannel);
                    success = true;
                } finally {
                    closeQuietly(outputChannel);
                    outputChannel = null;
                    closeQuietly(inputChannel);
                    inputChannel = null;
                }
            } else if (VersionUtil.isAndroid5()) {
                // 方式2: Android 5.0+ 使用 DocumentFile (SAF)
                DocumentFile documentFile = getDocumentFile(context, destFile, false, true);
                if (documentFile != null) {
                    os = context.getContentResolver().openOutputStream(documentFile.getUri());
                    if (os != null) {
                        copyStream(fis, os);
                        success = true;
                    }
                }
            } else if (VersionUtil.isKitkat()) {
                // 方式3: Android 4.4 使用 MediaStore URI
                Uri uri = getUriFromFile(context, destFile.getAbsolutePath());
                if (uri != null) {
                    os = context.getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        copyStream(fis, os);
                        success = true;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error when copying file from " + sourceFile.getAbsolutePath()
                    + " to " + destFile.getAbsolutePath(), e);
            success = false;
        } finally {
            closeQuietly(fis);
            closeQuietly(os);
            closeQuietly(inputChannel);
            closeQuietly(outputChannel);
        }

        return success;
    }

    /**
     * 使用缓冲区复制流数据
     */
    private static void copyStream(FileInputStream fis, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
    }

    /**
     * 静默关闭 Closeable
     */
    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 静默关闭 FileChannel
     */
    private static void closeQuietly(FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 删除文件，支持标准删除、DocumentFile、MediaStore
     */
    public static boolean deleteFile(Context context, File file) {
        if (file.delete()) {
            return true;
        }

        try {
            if (VersionUtil.isAndroid5()) {
                DocumentFile documentFile = getDocumentFile(context, file, false, true);
                return documentFile != null && documentFile.delete();
            }

            if (VersionUtil.isKitkat()) {
                try {
                    Uri uri = getUriFromFile(context, file.getAbsolutePath());
                    if (uri != null) {
                        context.getContentResolver().delete(uri, null, null);
                    }
                    return !file.exists();
                } catch (Exception e) {
                    Log.e(TAG, "Error when deleting file " + file.getAbsolutePath(), e);
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error when deleting file " + file.getAbsolutePath(), e);
        }

        return !file.exists();
    }

    /**
     * 移动文件
     */
    public static boolean moveFile(Context context, File sourceFile, File destFile) {
        try {
            if (sourceFile.renameTo(destFile)) {
                return true;
            }
            if (copyFile(context, sourceFile, destFile)) {
                return deleteFile(context, sourceFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error when moving file", e);
        }
        return false;
    }

    /**
     * 重命名文件/文件夹
     */
    public static boolean rename(Context context, File sourceFile, File destFile) {
        try {
            if (sourceFile.renameTo(destFile)) {
                return true;
            }

            if (destFile.exists()) {
                return false;
            }

            if (VersionUtil.isAndroid5() && sourceFile.getParent().equals(destFile.getParent())) {
                DocumentFile documentFile = getDocumentFile(context, sourceFile, true, true);
                return documentFile != null && documentFile.renameTo(destFile.getName());
            }

            if (!mkdir(context, destFile)) {
                return false;
            }

            File[] files = sourceFile.listFiles();
            if (files == null) {
                return true;
            }

            for (File file : files) {
                if (!copyFile(context, file, new File(destFile, file.getName()))) {
                    return false;
                }
            }

            for (File file : files) {
                if (!deleteFile(context, file)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error when renaming", e);
            return false;
        }
    }

    /**
     * 获取临时文件
     */
    public static File getTempFile(Context context, File file) {
        return new File(context.getExternalFilesDir(null), file.getName());
    }

    /**
     * 创建目录
     */
    public static boolean mkdir(Context context, File file) {
        try {
            if (file.exists()) {
                return file.isDirectory();
            }

            if (file.mkdir()) {
                return true;
            }

            if (VersionUtil.isAndroid5()) {
                DocumentFile documentFile = getDocumentFile(context, file, true, true);
                return documentFile != null && documentFile.exists();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error when creating directory", e);
        }

        return false;
    }

    /**
     * 删除目录
     */
    public static boolean rmDir(Context context, File file, boolean deleteContents) {
        try {
            if (!file.exists()) {
                return true;
            }

            if (!file.isDirectory()) {
                return false;
            }

            String[] list = file.list();
            if (list != null && list.length > 0) {
                if (!deleteContents || !deleteFilesInFolder(context, file)) {
                    return false;
                }
            }

            if (file.delete()) {
                return true;
            }

            if (VersionUtil.isAndroid5()) {
                DocumentFile documentFile = getDocumentFile(context, file, true, true);
                return documentFile != null && documentFile.delete();
            }

            if (VersionUtil.isKitkat()) {
                ContentResolver contentResolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put("_data", file.getAbsolutePath());
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                contentResolver.delete(
                        MediaStore.Files.getContentUri("external"),
                        "_data=?",
                        new String[]{file.getAbsolutePath()}
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error when removing directory", e);
        }

        return !file.exists();
    }

    /**
     * 删除文件夹中的文件
     */
    public static boolean deleteFilesInFolder(Context context, File file) {
        String[] list = file.list();
        boolean success = true;

        if (list != null) {
            for (String name : list) {
                File childFile = new File(file, name);
                if (!childFile.isDirectory() && !deleteFile(context, childFile)) {
                    Log.w(TAG, "Failed to delete file: " + name);
                    success = false;
                }
            }
        }

        return success;
    }

    /**
     * 检查文件是否可写
     */
    public static boolean isWritable(File file) throws IOException {
        boolean exists = file.exists();
        try {
            try (FileOutputStream fos = new FileOutputStream(file, true)) {
                // 尝试打开文件写入
            }
            boolean canWrite = file.canWrite();
            if (!exists) {
                file.delete();
            }
            return canWrite;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
     * 检查文件是否可写（标准方式或SAF方式）
     */
    public static boolean isWritableNormalOrSaf(Context context, File file) {
        try {
            if (file.exists() && file.isDirectory()) {
                int index = 0;
                File tempFile;
                do {
                    index++;
                    tempFile = new File(file, "__dsfile_test_writing_file__" + index);
                } while (tempFile.exists());

                if (isWritable(tempFile)) {
                    return true;
                }

                DocumentFile documentFile = getDocumentFile(context, tempFile, false, false);
                if (documentFile == null) {
                    return false;
                }

                boolean writable = documentFile.canWrite() && tempFile.exists();
                documentFile.delete();
                return writable;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking writable status", e);
        }

        return false;
    }

    /**
     * 获取外部SD卡路径列表
     */
    private static String[] getExtSdCardPaths(Context context) throws IOException {
        List<String> paths = new ArrayList<>();
        File externalFilesDir = context.getExternalFilesDir("external");

        for (File file : ContextCompat.getExternalFilesDirs(context, "external")) {
            if (file != null && !file.equals(externalFilesDir)) {
                String path = file.getAbsolutePath();
                int index = path.lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w(TAG, "Unexpected external file dir: " + path);
                } else {
                    String sdPath = path.substring(0, index);
                    try {
                        sdPath = new File(sdPath).getCanonicalPath();
                    } catch (IOException ignored) {
                    }
                    paths.add(sdPath);
                }
            }
        }

        return paths.toArray(new String[0]);
    }

    /**
     * 获取文件所在的外部SD卡根路径
     */
    public static String getExtSdCardFolder(Context context, File file) {
        try {
            String canonicalPath = file.getCanonicalPath();
            for (String path : getExtSdCardPaths(context)) {
                if (canonicalPath.startsWith(path)) {
                    return path;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * 判断文件是否在外部SD卡上
     */
    public static boolean isOnExtSdCard(Context context, File file) {
        return getExtSdCardFolder(context, file) != null;
    }

    /**
     * 判断是否需要授予权限
     */
    public static boolean isNeedGrantPermission(Context context, File file) {
        if (file.exists() && file.isDirectory()) {
            return !isWritableNormalOrSaf(context, file);
        }
        return false;
    }

    /**
     * 获取 DocumentFile 对象（SAF框架）
     */
    public static DocumentFile getDocumentFile(Context context, File file, boolean createDirectory, boolean createIfNotExist) {
        if (!VersionUtil.isAtLeastVersion(19)) {
            return null;
        }

        try {
            List<UriPermission> persistedUriPermissions = context.getContentResolver().getPersistedUriPermissions();
            int size = persistedUriPermissions.size();

            if (size == 0) {
                return null;
            }

            Uri[] uriArr = new Uri[size];
            for (int i = 0; i < size; i++) {
                uriArr[i] = persistedUriPermissions.get(i).getUri();
            }

            String canonicalPath = file.getCanonicalPath();
            Matcher matcher = Pattern.compile("/storage/(\\w{4}-\\w{4})/.*").matcher(canonicalPath);
            String strGroup = "";
            if (matcher.matches() && matcher.groupCount() > 0) {
                strGroup = matcher.group(1);
            }

            Uri matchedUri = null;
            String fullPathFromTreeUri = null;

            for (int i = 0; i < size; i++) {
                Uri uri = uriArr[i];
                String path = getFullPathFromTreeUri(context, uri);

                if ((TextUtils.isEmpty(strGroup) || uri.toString().contains(strGroup))
                        && canonicalPath.startsWith(path)) {
                    matchedUri = uri;
                    fullPathFromTreeUri = path;
                    break;
                }
            }

            if (fullPathFromTreeUri == null) {
                return null;
            }

            DocumentFile documentFileFromTreeUri = DocumentFile.fromTreeUri(context, matchedUri);

            if (canonicalPath.length() < fullPathFromTreeUri.length()) {
                return null;
            }

            if (canonicalPath.length() == fullPathFromTreeUri.length()) {
                return fullPathFromTreeUri.equals(canonicalPath) ? documentFileFromTreeUri : null;
            }

            String relativePath = canonicalPath.substring(fullPathFromTreeUri.length() + 1);
            String[] pathParts = relativePath.split("/");

            for (int i = 0; i < pathParts.length; i++) {
                DocumentFile foundFile = documentFileFromTreeUri.findFile(pathParts[i]);
                if (foundFile != null) {
                    documentFileFromTreeUri = foundFile;
                } else if (i < pathParts.length - 1) {
                    if (!createIfNotExist) {
                        return null;
                    }
                    documentFileFromTreeUri = documentFileFromTreeUri.createDirectory(pathParts[i]);
                } else if (createDirectory) {
                    documentFileFromTreeUri = documentFileFromTreeUri.createDirectory(pathParts[i]);
                } else {
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            MimeTypeMap.getFileExtensionFromUrl(canonicalPath)
                    );
                    if (mimeType == null) {
                        mimeType = "";
                    }
                    documentFileFromTreeUri = documentFileFromTreeUri.createFile(mimeType, pathParts[i]);
                }
            }

            return documentFileFromTreeUri;

        } catch (Exception e) {
            Log.e(TAG, "Error getting DocumentFile", e);
            return null;
        }
    }

    /**
     * 从 TreeUri 获取完整文件路径
     */
    private static String getFullPathFromTreeUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }

        try {
            String volumePath = getVolumePath(context, getVolumeIdFromTreeUri(uri));
            if (volumePath == null) {
                return File.separator;
            }

            if (volumePath.endsWith(File.separator)) {
                volumePath = volumePath.substring(0, volumePath.length() - 1);
            }

            String documentPath = getDocumentPathFromTreeUri(uri);
            if (documentPath.endsWith(File.separator)) {
                documentPath = documentPath.substring(0, documentPath.length() - 1);
            }

            if (documentPath.length() <= 0) {
                return volumePath;
            }

            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            }

            return volumePath + File.separator + documentPath;
        } catch (Exception e) {
            Log.e(TAG, "Error getting full path from tree URI", e);
            return null;
        }
    }

    /**
     * 获取存储卷路径
     */
    private static String getVolumePath(Context context, String volumeId) {
        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Class<?> storageVolumeClass = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeListMethod = storageManager.getClass().getMethod("getVolumeList");
            Method getUuidMethod = storageVolumeClass.getMethod("getUuid");
            Method getPathMethod = storageVolumeClass.getMethod("getPath");
            Method isPrimaryMethod = storageVolumeClass.getMethod("isPrimary");

            Object volumes = getVolumeListMethod.invoke(storageManager);
            int length = Array.getLength(volumes);

            for (int i = 0; i < length; i++) {
                Object volume = Array.get(volumes, i);
                String uuid = (String) getUuidMethod.invoke(volume);

                if (((Boolean) isPrimaryMethod.invoke(volume)) && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPathMethod.invoke(volume);
                }

                if (uuid != null && uuid.equals(volumeId)) {
                    return (String) getPathMethod.invoke(volume);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting volume path", e);
        }

        return null;
    }

    /**
     * 从 TreeUri 获取卷ID
     */
    private static String getVolumeIdFromTreeUri(Uri uri) {
        String documentId = DocumentsContract.getTreeDocumentId(uri);
        String[] parts = documentId.split(":");
        return parts.length > 0 ? parts[0] : null;
    }

    /**
     * 从 TreeUri 获取文档路径
     */
    private static String getDocumentPathFromTreeUri(Uri uri) {
        String documentId = DocumentsContract.getTreeDocumentId(uri);
        String[] parts = documentId.split(":");
        return (parts.length >= 2 && parts[1] != null) ? parts[1] : File.separator;
    }

    /**
     * 根据文件路径获取 Uri（用于 MediaStore）
     */
    public static Uri getUriFromFile(Context context, String filePath) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;

        try {
            cursor = contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    new String[]{"_id"},
                    "_data = ?",
                    new String[]{filePath},
                    "date_added desc"
            );

            if (cursor == null) {
                return null;
            }

            cursor.moveToFirst();

            if (cursor.isAfterLast()) {
                cursor.close();
                ContentValues contentValues = new ContentValues();
                contentValues.put("_data", filePath);
                return contentResolver.insert(
                        MediaStore.Files.getContentUri("external"),
                        contentValues
                );
            }

            int id = cursor.getInt(cursor.getColumnIndex("_id"));
            Uri uri = MediaStore.Files.getContentUri("external")
                    .buildUpon()
                    .appendPath(Integer.toString(id))
                    .build();

            return uri;
        } catch (Exception e) {
            Log.e(TAG, "Error getting URI from file", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
