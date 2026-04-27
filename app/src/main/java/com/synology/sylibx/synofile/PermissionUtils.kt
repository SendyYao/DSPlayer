package com.synology.sylibx.synofile


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.synology.sylib.utilities.activityinterceptor.ActivityInterceptor
import com.synology.sylib.utilities.contextprovider.SynoContextProvider
import com.synology.sylibx.synofile.ExtensionsKt.hasPermission
import com.synology.sylibx.synofile.TakePermissionActivity
import com.whisperyao.dsplayer.R
import java.io.File
import kotlin.Unit
import kotlin.jvm.JvmStatic


object PermissionUtils {

    @JvmStatic
    @JvmOverloads
    fun checkPermission(
        activity: Activity?,
        requestCode: Int,
        targetPath: String = "",
        requiredExist: Boolean = true,
        showSAFHint: Boolean = true
    ): GrantStatus {
        val status = checkWithTargetPath(targetPath, requiredExist)

        if (status.isNeedRequest) {
            requestFilePermission(
                activity,
                requestCode,
                status,
                targetPath,
                showSAFHint
            )
        }

        return status
    }

    @JvmStatic
    @JvmOverloads
    fun checkPermission(
        fragment: Fragment?,
        requestCode: Int,
        targetPath: String = "",
        requiredExist: Boolean = true,
        showSAFHint: Boolean = true
    ): GrantStatus {
        val status = checkWithTargetPath(targetPath, requiredExist)

        if (status.isNeedRequest) {
            requestFilePermission(
                fragment,
                requestCode,
                status,
                targetPath,
                showSAFHint
            )
        }

        return status
    }

    @JvmStatic
    @JvmOverloads
    fun requestFilePermission(
        activity: Activity?,
        requestCode: Int,
        status: GrantStatus,
        targetPath: String,
        showSAFHint: Boolean = true
    ) {
        val intent = TakePermissionActivity.getRequestIntent(
            status,
            targetPath,
            showSAFHint
        )

        activity?.let {
            ActivityInterceptor.activityForResult(
                TakePermissionActivity::class.java,
                it,
                intent,
                requestCode,
                null,
                null
            )
        }
    }

    @JvmStatic
    @JvmOverloads
    fun requestFilePermission(
        fragment: Fragment?,
        requestCode: Int,
        status: GrantStatus,
        targetPath: String,
        showSAFHint: Boolean = true
    ) {
        val context = fragment?.context ?: return

        val requestIntent = TakePermissionActivity.getRequestIntent(
            status,
            targetPath,
            showSAFHint
        )

        val intent = ActivityInterceptor.activityForResultIntent(
            TakePermissionActivity::class.java,
            context,
            requestIntent,
            requestCode,
            null,
            null
        ) ?: return

        fragment.startActivityForResult(intent, requestCode)
    }

    @JvmStatic
    fun handlePathPermissionResult(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false

        return try {
            SynoContextProvider.get()
                .contentResolver
                .takePersistableUriPermission(uri, 3)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    @JvmStatic
    fun checkGrantStatus(folderFile: File): GrantStatus {
        if (Environment.getExternalStorageState(folderFile) != "mounted") {
            return GrantStatus.Invalid
        }

        val path = folderFile.path

        return when {
            SAFUtils.isAppFolderPath(path) -> {
                GrantStatus.Granted
            }

            SAFUtils.isLimitDataPath(path) -> {
                if (!SAFUtils.isPathAccessible(path)) {
                    GrantStatus.NeedSAF
                } else {
                    GrantStatus.Granted
                }
            }

            hasStorageManagerPermission() -> {
                if (!isStorageManager()) {
                    GrantStatus.NeedManager
                } else {
                    GrantStatus.Granted
                }
            }

            !SAFUtils.isInternalPath(path) -> {
                if (!SAFUtils.isPathAccessible(path)) {
                    GrantStatus.NeedSAFForSD
                } else {
                    GrantStatus.Granted
                }
            }

            SAFUtils.isScopedStorageEnvironment() -> {
                if (!SAFUtils.isPathAccessible(path)) {
                    GrantStatus.NeedSAF
                } else {
                    GrantStatus.Granted
                }
            }

            !isStoragePermissionGranted() -> {
                GrantStatus.NeedStorage
            }

            else -> GrantStatus.Granted
        }
    }

    @JvmStatic
    fun isStoragePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            SynoContextProvider.get(),
            "android.permission.WRITE_EXTERNAL_STORAGE"
        ) == 0 || isStorageManager()
    }

    @JvmStatic
    fun hasStorageManagerPermission(): Boolean {
        return Build.VERSION.SDK_INT >= 30 &&
                SynoContextProvider.get().hasPermission("android.permission.MANAGE_EXTERNAL_STORAGE")
    }

    @JvmStatic
    fun isStorageManager(): Boolean {
        return Build.VERSION.SDK_INT >= 30 &&
                Environment.isExternalStorageManager()
    }

    private fun checkWithTargetPath(
        targetPath: String,
        requiredExist: Boolean
    ): GrantStatus {
        val status = if (targetPath.isNotEmpty()) {
            val file = ObjectProvider.provideFile(targetPath)

            if (requiredExist && !file.exists()) {
                GrantStatus.Invalid
            } else {
                checkGrantStatus(file)
            }
        } else {
            GrantStatus.NeedSAF
        }

        return if (
            status.isNeedSAF &&
            !pickerAvailable()
        ) {
            GrantStatus.PickerUnAvailable
        } else {
            status
        }
    }

    @JvmStatic
    @JvmOverloads
    fun showHintDialog(
        context: Context,
        type: HintDialogType,
        grantAction: () -> Unit,
        cancelAction: (() -> Unit)? = null
    ) {
        val appName = getAppName(context)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.fragment_grant_permisssion, null)

        val description = view.findViewById<TextView>(R.id.description)
        val image = view.findViewById<ImageView>(R.id.image)

        val stringRes = when (type) {
            HintDialogType.SD -> R.string.sdcard
            HintDialogType.Root -> R.string.internal_storage
            else -> R.string.folder
        }

        val imageRes = when (type) {
            HintDialogType.SD -> R.drawable.thumbnail_sd_permission
            HintDialogType.Root -> R.drawable.thumbnail_storage_permission
            else -> R.drawable.thumbnail_folder_permission
        }

        val targetName = context.getString(stringRes)

        description.text = context.getString(
            R.string.storage_description,
            appName,
            targetName
        )

        image.setImageResource(imageRes)

        ObjectProvider.provideAlertDialogBuilder(
            context,
            0x7f140342
        )
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                grantAction()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                cancelAction?.invoke()
            }
            .setOnCancelListener {
                cancelAction?.invoke()
            }
            .show()
    }

    private fun getAppName(context: Context): String {
        val info = context.packageManager.getApplicationInfo(
            context.applicationInfo.packageName,
            0
        )

        return context.packageManager
            .getApplicationLabel(info)
            .toString()
    }

    private fun pickerAvailable(): Boolean {
        return TakePermissionActivity.getRequestIntent(
            GrantStatus.NeedSAF,
            "",
            false
        ).resolveActivity(
            SynoContextProvider.get().packageManager
        ) != null
    }
}