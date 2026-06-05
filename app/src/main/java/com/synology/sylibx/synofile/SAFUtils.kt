package com.synology.sylibx.synofile

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.synology.sylib.utilities.contextprovider.SynoContextProvider
import com.synology.sylibx.synofile.Extensions.copyRecursivelySyno
import com.synology.sylibx.synofile.Extensions.getValidFile
import com.synology.sylibx.synofile.Extensions.isUnderPath
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException

object SAFUtils {

    private const val DENIED_CHAR_PATTERN = "[*?:|\"<>]"
    private const val DOCUMENT_URI_PATTERN =
        "content://com.android.externalstorage.documents/tree/primary:%s/document/primary:"
    private const val EXTERNAL_STORAGE_ROOT = "/storage/emulated/0"
    private const val FOLDER_HOME = "Documents"
    private const val PATH_ANDROID_DATA = "/Android/data"
    private const val TAG = "SAFUtils"
    private const val TAG_EXTERNAL_STORAGE = "primary"
    private const val TAG_EXTERNAL_STORAGE_DOCUMENTS = "home"

    private val DATA_FOLDER_LIST = listOf("Android/data", "Android/obb")

    private val cacheBasePath = HashMap<Uri, String>()
    private val cacheBaseDoc = HashMap<Uri, DocumentFile>()
    private val cacheVolumePath = HashMap<Uri, String>()

    enum class ConflictAction {
        Rename,
        Stop,
        Overwrite;

        fun isStop() = this == Stop
        fun isRename() = this == Rename
        fun isOverwrite() = this == Overwrite
    }

    @JvmStatic
    fun getExternalStorageRoot(): String {
        val externalFilesDirs = SynoContextProvider.get().getExternalFilesDirs(null)

        val file = externalFilesDirs.firstOrNull {
            it != null && Environment.isExternalStorageEmulated(it)
        }

        return file?.path
            ?.substringBefore(PATH_ANDROID_DATA)
            ?: EXTERNAL_STORAGE_ROOT
    }

    @JvmStatic
    fun isScopedStorageEnvironment(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 30 ->
                !Environment.isExternalStorageLegacy() &&
                        !Environment.isExternalStorageManager()

            Build.VERSION.SDK_INT >= 29 ->
                !Environment.isExternalStorageLegacy()

            else -> false
        }
    }

    @JvmStatic
    fun isScopedStoragePath(path: String): Boolean {
        if (isAppFolderPath(path)) return false
        if (isLimitDataPath(path)) return true
        if (PermissionUtils.isStorageManager()) return false

        return isScopedStorageEnvironment() || !isInternalPath(path)
    }

    @JvmStatic
    fun isInternalPath(path: String): Boolean {
        return path.isUnderPath(getExternalStorageRoot())
    }

    @JvmStatic
    fun isAppFolderPath(path: String): Boolean {
        val externalFilesDirs = SynoContextProvider.get().getExternalFilesDirs("")

        for (file in externalFilesDirs) {
            val strRemoveSuffix = file?.path?.removeSuffix("files")
            if (path.isUnderPath(strRemoveSuffix)) return true
        }

        val path3 = SynoContextProvider.get()
            .filesDir
            .path
            .removeSuffix("files")

        return path.isUnderPath(path3)
    }

    @JvmStatic
    fun isLimitDataPath(path: String): Boolean {
        if (Build.VERSION.SDK_INT >= 30) {
            for (item in DATA_FOLDER_LIST) {
                val dataPath = File(getExternalStorageRoot(), item).path
                if (path.isUnderPath(dataPath)) return true
            }
        }
        return false
    }

    @JvmStatic
    fun isPathAccessible(targetPath: String): Boolean {
        val arrayList = getPersistedUris().map {
            getPathFromTreeUri(it) ?: ""
        }

        for (str in arrayList) {
            if (
                targetPath.isUnderPath(str) &&
                ObjectProvider.provideFile(str).exists()
            ) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getPersistedUris(): List<Uri> {
        return ObjectProvider
            .provideContext()
            .contentResolver
            .persistedUriPermissions
            .map { it.uri }
    }

    @JvmStatic
    fun getBaseInfoPair(
        context: Context,
        fullPath: String
    ): Pair<DocumentFile, String>? {

        var next: Uri? = null
        var pathFromTreeUri: String? = null

        for (uri in getPersistedUris()) {
            val cached = cacheBasePath[uri]

            if (cached != null && fullPath.isUnderPath(cached)) {
                next = uri
                pathFromTreeUri = cached
                break
            }

            val current = getPathFromTreeUri(uri)
            if (current != null) {
                cacheBasePath[uri] = current

                if (fullPath.isUnderPath(current)) {
                    next = uri
                    pathFromTreeUri = current
                    break
                }
            }
        }

        if (next == null || pathFromTreeUri == null) {
            return null
        }

        val documentFileFromTreeUri =
            cacheBaseDoc[next]
                ?: DocumentFile.fromTreeUri(context, next)?.also {
                    cacheBaseDoc[next] = it
                }
                ?: return null

        return documentFileFromTreeUri to pathFromTreeUri
    }

    @JvmStatic
    fun getDocumentFile(
        context: Context,
        fullPath: String
    ): DocumentFile? {

        val baseInfoPair = getBaseInfoPair(context, fullPath)
            ?: return null

        val uri = baseInfoPair.first.uri

        var strRemovePrefix =
            fullPath.removePrefix(baseInfoPair.second)

        if (uri.toString().endsWith("%3A")) {
            strRemovePrefix =
                strRemovePrefix.removePrefix("/")
        }

        return getDocumentForPath(
            context,
            strRemovePrefix,
            uri
        )
    }

    private fun getDocumentForPath(
        context: Context,
        filePath: String,
        baseDocUri: Uri
    ): DocumentFile? {
        val documentFile =
            DocumentFile.fromTreeUri(
                context,
                Uri.parse(baseDocUri.toString() + Uri.encode(filePath))
            )

        return if (documentFile?.exists() == true) {
            documentFile
        } else {
            null
        }
    }

    @JvmStatic
    @JvmOverloads
    fun createDocumentFile(
        context: Context,
        fullPath: String,
        isDir: Boolean = false
    ): DocumentFile? {

        val baseInfoPair =
            getBaseInfoPair(context, fullPath)
                ?: return null

        var first = baseInfoPair.first
        val uri = first.uri
        val second = baseInfoPair.second

        if (fullPath.equals(second, true)) {
            return first
        }

        val strSubstring =
            fullPath.substring(second.length)

        val strArr = strSubstring
            .removePrefix("/")
            .split("/")
            .toTypedArray()

        var path = "/"

        for (i in strArr.indices) {
            path = File(path, strArr[i]).path

            val documentForPath =
                getDocumentForPath(context, path, uri)

            first = when {
                documentForPath != null -> documentForPath
                i < strArr.lastIndex || isDir ->
                    first.createDirectory(strArr[i])
                else ->
                    first.createFile("", strArr[i])
            } ?: return null
        }

        return first
    }

    @JvmStatic
    fun getPathFromDocUri(docUri: Uri): String {
        return File(
            getPathFromTreeUri(docUri),
            getDocumentPathFromDocUri(docUri)
        ).path
    }

    @JvmStatic
    fun getPathFromTreeUri(treeUri: Uri): String? {
        val context = SynoContextProvider.get()

        val (first, secondRaw) =
            getVolumeInfoFromTreeUri(treeUri)

        val str = cacheVolumePath[treeUri]
            ?: getVolumePath(context, first)?.also {
                cacheVolumePath[treeUri] = it
            }
            ?: return null

        val second = secondRaw.trim('/')

        return if (second.isEmpty()) {
            str
        } else {
            str + File.separator + second
        }
    }

    @JvmStatic
    fun getDocumentPathFromDocUri(uri: Uri): String {
        val docId = DocumentsContract.getDocumentId(uri)
        val strArr = docId.split(":").toTypedArray()

        return if (strArr.size >= 2) {
            strArr[1]
        } else {
            File.separator
        }
    }

    private fun getVolumeInfoFromTreeUri(
        uri: Uri
    ): Pair<String, String> {

        val docId = DocumentsContract.getTreeDocumentId(uri)
        val strArr = docId.split(":").toTypedArray()

        val str = strArr[0]
        val second =
            if (strArr.size >= 2) strArr[1]
            else File.separator

        return str to second
    }

    fun getDocumentUriFromPath(path: String): Uri {
        var pathVar = path
        var zExists: Boolean

        val str = String.format(DOCUMENT_URI_PATTERN, "")

        if (!pathVar.startsWith(getExternalStorageRoot())) {
            return Uri.parse(str)
        }

        do {
            val fileProvideFile =
                ObjectProvider.provideFile(pathVar)

            zExists = fileProvideFile.exists()

            if (!zExists) {
                pathVar = fileProvideFile.parent
                    ?: getExternalStorageRoot()
            }
        } while (!zExists)

        val strRemovePrefix = pathVar
            .removePrefix(getExternalStorageRoot())
            .removePrefix("/")

        val strEncode = Uri.encode(strRemovePrefix)

        var string = str + strEncode

        for (str2 in DATA_FOLDER_LIST) {
            if (strRemovePrefix.startsWith(str2, true)) {
                val strEncode2 = Uri.encode(str2)

                val str3 = String.format(
                    DOCUMENT_URI_PATTERN,
                    strEncode2
                )

                string = str3 + strEncode
            }
        }

        return Uri.parse(string)
    }

    @JvmStatic
    fun hasSpecialCharacter(path: String): Boolean {
        return Regex(DENIED_CHAR_PATTERN)
            .containsMatchIn(path)
    }

    fun clearCache() {
        cacheBaseDoc.clear()
        cacheBasePath.clear()
        cacheVolumePath.clear()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(
        source: File,
        target: File,
        conflictAction: ConflictAction? = ConflictAction.Stop
    ): Boolean {
        if (conflictAction?.isStop() == true && target.exists()) {
            throw IllegalArgumentException("Target file already exist")
        }

        val synoFileProvideSynoFile =
            ObjectProvider.provideSynoFile(source)

        val synoFileProvideSynoFile2 =
            ObjectProvider.provideSynoFile(target)

        return if (conflictAction?.isOverwrite() ?: false) {
            synoFileProvideSynoFile.copyRecursivelySyno(
                synoFileProvideSynoFile2,
                true
            )
        } else {
            synoFileProvideSynoFile.copyRecursivelySyno(
                synoFileProvideSynoFile2.getValidFile(),
                false
            )
        }
    }

    private fun isValidPath(path: String): Boolean {
        return path.startsWith(
            "${File.separatorChar}storage${File.separatorChar}",
            ignoreCase = true
        )
    }

    @JvmStatic
    fun isSameVolume(
        sourcePath: String,
        targetPath: String
    ): Boolean {
        if (!isValidPath(sourcePath) || !isValidPath(targetPath)) {
            return false
        }

        val sourceVolume = sourcePath
            .split(File.separatorChar)
            .getOrNull(2)

        val targetVolume = targetPath
            .split(File.separatorChar)
            .getOrNull(2)

        return sourceVolume.equals(
            targetVolume,
            ignoreCase = true
        )
    }

    @JvmStatic
    fun isSameVolume(
        sourceFile: File,
        targetFile: File
    ): Boolean {
        return isSameVolume(
            sourceFile.path,
            targetFile.path
        )
    }

    @JvmStatic
    @Throws(
        IllegalAccessException::class,
        NoSuchMethodException::class,
        SecurityException::class,
        IllegalArgumentException::class,
        InvocationTargetException::class
    )
    private fun reflectionGetVolumeR(
        storageManager: StorageManager,
        volumeId: String
    ): String? {
        val method = StorageVolume::class.java.getMethod("getDirectory")

        val storageVolumes = storageManager.storageVolumes

        for (storageVolume in storageVolumes) {
            val uuid = storageVolume.uuid

            if (storageVolume.isPrimary && TAG_EXTERNAL_STORAGE == volumeId) {
                val objInvoke = method.invoke(storageVolume)
                val file = objInvoke as? File
                return file?.path
            }

            if (uuid != null && uuid == volumeId) {
                val objInvoke2 = method.invoke(storageVolume)
                val file2 = objInvoke2 as? File
                return file2?.path
            }
        }

        return null
    }

    @JvmStatic
    @Throws(
        IllegalAccessException::class,
        NoSuchMethodException::class,
        SecurityException::class,
        IllegalArgumentException::class,
        InvocationTargetException::class
    )
    private fun reflectionGetVolumeN(
        storageManager: StorageManager,
        volumeId: String
    ): String? {
        val method = StorageVolume::class.java.getMethod("getPath")

        val storageVolumes = storageManager.storageVolumes

        for (storageVolume in storageVolumes) {
            val uuid = storageVolume.uuid

            if (storageVolume.isPrimary && TAG_EXTERNAL_STORAGE == volumeId) {
                val objInvoke = method.invoke(storageVolume)
                return objInvoke as? String
            }

            if (uuid != null && uuid == volumeId) {
                val objInvoke2 = method.invoke(storageVolume)
                return objInvoke2 as? String
            }
        }

        return null
    }

    @JvmStatic
    private fun getVolumePath(
        context: Context,
        volumeId: String
    ): String? {
        return try {
            val storageManager = ContextCompat.getSystemService(
                context,
                StorageManager::class.java
            ) ?: return null

            if (volumeId == "home") {
                File(
                    getExternalStorageRoot(),
                    FOLDER_HOME
                ).path
            } else {
                if (Build.VERSION.SDK_INT >= 30) {
                    reflectionGetVolumeR(storageManager, volumeId)
                } else {
                    reflectionGetVolumeN(storageManager, volumeId)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}