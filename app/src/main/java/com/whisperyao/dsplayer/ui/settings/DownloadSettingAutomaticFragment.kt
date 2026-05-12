package com.whisperyao.dsplayer.ui.settings

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.synology.sylibx.synofile.SynoFile
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.provider.DatabaseAccesser
import com.whisperyao.dsplayer.ui.BasePreferenceFragment
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.extension.ExtensionsKt.safeDismiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DownloadSettingAutomaticFragment : BasePreferenceFragment() {

    private val originalCacheLimit = AudioPreference.getSongCacheLimit()

    @Inject
    lateinit var progressDialog: ProgressDialog

    override fun getTitleResId(): Int {
        return R.string.cache_setting_automatic
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        addPreferencesFromResource(R.xml.preferences_download_automatic)

        findPreference<Preference>("clear_auto_cache")
            ?.setOnPreferenceClickListener {
                showClearCacheDialog()
                true
            }
    }

    override fun onPause() {
        if (originalCacheLimit != AudioPreference.getSongCacheLimit()) {
            CacheManager.getInstance().rotateSong(0L)
            Common.gIsClearLocalCache = true
        }

        super.onPause()
    }

    private fun showClearCacheDialog() {
        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.cache_setting_delete_all_automatic_songs)
            .setMessage(R.string.msg_delete_all_auto_cache)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteAllAutomaticCache()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteAllAutomaticCache() {
        progressDialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    clearAutomaticCacheInternal()
                }
            } finally {
                progressDialog.safeDismiss()
            }
        }
    }

    private fun clearAutomaticCacheInternal() {
        val databaseAccessor = DatabaseAccesser.getInstance()

        try {
            val songs = databaseAccessor.doEnumAllSongs(null, 1)

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

            databaseAccessor.deleteAllSongs(1)

            AudioPreference.setAutoCacheSize(0L)

            Common.gIsClearLocalCache = true

        } catch (_: Exception) {

        } finally {
            databaseAccessor.close()
        }
    }
}