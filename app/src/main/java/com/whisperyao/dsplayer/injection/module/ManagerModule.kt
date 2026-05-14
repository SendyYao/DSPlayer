package com.whisperyao.dsplayer.injection.module

import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.StateManager
import com.whisperyao.dsplayer.download.TaskManager
import com.whisperyao.dsplayer.mediasession.service.AbstractMediaBrowserService
import com.whisperyao.dsplayer.mediasession.service.PlaybackService
import com.whisperyao.dsplayer.mediasession.service.RemoteControllerService
import com.whisperyao.dsplayer.model.data.DataModelManager
import com.whisperyao.dsplayer.model.data.PlayingQueueManager
import com.whisperyao.dsplayer.playing.NowPlayingManager
import com.whisperyao.dsplayer.playing.Player
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import dagger.Module;
import dagger.Provides;


@Module
class ManagerModule {

    @Provides
    fun provideDataModelManager(): DataModelManager {
        return DataModelManager.getMInstance()
    }

    @Provides
    fun provideStateManager(): StateManager {
        return StateManager.getInstance()
    }

    @Provides
    fun provideTaskManagerInstance(manager: DataModelManager): TaskManager {
        return manager.taskManager
    }

    @Provides
    fun providePlayingManagerInstance(manager: DataModelManager): PlayingQueueManager {
        return manager.playingQueueManager
    }

    @Provides
    fun providePlayingStatusManagerInstance(manager: DataModelManager): PlayingStatusManager {
        return manager.playingStatusManager
    }

    @Provides
    fun providePlayingStatusPlayerInstance(manager: PlayingStatusManager): Player {
        return manager.player
    }

    @Provides
    fun providePlaybackServiceClass(player: Player): Class<out AbstractMediaBrowserService> {
        return if (player.isPlayModeRenderer()) {
            RemoteControllerService::class.java
        } else if (player.isPlayModeChromeCast()) {
            PlaybackService::class.java
        } else {
            PlaybackService::class.java
        }
    }

    @Provides
    fun provideNowPlayingManager(): NowPlayingManager {
        return NowPlayingManager(Common.getDsId() as String)
    }
}