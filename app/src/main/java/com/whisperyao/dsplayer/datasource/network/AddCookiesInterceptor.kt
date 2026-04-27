package com.whisperyao.dsplayer.datasource.network

import android.text.TextUtils
import com.google.common.net.HttpHeaders
import com.synology.sylib.syhttp3.cookieStore.CipherPersistentCookieStore
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpCookie

class AddCookiesInterceptor: Interceptor {
    private val cookieStore: CipherPersistentCookieStore

    constructor(cookieStore: CipherPersistentCookieStore) {
        this.cookieStore = cookieStore
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val builderNewBuilder = chain.request().newBuilder()
        val list: List<HttpCookie> = cookieStore[chain.request().url.toUri()] ?: emptyList()
        if (list.isNotEmpty()) {
            val arrayList = list.map { it.toString() }
            val cookieStr = TextUtils.join("; ", arrayList)
            builderNewBuilder.addHeader(HttpHeaders.COOKIE, cookieStr)
        }
        return chain.proceed(builderNewBuilder.build())
    }

}