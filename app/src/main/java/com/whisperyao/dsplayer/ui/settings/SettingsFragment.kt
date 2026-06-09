package com.whisperyao.dsplayer.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
// import com.synology.sylibx.applog.ui.SyLogUi
import com.synology.sylibx.synofile.SAFUtils
import com.whisperyao.dsplayer.CacheManager
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.ConnectionManager
// import com.whisperyao.dsplayer.EqualizerChooserActivity
import com.whisperyao.dsplayer.PlayerChooserActivity
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.net.WebAPI
import com.whisperyao.dsplayer.playing.EqualizerSettings
import com.whisperyao.dsplayer.ui.BasePreferenceFragment
import com.whisperyao.dsplayer.ui.preference.LoginLogoutPreference
import com.whisperyao.dsplayer.ui.preference.VersionPreference
import com.whisperyao.dsplayer.util.AudioPreference
import com.whisperyao.dsplayer.util.ConnectionManagerProvider
import com.whisperyao.dsplayer.util.DeviceCustomization
import com.whisperyao.dsplayer.util.ShareAnalyticUtils
import com.whisperyao.dsplayer.util.StoragePermissionHelper
import com.whisperyao.dsplayer.util.SynoLog
import com.whisperyao.dsplayer.util.Utilities
import com.whisperyao.dsplayer.util.extension.openSettings
import com.whisperyao.dsplayer.util.extension.Extensions.removePreference
import javax.inject.Inject

class SettingsFragment : BasePreferenceFragment(),
    Preference.OnPreferenceClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val LOG_TAG = "SettingsFragment"

        private const val CATEGORY_GENERAL = "category_general"
        private const val CATEGORY_LOGIN_INFO = "category_login_info"
        private const val CATEGORY_TRANSCODE = "category_transcode"

        private const val KEY_CACHE_AUTO_SIZE = "cache_auto_size"
        private const val KEY_CACHE_MANUAL_SIZE = "cache_manual_size"
        private const val KEY_CACHE_PATH = "cache_path"

        private const val KEY_LOGIN_INFO_ACCOUNT = "login_info_account"
        private const val KEY_LOGIN_INFO_ADDRESS = "login_info_address"
        private const val KEY_LOGIN_LOGOUT_BUTTON = "login_logout_button"

        private const val KEY_SELECT_PLAYER = "select_player"
        private const val KEY_SELECT_EQUALIZER = "select_equalizer"
    }

    @Inject
    lateinit var injectActivity: FragmentActivity

    private var cacheAutoSizePref: PreferenceScreen? = null
    private var cacheManualSizePref: PreferenceScreen? = null
    private var cachePathPref: Preference? = null

    private var selectPlayerPref: PreferenceScreen? = null
    private var selectEqualizerPref: PreferenceScreen? = null

    private val originalLibraryPref = AudioPreference.getPersonalPref()
    private val originalCacheLimit = AudioPreference.getSongCacheLimit()

    override fun getTitleResId(): Int = R.string.settings

    private val playerChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val playerName = Common.getPlayerName() ?: return@registerForActivityResult
            selectPlayerPref?.summary = playerName
        }

    private val equalizerChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val equalizerType =
                EqualizerSettings.getInstance().currentEqualizerType
                    ?: return@registerForActivityResult

            selectEqualizerPref?.summary = equalizerType.name
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        AudioPreference.migrateTranscodeForFormat(injectActivity)

        addPreferencesFromResource(R.xml.preferences)

        findPreference<PreferenceCategory>("analytics_setting")?.isVisible = true

        AudioPreference.getSharedPreferences().registerOnSharedPreferenceChangeListener(this)

        initLoginSection()
        initCacheSection()
        initGeneralSection()
        initTranscodeSection()
        initEqualizerSection()
        initDeveloperSection()
    }

    private fun initLoginSection() {

        val addressPref = findPreference<PreferenceScreen>(KEY_LOGIN_INFO_ADDRESS)

        val accountPref = findPreference<PreferenceScreen>(KEY_LOGIN_INFO_ACCOUNT)

        val logoutPref = findPreference<LoginLogoutPreference>(KEY_LOGIN_LOGOUT_BUTTON)

        logoutPref?.callback = object : LoginLogoutPreference.Callback {

            override fun login() {
                // Common.openLogin(injectActivity)
            }

            override fun logout() {
                val provider = activity as? ConnectionManagerProvider

                // Common.showLogoutDialog(provider)
            }
        }

        logoutPref?.isChecked = !Common.isLogin()

        if (Common.isLogin()) {

            addressPref?.summary = AudioPreference.getUserInputAddress()

            accountPref?.summary = AudioPreference.getAccount()

        } else {

            val loginCategory = findPreference<PreferenceCategory>(CATEGORY_LOGIN_INFO)

            loginCategory?.let {
                it.removePreference( addressPref)
                it.removePreference(accountPref)
            }
        }
    }

    private fun initCacheSection() {

        cacheAutoSizePref = findPreference(KEY_CACHE_AUTO_SIZE)

        cacheManualSizePref = findPreference(KEY_CACHE_MANUAL_SIZE)

        cachePathPref = findPreference(KEY_CACHE_PATH)

        cacheAutoSizePref?.onPreferenceClickListener = this
        cacheManualSizePref?.onPreferenceClickListener = this

        updateCacheSize()
        refreshCachePath()

        SynoLog.d("SettingsFragment", "cachePathPref: ${cachePathPref?.summary}, cacheAutoSizePref: ${cacheAutoSizePref?.key}")
        cachePathPref?.setOnPreferenceClickListener {

            if (
                SAFUtils.isScopedStorageEnvironment() ||
                StoragePermissionHelper.permissionGranted()
            ) {

                StoragePermissionHelper.showFilePicker(this, 0)

            } else {

                StoragePermissionHelper.showFilePicker(this, 0)
            }

            true
        }
    }

    private fun initGeneralSection() {

        val generalCategory = findPreference<PreferenceCategory>(CATEGORY_GENERAL)

        selectPlayerPref = generalCategory?.findPreference(KEY_SELECT_PLAYER)

        if (Common.isLogin()) {

            selectPlayerPref?.onPreferenceClickListener = this

            Common.getPlayerName().let {
                selectPlayerPref?.summary = it
            }

        } else {

            generalCategory?.removePreference(selectPlayerPref)
        }

        val personalLibraryPref =
            generalCategory?.findPreference<ListPreference>(
                AudioPreference.PREFERENCE_PERSONAL
            )

        if (
            !Common.isLogin() ||
            !Common.supportPersonalLibrary()
        ) {

            generalCategory?.let {
                it.removePreference(personalLibraryPref)
            }
        }

        if (!DeviceCustomization.supportRemoveVolume()) {

            generalCategory?.let {

                it.removePreference(
                    findPreference(
                        AudioPreference.PREFERENCE_ENABLE_REMOTE_CONTROLLER
                    )
                )

                it.removePreference(
                    findPreference(
                        AudioPreference.PREFERENCE_ENABLE_REMOTE_CONTROLLER_DESCRIPTION
                    )
                )
            }
        }
    }

    private fun initTranscodeSection() {

        val transcodeCategory =
            findPreference<PreferenceCategory>(CATEGORY_TRANSCODE)

        val transcodePref =
            transcodeCategory?.findPreference<ListPreference>(
                AudioPreference.PREFERENCE_TRANSCODE
            )

        val qualityPref =
            transcodeCategory?.findPreference<ListPreference>(
                AudioPreference.PREFERENCE_TRANSCODE_QUALITY
            )

        val shouldShowCategory: Boolean
        val shouldShowTranscode: Boolean
        val shouldShowQuality: Boolean
        val shouldShowForceTranscode: Boolean

        if (Common.isLogin()) {

            val supportMp3 =
                Common.getTranscodeType().supportMP3()

            if (ConnectionManager.isUseWebAPI()) {

                shouldShowCategory = true
                shouldShowTranscode = true
                shouldShowQuality = true
                shouldShowForceTranscode = !supportMp3

            } else {

                shouldShowCategory = supportMp3
                shouldShowTranscode = supportMp3
                shouldShowQuality = false
                shouldShowForceTranscode = false
            }

        } else {

            shouldShowCategory = false
            shouldShowTranscode = false
            shouldShowQuality = false
            shouldShowForceTranscode = false
        }

        if (!shouldShowCategory) {

            transcodeCategory?.removePreference(
                preferenceScreen
            )

            return
        }

        if (shouldShowTranscode) {

            transcodePref?.setOnPreferenceChangeListener { _, newValue ->

                val enableQuality =
                    newValue == "MP3"

                qualityPref?.isEnabled = enableQuality

                true
            }

            qualityPref?.isEnabled =
                transcodePref?.value == "MP3"

        } else {

            transcodeCategory?.removePreference(transcodePref)
        }

        if (!shouldShowQuality) {

            transcodeCategory?.removePreference(qualityPref)
        }

        if (!shouldShowForceTranscode) {

            val forcePref =
                transcodeCategory?.findPreference<Preference>(
                    AudioPreference.PREFERENCE_FORCE_TRANSCODE
                )

            transcodeCategory?.removePreference(forcePref)
        }
    }

    private fun initEqualizerSection() {

        val soundEffectCategory = findPreference<PreferenceCategory>(
            AudioPreference.PREFERENCE_CATEGORY_SOUND_EFFECT
        )

        val equalizerSettings =
            EqualizerSettings.getInstance()

        val initialized = equalizerSettings.isEqualizerInit || equalizerSettings.init()

        if (!initialized) {

            soundEffectCategory?.let {
                preferenceScreen.removePreference(it)
            }

            return
        }

        equalizerSettings.notifyUpdateEqualizer()

        val enableEqualizerPref =
            findPreference<CheckBoxPreference>(
                AudioPreference.PREFERENCE_ENABLE_EQUALIZER
            )

        enableEqualizerPref?.setOnPreferenceChangeListener { _, newValue ->

            val enabled = newValue as? Boolean ?: false

            selectEqualizerPref?.isEnabled = enabled

            equalizerSettings.setEnableEqualizer(enabled)
            equalizerSettings.notifyUpdateEqualizer()

            if (enabled) {

                AlertDialog.Builder(injectActivity)
                    .setTitle(R.string.pref_enable_equalizer)
                    .setMessage(R.string.equalizer_influence_gapless)
                    .setPositiveButton(R.string.str_ok, null)
                    .show()
            }

            true
        }

        selectEqualizerPref =
            findPreference(KEY_SELECT_EQUALIZER)

        selectEqualizerPref?.onPreferenceClickListener = this

        selectEqualizerPref?.isEnabled =
            AudioPreference.enableEqualizer()

        equalizerSettings.currentEqualizerType?.let {
            selectEqualizerPref?.summary = it.name
        }
    }

    private fun initDeveloperSection() {

        val versionPref = preferenceScreen.findPreference<VersionPreference>(WebAPI.VERSION)

        versionPref?.setEasterEggListener {

            val developerCategory =
                preferenceScreen.findPreference<PreferenceCategory>(
                    "developer"
                )

            developerCategory?.isVisible = true

            // SyLogUi.setSettingEnabled(true)
        }

        preferenceScreen
            .findPreference<PreferenceCategory>("developer")
            ?.isVisible = false
    }

    override fun onResume() {
        super.onResume()

        findPreference<Preference>("share_analytics")?.let {

            val enabled = ShareAnalyticUtils.isEnableShareAnalytics(it.context)

            it.summary = getString(
                if (enabled) {
                    R.string.str_on
                } else {
                    R.string.str_off
                }
            )
        }

        updateCacheSize()
        refreshCachePath()
    }

    override fun onPause() {

        if (AudioPreference.getPersonalPref() != originalLibraryPref) {
            Common.gLibraryChanged = true
        }

        if (originalCacheLimit != AudioPreference.getSongCacheLimit()) {

            CacheManager.getInstance().rotateSong(0L)

            Common.gIsClearLocalCache = true
        }

        super.onPause()
    }

    override fun onDestroy() {

        AudioPreference.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)

        super.onDestroy()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {

        SynoLog.d(
            LOG_TAG,
            "onPreferenceClick : ${preference.key}"
        )

        when (preference.key) {

            KEY_CACHE_MANUAL_SIZE -> {
                this.openSettings(
                    R.string.type_manual_download
                )
            }

            KEY_CACHE_AUTO_SIZE -> {

                if (!StoragePermissionHelper.permissionGranted()) {

                    StoragePermissionHelper.showHintsThenAskPermission(
                        this,
                        StoragePermissionHelper.REQUEST_CODE_AUTO_DOWNLOAD
                    )

                } else {

                    openAutoDownloadPage()
                }
            }

            KEY_SELECT_PLAYER -> {

                activity?.let {
                    playerChooserLauncher.launch(
                        Intent(it, PlayerChooserActivity::class.java)
                    )
                }
            }

            KEY_SELECT_EQUALIZER -> {
                // activity?.let { equalizerChooserLauncher.launch( Intent(it, EqualizerChooserActivity::class.java) )}
            }
        }

        return true
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {

        when (key) {

            AudioPreference.PREF_SONG_MANUAL_CACHE_SIZE,
            AudioPreference.PREF_SONG_AUTO_CACHE_SIZE -> {
                SynoLog.d("updateCacheSize", "updateCacheSize in onSharedPreferenceChanged")
                updateCacheSize()
            }

            KEY_CACHE_PATH -> {
                SynoLog.d("SettingsFragment", "refreshCachePath in onSharedPreferenceChanged")
                refreshCachePath()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {

            9478 -> {

                StoragePermissionHelper.onActivityResult(
                    this,
                    resultCode,
                    data
                )

                if (data?.data != null) {
                    openAutoDownloadPage()
                }
            }

            9487 -> {

                StoragePermissionHelper.onActivityResult(
                    this,
                    resultCode,
                    data
                )
            }
        }
    }

    private fun updateCacheSize() {

        cacheAutoSizePref?.summary = Utilities.getCacheSettingString(resources, 1)

        cacheManualSizePref?.summary = Utilities.getCacheSettingString(resources, 2)
    }

    private fun refreshCachePath() {

        if (StoragePermissionHelper.permissionGranted()) {
            cachePathPref?.summary = AudioPreference.getSongCacheFolder()
        } else {
            cachePathPref?.setSummary(R.string.storage_permission_denied_preference)
        }
    }

    private fun openAutoDownloadPage() {
        this.openSettings(
            R.string.type_auto_download
        )
    }
}