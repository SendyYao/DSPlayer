package com.whisperyao.dsplayer.util

import android.content.Context
import android.text.TextUtils
import com.synology.sylib.security.KsHelper
import com.synology.sylib.security.data.KsCipherData
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext
import javax.inject.Inject

class DataKeyStoreHelper {
    private var context: Context

    @Inject
    constructor(@ApplicationContext context: Context) {
        this.context = context
    }
    fun getContext(): Context {
        return this.context
    }

    fun decodeData(data: String): String? {
        val ksHelper: KsHelper = KsHelper.get(this.context)
        if (ksHelper != null) {
            return ksHelper.decryptAsString(KsCipherData.fromEncoded(data), data)
        }
        return null
    }

    fun encodeData(data: String): String? {
        if (TextUtils.isEmpty(data)) {
            return ""
        }
        val ksHelper: KsHelper = KsHelper.get(this.context)
        val ksCipherDataEncrypt: KsCipherData? = if (ksHelper != null) ksHelper.encrypt(data) else null
        val encode: String? = ksCipherDataEncrypt?.encoded
        return encode ?: data
    }
}