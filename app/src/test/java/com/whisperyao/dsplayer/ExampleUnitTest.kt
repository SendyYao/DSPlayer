package com.whisperyao.dsplayer

import com.whisperyao.dsplayer.App.Companion.connectionManager
import com.whisperyao.dsplayer.datasource.network.ConnectionManager
import com.whisperyao.dsplayer.datasource.network.exception.InvalidUrlException
import com.whisperyao.dsplayer.datasource.network.exception.NotSupportApiLoginException
import com.whisperyao.dsplayer.ui.login.ConnectData
import com.whisperyao.dsplayer.util.SynoLog
import kotlinx.coroutines.CancellationException
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
import java.net.URL
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.jvm.Throws

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    @Throws
    fun getQueryAll(urls: List<URL>): ConnectionManager.QueryResult {
        try {
            return connectionManager.queryAll(urls)
        } catch (e: NotSupportApiLoginException) {
            throw e
        } catch (e2: SSLHandshakeException) {
            throw e2
        } catch (e3: SSLPeerUnverifiedException) {
            throw e3
        } catch (th: Throwable) {
            val case: Throwable? = th.cause
            if (case is CancellationException) {
                throw th
            }
            if (case != null) {
                throw case
            }
            throw IOException("queryAll() failed: pair == null")
        }
    }
    @Test
    fun get_urls(){
        val string: String = BuildConfig.NAS_ADDRESS.trim()
        val urls: List<URL> = ConnectData(string, true).possibleUrlList
        if (urls.isEmpty()) {
            throw InvalidUrlException()
        }
        val queryAll: ConnectionManager.QueryResult = getQueryAll(urls)
        SynoLog.d("ExampleUnitTest", queryAll.queryVo?.data.toString())
    }
}