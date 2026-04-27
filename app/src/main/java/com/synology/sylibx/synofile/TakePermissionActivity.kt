package com.synology.sylibx.synofile


import android.content.Intent;
import android.net.Uri;
import android.os.Build
import android.os.Bundle;
import com.synology.sylib.utilities.activityinterceptor.BaseResultInterceptorActivity;
import com.synology.sylib.utilities.contextprovider.SynoContextProvider;
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class TakePermissionActivity : BaseResultInterceptorActivity() {

    companion object {
        private const val EXTRA_GRANT_MANAGER = "grant_manager"
        private const val EXTRA_GRANT_STATUS = "grant_status"
        private const val EXTRA_SHOW_SAF_HINT = "show_saf_hint"
        private const val EXTRA_TARGET_PATH = "target_path"

        @JvmStatic
        @JvmOverloads
        fun getRequestIntent(
            status: GrantStatus,
            targetPath: String,
            showSAFHint: Boolean = true
        ): Intent {
            val intent = if (status.isNeedManager) {
                Intent(
                    "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
                    Uri.parse("package:${SynoContextProvider.get().packageName}")
                ).apply {
                    putExtra(EXTRA_GRANT_MANAGER, true)
                }
            } else {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    addFlags(65536)

                    putExtra(
                        "android.provider.extra.INITIAL_URI",
                        SAFUtils.getDocumentUriFromPath(targetPath)
                    )
                }
            }

            intent.putExtra(EXTRA_GRANT_STATUS, status.value)
            intent.putExtra(EXTRA_TARGET_PATH, targetPath)
            intent.putExtra(EXTRA_SHOW_SAF_HINT, showSAFHint)

            return intent
        }
    }

    private lateinit var grantStatus: GrantStatus
    private var isGrantManager: Boolean = false
    private var showSAFHint: Boolean = true
    private var targetPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)

        grantStatus = GrantStatus.fromValue(
            getRawIntent().getStringExtra(EXTRA_GRANT_STATUS)
        )

        targetPath = getRawIntent().getStringExtra(EXTRA_TARGET_PATH) ?: ""

        isGrantManager = getRawIntent().getBooleanExtra(
            EXTRA_GRANT_MANAGER,
            false
        )

        showSAFHint = getRawIntent().getBooleanExtra(
            EXTRA_SHOW_SAF_HINT,
            true
        )
    }

    fun requestPermission() {
        super.onStartIntercept()
    }

    override fun onStartIntercept() {
        when {
            grantStatus.isNeedManager -> {
                requestPermission()
            }

            grantStatus.isNeedSAF -> {
                val type = when {
                    grantStatus == GrantStatus.NeedSAFForSD -> {
                        HintDialogType.SD
                    }

                    Build.VERSION.SDK_INT < 30 &&
                            targetPath.isNotEmpty() -> {
                        HintDialogType.Root
                    }

                    else -> {
                        HintDialogType.Folder
                    }
                }

                if (showSAFHint) {
                    PermissionUtils.showHintDialog(
                        this,
                        type,
                        grantAction = {
                            requestPermission()
                        },
                        cancelAction = {
                            setResult(GrantResult.Cancel.code)
                            finish()
                        }
                    )
                } else {
                    requestPermission()
                }
            }

            grantStatus.isNeedStorage -> {
                Dexter.withContext(this)
                    .withPermission(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    .withListener(
                        object : PermissionListener {
                            override fun onPermissionGranted(
                                response: PermissionGrantedResponse?
                            ) {
                                setResult(GrantResult.Granted.code)
                                finish()
                            }

                            override fun onPermissionDenied(
                                response: PermissionDeniedResponse?
                            ) {
                                setResult(GrantResult.Denied.code)
                                finish()
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: PermissionRequest?,
                                token: PermissionToken?
                            ) {
                                token?.continuePermissionRequest()
                            }
                        }
                    )
                    .check()
            }

            grantStatus.isGranted -> {
                setResult(GrantResult.Granted.code)
                finish()
            }

            else -> {
                setResult(GrantResult.Cancel.code)
                finish()
            }
        }
    }

    override fun onActivityResultCallback(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        if (isGrantManager) {
            setResult(
                if (ObjectProvider.isExternalStorageManager()) {
                    GrantResult.Granted.code
                } else {
                    GrantResult.Denied.code
                }
            )
            return true
        }

        PermissionUtils.handlePathPermissionResult(data)

        if (resultCode == RESULT_OK) {
            if (targetPath.isEmpty()) {
                setResult(GrantResult.Granted.code, data)
                return true
            }

            setResult(
                if (SAFUtils.isPathAccessible(targetPath)) {
                    GrantResult.Granted.code
                } else {
                    GrantResult.Denied.code
                }
            )
            return true
        }

        setResult(GrantResult.Cancel.code)
        return true
    }
}