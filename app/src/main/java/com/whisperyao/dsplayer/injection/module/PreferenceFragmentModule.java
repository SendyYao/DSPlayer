package com.whisperyao.dsplayer.injection.module;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import dagger.Module;
import dagger.Provides;

@Module
public class PreferenceFragmentModule {
    @Provides
    public Context provideContext(FragmentActivity activity) {
        return activity;
    }

    @Provides
    public Fragment provideFragment(PreferenceFragmentCompat preferenceFragment) {
        return preferenceFragment;
    }

    @Provides
    public FragmentActivity provideFragmentActivity(PreferenceFragmentCompat preferenceFragment) {
        return preferenceFragment.getActivity();
    }

    @Provides
    public PreferenceScreen providePreferenceScreen(PreferenceFragmentCompat preferenceFragment) {
        return preferenceFragment.getPreferenceScreen();
    }

    @Provides
    public PreferenceManager providePreferenceManager(PreferenceFragmentCompat preferenceFragment) {
        return preferenceFragment.getPreferenceManager();
    }

}
