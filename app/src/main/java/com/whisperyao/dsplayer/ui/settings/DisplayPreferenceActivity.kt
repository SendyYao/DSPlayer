package com.whisperyao.dsplayer.ui.settings

import android.os.Bundle
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.activity.BaseActivity
import com.whisperyao.dsplayer.databinding.ActivitySettingsBinding
import com.whisperyao.dsplayer.datasource.network.ConnectionManager
import com.whisperyao.dsplayer.ui.BasePreferenceFragment

class DisplayPreferenceActivity : BaseActivity() {

    companion object {
        private const val FRAGMENT_TAG__PREFS = "PrefsFragment"
    }

    private var mPrefsFragment: BasePreferenceFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        toolbar.setTitle(R.string.settings)

        val fragmentTypeKey = getString(R.string.fragment_type)

        val fragmentType = intent?.extras?.getString(fragmentTypeKey)

        val prefsFragment: BasePreferenceFragment? = when (fragmentType) {
            getString(R.string.type_analytics) -> {
                // PrefsShareAnalyticsFragment()
                null
            }

            getString(R.string.type_auto_download) -> {
                DownloadSettingAutomaticFragment()
            }

            getString(R.string.type_manual_download) -> {
                DownloadSettingManualFragment()
            }

            getString(R.string.type_login_settings) -> {
                // LoginSettingsFragment()
                null
            }

            getString(R.string.type_settings) -> {
                SettingsFragment()
            }

            getString(R.string.developer_mode) -> {
                // PrefDeveloperModeFragment()
                null
            }

            else -> null
        }

        mPrefsFragment = prefsFragment

        prefsFragment?.let { fragment ->
            toolbar.setTitle(fragment.titleResId)

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    fragment,
                    FRAGMENT_TAG__PREFS
                )
                .commit()
        }
    }

    override fun provideConnectionManager(): ConnectionManager {
        return connectionManager
    }
}