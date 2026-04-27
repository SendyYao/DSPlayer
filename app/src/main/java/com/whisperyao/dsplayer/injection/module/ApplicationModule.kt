package com.whisperyao.dsplayer.injection.module


import android.content.Context
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext
import dagger.Module
import dagger.Provides

@Module
class ApplicationModule {

    @Provides
    @ApplicationContext
    fun provideContext(app: App): Context =
        app.applicationContext
}