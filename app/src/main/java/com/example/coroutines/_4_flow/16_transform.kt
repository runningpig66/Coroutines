package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-28
 * @time 19:28
 *
 * 050.4.16-transform() 系列操作符
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking {
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
        // flow1.map { it + 1 }
        /* fun <T, R> Flow<T>.transform(crossinline transform: suspend FlowCollector<R>.(value: T) -> Unit): Flow<R>
        具备最高自由度的数据流泛化转换操作符，可视为 [filter] 与 [map] 等线性操作符的底层构建块。
        其核心机制是将下游的 [FlowCollector] 作为挂起转换函数 [transform] 的隐式接收者直接暴露给调用方。
        这意味着针对上游发射的每一个元素，开发者均可自主掌握发射控制权，自由决定是将其静默跳过（实现过滤）、执行一对一映射，
        还是进行裂变发射多次（实现一对多）。由于直接赋予了操作发射器的能力，该操作符在底层强制采用了安全流（Safe Flow）机制，
        以严格保障协程上下文的封装与异常的透明性；同时，闭包内所有的挂起调用均天然融入流的结构化并发体系，严格响应并传递任何协作式取消信号。*/
        flow1.transform {
            if (it > 0) {
                repeat(it) { _ ->
                    emit("$it - hello")
                }
            }
        }.collect { log("1: $it") }

        /* fun <T, R> Flow<T>.transformWhile(transform: suspend FlowCollector<R>.(value: T) -> Boolean): Flow<R>
        结合了高度自由的并发发射控制与动态状态拦截的复合变换操作符，可视为 [takeWhile] 的泛化与能力超集。
        该操作符将下游的 [FlowCollector] 作为隐式接收者暴露给挂起闭包，允许开发者在单次迭代中执行跳过、单一映射或裂变发射多次等灵活操作。
        闭包需返回一个布尔状态值以主导数据流的时序生命周期：只要条件返回 true，上游收集与转换即持续运转；
        一旦返回 false，底层会立刻抛出控制流异常（AbortFlowException）以逆向击穿并强制熔断上游的挂起执行栈，
        随后在操作符内部静默捕获并消化该异常。这种依赖异常回溯的短路机制，不仅能在不破坏外部协程结构化并发的前提下安全截断流，
        还能完美支撑“在满足终止条件的那一刻，仍需向外发射最终态数据”（如精准上报 100% 下载进度条完毕信号）的复杂工程场景。*/
        flow1.transformWhile { // transform() + takeWhile()
            // if (it > 3) return@transformWhile false
            if (it in 1..3) {
                repeat(it) { _ ->
                    emit("$it - hello")
                }
            }
            it < 3
        }.collect { log("2: $it") }

        /* fun <T, R> Flow<T>.transformLatest(transform: suspend FlowCollector<R>.(value: T) -> Unit): Flow<R>
        带有抢占式取消（Preemptive Cancellation）机制的并发变换操作符。该操作符将下游的 [FlowCollector] 作为隐式接收者暴露给挂起闭包，
        赋予开发者自由控制数据发射频次（如实现过滤或一对多发射）的能力。它在底层基于 [ChannelFlow] 架构构建并自带并发缓冲区，
        框架会为上游发射的每一个元素派生独立的子协程来执行转换逻辑。其关键特性在于严格的时序淘汰机制：当上游发射新元素时，
        若前一个元素的转换协程仍处于挂起执行状态，底层并发引擎会立即向上个任务发送取消信号以强制中断其后续逻辑，随后启动新协程处理最新元素。
        这种基于结构化并发的过期任务丢弃策略，有效避免了无效的计算与资源占用，适用于搜索联想提示、高频 UI 状态同步等包含耗时异步操作且仅依赖最新数据的工程场景。*/
        flow2.transformLatest {
            delay(50)
            emit("$it - start")
            delay(100)
            emit("$it -end")
        }.collect { log("3: $it") }
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-1] 1: 1 - hello
13 [DefaultDispatcher-worker-1] 1: 2 - hello
13 [DefaultDispatcher-worker-1] 1: 2 - hello
13 [DefaultDispatcher-worker-1] 1: 3 - hello
13 [DefaultDispatcher-worker-1] 1: 3 - hello
13 [DefaultDispatcher-worker-1] 1: 3 - hello
13 [DefaultDispatcher-worker-1] 1: 4 - hello
13 [DefaultDispatcher-worker-1] 1: 4 - hello
13 [DefaultDispatcher-worker-1] 1: 4 - hello
13 [DefaultDispatcher-worker-1] 1: 4 - hello
13 [DefaultDispatcher-worker-1] 1: 5 - hello
14 [DefaultDispatcher-worker-1] 1: 5 - hello
14 [DefaultDispatcher-worker-1] 1: 5 - hello
14 [DefaultDispatcher-worker-1] 1: 5 - hello
14 [DefaultDispatcher-worker-1] 1: 5 - hello
21 [DefaultDispatcher-worker-1] 2: 1 - hello
22 [DefaultDispatcher-worker-1] 2: 2 - hello
22 [DefaultDispatcher-worker-1] 2: 2 - hello
22 [DefaultDispatcher-worker-1] 2: 3 - hello
22 [DefaultDispatcher-worker-1] 2: 3 - hello
22 [DefaultDispatcher-worker-1] 2: 3 - hello
216 [DefaultDispatcher-worker-1] 3: 1 - start
321 [DefaultDispatcher-worker-2] 3: 2 - start
431 [DefaultDispatcher-worker-2] 3: 3 - start
540 [DefaultDispatcher-worker-1] 3: 3 -end
 */
