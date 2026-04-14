package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-12
 * @time 2:40
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    /*try {
        val thread = object : Thread() {
            override fun run() {
                throw RuntimeException()
            }
        }
        thread.start()
        println("Thread started")
        Thread.sleep(1000)
        println("Thread slept")

        thread {
            throw RuntimeException()
        }

        scope.launch {
            throw RuntimeException()
        }
    } catch (e: Exception) {

    }*/
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
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
