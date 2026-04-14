package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author runningpig66
 * @date 2026-04-08
 * @time 4:44
 */
fun main() = runBlocking<Unit> {
    // val scope = CoroutineScope(Dispatchers.IO)
    // val job = scope.launch(Dispatchers.Default) {
    //     val outer = scope.coroutineContext[ContinuationInterceptor]
    //     val inner = this.coroutineContext[ContinuationInterceptor]
    //     println("outer: $outer") // outer: Dispatchers.IO
    //     println("inner: $inner") // inner: Dispatchers.Default
    // }
    // job.join()

    val scope = CoroutineScope(Dispatchers.IO)
    var outerJob: Job? = null
    var innerJob: Job? = null
    var innerScope: CoroutineScope? = null
    val innerJob2 = scope.launch(Dispatchers.Default) {
        outerJob = scope.coroutineContext[Job]
        innerJob = this.coroutineContext[Job]
        innerScope = this

        launch {

        }
    }

    innerJob2.cancel()

    scope.async {

    }

    println("innerJob2: $innerJob2")
    println("innerJob: $innerJob")
    println("innerJob2 === innerJob: ${innerJob2 === innerJob}") // true
    println("outerJob: $outerJob")
    println("innerJob2 === innerScope: ${innerJob2 === innerScope}") // true
}
