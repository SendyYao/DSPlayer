package com.whisperyao.dsplayer

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.whisperyao.dsplayer.App.Companion.connectionManager
import com.whisperyao.dsplayer.activity.HomeActivity
import com.whisperyao.dsplayer.activity.SongListActivity
import com.whisperyao.dsplayer.datasource.network.api.SynoApiInfo
import com.whisperyao.dsplayer.datasource.network.exception.ApiException
import com.whisperyao.dsplayer.datasource.network.exception.NotSupportApiLoginException
import com.whisperyao.dsplayer.datasource.network.vo.ApiPath
import com.whisperyao.dsplayer.net.WebAPI
import com.whisperyao.dsplayer.ui.login.ConnectData
import com.whisperyao.dsplayer.util.SessionManager
import com.whisperyao.dsplayer.util.SynoLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val etAddress = findViewById<EditText>(R.id.etAddress)
        val etAccount = findViewById<EditText>(R.id.etAccount)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val switchHttps = findViewById<Switch>(R.id.switchHttps)
        val loginBtn = findViewById<Button>(R.id.btnLogin)

        loginBtn.setOnClickListener {
            val baseUrl = etAddress.text.toString().trim()
            val account = etAccount.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val useHttps = switchHttps.isChecked

            // if (isInputValid(baseUrl, account, password)) return@setOnClickListener
            // enterSongList(baseUrl, account, password, useHttps)
            setKnownAPIS()

            Toast.makeText(this@MainActivity, "Login Success & setKnownAPIs", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
        }
    }

    private fun isInputValid(baseUrl: String, account: String, password: String): Boolean {
        if (baseUrl.isEmpty() || account.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    private fun enterSongList(baseUrl: String, account: String, password: String, useHttps: Boolean) {
        login(baseUrl, account, password, useHttps) { success, response ->
            runOnUiThread {
                val json = JSONObject(response)

                if (!json.getBoolean("success")) {
                    val error = json.getJSONObject("error")
                    val code = error.getInt("code")

                    if (code == 403) {
                        val token = error.getJSONObject("errors").getString("token")

                        // 👉 弹出 OTP 输入框
                        showOtpDialog(baseUrl, account, password, useHttps, token)
                    }
                }
                if (success) {
                    Log.d("LOGIN", "Success: $response")
                } else {
                    Log.e("LOGIN", "Fail: $response")
                }
            }
        }
        startActivity(Intent(this, SongListActivity::class.java))

    }

    fun login(baseUrl: String, account: String, password: String, useHttps: Boolean, callback: (Boolean, String?) -> Unit) {
        val protocol = if (useHttps) "https" else "http"

        val url = "$protocol://$baseUrl:5000/webapi/auth.cgi?" +
                "api=SYNO.API.Auth&version=3&method=login" +
                "&account=$account&passwd=$password" +
                "&session=AudioStation&format=sid"

        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body?.contains("\"success\":true") == true) {
                    callback(true, body)
                } else {
                    callback(false, body)
                }
            }
        })
    }

    fun loginWithOtp(baseUrl: String, account: String, password: String, useHttps: Boolean, token: String, otpCode: String) {
        val protocol = if (useHttps) "https" else "http"

        val url = "$protocol://$baseUrl:5000/webapi/auth.cgi?" +
                "api=SYNO.API.Auth&version=3&method=login" +
                "&account=$account" +
                "&passwd=$password" +
                "&session=AudioStation" +
                "&format=sid" +
                "&otp_code=$otpCode" +
                "&otp_token=$token"

        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    try {
                        val json = JSONObject(body!!)

                        if (json.getBoolean("success")) {
                            val sid = json.getJSONObject("data").getString("sid")

                            Log.d("LOGIN", "SID: $sid")

                            SessionManager.saveSid(this@MainActivity, sid)

                            Toast.makeText(this@MainActivity, "Login Success", Toast.LENGTH_SHORT)
                                .show()

                            startActivity(Intent(this@MainActivity, SongListActivity::class.java))
                        } else {
                            Toast.makeText(this@MainActivity, "OTP Failed", Toast.LENGTH_SHORT)
                                .show()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    fun showOtpDialog(baseUrl: String, account: String, password: String, useHttps: Boolean, token: String) {
        val editText = EditText(this)
        editText.hint = "Enter OTP Code"

        AlertDialog.Builder(this)
            .setTitle("Two-Factor Authentication")
            .setMessage("Please enter verification code")
            .setView(editText)
            .setPositiveButton("Confirm") { _, _ ->
                val otpCode = editText.text.toString().trim()

                loginWithOtp(baseUrl, account, password, useHttps, token, otpCode)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun setKnownAPIS() {
        val apis = getSharedPreferences("login_prefs", MODE_PRIVATE).getString("webApi", "")
        val sharedPreferences = App.getContext().getSharedPreferences("login_prefs", MODE_PRIVATE)
        SynoLog.d("MainActivity", sharedPreferences.getString("webApi", ""))
//        apis?.isEmpty()?.let { if (!it) return }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urls = ConnectData(BuildConfig.NAS_ADDRESS, true).possibleUrlList

                if (urls.isEmpty()) {
                    return@launch
                }
                SynoLog.d("MainActivity", "urls: $urls")
                val result = connectionManager.queryAll(urls)
                SynoLog.d("handleUrl", result.httpUrl.toString())
                SynoLog.d("handleUrl", result.queryVo?.data.toString())
                handleUrl(result)

            } catch (e: Exception) {
                SynoLog.d("MainActivity", e.message)
            }
        }
    }
    @Throws(
        NotSupportApiLoginException::class,
        JSONException::class,
        CancellationException::class,
        ApiException::class
    )
    private fun handleUrl(result: com.whisperyao.dsplayer.datasource.network.ConnectionManager.QueryResult) {
        val apiMap = result.queryVo?.data

        SynoLog.d("handleUrl", apiMap.toString())

        if (apiMap != null) {
            val infoApi = apiMap["SYNO.AudioStation.Info"]

            if (infoApi != null) {
                if (infoApi.maxVersion < 2) {
                    throw NotSupportApiLoginException()
                }

                WebAPI.newInstance().setKnownAPIs(apiMap)
            } else {
                throw ApiException(
                    SynoApiInfo.INSTANCE,
                    ApiException.CUSTOM_PACKAGE_NOT_FOUND
                )
            }
        }

        connectionManager.setEnvironment(apiMap as HashMap<String, ApiPath>, result.httpUrl)
    }
}