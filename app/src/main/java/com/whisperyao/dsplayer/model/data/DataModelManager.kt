package com.whisperyao.dsplayer.model.data

import android.content.Context
import com.synology.sylib.security.KeyStoreHelper
import com.whisperyao.dsplayer.StateManager
import com.whisperyao.dsplayer.download.TaskManager
import com.whisperyao.dsplayer.playing.ChromeCastHelper
import com.whisperyao.dsplayer.playing.PlayingStatusManager
import jakarta.inject.Inject

class DataModelManager private constructor(context: Context) {

    val taskManager: TaskManager
    val playingQueueManager: PlayingQueueManager
    val chromeCastHelper: ChromeCastHelper
    val playingStatusManager: PlayingStatusManager

    @Inject
    lateinit var stateManager: StateManager

    init {
        KeyStoreHelper.initDefaultSingleton(context)

        taskManager = TaskManager()
        playingQueueManager = PlayingQueueManager()

        chromeCastHelper = ChromeCastHelper(context)

        playingStatusManager = PlayingStatusManager(chromeCastHelper)
    }

    companion object {
        @Volatile
        private var _instance: DataModelManager? = null

        @JvmStatic
        fun initInstance(context: Context) {
            if (_instance == null) {
                synchronized(this) {
                    if (_instance == null) {
                        _instance = DataModelManager(
                            context.applicationContext
                        )
                    }
                }
            }
        }

        @JvmStatic
        fun getMInstance(): DataModelManager {
            return _instance
                ?: throw IllegalStateException(
                    "DataModelManager is not initialized, call initInstance() first."
                )
        }

        @JvmStatic
        fun setInstance(dataModelManager: DataModelManager) {
            _instance = dataModelManager
        }

        val instance: DataModelManager
            get() = getMInstance()
    }
}