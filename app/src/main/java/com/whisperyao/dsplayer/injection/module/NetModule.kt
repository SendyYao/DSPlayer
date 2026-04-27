package com.whisperyao.dsplayer.injection.module

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.whisperyao.dsplayer.datasource.network.LoginInfoManager
import com.whisperyao.dsplayer.datasource.network.MyHttpClient
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext
import com.synology.sylib.syhttp3.cookieStore.CipherPersistentCookieStore
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton


@Module
class NetModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return PreferenceManager
            .getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    @Named(LoginInfoManager.PREF_NAME)
    fun provideLoginSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            LoginInfoManager.PREF_NAME,
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setFieldNamingPolicy(
                FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES
            )
            .create()
    }

    @Provides
    @Singleton
    fun provideCookieStore(
        @ApplicationContext context: Context
    ): CipherPersistentCookieStore {
        return CipherPersistentCookieStore(context)
    }

    @Provides
    fun provideMyHttpClient(
        cookieStore: CipherPersistentCookieStore,
        preferenceManager: com.whisperyao.dsplayer.datasource.network.PreferenceManager
    ): MyHttpClient {
        return MyHttpClient(
            cookieStore,
            preferenceManager.isVerifyCertification(),
            0L
        )
    }
}