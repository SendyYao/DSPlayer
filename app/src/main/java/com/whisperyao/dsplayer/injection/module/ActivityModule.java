package com.whisperyao.dsplayer.injection.module;

import android.app.Activity;
import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module(includes = {ContextBasedModule.class})
public class ActivityModule {
    @Provides
    Context provideContext(Activity activity) {
        return activity;
    }

}
