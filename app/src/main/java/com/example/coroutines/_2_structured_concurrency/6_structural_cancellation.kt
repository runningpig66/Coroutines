package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.measureTime

/**
 * @author runningpig66
 * @date 2026-04-10
 * @time 5:42
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val parentJob = scope.launch {
        val childJob = launch {
            println("Child job started")
            // try {
            //     delay(3000)
            // } catch (e: CancellationException) {
            //
            // }
            // Thread.sleep(3000)
            delay(3000)
            println("Child job finished")
        }
    }
    delay(1000)
    parentJob.cancel()
    measureTime { parentJob.join() }.also { println("Duration: $it") }
    // parentJob.cancelAndJoin()
    delay(10000)
}
