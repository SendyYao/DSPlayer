package com.whisperyao.dsplayer.datasource.network.api

import com.google.gson.Gson;
import com.whisperyao.dsplayer.datasource.network.exception.ApiException;
import com.whisperyao.dsplayer.datasource.network.exception.NotSupportApiLoginException;
import com.whisperyao.dsplayer.datasource.network.vo.QueryVo;
import com.whisperyao.dsplayer.net.WebAPI;
import com.synology.sylib.syhttp3.SyHttpClient;
import com.whisperyao.dsplayer.util.SynoLog
import java.net.URL;
import java.util.concurrent.Callable;
import kotlin.Pair;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request;


class QueryCallable(
    private val httpClient: SyHttpClient,
    private val url: URL
) : Callable<Pair<URL, QueryVo>> {

    private lateinit var mCall: Call

    override fun call(): Pair<URL, QueryVo> {
        val httpUrl = "$url/webapi/query.cgi".toHttpUrlOrNull()
            ?: error("Required value was null.")

        mCall = httpClient.newCall(
            Request.Builder()
                .url(httpUrl)
                .post(
                    FormBody.Builder()
                        .add(WebAPI.API, "SYNO.API.Info")
                        .add("method", "query")
                        .add(WebAPI.VERSION, "1")
                        .add("query", "all")
                        .build()
                )
                .build()
        )
        val jsonStr = mCall.execute().body?.string()
        SynoLog.d("QueryCallable", "jsonStr: $jsonStr")
        val queryVo = Gson().fromJson(
            jsonStr,
            QueryVo::class.java
        )

        if (queryVo?.success != true) {
            testIsCgi()
            throw ApiException(SynoApiInfo.INSTANCE, -2, null)
        }

        return url to queryVo
    }

    private fun testIsCgi() {
        val httpUrl = "$url/audio/iPhone/login.cgi".toHttpUrlOrNull()
            ?: error("Required value was null.")

        mCall = httpClient.newCall(
            Request.Builder()
                .url(httpUrl)
                .post(FormBody.Builder().build())
                .build()
        )

        if (mCall.execute().code == 200) {
            throw NotSupportApiLoginException()
        }
    }
}