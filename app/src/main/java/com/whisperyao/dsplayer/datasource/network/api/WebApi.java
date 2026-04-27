package com.whisperyao.dsplayer.datasource.network.api;


public interface WebApi {
    int errorStringRes(int errorCode);

    String name();

    int[] supportedVersion();
}