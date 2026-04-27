package com.whisperyao.dsplayer.injection.module;

import android.app.Service;

import com.whisperyao.dsplayer.mediasession.service.PlaybackService;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ServiceBindingModule {
    @Module(includes = {ServiceModule.class, NotificationModule.class, ManagerModule.class, PlaybackModule.class})
    public static class PlaybackServiceInstanceModule {
        @Provides
        Service providerService(PlaybackService playbackService) {
            return playbackService;
        }
    }

    @ContributesAndroidInjector(modules = {PlaybackServiceInstanceModule.class})
    abstract PlaybackService playbackService();


}
