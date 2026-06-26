package com.synology.sylibx.synofile

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import com.synology.sylib.utilities.contextprovider.SynoContextProvider;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

object ObjectProvider {

    interface ObserverEvent {
        fun onEvent(event: Int, path: String?)
    }

    fun provideContext(): Context {
        return SynoContextProvider.get()
    }

    fun provideSynoFile(file: File): SynoFile {
        return SynoFile(file)
    }

    fun provideSynoFile(path: String): SynoFile {
        return SynoFile(path)
    }

    fun provideSynoFile(path: String, name: String): SynoFile {
        return SynoFile(path, name)
    }

    fun provideFile(path: String): File {
        return File(path)
    }

    fun provideFileInputStream(file: File): FileInputStream {
        return FileInputStream(file)
    }

    fun provideFileInputStream(fileDescriptor: FileDescriptor): FileInputStream {
        return FileInputStream(fileDescriptor)
    }

    @JvmOverloads
    fun provideFileOutputStream(
        file: File,
        append: Boolean = true
    ): FileOutputStream {
        return if (append) {
            FileOutputStream(file, true)
        } else {
            FileOutputStream(file)
        }
    }

    fun provideFileOutputStream(
        fileDescriptor: FileDescriptor
    ): FileOutputStream {
        return FileOutputStream(fileDescriptor)
    }

    fun provideFileObserver(
        file: File,
        flag: Int,
        observer: ObserverEvent
    ): FileObserver {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(file, flag) {
                override fun onEvent(event: Int, path: String?) {
                    observer.onEvent(event, path)
                }
            }
        } else {
            throw UnsupportedOperationException(
                "Android SDK < 29 is not supported"
            )
        }
    }

    fun provideFileSeparator(): String {
        return File.separator
    }

    fun provideAlertDialogBuilder(
        context: Context,
        style: Int
    ): AlertDialog.Builder {
        return AlertDialog.Builder(context, style)
    }

    fun isExternalStorageManager(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            false
        }
    }
}