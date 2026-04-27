package com.whisperyao.dsplayer.injection.module

import com.whisperyao.dsplayer.ConnectionManager
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient

@Module
class PlaybackModule {
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val client: OkHttpClient = ConnectionManager.getHttpClient().client
        return client
    }
}