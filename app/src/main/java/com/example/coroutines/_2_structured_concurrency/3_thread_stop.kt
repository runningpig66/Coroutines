package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

/**
 * @author runningpig66
 * @date 2026-04-09
 * @time 4:10
 */
fun main() = runBlocking<Unit> {
    val thread = thread {
        println("Thread: I'm running!")
        Thread.sleep(2000)
        println("Thread: I'm done!")
    }
    Thread.sleep(1000)
    thread.stop()
    // thread.interrupt()
}
