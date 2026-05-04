package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-29
 * @time 16:19
 *
 * 053.4.19-onEach() 操作符
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf(1, 2, 3, 4, 5)
    scope.launch {
        /* fun <T> Flow<T>.onEach(action: suspend (T) -> Unit): Flow<T>
        透明的副作用（Side-effect）注入操作符，其设计语义与 Java Stream 的 [peek] 类似，主要用于数据流转过程中的状态观测与外部状态同步。
        在底层实现上，该操作符基于 [transform] 构建了严格的线性拦截机制：对于上游发射的每一个元素，框架必定优先挂起执行 [action] 闭包，
        待该闭包彻底恢复（Resume）后，才会调用底层的 emit 函数将原元素向下游转发。因此，虽然该操作符不具备修改或过滤流元素的能力，
        但闭包内的任何挂起耗时均会直接阻塞当前元素的向下传递。适用于日志记录、埋点上报、本地缓存持久化等不改变核心数据流向、但严格要求执行时序的工程场景。*/
        flow1.onEach {
            log("onEach1: $it")
        }.onEach {
            log("onEach2: $it")
        }.filter {
            it % 2 == 0
        }.onEach {
            log("onEach3: $it")
        }.collect {
            log("collect: $it")
        }
    }
    delay(10000)
}
/* Output:
0 [main] onEach1: 1
15 [main] onEach2: 1
15 [main] onEach1: 2
15 [main] onEach2: 2
15 [main] onEach3: 2
16 [main] collect: 2
16 [main] onEach1: 3
16 [main] onEach2: 3
16 [main] onEach1: 4
16 [main] onEach2: 4
16 [main] onEach3: 4
16 [main] collect: 4
16 [main] onEach1: 5
16 [main] onEach2: 5
 */
