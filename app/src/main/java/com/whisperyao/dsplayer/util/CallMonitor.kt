package com.whisperyao.dsplayer.util

class CallMonitor {
    companion object {
        private val callCounts = mutableMapOf<String, Int>()
        private val lastCallTimes = mutableMapOf<String, Long>()
        private val lock = Any()

        @JvmStatic
        fun hit(key: String, threshold: Int = 10) {
            val currentTime = System.currentTimeMillis()
            synchronized(lock) {
                val count = callCounts.getOrDefault(key, 0) + 1
                callCounts[key] = count
                val lastTime = lastCallTimes.getOrDefault(key, 0L)

                if (currentTime - lastTime > 1000) {
                    println("$key called $count times in last second")

                    if (count > threshold) {
                        println("WARNING: Excessive calls detected for $key!")
                        Thread.currentThread().stackTrace.forEach {
                            println("  at $it")
                        }
                    }

                    callCounts[key] = 0
                    lastCallTimes[key] = currentTime
                }
            }
        }
    }
}