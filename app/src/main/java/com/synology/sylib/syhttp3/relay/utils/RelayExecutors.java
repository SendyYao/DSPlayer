package com.synology.sylib.syhttp3.relay.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/* loaded from: classes2.dex */
public class RelayExecutors {
    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue();

    public static ExecutorService newAsyncTaskExecutor(String str) {
        int iAvailableProcessors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(iAvailableProcessors + 1, (iAvailableProcessors * 2) + 1, 1L, TimeUnit.SECONDS, sPoolWorkQueue, new DefaultThreadFactory(str));
    }

    public static ExecutorService newFixedThreadPool(String str, int i) {
        return new ThreadPoolExecutor(i, i, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), new DefaultThreadFactory(str));
    }

    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        DefaultThreadFactory(String str) {
            SecurityManager securityManager = System.getSecurityManager();
            this.group = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = str + " pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override // java.util.concurrent.ThreadFactory
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != 5) {
                thread.setPriority(5);
            }
            return thread;
        }
    }
}