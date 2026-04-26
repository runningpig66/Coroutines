package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-21
 * @time 15:21
 *
 * 040.4.6-Flow 的功能定位
 *
 * Flow 的执行机制基于高阶函数与延续体的同步回调模型。调用 flow 构建器仅实例化流对象并封装挂起函数闭包，不触发数据生产。
 * 调用 collect 方法触发流的执行后，控制流进入 flow 闭包内部顺序推进。
 * 执行至 emit 方法时，系统携带被发射的元素数据，在 emit 调用当前的协程上下文中，就地且同步地执行 collect 闭包定义的逻辑。
 * 这种执行路径决定了 collect 闭包的运行时协程上下文严格依附于触发该次 emit 所在的协程上下文。
 *
 * 基础的 FlowCollector 实例具备非线程安全特性。跨协程并发执行 emit 方法将导致下游 collect 闭包被并发调用，进而引发数据竞争现象。
 * 底层架构建立上下文一致性约束规避此风险，在 collect 调用处隐式注入 SafeCollector 实例进行运行时校验。
 * SafeCollector 在实例构建阶段捕获并记录发起 collect 调用的初始协程 Job 作为合法基准上下文。
 *
 * 在后续的每一次数据发射阶段，SafeCollector 强制校验当前执行 emit 方法的协程上下文。若当前协程 Job 匹配基准 Job，
 * 或当前协程是通过 coroutineScope 等作用域构建器派生的严格同步子协程，底层机制判定基准父协程处于强挂起状态，
 * 当前控制流符合串行执行特征，进而放行发射操作。若检测到 emit 调用发生于由 launch 或 async 启动的异步并发子协程或独立协程中，
 * SafeCollector 判定该调用违背了流的单向顺序执行约束，随即引发 IllegalStateException 异常并终止控制流。
 *
 * notes: 1. Kotlin Flow 执行机制与上下文一致性（Flow Invariant）分析笔记.md
 * notes: 2. Flow 异步环境约束与上下文保留机制分析笔记.md
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val numFlow = flow {
        emit(0)
        /*coroutineScope {
            log("collect job?2: ${this.coroutineContext.job.parent}") // @2a48dfd5
        }
        println("collected num: $it")*/
        launch { // this@runBlocking
            log("emit1 in job: ${this.coroutineContext.job}") // 1f57539
            emit(1) // this@flow
            /*coroutineScope {
                log("collect job?2: ${this.coroutineContext.job.parent}") // 为了验证挂起函数collect()内部是否切换过协程
            }
            println("collected num: $it")*/
        }
        coroutineScope {
            log("emit0 in job: ${this.coroutineContext.job.parent}") // @2a48dfd5
            log("emit2 in job: ${this.coroutineContext.job}") // 58431f17
            emit(2)
            /*coroutineScope {
                log("collect job?2: ${this.coroutineContext.job.parent}") // @58431f17
            }
            println("collected num: $it")*/
            launch {
                log("emit3 in job: ${this.coroutineContext.job}") // @520d1162
                emit(3)
                /*coroutineScope {
                    log("collect job?2: ${this.coroutineContext.job.parent}") // 为了验证挂起函数collect()内部是否切换过协程
                }
                println("collected num: $it")*/
            }
        }
    }
    scope.launch {
        log("collect job?1: ${this.coroutineContext.job}") // @2a48dfd5
        numFlow.collect {
            coroutineScope {
                log("collect job?2: ${this.coroutineContext.job.parent}") // 为了验证挂起函数collect()内部是否切换过协程
            }
            println("collected num: $it")
        }
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-1] collect job?1: StandaloneCoroutine{Active}@2a48dfd5
35 [DefaultDispatcher-worker-1] collect job?2: StandaloneCoroutine{Active}@2a48dfd5
collected num: 0
43 [main] emit1 in job: StandaloneCoroutine{Active}@1f57539
43 [DefaultDispatcher-worker-1] emit0 in job: StandaloneCoroutine{Active}@2a48dfd5
43 [DefaultDispatcher-worker-1] emit2 in job: ScopeCoroutine{Active}@58431f17
44 [DefaultDispatcher-worker-1] collect job?2: ScopeCoroutine{Active}@58431f17
collected num: 2
45 [DefaultDispatcher-worker-2] emit3 in job: StandaloneCoroutine{Active}@520d1162
Exception in thread "main" java.lang.IllegalStateException: Flow invariant is violated:
		Emission from another coroutine is detected.
		Child of StandaloneCoroutine{Active}@1f57539, expected child of StandaloneCoroutine{Active}@2a48dfd5.
		FlowCollector is not thread-safe and concurrent emissions are prohibited.
		To mitigate this restriction please use 'channelFlow' builder instead of 'flow'
		...

Process finished with exit code 1
 */
