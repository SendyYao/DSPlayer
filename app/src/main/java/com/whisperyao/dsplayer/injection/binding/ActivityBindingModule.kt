package com.whisperyao.dsplayer.injection.binding

import androidx.appcompat.app.AppCompatActivity
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.injection.module.AppCompatActivityModule
import com.whisperyao.dsplayer.injection.module.ContextBasedModule
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector


@Module
abstract class ActivityBindingModule {
    @ContributesAndroidInjector(modules = [HomeActivityInstanceModule::class])
    abstract fun homeActivity(): HomeActivity

    @Module(includes = [AppCompatActivityModule::class, ContextBasedModule::class])
    class HomeActivityInstanceModule {
        @Provides
        fun appCompatActivity(homeActivity: HomeActivity): AppCompatActivity {
            return homeActivity
        }
    }

}