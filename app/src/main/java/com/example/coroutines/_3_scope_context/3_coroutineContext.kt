package com.example.coroutines._3_scope_context

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * @author runningpig66
 * @date 2026-04-15
 * @time 13:11
 *
 * 029.3.3-从挂起函数里获取 Coroutine Context
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    scope.launch {
        showDispatcher()
    }

    withContext(CoroutineName("Caller")) {
        CoroutineScope(CoroutineName("Receiver")).suspendWithScope()
    }

    delay(10000)
}

suspend fun CoroutineScope.suspendWithScope() {
    println(coroutineContext[CoroutineName]) // CoroutineName(Receiver)
    println(coroutineContext[ContinuationInterceptor]) // null
    println(currentCoroutineContext()[CoroutineName]) // CoroutineName(Caller)
    println(currentCoroutineContext()[ContinuationInterceptor]) // BlockingEventLoop@7f13d6e
}

// Flow flow()
private fun flowFun() {
    flow<String> {
        coroutineContext
    }
    GlobalScope.launch {
        flow<String> {
            coroutineContext // this@launch.coroutineContext
            currentCoroutineContext()
        }
    }
}

private suspend fun showDispatcher() {
    delay(1000)
    currentCoroutineContext()
    println("Dispatcher: ${coroutineContext[ContinuationInterceptor]}")
}
