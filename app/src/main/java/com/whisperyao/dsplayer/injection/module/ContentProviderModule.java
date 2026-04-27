package com.whisperyao.dsplayer.injection.module;

import android.content.ContentProvider;
import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module

public class ContentProviderModule {
    @Provides
    Context providerContext(ContentProvider contentProvider) {
        return contentProvider.getContext();
    }
}
