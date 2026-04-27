package com.whisperyao.dsplayer.injection

import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.injection.module.ApplicationModule
import com.whisperyao.dsplayer.injection.binding.ActivityBindingModule
import com.whisperyao.dsplayer.injection.module.ManagerModule
import com.whisperyao.dsplayer.injection.binding.ContentProviderBindingModule
import com.whisperyao.dsplayer.injection.binding.SupportFragmentBindingModule
import com.whisperyao.dsplayer.injection.module.NetModule
import com.whisperyao.dsplayer.injection.module.ServiceBindingModule
import dagger.android.support.AndroidSupportInjectionModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        ApplicationModule::class,
        ActivityBindingModule::class,
        ContentProviderBindingModule::class,
        ServiceBindingModule::class,
        SupportFragmentBindingModule::class,
        ManagerModule::class,
        NetModule::class
    ]
)
interface ApplicationInjector : AndroidInjector<App> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(app: App): Builder

        fun applicationModule(module: ApplicationModule): Builder

        fun build(): ApplicationInjector
    }
}