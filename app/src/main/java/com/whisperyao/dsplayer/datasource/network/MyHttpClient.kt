package com.whisperyao.dsplayer.datasource.network


import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.conn.ssl.StrictHostnameVerifier
import com.synology.sylib.syhttp3.SyHttpClient
import com.synology.sylib.syhttp3.VerifyCertsManager
import com.synology.sylib.syhttp3.cookieStore.CipherPersistentCookieStore
import com.synology.sylib.syhttp3.interceptors.UserAgentInterceptor
import com.whisperyao.dsplayer.util.FlipperUtils
import java.net.CookieManager
import java.net.CookiePolicy
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager


class MyHttpClient: SyHttpClient {
    constructor(cookieStore: CipherPersistentCookieStore?, z: Boolean, j: Long) {
        cookieHandler = CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL)
        addInterceptor(0, UserAgentInterceptor())
        FlipperUtils.INSTANCE.addDebugInterceptors(this)
        setTimeOut(j)
        try {
            var verifyCertsManager: VerifyCertsManager = VerifyCertsManager.getInstance(z)
            val trustManagerArr = arrayOf<TrustManager?>(verifyCertsManager)
            var sSLContext: SSLContext = SSLContext.getInstance("TLS")
            sSLContext.init(null, trustManagerArr, SecureRandom())
//            setSslSocketFactory(sSLContext.socketFactory)
            isVerifyCertificate = z
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }


    fun setVerifyCertification(verify: Boolean) {
        isVerifyCertificate = verify
        if (verify) {
            setHostnameVerifier(StrictHostnameVerifier())
        } else {
            setHostnameVerifier { _, _ ->
                return@setHostnameVerifier true
            }

        }
    }

    fun setTimeOut(connectTimeout: Long) {
        setConnectTimeout(connectTimeout, TimeUnit.SECONDS)
        setReadTimeout(connectTimeout, TimeUnit.SECONDS)
    }
}