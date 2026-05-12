package com.whisperyao.dsplayer.injection.binding

import androidx.preference.PreferenceFragmentCompat
import com.whisperyao.dsplayer.injection.module.ContextBasedModule;
import com.whisperyao.dsplayer.injection.module.PreferenceFragmentModule
import com.whisperyao.dsplayer.ui.settings.DownloadSettingAutomaticFragment
import com.whisperyao.dsplayer.ui.settings.DownloadSettingManualFragment
import com.whisperyao.dsplayer.ui.settings.SettingsFragment
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentBindingModule {

    @ContributesAndroidInjector(modules = [DownloadSettingAutomaticFragmentInstanceModule::class])
    abstract fun downloadSettingAutomaticFragment(): DownloadSettingAutomaticFragment

    @ContributesAndroidInjector(modules = [DownloadSettingManualFragmentInstanceModule::class])
    abstract fun downloadSettingManualFragment(): DownloadSettingManualFragment

    @ContributesAndroidInjector(modules = [SettingsFragmentInstanceModule::class])
    abstract fun settingsFragment(): SettingsFragment

    @Module(includes = [PreferenceFragmentModule::class, ContextBasedModule::class])
    class DownloadSettingManualFragmentInstanceModule {

        @Provides
        fun provideDownloadSettingManualFragment(
            fragment: DownloadSettingManualFragment
        ): PreferenceFragmentCompat {
            return fragment
        }
    }

    @Module(includes = [PreferenceFragmentModule::class, ContextBasedModule::class])
    class DownloadSettingAutomaticFragmentInstanceModule {

        @Provides
        fun provideDownloadSettingAutomaticFragment(
            fragment: DownloadSettingAutomaticFragment
        ): PreferenceFragmentCompat {
            return fragment
        }
    }

    @Module(includes = [PreferenceFragmentModule::class, ContextBasedModule::class])
    class SettingsFragmentInstanceModule {

        @Provides
        fun provideSettingsFragment(
            fragment: SettingsFragment
        ): PreferenceFragmentCompat {
            return fragment
        }
    }
}