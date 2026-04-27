package com.whisperyao.dsplayer.util

import android.content.Context
import android.text.TextUtils
import com.synology.sylib.security.KsHelper
import com.synology.sylib.security.data.KsCipherData
import com.whisperyao.dsplayer.injection.qualifier.ApplicationContext

class DataKeyHelper {
    private var context: Context
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

    fun encodeData(data: String): String {
        if (TextUtils.isEmpty(data)) {
            return ""
        }
        val ksHelper: KsHelper = KsHelper.get(this.context)
        val ksCipherDataEncrypt: KsCipherData? = ksHelper.encrypt(data)
        val encode: String? = ksCipherDataEncrypt?.encoded
        return encode ?: data
    }
}