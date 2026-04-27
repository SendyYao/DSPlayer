package com.synology.sylibx.synofile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.synology.sylibx.synofile.ObjectProvider.provideSynoFile
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utilities
import com.whisperyao.dsplayer.util.Utils
import java.io.File
import java.io.IOException


object ExtensionsKt {

    fun <K, V> Map<K, V>.getOrDefaultExt(k: K, v: V): V {
        val v2 = this[k]
        return v2 ?: v
    }

    fun String.isUnderPath(str2: String?): Boolean {
        if (str2 == null) {
            return false
        }

        val path = File(this).path
        val strReplace = path.replace("\\", "/")

        val path2 = File(str2).path
        val strReplace2 = path2.replace("\\", "/")

        if (strReplace.equals(strReplace2, true)) {
            return true
        }

        return strReplace.startsWith(strReplace2, true)
    }

    fun File.getValidFile(): File {
        val strParentPath = parentPath(this)
        val nameWithoutExtension = nameWithoutExtension

        val synoFileArrListFiles = provideSynoFile(strParentPath)
            .listFiles()

        val arrayList = ArrayList<String>(synoFileArrListFiles.size)

        for (synoFile in synoFileArrListFiles) {
            arrayList.add(synoFile.name)
        }

        var name = this.name
        var i = 1

        while (arrayList.contains(name)) {
            name = "$nameWithoutExtension ($i)"

            if (extension.isNotEmpty()) {
                name = "$name.$extension"
            }

            i++
        }

        return if (strParentPath.isNotEmpty()) {
            File(strParentPath, name)
        } else {
            File(name)
        }
    }

    private fun parentPath(file: File): String {
        val path = file.path

        var i = 0

        for (i2 in path.indices) {
            if (path[i2].toString() == File.separator) {
                i++
            }
        }

        if (i == 1) {
            return File.separator
        }

        return path.substringBeforeLast(File.separator, "")
    }

    fun Context.hasPermission(permission: String): Boolean {
        return try {
            val strArr = packageManager
                .getPackageInfo(packageName, 4096)
                .requestedPermissions

            strArr?.contains(permission) == true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun Context.registerReceiverCompat(
        receiver: BroadcastReceiver,
        intentFilter: IntentFilter,
        exported: Boolean = false
    ) {
        if (Utils.isSdk33()) {
            registerReceiver(
                receiver,
                intentFilter,
                if (exported)
                    Context.RECEIVER_EXPORTED
                else
                    Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(receiver, intentFilter)
        }
    }

    @Throws(IOException::class)
    fun SynoFile.copyToSyno(
        target: SynoFile,
        z: Boolean = false,
        i: Int = 8192
    ): SynoFile {
        if (!exists()) {
            throw NoSuchFileException(
                file = this,
                reason = "The source file doesn't exist."
            )
        }

        if (target.exists()) {
            if (!z) {
                throw FileAlreadyExistsException(
                    this,
                    target,
                    "The destination file already exists."
                )
            }

            if (!target.delete()) {
                throw FileAlreadyExistsException(
                    this,
                    target,
                    "Tried to overwrite the destination, but failed to delete it."
                )
            }
        }

        if (isDirectory) {
            if (!target.mkdirs()) {
                throw FileSystemException(
                    this,
                    target,
                    "Failed to create target directory."
                )
            }
        } else {
            val parentFile = target.parentFile
            parentFile?.mkdirs()

            if (target.isScopedStorage()) {
                target.createNewFile()
            }

            val inputStream = getInputStream()
                ?: throw FileSystemException(
                    this,
                    target,
                    "Get InputStream failed"
                )

            val outputStream = target.getOutputStream()
                ?: throw FileSystemException(
                    this,
                    target,
                    "Get OutputStream failed"
                )

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output, i)
                }
            }
        }

        return target
    }

    fun SynoFile.copyRecursivelySyno(
        target: File,
        z: Boolean = false,
        onError: (File, IOException) -> OnErrorAction = { _, exception ->
            throw exception
        }
    ): Boolean {
        if (!exists()) {
            return onError(
                this,
                NoSuchFileException(
                    this,
                    reason = "The source file doesn't exist."
                )
            ) != OnErrorAction.TERMINATE
        }

        return try {
            val it = walkTopDown()
                .onFail { f, e ->
                    if (onError(f, e) == OnErrorAction.TERMINATE) {
                        throw TerminateException(f)
                    }
                }
                .iterator()

            while (it.hasNext()) {
                val synoFileProvideSynoFile =
                    ObjectProvider.provideSynoFile(it.next())

                if (!synoFileProvideSynoFile.exists()) {
                    if (
                        onError(
                            synoFileProvideSynoFile,
                            NoSuchFileException(
                                synoFileProvideSynoFile,
                                reason = "The source file doesn't exist."
                            )
                        ) == OnErrorAction.TERMINATE
                    ) {
                        return false
                    }
                } else {
                    val relativeString =
                        synoFileProvideSynoFile.relativeTo(this).path

                    val path = target.path

                    val synoFileProvideSynoFile2 =
                        provideSynoFile(path, relativeString)

                    if (
                        synoFileProvideSynoFile2.exists() &&
                        (
                                !synoFileProvideSynoFile.isDirectory ||
                                        !synoFileProvideSynoFile2.isDirectory
                                )
                    ) {
                        if (z) {
                            if (synoFileProvideSynoFile2.isDirectory) {
                                synoFileProvideSynoFile2.deleteRecursively()
                            } else {
                                synoFileProvideSynoFile2.delete()
                            }
                        }

                        if (
                            onError(
                                synoFileProvideSynoFile2,
                                FileAlreadyExistsException(
                                    synoFileProvideSynoFile,
                                    synoFileProvideSynoFile2,
                                    "The destination file already exists."
                                )
                            ) == OnErrorAction.TERMINATE
                        ) {
                            return false
                        }
                    }

                    if (synoFileProvideSynoFile.isDirectory) {
                        synoFileProvideSynoFile2.mkdirs()
                    } else if (
                        synoFileProvideSynoFile
                            .copyToSyno(synoFileProvideSynoFile2, z)
                            .length() != synoFileProvideSynoFile.length()
                    ) {
                        if (
                            onError(
                                synoFileProvideSynoFile,
                                IOException(
                                    "Source file wasn't copied completely, length of destination file differs."
                                )
                            ) == OnErrorAction.TERMINATE
                        ) {
                            return false
                        }
                    }
                }
            }

            true
        } catch (e: TerminateException) {
            false
        }
    }

    fun Utilities.getProperFile(targetPath: String): SynoFile {
        val file = SynoFile(targetPath, null)

        if (!file.exists()) {
            SynoLog.d("getProperName", targetPath)
            return file
        }

        val dotIndex = targetPath.lastIndexOf(".")
        val prefix = targetPath.substring(0, dotIndex)
        val suffix = targetPath.substring(dotIndex)

        var index = 1

        while (true) {
            val path = "$prefix$index$suffix"
            SynoLog.d("getProperName", path)

            val properFile = SynoFile(path, null)

            if (!properFile.exists()) {
                return properFile
            }

            index++
        }
    }
}