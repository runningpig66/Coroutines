package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-29
 * @time 16:55
 *
 * 054.4.20-chunked() 操作符
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf(1, 2, 3, 4, 5)
    scope.launch {
        /* fun <T> Flow<T>.chunked(size: Int): Flow<List<T>>
        微批处理（Micro-batching）变换操作符。该操作符将上游流经的元素在局部进行缓存聚合，并按指定的 [size] 尺寸打包为只读集合向下游发射。
        其底层实现采取了严谨的内存管理策略：首先是惰性分配，内部容器仅在实际接收到首个元素时才进行堆内存分配，避免了空流场景下的对象开销。
        其次是引用隔离机制，在当前批次满载并触发 emit 发射后，内部会主动解除对该集合的局部引用（置为 null），并在接收后续数据时重新分配全新集合。
        该机制未采用通过 clear() 复用容器的方案，旨在以轻微的对象创建开销换取严格的数据隔离安全。它确保交由下游的每个数据块拥有独立的内存地址，
        从而防止下游在本地缓存集合时遭遇数据覆写问题，以及结合 buffer 等操作符跨协程边界时，因共享可变状态引发的并发修改异常。
        此外，在感知到上游终止信号时，该操作符会自动将不足阈值的残余元素打包发射，确保数据流转的零丢失。*/
        flow1.chunked(2).collect { log("chunked: $it") }
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-1] chunked: [1, 2]
18 [DefaultDispatcher-worker-1] chunked: [3, 4]
18 [DefaultDispatcher-worker-1] chunked: [5]
 */
