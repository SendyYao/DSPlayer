package com.whisperyao.dsplayer.download

import android.app.AlertDialog
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.whisperyao.dsplayer.R
import com.whisperyao.dsplayer.adapters.TaskAdapter
import com.whisperyao.dsplayer.databinding.DownloadTaskBinding
import com.whisperyao.dsplayer.item.SongItem
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.internal.functions.Functions
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TaskActivity : DaggerAppCompatActivity() {

    private lateinit var adapter: TaskAdapter

    private var isServiceOn = false

    @Inject
    lateinit var mTaskManager: TaskManager

    private lateinit var mUpdateDisposable: Disposable

    private val mConsumer = Consumer<Boolean> {
        adapter.setData(mTaskManager.getQueue())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DownloadOperator.bindService(
            this,
            object : ServiceConnection {

                override fun onServiceConnected(
                    name: ComponentName?,
                    service: IBinder?
                ) {
                    isServiceOn = true
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    isServiceOn = false
                }
            }
        )

        val binding = DownloadTaskBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val recyclerView = binding.recyclerView

        adapter = TaskAdapter(mTaskManager).apply {
            setIsListMode(true)

            setOnItemClickListener { view, item, _ ->
                if (view.id == R.id.SongItemShortCut) {
                    getQuickAction(view, item).show()
                }
            }
        }

        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        recyclerView.adapter = adapter

        val toolbar = binding.toolbarContainer.toolbar

        toolbar.setTitle(R.string.tasks)

        setSupportActionBar(toolbar)

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        mUpdateDisposable =
            Observable.interval(1000L, TimeUnit.MILLISECONDS)
                .map {
                    true
                }
                .mergeWith(mTaskManager.messageObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    mConsumer,
                    Functions.emptyConsumer()
                )
    }

    override fun onDestroy() {
        DownloadOperator.unbindFromService(this)

        if (!mUpdateDisposable.isDisposed) {
            mUpdateDisposable.dispose()
        }

        super.onDestroy()
    }

    private fun getQuickAction(
        anchor: View,
        song: SongItem
    ): PopupMenu {

        return PopupMenu(this, anchor).apply {

            inflate(R.menu.task_menu)

            setOnMenuItemClickListener { menuItem ->

                if (menuItem.itemId == R.id.ItemAction_DELETE) {

                    AlertDialog.Builder(this@TaskActivity)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.remove_select)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            deleteTask(song)
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()
                }

                true
            }
        }
    }

    private fun deleteTask(item: SongItem) {

        mTaskManager.remove(item)

        if (isServiceOn) {
            DownloadOperator.notifyDeleteTask()
        }
    }
}