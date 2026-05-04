package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-27
 * @time 20:51
 *
 * 048.4.14-drop()、take() 系列操作符
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf(1, 2, 3, 4, 5, 1)
    scope.launch {
        /* fun <T> Flow<T>.drop(count: Int): Flow<T>
        丢弃数据流起始端的前 [count] 个元素。该操作符内部维护一个轻量级计数器，在丢弃数量达到指定阈值前执行拦截逻辑；
        一旦达到阈值，该操作符将转变为透明代理，对后续产生的所有元素进行无条件放行，不再产生额外的条件判断开销。*/
        flow1.drop(2).collect { log("1: $it") } // 3,4,5,1

        /* fun <T> Flow<T>.dropWhile(predicate: suspend (T) -> Boolean): Flow<T>
        基于给定条件 [predicate] 动态丢弃流头部元素，直到遇到首个使条件返回 false 的元素为止。
        该操作符表现为一个一次性状态闸门而非全局过滤器，一旦首个临界元素打破了拦截条件，该内部状态判定机制即宣告失效，
        后续产生的所有元素（即使再次满足 predicate 条件）都将被无条件放行，不再参与逻辑运算。*/
        flow1.dropWhile { it != 3 }.collect { log("2: $it") } // 3,4,5,1

        /* fun <T> Flow<T>.take(count: Int): Flow<T>
        仅从数据流起始端提取前 [count] 个元素并发往外层下游。为了在达到提取数量后极速终止上游继续生产无效数据，
        该操作符会在内部触发控制流异常（AbortFlowException）以逆向击穿并终止上游流的执行闭包，随后在操作符内部局部捕获并消耗该异常。
        这种利用异常进行栈回溯的底层机制，能够安全地强制释放上游资源，避免协程泄漏。*/
        flow1.take(2).collect { log("3: $it") } // 1,2

        /* fun <T> Flow<T>.takeWhile(predicate: suspend (T) -> Boolean): Flow<T>
        持续提取并放行上游元素，直到遇到首个使 [predicate] 条件返回 false 的临界元素时终止。
        该机制结合了一次性状态校验与异常熔断逻辑，在遇到不满足条件的元素瞬间，立即停止放行并抛出内部取消异常以强制中断上游数据流的挂起执行，
        随后通过静默吞噬该异常来完成整个数据流的正常闭环与上下游资源回收。*/
        flow1.takeWhile { it != 3 }.collect { log("4: $it") } // 1,2
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-1] 1: 3
37 [DefaultDispatcher-worker-1] 1: 4
37 [DefaultDispatcher-worker-1] 1: 5
37 [DefaultDispatcher-worker-1] 1: 1
48 [DefaultDispatcher-worker-1] 2: 3
48 [DefaultDispatcher-worker-1] 2: 4
49 [DefaultDispatcher-worker-1] 2: 5
49 [DefaultDispatcher-worker-1] 2: 1
57 [DefaultDispatcher-worker-1] 3: 1
59 [DefaultDispatcher-worker-1] 3: 2
76 [DefaultDispatcher-worker-1] 4: 1
76 [DefaultDispatcher-worker-1] 4: 2
 */
