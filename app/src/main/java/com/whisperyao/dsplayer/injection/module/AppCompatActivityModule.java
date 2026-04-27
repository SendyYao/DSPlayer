package com.whisperyao.dsplayer.injection.module;

import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import dagger.Module;
import dagger.Provides;

@Module(includes = {ActivityModule.class})
public class AppCompatActivityModule {
    @Provides
    Activity activity(AppCompatActivity appCompatActivity) {
        return appCompatActivity;
    }

    @Provides
    FragmentManager provideFragmentManager(AppCompatActivity appCompatActivity) {
        return appCompatActivity.getSupportFragmentManager();
    }
}
