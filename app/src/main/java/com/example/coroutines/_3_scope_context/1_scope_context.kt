package com.example.coroutines._3_scope_context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-14
 * @time 17:45
 */
fun main() = runBlocking<Unit> {
    val context: CoroutineContext = Dispatchers.IO + Job() + Job()
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    val job = scope.launch {
        this.coroutineContext[Job]
        coroutineContext.job
        val interceptor = coroutineContext[ContinuationInterceptor]
    }
    delay(10000)
}
