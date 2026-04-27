package com.whisperyao.dsplayer.injection.module;

import android.app.Service;
import android.content.Context;
import dagger.Module;
import dagger.Provides;

@Module(includes = {ContextBasedModule.class})
public class ServiceModule {
    @Provides
    Context provideContext(Service service) {
        return service;
    }
}