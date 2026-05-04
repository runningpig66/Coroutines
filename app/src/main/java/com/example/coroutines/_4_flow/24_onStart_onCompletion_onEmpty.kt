package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-05-03
 * @time 23:34
 *
 * 058.4.24-onStart() 等全流程监听系列操作符
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flow {
        try {
            for (i in 1..5) {
                emit(i)
            }
        } catch (e: Exception) {
            log("try / catch: $e")
        }
    }
        /* fun <T> Flow<T>.onStart(action: suspend FlowCollector<T>.() -> Unit): Flow<T>
        流生命周期前置监听与干预操作符。该操作符会在下游触发终端收集动作时、且真正向上游转发收集请求之前被同步调用。
        基于底层 unsafeFlow 的嵌套包装结构，当数据流链路中存在多个连续的 onStart 调用时，其闭包逻辑的执行顺序与声明顺序相反，处于最下游的拦截器会优先执行。
        其闭包签名隐式暴露了流收集器（FlowCollector），允许开发者在真实触发上游耗时生产逻辑前，优先向下游发射初始值或前置状态标志。
        在执行该闭包期间若发生任何异常，异常信号将直接向下游的收集链透传，同时会物理阻断当前层级对上游流的 collect 调用，
        导致更上游的数据生产逻辑被静默跳过。此机制适用于前置状态注入、流启动前的资源校验以及业务链路的统一埋点。*/
        .onStart {
            log("onStart 1")
            throw RuntimeException("onStart error")
        }
        .onStart {
            log("onStart 2")
        }
        /* fun <T> Flow<T>.onCompletion(action: suspend FlowCollector<T>.(cause: Throwable?) -> Unit): Flow<T>
        流生命周期终止监听操作符，其执行语义等价于声明式的 finally 块。该操作符会在流的收集过程结束时被调用，无论结束原因是正常完成、发生异常还是响应协程取消。
        其底层通过 try-catch 结构包裹对上游的 collect 调用，因此能够完整感知执行链路（包含上下游）抛出的所有异常。
        闭包内提供 cause 参数用于判断流的终止状态（为空即代表正常结束），并隐式暴露流收集器以支持在尾部追加发射元素。
        基于链路安全考量，若流因异常或取消而终止，底层会切换使用 ThrowingCollector 接管回调逻辑，物理阻断闭包内的 emit 调用动作并直接抛出原始异常，
        从而防止向已失效的下游状态机继续传递数据。适用于资源释放、执行状态统计及收尾数据的追加发射。*/
        .onCompletion {
            log("onCompletion: $it")
        }
        /* fun <T> Flow<T>.onEmpty(action: suspend FlowCollector<T>.() -> Unit): Flow<T>
        空流兜底注入操作符。该操作符会在上游数据流正常完成收集过程，且在整个生命周期内未向下游发射过任何元素时被触发调用。
        其底层机制通过维护一个局部的布尔状态标志位来追踪上游的发射行为，任何穿透该层级的元素传递均会将此标志位置为失效。
        只有当收集动作彻底结束且标志位仍保持初始值时，底层机制才会实例化 SafeCollector 并执行开发者传入的闭包逻辑。
        闭包内隐式暴露了流收集器（FlowCollector），允许在此兜底阶段安全地向下游追加发射默认元素或状态补偿信号。适用于空数据回退、默认占位状态下发等工程场景。*/
        .onEmpty {
            log("onEmpty")
        }
        .catch {
            log("catch: $it")
        }
    scope.launch {
        flow1.collect {
            log("Data: $it")
        }
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-1] onStart 2
22 [DefaultDispatcher-worker-1] onStart 1
27 [DefaultDispatcher-worker-1] onCompletion: java.lang.RuntimeException: onStart error
28 [DefaultDispatcher-worker-1] catch: java.lang.RuntimeException: onStart error
 */
