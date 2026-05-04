package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.runBlocking

/**
 * @author runningpig66
 * @date 2026-04-28
 * @time 22:49
 *
 * 050.4.16-transform() 系列操作符
 *
 * 测试：takeWhile 与 transformWhile 在边界数据上的时序差异
 * 演示在需要包含临界值（如 100% 进度）并随即终止流的场景下，两者的底层执行时序差异，以及 [takeWhile] 潜在的多余拉取（Over-fetching）现象。
 * 1. takeWhile 的延迟终止现象：当使用 it <= 100 作为条件以确保临界值被发射时，元素 100 的评估结果为 true，流被正常放行且并未触发取消。
 * 为触发流的终止，迫使上游发射了一个越界数据（如 120）。只有当这个额外的脏数据进入闭包并使条件评估为 false 时，才会触发底层异常去终止流。
 * 在无限流或复杂的挂起操作中，这种被迫的索要行为极易引发挂起死锁或冗余计算。
 * 2. transformWhile 的即时截断机制：采用发射权与状态评估分离的模式：通过先调用 emit(it)，确保临界值（100）必定被下游接收。
 * 随后的 it < 100 语句会立刻评估为 false，并在当前调用帧内直接抛出控制流异常 (AbortFlowException)。
 * 这使得框架在处理完临界数据的当刻就安全终止了上游，彻底阻断了越界数据的产生，实现了更精确的流控逻辑。
 */
fun main() = runBlocking {
    val progressFlow = flowOf(20, 50, 80, 100, 120)

    progressFlow.takeWhile {
        log("takeWhile received: $it")
        it <= 100
    }.collect { log("Progress 1: $it%") }

    progressFlow.transformWhile {
        log("transformWhile received: $it")
        emit(it)
        it < 100
    }.collect { log("Progress 2: $it%") }
}
/* Output:
0 [main] takeWhile received: 20
21 [main] Progress 1: 20%
21 [main] takeWhile received: 50
21 [main] Progress 1: 50%
21 [main] takeWhile received: 80
21 [main] Progress 1: 80%
21 [main] takeWhile received: 100
22 [main] Progress 1: 100%
22 [main] takeWhile received: 120
37 [main] transformWhile received: 20
50 [main] Progress 2: 20%
50 [main] transformWhile received: 50
50 [main] Progress 2: 50%
50 [main] transformWhile received: 80
50 [main] Progress 2: 80%
50 [main] transformWhile received: 100
51 [main] Progress 2: 100%
 */
