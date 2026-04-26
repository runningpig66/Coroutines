package com.example.coroutines._4_flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-24
 * @time 17:30
 *
 * 045.4.11-distinct UntilChanged()
 *
 * notes: 3. Kotlin Flow Distinct 源码解析.md
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf("running", "pig", "RunningPig", "runningPig")
    scope.launch {
        /* fun <T> Flow<T>.distinctUntilChanged(): Flow<T>
        连续去重操作符。该操作符在底层状态机中仅缓存上一次向下游成功发射的元素引用。当上游产生新元素时，控制流会调用标准对象的 equals() 方法，
        将新元素与内部缓存的引用进行比对。若比对结果为 true，则拦截并丢弃当前元素；若结果为 false，则将该元素放行至下游，并同步更新状态机中的缓存引用。
        此机制仅针对执行时间轴上相邻的连续元素进行局部校验，内部不维护全量数据集，因此不提供全局时间范围内的唯一性过滤。*/
        flow1.distinctUntilChanged().collect { println("1: $it") } // == equals()

        /* Flow<T>.distinctUntilChanged(areEquivalent: (old: T, new: T) -> Boolean): Flow<T>
        自定义比较逻辑的连续去重操作符。该节点允许调用端显式注入定制化的判定闭包，以取代默认的 equals() 校验机制。
        当新数据到达时，底层逻辑会将缓存的上一有效元素与当前新元素一并传入该闭包进行求值。若运算结果返回 true，系统即判定当前输入元素状态冗余并将其拦截；
        若返回 false，则触发下游分发并刷新状态缓存。此机制支持在处理复杂数据实体时，绕过全字段等值的限制，依据外部注入的具体业务规则来决定是否拦截相邻元素。*/
        flow1.distinctUntilChanged { old, new -> old.equals(new, ignoreCase = true) }
            .collect { println("2: $it") }

        /* fun <T, K> Flow<T>.distinctUntilChangedBy(keySelector: (T) -> K): Flow<T>
        基于特征键提取的连续去重操作符。该节点在处理上游下发的数据流时，会先通过外部传入的特征提取闭包，计算出当前原始元素的特征标识。
        底层状态机仅在内存中缓存上一个成功发放下游元素的特征标识。在具体的执行时序上，当第一个元素到达时，其特征标识被计算并缓存，原始元素直接下发。
        当后续新元素到达时，若新计算的特征标识与当前缓存的标识按常规判断为相等，当前的新元素将被拦截丢弃；
        若两者不相等，该新元素将被放行至下游收集器，同时底层缓存会被立即更新为这个新的特征标识。
        此机制允许开发者仅依据数据对象的局部字段或映射结果作为过滤依据，同时确保下游业务链路接收到的依然是未经修改的完整原始对象。*/
        flow1.distinctUntilChangedBy { it.uppercase() }.collect { println("3: $it") }
    }
    delay(10000)
}
/* Output:
1: running
1: pig
1: RunningPig
1: runningPig
2: running
2: pig
2: RunningPig
3: running
3: pig
3: RunningPig
 */
