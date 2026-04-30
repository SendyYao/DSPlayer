package com.whisperyao.dsplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.whisperyao.dsplayer.App
import com.whisperyao.dsplayer.Common
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.item.SongItem
import com.whisperyao.dsplayer.ui.login.ConnectData
import com.whisperyao.dsplayer.util.SynoLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TestFragment : ContentFragment() {

    private lateinit var etIp: EditText
    private lateinit var btnTest: Button
    private lateinit var tvResult: TextView

    private val connectionManager by lazy {
        App.connectionManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        SynoLog.d("PlaylistFragment", "onCreateView")
        mContentView = inflater.inflate(R.layout.test_fragment, null)

        SynoLog.i("PlaylistFragment", "isInitialized: $isInitialized")
        if (!isInitialized) {
            isInitialized = true
            blDoRefresh = false
        }

        return mContentView
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

    override fun toggleView() { }

    override fun onPageSelected() { }

    override fun allItemPlayAction(action: Common.ItemAction?) { }

    override fun canLoadMore(): Boolean = false

    override fun canMultiEdit(): Boolean = false

    override fun canSetView(): Boolean = false

    override fun getSelectedItems(): ArrayList<SongItem>? = null

    override fun isEditMode(): Boolean = false

    override fun isPlayable(): Boolean = false

    override fun setEditMode(edit: Boolean) { }

    override fun loadContent(refresh: Boolean) {}

    override fun markAllItem(mark: Boolean) { }

    override fun onScrollToBottom(view: AbsListView?) { }

    override fun scrollToTop() { }
}