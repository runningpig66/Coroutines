package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author runningpig66
 * @date 2026-04-14
 * @time 14:34
 *
 * [RootJob (SupervisorJob)]
 *     │
 *     └── [C1 的 Job (StandaloneCoroutine)]
 *             │
 *             └── [显式注入的 SupervisorJob (纯管理节点, 阻断 C3 异常向 C1 传播的防火墙)]
 *                     │
 *                     └── [C2 的 Job (StandaloneCoroutine)]
 *                             │
 *                             └── [C3 的 Job (StandaloneCoroutine, 抛出 Error!)]
 *
 * SupervisorJob 机制总结：
 * 1. 异常隔离的底层实现（防火墙机制）：在常规的结构化并发中，子协程的未捕获异常会无条件导致父节点进入 Cancelling 状态，进而引发同级和下级的级联取消。
 * SupervisorJob 的核心设计在于其内部重写了 childCancelled(cause: Throwable) 方法并强制返回 false。这意味着当子节点向上汇报异常时，
 * SupervisorJob 拒绝接管该异常，且不会改变自身的 Active 状态。这种状态的冻结在物理链路上彻底切断了异常的向上传播和向下取消。
 * 2. “有效根协程”的动态生成与 Handler 触发条件：协程异常处理严格遵循“向上委托”原则，子协程自身的 CoroutineExceptionHandler 默认会被忽略。
 * 但当协程的直接父节点是 SupervisorJob 时，由于父节点拒绝接管异常（返回 false），该抛出异常的子协程在物理层面上被迫降级为“有效根协程”（Effective Root）。
 * 此时，该协程必须就地处理未捕获异常，因此它自身 Context 中携带的 CoroutineExceptionHandler 才得以被触发执行。
 * 3. 上下文合并规则与 supervisorScope 映射：当通过 launch(SupervisorJob() + handler) 启动协程时，底层的上下文合并机制会对传入的元素进行剥离。
 * SupervisorJob 被提取并指派为新协程的父节点（作为不干预异常的管理节点），而 handler 则被合并入新协程自身的上下文中（负责执行最终的异常拦截）。
 * 这种机制使得挂载在同一个 SupervisorJob 下的多个“兄弟协程”可以互不干扰地独立运行，并精确触发各自专属的异常处理器。
 * 官方提供的 supervisorScope 挂起函数正是基于这一底层机制封装的标准 API，它为实现局部异常隔离和复杂并发业务（如多模块独立加载）提供了可靠的架构支持。
 *
 * Ⅰ. 异常隔离的底层物理路由：在 JobSupport 的状态机中，子协程生命周期终结时会触发 finalizeFinishingState。
 * 异常处理遵循短路或公式：handled = cancelParent(cause) || handleJobException(cause)
 * SupervisorJob 的核心架构设计在于其重写了 childCancelled() 方法并强制返回 false。这意味着当子节点向上汇报异常时，
 * 前置条件 cancelParent 必定失败，SupervisorJob 拒绝接管异常且自身状态保持 Active，从而在物理链路上彻底切断了异常向父级蔓延与向下级连坐的通道。
 * Ⅱ. “有效根协程”的动态确立与 CEH 触发：基于上述短路逻辑，当父节点（SupervisorJob）拒绝处理异常时，执行流被迫流转至后半段 handleJobException()。
 * 此时，该抛出异常的子协程在逻辑上被迫降级为“有效根协程”，它必须就地自行处理该异常。
 * 只有在这种孤立状态下，该协程 Context 中携带的 CoroutineExceptionHandler (CEH) 才具备执行权限并被触发。
 * Ⅲ. 构建器在异常兜底时的行为分歧：当协程被迫作为“有效根协程”自行处理异常时，不同的构建器在 handleJobException 的重写逻辑上存在根本差异：
 * (A) Launch (StandaloneCoroutine)：重写了该方法。它会主动寻址并触发 CEH；若无 CEH，则交由线程的 UncaughtExceptionHandler 打印堆栈并返回 true，标志异常已处理。
 * (B) Async (DeferredCoroutine)：未重写该方法，沿用 JobImpl 默认实现返回 false。异常未被外部拦截，
 * 而是作为 CompletedExceptionally(cause) 被静默封存于底层的状态机 _state 中。程序不会崩溃，异常仅在后续显式调用 await() 时通过同步调用栈被重新抛出。
 * Ⅳ. 异步传播与同步抛出的安全边界：SupervisorJob 及其衍生 API（如 supervisorScope）仅能隔离基于 Job 树状结构的“异步异常汇报”。
 * 若开发者在 supervisorScope 的 Lambda 内部对异常完成的 Deferred 调用 await()，该异常将被转换为作用域内部的同步异常。
 * 若未被 try-catch 捕获，将直接导致父作用域自身的代码块崩溃，进而触发标准的向下级联取消。
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(SupervisorJob())
    val supervisorJob = SupervisorJob()
    val job = Job()
    scope.launch { // C1
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught in handler: $exception")
        }
        launch(SupervisorJob(this.coroutineContext.job) + handler) { // C2
            launch { // C3
                throw RuntimeException("Error!")
            }
        }

        val handler1 = CoroutineExceptionHandler { _, exception ->
            println("Caught in handler1: $exception")
        }
        val handler2 = CoroutineExceptionHandler { _, exception ->
            println("Caught in handler2: $exception")
        }
        launch(supervisorJob + handler1) {
            throw RuntimeException("Error!1")
        }
        launch(supervisorJob + handler2) {
            throw RuntimeException("Error!2")
        }
    }
    delay(1000)
    println("Parent Job cancelled: ${job.isCancelled}")
    delay(10000)
}
