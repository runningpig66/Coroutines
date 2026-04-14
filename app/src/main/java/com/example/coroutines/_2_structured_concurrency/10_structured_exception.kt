package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.exitProcess

/**
 * @author runningpig66
 * @date 2026-04-12
 * @time 3:11
 */
fun main() = runBlocking<Unit> {
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        println("Default caught $e in thread $t")
        exitProcess(1)
    }
    /*thread {
        Thread.currentThread().setUncaughtExceptionHandler { t, e ->
            println("Caught $e in thread $t")
        }
        throw RuntimeException("Thread error!")
    }

    val thread = Thread {
        throw RuntimeException("Thread error!")
    }
    // thread.setUncaughtExceptionHandler { t, e ->
    //     println("Caught $e in thread $t")
    // }
    thread.start()*/
    val scope = CoroutineScope(EmptyCoroutineContext)
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught in Coroutine: $exception")
    }
    scope.launch(handler) {
        launch {
            throw RuntimeException("Error!")
        }
        launch {

        }
    }
    delay(10000)
}
