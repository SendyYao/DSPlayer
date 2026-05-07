package com.whisperyao.dsplayer.injection.module;

import android.app.Service;

import com.whisperyao.dsplayer.download.DownloadService;
import com.whisperyao.dsplayer.mediasession.service.PlaybackService;
import com.whisperyao.dsplayer.mediasession.service.RemoteControllerService;

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

    @Module(includes = {ServiceModule.class, NotificationModule.class, ManagerModule.class, PlaybackModule.class})
    public static class DownloadServiceInstanceModule {
        @Provides
        Service providerService(DownloadService downloadService) {
            return downloadService;
        }
    }

    @Module(includes = {ServiceModule.class, NotificationModule.class, ManagerModule.class, PlaybackModule.class})
    public static class RemoteControllerServiceInstanceModule {
        @Provides
        Service providerService(RemoteControllerService remoteControllerService) {
            return remoteControllerService;
        }
    }


    @ContributesAndroidInjector(modules = {DownloadServiceInstanceModule.class})
    abstract DownloadService downloadService();

    @ContributesAndroidInjector(modules = {PlaybackServiceInstanceModule.class})
    abstract PlaybackService playbackService();

    @ContributesAndroidInjector(modules = {RemoteControllerServiceInstanceModule.class})
    abstract RemoteControllerService remoteControllerService();

}
