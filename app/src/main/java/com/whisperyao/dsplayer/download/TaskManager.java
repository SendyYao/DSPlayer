package com.whisperyao.dsplayer.download;

import com.whisperyao.dsplayer.item.SongItem;
import com.whisperyao.dsplayer.util.SynoLog;

import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private Subject<Boolean> mMessageSubject = PublishSubject.create();
    private final LinkedList<SongItem> jobQueue = new LinkedList<>();
    private ConcurrentHashMap<String, Integer> progressMap = new ConcurrentHashMap<>();

    public Subject<Boolean> getMessageObservable() {
        return this.mMessageSubject;
    }

    public void add(ArrayList<SongItem> songList) {
        if (!songList.isEmpty()) {
            SynoLog.d("TaskManager", "download: " + songList.get(0).getTitle());
        }
        synchronized (this.jobQueue) {
            this.jobQueue.addAll(songList);
        }
    }

    public void add(SongItem song) {
        if (song == null) {
            return;
        }
        synchronized (this.jobQueue) {
            if (this.jobQueue.contains(song)) {
                return;
            }
            this.jobQueue.add(song);
        }
    }

    public boolean contains(SongItem song) {
        boolean zContainTask;
        if (song == null) {
            return false;
        }
        synchronized (this.jobQueue) {
            zContainTask = containTask(song);
        }
        return zContainTask;
    }

    private boolean containTask(SongItem song) {
        boolean zEqualsByIdPathTrack = false;
        if (song == null) {
            return false;
        }
        Iterator<SongItem> it = this.jobQueue.iterator();
        while (it.hasNext() && !(zEqualsByIdPathTrack = it.next().equalsByIdPathTrack(song))) {
        }
        return zEqualsByIdPathTrack;
    }

    public SongItem get(int pos) {
        synchronized (this.jobQueue) {
            if (pos >= 0) {
                if (pos < this.jobQueue.size()) {
                    return this.jobQueue.get(pos);
                }
            }
            return null;
        }
    }

    public ArrayList<SongItem> getQueue() {
        ArrayList<SongItem> arrayList;
        synchronized (this.jobQueue) {
            arrayList = new ArrayList<>(this.jobQueue);
        }
        return arrayList;
    }

    public SongItem peek() {
        SongItem songItemPeek;
        synchronized (this.jobQueue) {
            songItemPeek = this.jobQueue.peek();
        }
        return songItemPeek;
    }

    public int size() {
        int size;
        synchronized (this.jobQueue) {
            size = this.jobQueue.size();
        }
        return size;
    }

    public void poll() {
        synchronized (this.jobQueue) {
            this.jobQueue.poll();
        }
        this.mMessageSubject.onNext(true);
    }

    public void remove(SongItem song) {
        synchronized (this.jobQueue) {
            this.jobQueue.remove(song);
        }
        this.mMessageSubject.onNext(true);
    }

    public void clear() {
        synchronized (this.jobQueue) {
            this.jobQueue.clear();
        }
        synchronized (this.progressMap) {
            this.progressMap.clear();
        }
        this.mMessageSubject.onNext(true);
    }

    public void setProgress(String path, int progress) {
        synchronized (this.progressMap) {
            if (progress < 0 || 100 < progress) {
                this.progressMap.remove(path);
            } else {
                this.progressMap.put(path, Integer.valueOf(progress));
            }
        }
    }

    public int getProgress(String key) {
        synchronized (this.progressMap) {
            if (!this.progressMap.containsKey(key)) {
                return 0;
            }
            return this.progressMap.get(key).intValue();
        }
    }
}