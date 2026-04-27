package com.whisperyao.dsplayer.injection.binding

import android.content.ContentProvider
import com.whisperyao.dsplayer.injection.module.ContextBasedModule
import com.whisperyao.dsplayer.injection.module.ContentProviderModule
import com.whisperyao.dsplayer.provider.AudioProvider
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector

@Module
abstract class ContentProviderBindingModule {

    @ContributesAndroidInjector(
        modules = [AudioProviderModule::class]
    )
    abstract fun audioProvider(): AudioProvider

    @ContributesAndroidInjector(
        modules = [ProviderInstanceModule::class]
    )
    abstract fun contentProvider(): ContentProvider

    @Module(
        includes = [
            ContextBasedModule::class,
            ContentProviderModule::class
        ]
    )
    class AudioProviderModule {

        @Provides
        fun provideContentProvider(
            audioProvider: AudioProvider
        ): ContentProvider = audioProvider
    }

    @Module(
        includes = [ContentProviderModule::class]
    )
    class ProviderInstanceModule {

        @Provides
        fun provideProvider(
            provider: ContentProvider
        ): ContentProvider = provider
    }
}