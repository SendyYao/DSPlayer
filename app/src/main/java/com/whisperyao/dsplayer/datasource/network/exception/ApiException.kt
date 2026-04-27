package com.whisperyao.dsplayer.datasource.network.exception


import com.google.gson.JsonElement
import com.whisperyao.dsplayer.datasource.network.api.WebApi

class ApiException(
    val webApi: WebApi,
    val errorCode: Int = 1,
    val errors: JsonElement? = null
) : RuntimeException() {

    fun isNeedOtpError(): Boolean {
        return needOtp(errorCode)
    }

    fun getResId(): Int {
        return webApi.errorStringRes(errorCode)
    }

    override fun toString(): String {
        return "api exception: $errorCode"
    }

    companion object {
        const val CUSTOM_PACKAGE_NOT_FOUND = -101
        const val FAILED_CONNECTION = -2

        fun isNoPermissionError(errorCode: Int): Boolean {
            return errorCode == 105 ||
                    errorCode == 119 ||
                    errorCode == 160
        }

        fun needOtp(errorCode: Int): Boolean {
            return errorCode == 403 ||
                    errorCode == 404
        }

        fun noSuchApi(webApi: WebApi): ApiException {
            return ApiException(webApi, 102)
        }

        fun determineForceLogoutError(throwable: Throwable): Boolean {
            return throwable is ApiException &&
                    isNoPermissionError(throwable.errorCode)
        }
    }
}