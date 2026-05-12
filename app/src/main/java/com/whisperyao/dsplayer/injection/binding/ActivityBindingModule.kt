package com.whisperyao.dsplayer.injection.binding

import androidx.appcompat.app.AppCompatActivity
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.download.TaskActivity
import com.whisperyao.dsplayer.injection.module.AppCompatActivityModule
import com.whisperyao.dsplayer.injection.module.ContextBasedModule
import com.whisperyao.dsplayer.ui.settings.DisplayPreferenceActivity
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector


@Module
abstract class ActivityBindingModule {
    @ContributesAndroidInjector(modules = [HomeActivityInstanceModule::class])
    abstract fun homeActivity(): HomeActivity

    @ContributesAndroidInjector(modules = [TaskActivityInstanceModule::class])
    abstract fun taskActivity(): TaskActivity

    @ContributesAndroidInjector(modules = [DisplayPreferenceActivityInstanceModule::class])
    abstract fun displayPreferenceActivity(): DisplayPreferenceActivity

    @Module(includes = [AppCompatActivityModule::class, ContextBasedModule::class])
    class HomeActivityInstanceModule {
        @Provides
        fun appCompatActivity(homeActivity: HomeActivity): AppCompatActivity {
            return homeActivity
        }
    }

    @Module(includes = [AppCompatActivityModule::class])
    class TaskActivityInstanceModule {
        @Provides
        fun appCompatActivity(taskActivity: TaskActivity): AppCompatActivity {
            return taskActivity
        }
    }

    @Module(includes = [AppCompatActivityModule::class, ContextBasedModule::class])
    class DisplayPreferenceActivityInstanceModule {
        @Provides
        fun displayPreferenceActivity(activity: DisplayPreferenceActivity): AppCompatActivity {
            return activity
        }
    }
}