package com.whisperyao.dsplayer.fragment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.ui.login.ConnectData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TestFragment : Fragment(R.layout.test_fragment) {

    private lateinit var etIp: EditText
    private lateinit var btnTest: Button
    private lateinit var tvResult: TextView

    private val connectionManager by lazy {
        App.connectionManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etIp = view.findViewById(R.id.etIp)
        btnTest = view.findViewById(R.id.btnTest)
        tvResult = view.findViewById(R.id.tvResult)

        btnTest.setOnClickListener {
            testQueryAll()
        }
    }

    private fun testQueryAll() {
        val ip = etIp.text.toString().trim()

        if (ip.isEmpty()) {
            tvResult.text = "IP不能为空"
            return
        }

        tvResult.text = "请求中..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urls = ConnectData(ip, true).possibleUrlList

                if (urls.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        tvResult.text = "URL列表为空"
                    }
                    return@launch
                }

                val result = connectionManager.queryAll(urls)

                withContext(Dispatchers.Main) {
                    tvResult.text = buildString {
                        appendLine("请求成功")
                        appendLine()
                        appendLine("queryVo:")
                        appendLine(result.queryVo?.data.toString())
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvResult.text = "请求失败：\n${e.stackTraceToString()}"
                }
            }
        }
    }
}