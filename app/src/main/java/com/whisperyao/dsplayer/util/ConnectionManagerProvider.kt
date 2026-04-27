package com.whisperyao.dsplayer.util

import com.whisperyao.dsplayer.datasource.network.ConnectionManager

interface ConnectionManagerProvider {
    fun provideConnectionManager(): ConnectionManager?
}