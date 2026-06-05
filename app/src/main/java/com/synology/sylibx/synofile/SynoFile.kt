package com.synology.sylibx.synofile

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.synology.sylib.utilities.contextprovider.SynoContextProvider
import java.io.File
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import com.synology.sylibx.synofile.Extensions.getValidFile
import com.whisperyao.dsplayer.util.SynoLog
import java.io.*
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes

open class SynoFile(
    var mContext: Context,
    file: File,
    var mDocFile: DocumentFile? = null
) : File(file.path) {

    private val scopedStorageDelegate by lazy {
        SAFUtils.isScopedStoragePath(path)
    }

    fun isScopedStorage(): Boolean {
        return scopedStorageDelegate
    }

    enum class OpenMode(val modeString: String) {
        ReadOnly("r"),
        WriteOnly("wt"),
        ReadWrite("rwt"),
        Append("wa"),
        ReadAppend("rwa");

        val modeFlag: Int
            get() {
                var flag = 0
                if ("r" in modeString) flag = flag or 0x10000000
                if ("w" in modeString) flag = flag or 0x28000000
                if ("t" in modeString) flag = flag or 0x00080000
                if ("a" in modeString) flag = flag or 0x02000000
                return flag
            }

        fun canAppend(): Boolean {
            return this == Append || this == ReadAppend
        }
    }

    constructor(context: Context, file: File) : this(context, file, null)

    constructor(context: Context, path: String) : this(
        context,
        File(path),
        null
    )

    constructor(
        context: Context,
        parent: String,
        child: String
    ) : this(
        context,
        File(parent, child),
        null
    )

    constructor(file: File) : this(
        SynoContextProvider.get(),
        file,
        null
    )

    constructor(path: String) : this(
        SynoContextProvider.get(),
        File(path),
        null
    )

    constructor(parent: String, child: String) : this(
        SynoContextProvider.get(),
        File(parent, child),
        null
    )

    constructor(docFile: DocumentFile) : this(
        SynoContextProvider.get(),
        File(SAFUtils.getPathFromDocUri(docFile.uri)),
        docFile
    )

    constructor(
        path: String,
        documentFile: DocumentFile?
    ) : this(
        SynoContextProvider.get(),
        File(path),
        documentFile
    )

    constructor(
        context: Context,
        path: String,
        documentFile: DocumentFile?
    ) : this(
        context,
        File(path),
        documentFile
    )

    constructor(
        parent: String,
        child: String,
        documentFile: DocumentFile?
    ) : this(
        SynoContextProvider.get(),
        File(parent, child),
        documentFile
    )

    constructor(
        context: Context,
        parent: String,
        child: String,
        documentFile: DocumentFile?
    ) : this(
        context,
        File(parent, child),
        documentFile
    )

    constructor(
        file: File,
        documentFile: DocumentFile?
    ) : this(
        SynoContextProvider.get(),
        file,
        documentFile
    )

    init {
        if (mDocFile != null || isScopedStorage()) {
            if (mDocFile == null) {
                mDocFile = SAFUtils.getDocumentFile(
                    mContext,
                    canonicalPath
                )
            }
        }
    }

    fun isAccessible(): Boolean {
        return PermissionUtils.checkGrantStatus(this).isGranted
    }

    fun getUri(pathIfLegacy: Boolean = true): Uri? {
        return if (!isScopedStorage()) {
            if (pathIfLegacy) Uri.parse(path)
            else Uri.fromFile(this)
        } else {
            mDocFile?.uri
        }
    }

    override fun list(): Array<String> {
        if (!isScopedStorage()) {
            return super.list() ?: emptyArray()
        }

        return listFiles().map { it.name }.toTypedArray()
    }

    override fun listFiles(): Array<SynoFile> {
        if (!isScopedStorage()) {
            return super.listFiles()
                ?.map { SynoFile(mContext, it) }
                ?.toTypedArray()
                ?: emptyArray()
        }

        return listFileWithFilter().toTypedArray()
    }

    override fun listFiles(filter: FileFilter): Array<SynoFile> {
        if (!isScopedStorage()) {
            return super.listFiles(filter)
                ?.map { SynoFile(mContext, it) }
                ?.toTypedArray()
                ?: emptyArray()
        }

        return listFileWithFilter(filter).toTypedArray()
    }

    private fun listFileWithFilter(
        filter: FileFilter? = null
    ): List<SynoFile> {
        val result = arrayListOf<SynoFile>()

        mDocFile?.listFiles()?.forEach { documentFile ->
            val documentPath =
                SAFUtils.getDocumentPathFromDocUri(documentFile.uri)

            if (filter == null || filter.accept(File(documentPath))) {
                val fileName = File(documentPath).name
                result.add(
                    SynoFile(
                        mContext,
                        path,
                        fileName,
                        documentFile
                    )
                )
            }
        }

        return result
    }

    override fun delete(): Boolean {
        if (!isScopedStorage()) {
            return super.delete()
        }

        val result = mDocFile?.delete() ?: false
        mDocFile = null
        return result
    }

    private fun createDoc(isDir: Boolean): DocumentFile? {
        return SAFUtils.createDocumentFile(
            mContext,
            canonicalPath,
            isDir
        )
    }

    override fun createNewFile(): Boolean {
        if (!isScopedStorage()) {
            parentFile?.mkdirs()
            return super.createNewFile()
        }

        if (mDocFile != null) return false

        mDocFile = createDoc(false)
        return mDocFile != null
    }

    override fun mkdir(): Boolean {
        if (!isScopedStorage()) {
            return super.mkdir()
        }
        SynoLog.i("SynoFile", "mDocFile != null: ${mDocFile != null}")
        if (mDocFile != null) return false

        mDocFile = createDoc(true)
        return mDocFile != null
    }

    override fun mkdirs(): Boolean {
        return if (!isScopedStorage()) {
            super.mkdirs()
        } else {
            mkdir()
        }
    }

    fun renameTo(
        dest: File,
        conflictAction: SAFUtils.ConflictAction
    ): Boolean {
        val destPath = dest.path

        if (SAFUtils.hasSpecialCharacter(destPath)) {
            return false
        }

        if (SAFUtils.isSameVolume(this, dest) &&
            !isScopedStorage()
        ) {
            return if (conflictAction.isRename()) {
                super.renameTo(
                    dest.getValidFile()
                )
            } else {
                super.renameTo(dest)
            }
        }

        if (parent == dest.parent &&
            conflictAction.isRename()
        ) {
            return mDocFile?.renameTo(dest.name) ?: false
        }

        return try {
            if (!SAFUtils.copyFile(this, dest, conflictAction)) {
                false
            } else {
                deleteRecursively()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun renameTo(dest: File): Boolean {
        return renameTo(dest, SAFUtils.ConflictAction.Rename)
    }

    override fun canRead(): Boolean {
        return if (!isScopedStorage()) {
            super.canRead()
        } else {
            mDocFile?.canRead() ?: false
        }
    }

    override fun canWrite(): Boolean {
        return if (!isScopedStorage()) {
            super.canWrite()
        } else {
            mDocFile?.canWrite() ?: false
        }
    }

    fun getParentSynoFile(): SynoFile {
        return SynoFile(mContext, parent, getParentDocFile())
    }

    fun getParentDocFile(): DocumentFile? {
        return mDocFile?.parentFile
            ?: parentFile?.let {
                SynoFile(mContext, it).mDocFile
            }
    }

    fun getFileDescriptor(
        mode: OpenMode = OpenMode.ReadOnly
    ): ParcelFileDescriptor? {
        return if (!isScopedStorage()) {
            ParcelFileDescriptor.open(this, mode.modeFlag)
        } else {
            getUri(false)?.let {
                mContext.contentResolver
                    .openFileDescriptor(it, mode.modeString)
            }
        }
    }

    fun getInputStream(): InputStream? {
        return if (!isScopedStorage()) {
            FileInputStream(this)
        } else {
            getUri(false)?.let {
                mContext.contentResolver.openInputStream(it)
            }
        }
    }

    fun getOutputStream(
        mode: OpenMode = OpenMode.ReadWrite
    ): OutputStream? {
        return if (!isScopedStorage()) {
            FileOutputStream(this, mode.canAppend())
        } else {
            getUri(false)?.let {
                mContext.contentResolver
                    .openOutputStream(it, mode.modeString)
            }
        }
    }

    fun getExtension(): String {
        val name = name
        if (!name.contains(".") || name.startsWith(".")) {
            return ""
        }
        return name.substringAfterLast(".")
    }

    fun getMimeType(): String {
        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(getExtension())
            ?: "application/octet-stream"
    }

    fun getAttribute(): BasicFileAttributes? {
        return try {
            Files.readAttributes(
                toPath(),
                BasicFileAttributes::class.java,
                *emptyArray<LinkOption>()
            )
        } catch (_: IOException) {
            null
        }
    }

    fun getFileKey(): String? {
        val value = getAttribute()?.fileKey().toString()
        val regex = Regex("\\(dev=[0-9a-fA-F]+?,ino=(\\d+)\\)")

        return if (regex.matches(value)) {
            regex.replace(value, "$1")
        } else {
            null
        }
    }

    override fun isDirectory(): Boolean {
        return if (!isScopedStorage()) {
            super.isDirectory()
        } else {
            mDocFile?.isDirectory ?: false
        }
    }

    override fun isFile(): Boolean {
        return if (!isScopedStorage()) {
            super.isFile()
        } else {
            mDocFile?.isFile ?: false
        }
    }

    override fun lastModified(): Long {
        return if (!isScopedStorage()) {
            super.lastModified()
        } else {
            mDocFile?.lastModified() ?: 0L
        }
    }

    override fun exists(): Boolean {
        return if (!isScopedStorage()) {
            super.exists()
        } else {
            mDocFile?.exists() ?: false
        }
    }

    override fun length(): Long {
        return if (!isScopedStorage()) {
            super.length()
        } else {
            mDocFile?.length() ?: 0L
        }
    }
}