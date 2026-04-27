package com.whisperyao.dsplayer.util

import com.google.gson.Gson;
import com.whisperyao.dsplayer.Common;
import com.whisperyao.dsplayer.ConnectionManager;
import com.whisperyao.dsplayer.datasource.network.vo.api.ApiAudioInfoVo;
import com.whisperyao.dsplayer.datasource.network.vo.base.BaseAudioInfoVo;
import com.whisperyao.dsplayer.datasource.network.vo.cgi.CgiAudioInfoVo;
import com.whisperyao.dsplayer.vos.PackageInfoVo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;


object ObjFile {

    private const val AUDIO_INFO = "audiostation_info"
    private const val PACKAGE_INFO = "package_info"

    private val gson = Gson()

    @JvmStatic
    fun saveAudioInfoToFile(info: BaseAudioInfoVo?) {
        val clazz = if (
            info != null &&
            info.serverType == ConnectionManager.ResourceType.API
        ) {
            ApiAudioInfoVo::class.java
        } else {
            CgiAudioInfoVo::class.java
        }

        saveObjectToFile(info, getAudioInfoFile(), clazz)
    }

    @JvmStatic
    fun getAudioInfoFromFile(): BaseAudioInfoVo? {
        return getObjectFromFile(
            getAudioInfoFile(),
            CgiAudioInfoVo::class.java
        ) ?: getObjectFromFile(
            getAudioInfoFile(),
            ApiAudioInfoVo::class.java
        )
    }

    @JvmStatic
    private fun getAudioInfoFile(): File {
        return File(Common.getDSaudioAppFolder() + AUDIO_INFO)
    }

    @JvmStatic
    fun savePackageInfoToFile(info: PackageInfoVo?) {
        saveObjectToFile(
            info,
            getPackageInfoFile(),
            PackageInfoVo::class.java
        )
    }

    @JvmStatic
    fun getPackageInfoFromFile(): PackageInfoVo? {
        return getObjectFromFile(
            getPackageInfoFile(),
            PackageInfoVo::class.java
        )
    }

    private fun getPackageInfoFile(): File {
        return File(Common.getDSaudioAppFolder() + PACKAGE_INFO)
    }

    @JvmStatic
    fun <T> saveObjectToFile(
        obj: Any?,
        file: File,
        clazz: Class<T>
    ) {
        try {
            if (file.exists()) {
                file.delete()
            }

            file.createNewFile()

            FileWriter(file.absoluteFile).use { writer ->
                gson.toJson(obj, clazz, writer)
                writer.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun <T> getObjectFromFile(
        file: File,
        clazz: Class<T>
    ): T? {
        if (!file.exists()) {
            return null
        }

        return try {
            FileInputStream(file).use { input ->
                InputStreamReader(input).use { reader ->
                    gson.fromJson(reader, clazz)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun <T> getObjectFromFile(
        file: File,
        type: Type
    ): T? {
        if (!file.exists()) {
            return null
        }

        return try {
            FileInputStream(file).use { input ->
                InputStreamReader(input).use { reader ->
                    gson.fromJson<T>(reader, type)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}