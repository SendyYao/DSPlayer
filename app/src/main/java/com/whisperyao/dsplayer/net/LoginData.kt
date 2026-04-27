package com.whisperyao.dsplayer.net

import java.net.URL

data class LoginData (
    var account: String,
    var enable_device_token: Boolean = false,
    var otpcode: String,
    var password: String,
    var url: URL
)