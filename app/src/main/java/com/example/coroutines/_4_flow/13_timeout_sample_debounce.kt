package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @author runningpig66
 * @date 2026-04-27
 * @time 13:50
 *
 * 047.4.13-timeout、sample、debounce()
 */
@OptIn(FlowPreview::class)
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flow {
        emit(0)
        delay(500)
        emit(1)
        delay(800)
        emit(2)
        delay(900)
        emit(3)
        delay(1000)
    }
    scope.launch {
        try {
            // fun <T> Flow<T>.timeout(timeout: Duration): Flow<T>
            flow1.timeout(1.seconds).collect { log("1: $it") }
        } catch (e: TimeoutCancellationException) {
            e.printStackTrace()
        }
    }
    scope.launch {
        // fun <T> Flow<T>.sample(period: Duration): Flow<T>
        flow1.sample(1.seconds).collect { log("2: $it") }
    }
    scope.launch {
        // fun <T> Flow<T>.debounce(timeout: Duration): Flow<T>
        flow1.debounce(1.seconds).collect { log("3: $it") }
    }
    delay(10000)
}

fun <T> Flow<T>.throttle(timeWindows: Duration): Flow<T> = flow {
    var lastTime = 0L
    collect {
        // 将抽象的时间段（如 1.seconds）转换为绝对的毫秒长整数（如 1000L），以便与系统时间戳（Long 类型）直接进行减法运算
        if (System.currentTimeMillis() - lastTime > timeWindows.inWholeMilliseconds) {
            emit(it)
            lastTime = System.currentTimeMillis()
        }
    }
}
/* Output:
0 [DefaultDispatcher-worker-1] 1: 0
486 [DefaultDispatcher-worker-7] 1: 1
966 [DefaultDispatcher-worker-2] 2: 1
1290 [DefaultDispatcher-worker-3] 1: 2
1976 [DefaultDispatcher-worker-1] 2: 2
2194 [DefaultDispatcher-worker-5] 1: 3
2979 [DefaultDispatcher-worker-4] 2: 3
3209 [DefaultDispatcher-worker-5] 3: 3
kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1s
 */
