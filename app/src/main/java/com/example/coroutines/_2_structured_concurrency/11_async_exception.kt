package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-12
 * @time 10:19
 *
 * 关于 async 构建器的异常处理与传播机制，需要从结构化并发和根协程行为两个维度理解。
 * 1. async 的双重异常传播特性：当协程内部发生非取消异常时，异常会沿 Job 树向上传播，直至到达传播链路的终端节点，从而触发整棵协程树的级联取消。
 * 对于 async 构建器而言，其异常处理展现出双重特性：一方面它参与上述结构化取消过程（向父节点汇报）；另一方面，异常信息会被封装在底层状态机
 * （CompletedExceptionally）与返回的 Deferred 对象中。当外部代码调用 await() 时，挂起恢复点会重新抛出该原始异常，使调用方能够直接感知到异步任务的失败。
 * 2. 异常处理器（CoroutineExceptionHandler）的生效边界与“向上委托”原则：协程内部发生异常时，严格遵循“优先向直接父节点委托”的底层协议。
 * 普通子协程的异常会被父协程无条件接管并继续向上传递，导致子协程自身的 CoroutineExceptionHandler 被状态机直接忽略。
 * 只有当协程的直接父节点拒绝接管异常（例如父节点是一个纯粹的管理节点，如普通的 Job() 或 SupervisorJob()，
 * 而非另一个 StandaloneCoroutine 等执行体）时，向上传播的链路才会被阻断。
 * 此时，该协程在物理层面成为“有效根协程”（Effective Root），被迫就地处理未捕获异常，其上下文中的 CoroutineExceptionHandler 才会被触发。
 * 3. 根节点 launch 与 async 的设计差异：在有效根协程处理未捕获异常时，两者行为存在本质分歧。
 * 根 launch 将未捕获异常视为系统级崩溃，会尝试通过配置的处理器进行拦截或交由线程 UncaughtExceptionHandler 处理；
 * 而根 async 则将其视为任务失败的合法结果，期望开发者通过 await() 显式读取并处理。因此，直接为根 async 配置 CoroutineExceptionHandler 是无意义且无效的。
 * 4. 级联取消的精准回溯：在观察级联取消的底层流转时，父 Job 向下分发给其他子协程的取消通知（JobCancellationException）会保留对触发源 Job 的引用。
 * 通过识别异常信息中记录的物理内存地址，开发者可以在复杂的嵌套结构中精准回溯导致整棵协程树崩溃的逻辑起点，这为调试多层并发任务提供了关键的追溯依据。
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught in Coroutine: $exception")
    }
    scope.async/*(handler)*/ {
        val deferred = async {
            delay(1000)
            throw RuntimeException("Error!")
        }
        launch(Job()) {
            try {
                // delay(2000)
                deferred.await()
            } catch (e: Exception) {
                println("Caught in await: $e")
            }
            try {
                delay(1000)
            } catch (e: Exception) {
                println("Caught in delay: $e")
            }
        }
        // delay(100)
        // cancel()
    }
    delay(10000)
}
