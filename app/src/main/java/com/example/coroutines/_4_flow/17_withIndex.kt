package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-29
 * @time 1:39
 *
 * 051.4.17-withIndex() 操作符
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf(1, 2, 3, 4, 5)
    scope.launch {
        /* fun <T> Flow<T>.withIndex(): Flow<IndexedValue<T>>
        将数据流中的每个元素与其对应的零基索引（Zero-based index）进行配对的转换操作符。
        该操作符会在内部维护一个递增的计数器，并将上游发射的每个原始元素包装为包含当前索引值和元素本体的 [IndexedValue] 数据类对象。
        此机制适用于下游业务逻辑严格依赖元素发射顺序或位置状态的场景。需要注意的是，由于该操作符会为每个元素引发包装类的实例化，
        若仅在数据流的终端收集阶段需要使用索引，推荐优先选择 [collectIndexed] 终端操作符，以规避不必要的对象创建与内存开销。*/
        flow1.withIndex().collect { log("1: ${it.index} - ${it.value}") }
        flow1.withIndex().collect { (index, value) -> log("2: $index - $value") }
        /* suspend inline fun <T> Flow<T>.collectIndexed(action: suspend (index: Int, value: T) -> Unit): Unit
        附带索引流转能力的终端收集操作符。该操作符会真正触发数据流的执行，并在所提供的挂起闭包 [action] 中同时向外暴露元素的零基索引与数据本体。
        其底层通过实例化一个内部的流收集器，并维护一个局部的状态变量来精准追踪发射频次，同时内置了针对超大流处理时的索引溢出检查机制（防止超出 Int.MAX_VALUE）。
        相较于使用中间操作符 [withIndex]，该函数直接在流的终端完成索引分发，完全避免了包装类对象的堆内存分配，
        是仅需在最终收集阶段读取位置状态时的最高效实现方案。在收集过程或上游执行中产生的任何异常，均会沿调用栈直接向外抛出。*/
        flow1.collectIndexed { index, value ->
            log("3: $index - $value")
        }
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-1] 1: 0 - 1
11 [DefaultDispatcher-worker-1] 1: 1 - 2
11 [DefaultDispatcher-worker-1] 1: 2 - 3
11 [DefaultDispatcher-worker-1] 1: 3 - 4
11 [DefaultDispatcher-worker-1] 1: 4 - 5
14 [DefaultDispatcher-worker-1] 2: 0 - 1
14 [DefaultDispatcher-worker-1] 2: 1 - 2
14 [DefaultDispatcher-worker-1] 2: 2 - 3
14 [DefaultDispatcher-worker-1] 2: 3 - 4
14 [DefaultDispatcher-worker-1] 2: 4 - 5
15 [DefaultDispatcher-worker-1] 3: 0 - 1
15 [DefaultDispatcher-worker-1] 3: 1 - 2
15 [DefaultDispatcher-worker-1] 3: 2 - 3
15 [DefaultDispatcher-worker-1] 3: 3 - 4
15 [DefaultDispatcher-worker-1] 3: 4 - 5
 */
