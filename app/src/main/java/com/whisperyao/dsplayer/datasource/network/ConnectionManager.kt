package com.whisperyao.dsplayer.datasource.network

import android.content.Context
import com.google.gson.Gson
import com.synology.sylib.syhttp3.cookieStore.CipherPersistentCookieStore
import com.whisperyao.dsplayer.datasource.network.api.QueryCallable
import com.whisperyao.dsplayer.datasource.network.api.SynoApiInfo
import com.whisperyao.dsplayer.datasource.network.exception.ApiException
import com.whisperyao.dsplayer.datasource.network.exception.NotSupportApiLoginException
import com.whisperyao.dsplayer.datasource.network.vo.ApiPath
import com.whisperyao.dsplayer.datasource.network.vo.QueryVo
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.net.URL;
import kotlin.coroutines.cancellation.CancellationException


class ConnectionManager {

    companion object {
        const val WEBAPI_DIR: String = "webapi/"
    }

    private var context: Context
    private var gson: Gson
    private var httpClient: MyHttpClient
    private var cookieStore: CipherPersistentCookieStore
    private var preferenceManager: PreferenceManager
    private var apiManager: ApiManager
    private var loginInfoManager: LoginInfoManager
    private lateinit var currentQueryAllExecutor: ExecutorService

    private lateinit var retrofit: Retrofit

    private var serviceMap: ConcurrentHashMap<Class<*>, Object>
    private val waitReLoginLock: ReentrantLock

    constructor(@ApplicationContext context: Context, preferenceManager: PreferenceManager, cookieStore: CipherPersistentCookieStore, gson: Gson, httpClient: MyHttpClient, loginInfoManager: LoginInfoManager, apiManager: ApiManager) {
        this.context = context
        this.preferenceManager = preferenceManager
        this.cookieStore = cookieStore
        this.gson = gson
        this.httpClient = httpClient
        this.loginInfoManager = loginInfoManager
        this.apiManager = apiManager
        if (isLinked()) {
            restoreEnvironment()
        }
        this.serviceMap = ConcurrentHashMap<Class<*>, Object>()
        this.waitReLoginLock = ReentrantLock()
    }

    fun getContext(): Context {
        return this.context
    }

    fun getRetrofitBuilder(): Retrofit.Builder {
        val builderAddConverterFactory: Retrofit.Builder = Retrofit.Builder().client(this.httpClient.client).addConverterFactory(GsonConverterFactory.create())
        return builderAddConverterFactory
    }

    fun setEnvironment(
        apiMap: HashMap<String, ApiPath>,
        baseUrl: HttpUrl?
    ) {
        apiManager.clearMap()
        apiManager.putAll(apiMap)

        loginInfoManager.httpUrl = baseUrl ?: "".toHttpUrlOrNull()

        val url = "${baseUrl}${if (baseUrl.toString().endsWith("/")) "" else "/"}$WEBAPI_DIR"

        retrofit = getRetrofitBuilder()
            .baseUrl(url.toHttpUrlOrNull()!!)
            .build()
    }

    fun restoreEnvironment() {
        val retrofitBuild = getRetrofitBuilder().baseUrl(this.loginInfoManager.httpUrl.toString() + WEBAPI_DIR).build()
        this.retrofit = retrofitBuild
    }

    fun isLinked(): Boolean {
        return this.loginInfoManager.isLinked
    }

    fun getCookieStore(): CipherPersistentCookieStore {
        return this.cookieStore
    }

    fun getPreferenceManager(): PreferenceManager {
        return this.preferenceManager
    }

    @Throws(Throwable::class)
    fun queryAll(urls: List<URL>): QueryResult {
        val executor = Executors.newFixedThreadPool(urls.size)
        val completionService = ExecutorCompletionService<Pair<URL, QueryVo>>(executor)

        try {
            currentQueryAllExecutor = executor

            urls.forEach { url ->
                completionService.submit(QueryCallable(httpClient, url))
            }

            var fallbackResult: Pair<HttpUrl, QueryVo>? = null
            var ioException: IOException? = null
            var apiException: ApiException? = null
            var throwable: Throwable? = null

            repeat(urls.size) { index ->
                try {
                    val future = if (index == 0) {
                        completionService.take()
                    } else {
                        completionService.poll(10, TimeUnit.SECONDS)
                    }

                    if (future == null || executor.isShutdown) {
                        return@repeat
                    }

                    val (url, queryVo) = future.get()

                    if (!queryVo.success) {
                        val error = queryVo.error
                            ?: throw JSONException("Query object has no error object.")

                        apiException = ApiException(
                            SynoApiInfo.INSTANCE,
                            error.code
                        )

                        return@repeat
                    }

                    val httpUrl = url.toString()
                        .toHttpUrlOrNull()
                        ?: error("Invalid url")

                    if (url.port != 5000 && url.port != 5001) {
                        return QueryResult(httpUrl, queryVo)
                    }

                    fallbackResult = httpUrl to queryVo

                } catch (e: ApiException) {
                    apiException = e
                } catch (e: IOException) {
                    if (ioException == null) {
                        ioException = e
                    }
                } catch (e: Exception) {
                    throwable = e.cause ?: e
                }
            }

            fallbackResult?.let {
                return QueryResult(it.first, it.second)
            }

            when {
                throwable is NotSupportApiLoginException -> throw throwable
                apiException != null -> throw apiException
                ioException != null -> throw ioException
                throwable != null -> throw throwable
                else -> throw CancellationException()
            }

        } finally {
            executor.shutdownNow()
//            currentQueryAllExecutor = null
        }
    }

    data class QueryResult(
        val httpUrl: HttpUrl? = null,
        val queryVo: QueryVo? = null
    )

}