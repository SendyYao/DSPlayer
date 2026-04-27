package com.whisperyao.dsplayer


import android.content.Context
import android.content.SharedPreferences
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.DownsampleMode
import com.google.gson.Gson
import com.synology.sylib.syhttp3.SyHttpClient
import com.synology.sylib.syhttp3.cookieStore.CipherPersistentCookieStore
import com.synology.sylib.syhttp3.relay.ServiceId
import com.synology.sylib.syhttp3.relay.utils.RelayUtil
import com.whisperyao.dsplayer.datasource.network.AddCookiesInterceptor
import com.whisperyao.dsplayer.datasource.network.ApiManager
import com.whisperyao.dsplayer.datasource.network.LoginInfoManager
import com.whisperyao.dsplayer.datasource.network.ConnectionManager
import com.whisperyao.dsplayer.datasource.network.MyHttpClient
import com.whisperyao.dsplayer.datasource.network.PreferenceManager
import com.whisperyao.dsplayer.injection.DaggerApplicationInjector
import com.whisperyao.dsplayer.injection.module.ApplicationModule
import com.whisperyao.dsplayer.model.data.DataModelManager
import com.whisperyao.dsplayer.util.DataKeyStoreHelper
import com.whisperyao.dsplayer.util.FlipperUtils
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication

class App : DaggerApplication() {

    companion object {
        private lateinit var instance: App

        @JvmStatic
        fun getContext(): Context {
            return instance
        }
        lateinit var connectionManager: ConnectionManager
    }


    lateinit var sharedPreferences: SharedPreferences
    lateinit var cookieStore: CipherPersistentCookieStore

    override fun onCreate() {
        super.onCreate()
        cookieStore = CipherPersistentCookieStore(this)
        // Common.init(this)
        instance = this
        FlipperUtils.init(instance)
        sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)
        initNetworkModule()
        connectionManager = ConnectionManager(
            this,
            PreferenceManager(this),
            cookieStore,
            Gson(),
            MyHttpClient(cookieStore, false, 30L), // ⚠️ 简化
            LoginInfoManager(this, sharedPreferences, DataKeyStoreHelper(this)),
            ApiManager(sharedPreferences)
        )
        initFresco()
        DataModelManager.initInstance(this)
        // AppDependencies.init()
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationInjector
            .builder()
            .application(this)
            .applicationModule(ApplicationModule())
            .build()
    }


    fun initNetworkModule() {
        SyHttpClient.setContext(this.applicationContext)
        RelayUtil.clearAllRelayRecords()
        SyHttpClient.setUseHolePunch(applicationContext, true)
        RelayUtil.addRelayInfo(packageName, "http", arrayOf(ServiceId.DSM), arrayOf("/webman/pingpong.cgi?quickconnect=true"))
        RelayUtil.addRelayInfo(packageName, "https", arrayOf(ServiceId.DSM_HTTPS), arrayOf("/webman/pingpong.cgi?quickconnect=true"))
    }

    fun initFresco() {
        if (Fresco.hasBeenInitialized()) {
            Fresco.shutDown()
        }
        val myHttpClient = MyHttpClient(connectionManager.getCookieStore(), connectionManager.getPreferenceManager().isVerifyCertification(), 30L)
        myHttpClient.addInterceptor(AddCookiesInterceptor(connectionManager.getCookieStore()))
        myHttpClient.client.dispatcher.maxRequestsPerHost = 3
        myHttpClient.client.dispatcher.maxRequests = 3
        val app: App = this
        Fresco.initialize(app, OkHttpImagePipelineConfigFactory.newBuilder(app,
            myHttpClient.client)
            .setDownsampleMode(DownsampleMode.ALWAYS)
            .setMainDiskCacheConfig(
                DiskCacheConfig.newBuilder(app)
                    .setMaxCacheSize(Long.MAX_VALUE)
                    .setMaxCacheSizeOnLowDiskSpace(Long.MAX_VALUE)
                    .setBaseDirectoryPath(instance.filesDir)
                    .setBaseDirectoryName("fresco")
                    .build()
            ).build()
        )
    }
}