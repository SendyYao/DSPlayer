package com.whisperyao.dsplayer.injection.module

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.ProgressDialog
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.injection.Constants
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext
import com.whisperyao.dsplayer.injection.qualifier.Default
import com.whisperyao.dsplayer.util.CoverUtil
//import com.whisperyao.dsplayer.util.firebase.FirebaseAnalyticsUtil
import com.synology.sylib.util.DeviceUtil
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;


@Module
class ContextBasedModule {

    @Provides
    fun provideAudioManager(context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: throw IllegalArgumentException("AudioManager is null")
    }

    @Provides
    fun provideActivityManager(context: Context): ActivityManager {
        return context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: throw IllegalArgumentException("ActivityManager is null")
    }

    @Provides
    fun provideConnectivityManager(context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: throw IllegalArgumentException("ConnectivityManager is null")
    }

    @Provides
    fun provideInputMethodManager(context: Context): InputMethodManager {
        return context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: throw IllegalArgumentException("InputMethodManager is null")
    }

    @Provides
    fun provideNotificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: throw IllegalArgumentException("NotificationManager is null")
    }

    @Provides
    fun providePowerManager(context: Context): PowerManager {
        return context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: throw IllegalArgumentException("PowerManager is null")
    }

    @Provides
    fun provideWifiManager(
        @ApplicationContext context: Context
    ): WifiManager {
        return context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: throw IllegalArgumentException("WifiManager is null")
    }

    @Provides
    fun provideWifiLock(
        service: Service,
        wifiManager: WifiManager
    ): WifiManager.WifiLock {
        return wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            service::class.java.simpleName
        )
    }

    @Provides
    fun provideContentResolver(context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Default
    fun provideSharedPreferences(
        context: Context
    ): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Default
    fun provideSharedPreferencesEditor(
        @Default sharedPreferences: SharedPreferences
    ): SharedPreferences.Editor {
        return sharedPreferences.edit()
    }

    @Provides
    fun provideCoverUtil(context: Context): CoverUtil {
        return CoverUtil(context)
    }

    @Provides
    @Named(Constants.PREF_LYRIC)
    fun provideLyricPreferences(
        @ApplicationContext applicationContext: Context
    ): SharedPreferences {
        return applicationContext.getSharedPreferences(
            Constants.PREF_LYRIC,
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Named(Constants.IS_MOBILE)
    fun provideIsMobile(context: Context): Boolean {
        return DeviceUtil.isMobile(context)
    }

    @Provides
    @Named(Constants.LESS_THEN_10_INCH)
    fun provideIsLessThen10(context: Context): Boolean {
        return !DeviceUtil.isTablet(context) ||
                DeviceUtil.is7inchTablet(context)
    }

    @Provides
    fun provideProgressDialog(context: Context): ProgressDialog {
        return ProgressDialog(context).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setMessage(context.getString(R.string.loading))
        }
    }

//    @Provides
//    fun provideFirebaseAnalyticsUtil(
//        context: Context
//    ): FirebaseAnalyticsUtil {
//        return FirebaseAnalyticsUtil(context)
//    }
}