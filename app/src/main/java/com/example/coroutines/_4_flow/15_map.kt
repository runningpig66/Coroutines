package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-28
 * @time 15:56
 *
 * 049.4.15-map() 系列操作符
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf(1, 2, 3, 4, 5)
    val flow2 = flow {
        delay(100)
        emit(1)
        delay(100)
        emit(2)
        delay(100)
        emit(3)
    }
    scope.launch {
        /* fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R>
        将上游数据流中的每个元素通过指定的挂起变换函数 [transform] 映射为新的结果值，并向下游发射该结果。
        该操作符是一个完全透明的一对一数据投射代理，它不会改变数据流的原始发射时序或生命周期，常用于数据结构转换、对象解构或基于单个元素的衍生计算。*/
        flow1.map { if (it == 3) null else it + 1 }.collect { log("1: $it") }

        /* fun <T, R: Any> Flow<T>.mapNotNull(crossinline transform: suspend (value: T) -> R?): Flow<R>
        结合了数据变换与空值过滤的复合操作符。它将上游元素通过挂起函数 [transform] 投射为可空的结果值，
        并在内部执行空安全校验：仅当映射结果非 null 时才会将其发往下游，若结果为 null 则对其执行静默丢弃。在工程实践中，
        它用于替代 map 与 filterNotNull 的链式调用，通过在单一闭包内完成变换与过滤，有效减少中间 Flow 对象的创建开销与挂起函数调用栈的深度。*/
        flow1.mapNotNull { if (it == 3) null else it + 1 }.collect { log("2: $it") }

        /*flow1.map { if (it == 3) null else it + 1 }.filterNotNull().collect { log("3: $it") }
        flow1.filter { it != 3 }.map { it + 1 }.collect { log("4: $it") }*/
        /* fun <T, R> Flow<T>.mapLatest(@BuilderInference transform: suspend (value: T) -> R): Flow<R>
        将上游数据流中的每个元素通过挂起函数 [transform] 映射为新的结果值。该操作符具备抢占式取消特性，
        当上游发射新元素时，若前一个元素的转换任务仍处于挂起执行状态，底层流架构会立即发送取消信号强制中断前一个任务，
        并利用最新元素启动新的转换协程。这种基于结构化并发的过期任务丢弃策略，有效避免了无意义的计算与资源等待，
        特别适用于搜索联想、实时状态同步等高频触发且包含耗时异步操作的业务场景，确保下游仅接收未被中断且最终执行成功的最新转换结果。*/
        flow2.mapLatest { delay(150); it + 1 }.collect { log("5: $it") }
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-1] 1: 2
15 [DefaultDispatcher-worker-1] 1: 3
15 [DefaultDispatcher-worker-1] 1: null
16 [DefaultDispatcher-worker-1] 1: 5
16 [DefaultDispatcher-worker-1] 1: 6
20 [DefaultDispatcher-worker-1] 2: 2
20 [DefaultDispatcher-worker-1] 2: 3
20 [DefaultDispatcher-worker-1] 2: 5
20 [DefaultDispatcher-worker-1] 2: 6
585 [DefaultDispatcher-worker-3] 5: 4
 */
