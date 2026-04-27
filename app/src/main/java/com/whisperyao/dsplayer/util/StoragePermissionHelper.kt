package com.whisperyao.dsplayer.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.synology.sylibx.synofile.GrantResult
import com.synology.sylibx.synofile.GrantStatus
import com.synology.sylibx.synofile.PermissionUtils
import com.synology.sylibx.synofile.SAFUtils
import com.synology.sylibx.synofile.SynoFile
import com.whisperyao.dsplayer.R
import java.io.File

object StoragePermissionHelper {

    private const val APP_NAME = "DSaudio"
    const val REQUEST_CODE_AUTO_DOWNLOAD = 9478
    const val REQUEST_CODE_STORAGE = 9487
    private const val TAG = "StoragePermissionHelper"

    private var hintDialog: AlertDialog? = null

    val defaultFolderPath: String =
        SAFUtils.getExternalStorageRoot() + "/DSaudio"

    fun permissionGranted(): Boolean {
        return getGrantStatus() == GrantStatus.Granted
    }

    private fun getGrantStatus(): GrantStatus {
        return PermissionUtils.checkGrantStatus(
            File(AudioPreference.getSongCacheFolder())
        )
    }

    @JvmOverloads
    fun showFilePicker(
        fragment: Fragment,
        requestCode: Int = REQUEST_CODE_STORAGE
    ) {
        PermissionUtils.checkPermission(
            fragment,
            requestCode
        )
    }

    @JvmOverloads
    fun showFilePicker(
        activity: Activity,
        requestCode: Int = REQUEST_CODE_STORAGE
    ) {
        PermissionUtils.checkPermission(
            activity,
            requestCode
        )
    }

    @JvmOverloads
    @JvmStatic
    fun showHintsThenAskPermission(
        fragment: Fragment,
        requestCode: Int = REQUEST_CODE_STORAGE
    ) {
        if (hintDialog?.isShowing == true) return

        hintDialog = AlertDialog.Builder(fragment.requireContext())
            .setMessage(R.string.storage_permission_request_message)
            .setPositiveButton(R.string.str_ok) { _, _ ->
                if (SAFUtils.isScopedStorageEnvironment()) {
                    showFilePicker(fragment, requestCode)
                } else {
                    PermissionUtils.checkPermission(
                        fragment,
                        requestCode,
                        SAFUtils.getExternalStorageRoot()
                    )
                }
            }
            .create()

        hintDialog?.show()
    }

    @JvmOverloads
    fun showHintsThenAskPermission(
        activity: Activity,
        requestCode: Int = REQUEST_CODE_STORAGE
    ) {
        if (hintDialog?.isShowing == true) return

        hintDialog = AlertDialog.Builder(activity)
            .setMessage(R.string.storage_permission_request_message)
            .setPositiveButton(R.string.str_ok) { _, _ ->
                if (SAFUtils.isScopedStorageEnvironment()) {
                    showFilePicker(activity, requestCode)
                } else {
                    PermissionUtils.checkPermission(
                        activity,
                        requestCode,
                        SAFUtils.getExternalStorageRoot()
                    )
                }
            }
            .create()

        hintDialog?.show()
    }

    fun onActivityResult(
        activity: Activity,
        resultCode: Int,
        data: Intent?
    ) {
        if (GrantResult.isGranted(resultCode)) {
            data?.data?.let {
                handleUriTreeAndCreateWorkPath(it)
            }
            return
        }

        if (permissionGranted()) return

        if (!SAFUtils.isScopedStorageEnvironment() &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            openAppSettingsIntent(activity)
        } else {
            Toast.makeText(
                activity,
                R.string.storage_permission_denied_message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @JvmStatic
    fun onActivityResult(
        fragment: Fragment,
        resultCode: Int,
        data: Intent?
    ) {
        if (GrantResult.isGranted(resultCode)) {
            data?.data?.let {
                handleUriTreeAndCreateWorkPath(it)
            }
            return
        }

        val activity = fragment.activity ?: return
        if (permissionGranted()) return

        if (!SAFUtils.isScopedStorageEnvironment() &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            openAppSettingsIntent(activity)
        } else {
            Toast.makeText(
                activity,
                R.string.storage_permission_denied_message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleUriTreeAndCreateWorkPath(treeUri: Uri) {
        var treePath = SAFUtils.getPathFromTreeUri(treeUri) ?: return

        SynoLog.d(TAG, "granted path = $treePath")

        if (treePath == SAFUtils.getExternalStorageRoot()) {
            val appPath = File(treePath, APP_NAME).path

            val created = SynoFile(appPath).mkdir()

            treePath = if (created) {
                appPath
            } else {
                val existingFolder = SynoFile(treePath)
                    .listFiles()
                    .firstOrNull {
                        it.name.equals(APP_NAME, ignoreCase = true)
                    }

                existingFolder?.let {
                    File(treePath, it.name).path
                } ?: ""
            }
        }

        if (treePath.isNotEmpty()) {
            AudioPreference.setSongCacheFolder(treePath)
        }
    }

    private fun openAppSettingsIntent(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}