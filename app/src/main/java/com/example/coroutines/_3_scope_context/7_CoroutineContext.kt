package com.example.coroutines._3_scope_context

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.ContinuationInterceptor

/**
 * @author runningpig66
 * @date 2026-04-19
 * @time 10:17
 */
fun main() = runBlocking<Unit> {
    val job1 = Job()
    val job2 = Job()
    val scope = CoroutineScope(Dispatchers.IO + job1 + CoroutineName("MyCoroutine") + job2)
    println("Job1: $job1, Job2: $job2")
    println("CoroutineContext: ${scope.coroutineContext}")
    scope.launch {
        val job: Job? = coroutineContext[Job]
        val interceptor: CoroutineDispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        val dispatcher: CoroutineDispatcher? = coroutineContext[CoroutineDispatcher]
        println("coroutineContext: $coroutineContext")
        println("coroutineContext after minusKey() ${coroutineContext.minusKey(Job)}")
    }
    delay(10000)
}
