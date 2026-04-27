package com.whisperyao.dsplayer.injection.module;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import dagger.Module;
import dagger.Provides;

@Module(includes = {ActivityModule.class})
public class SupportFragmentModule {
    @Provides
    Activity provideActivity(FragmentActivity fragmentActivity) {
        return fragmentActivity;
    }

    @Provides
    FragmentManager provideFragmentManager(Fragment fragment) {
        return fragment.getFragmentManager();
    }

    @Provides
    FragmentActivity provideFragmentActivity(Fragment fragment) {
        return fragment.getActivity();
    }

}
