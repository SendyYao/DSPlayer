package com.whisperyao.dsplayer.ui.settings

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.synology.sylibx.synofile.SynoFile
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.provider.DatabaseAccesser
import com.whisperyao.dsplayer.ui.BasePreferenceFragment
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.extension.Extensions.safeDismiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DownloadSettingManualFragment : BasePreferenceFragment() {

    @Inject
    lateinit var progressDialog: ProgressDialog

    override fun getTitleResId(): Int {
        return R.string.cache_setting_manual
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        addPreferencesFromResource(R.xml.preferences_download_manual)

        findPreference<Preference>("clear_all_manual_cache")
            ?.setOnPreferenceClickListener {
                showClearCacheDialog()
                true
            }
    }

    private fun showClearCacheDialog() {
        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.cache_setting_delete_all_manual_songs)
            .setMessage(R.string.msg_delete_all_manual_cache)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteAllManualCache()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteAllManualCache() {
        progressDialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    clearManualCacheInternal()
                }
            } finally {
                progressDialog.safeDismiss()
            }
        }
    }

    private fun clearManualCacheInternal() {
        val databaseAccessor = DatabaseAccesser.getInstance()

        try {
            val songs = databaseAccessor.doEnumAllSongs(null, 2)

            songs?.forEach { song ->

                song.cachePath?.let { path ->
                    SynoFile(path).delete()
                }

                song.coverPath?.let { path ->
                    SynoFile(path).delete()
                }

                song.lyricPath?.let { path ->
                    SynoFile(path).delete()
                }
            }

            databaseAccessor.deleteAllManual()

            AudioPreference.setManualCacheSize(0L)

            Common.gIsClearLocalCache = true

        } catch (_: Exception) {

        } finally {
            databaseAccessor.close()
        }
    }
}