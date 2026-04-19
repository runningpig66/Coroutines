package com.example.coroutines._3_scope_context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-15
 * @time 18:17
 *
 * coroutineScope 挂起函数机制：
 * 1. 结构化并发边界与生命周期屏障：coroutineScope 的核心职责是在当前执行环境中构建一个局部、安全的结构化并发作用域。
 * 当外部协程调用此挂起函数时，外部协程的执行流会被挂起，直到该作用域闭包内启动的所有子协程全部执行完毕（或被取消）。
 * 这种机制将微观上的异步并发任务，在宏观上封装为了一个同步的阻塞点，确保不会发生后台任务泄漏。
 * 2. 物理调度与未分发执行机制 (UndispatchedCoroutine)：在性能与调度层面，coroutineScope 底层通过 ScopeCoroutine（一种未分发协程）实现。
 * 与常规协程构建器将任务提交到调度器队列等待分配不同，coroutineScope 会在当前物理线程的调用栈上直接、同步地执行闭包内的代码，
 * 直到遇到内部的第一个挂起点（如网络请求或 delay）时，才会让出物理线程的控制权。这种免排队的机制避免了不必要的线程上下文切换开销，使其成为一种极轻量级的任务分组工具。
 * 3. 上下文继承策略：该函数严格继承外部调用方（Continuation）的完整 CoroutineContext（包含调度器等所有元素），并在此基础上生成一个全新的子 Job。
 * 由于不接受任何外部 Context 参数，它不具备切换线程调度器或修改协程配置的能力，仅用于基于现有上下文建立局部的任务树节点。
 * 4. 异常隔离与 try-catch 可捕获原理：在异常传播链路上，coroutineScope 表现为一种可阻断的异常转换边界。当其内部任意子协程抛出异常时，
 * 触发的取消机制会终止作用域内所有其余活动的子协程。在等待所有子节点清理完毕后，该作用域会拦截异常沿着 Job 树向顶层抛出的默认行为。
 * 作为替代，它通过调用外部协程状态机的 resumeWithException 方法，在外部协程被重新唤醒的挂起点位置，原封不动地将异常抛出。
 * 正是因为异常在当前物理执行流中被实质性地同步抛出，开发者在 coroutineScope 外层编写的常规 try-catch 代码块才能准确捕获这些跨线程子任务产生的异常，解决了纯异步回调带来的异常逃逸问题。
 *
 * supervisorScope 挂起函数机制：
 * 1. 单向传播的异常隔离边界：supervisorScope 的核心设计目的是在结构化并发树中构建异常隔离层。
 * 其底层由 SupervisorCoroutine 实例支撑，重写了异常向上传播的默认逻辑。当内部某个子协程因未捕获异常而终止时，
 * 该作用域会忽略此取消请求（底层 childCancelled 返回 false）。因此，单一子协程的失败不会导致作用域自身的取消，
 * 也不会触发对其他同级兄弟协程的级联取消。这种机制适用于处理多个相互独立、互不干扰的并发任务。
 * 2. 严格的子协程异常处理责任：由于作用域切断了异常的向上传导链且不提供统一的异常捕获机制，异常处理的责任被完全下放至每一个子协程。
 * 开发者必须为内部的每一个 launch 构建器显式配置 CoroutineExceptionHandler，或在代码块内部使用 try-catch。
 * 若未做处理，launch 产生的异常将逃逸至当前物理线程的 UncaughtExceptionHandler，进而导致进程终止。
 * 此外，由于此类异常未通过挂起恢复机制抛出，supervisorScope 外部的 try-catch 无法对其进行捕获。
 * 3. 闭包内部逻辑的异常陷阱：该隔离机制仅保护作用域免受子协程内部异常的影响。如果在 supervisorScope 的闭包主逻辑中直接触发异常
 * （例如：未加 try-catch 保护直接调用已失败的 deferred.await()），在物理层面上等同于作用域自身抛出未捕获异常。
 * 在此情形下，隔离机制不生效，supervisorScope 会终止运行，取消其内部所有活动的子任务，随后唤醒外部协程并抛出该异常。
 * 4. 物理调度与生命周期等待语义：在线程调度与挂起表现上，与 coroutineScope 保持一致。它采用未分发（Undispatched）策略，
 * 在当前物理线程的调用栈上同步执行闭包代码，直至遇到首个挂起点才释放线程控制权。同时，它严格遵守结构化并发的生命周期语义：
 * 外部协程执行至此将被挂起，必须等待内部所有受监督的子协程全部进入完成状态后，才会恢复外部协程的执行并返回结果。
 *
 * 协程结构化并发与异常处理机制：
 * 1. 挂起函数异常抛出机制 (Continuation & resumeWithException)：跨线程异步任务的异常无法通过常规线程执行栈捕获。
 * try-catch 能够捕获协程异常的底层依据在于：挂起函数作为状态机（Continuation）的挂起点，当接收到子任务的异常信号时，
 * 会在恢复外部协程执行上下文时，通过 resumeWithException 接口在当前调用栈中重抛该异常。
 * 结论：try-catch 仅适用于拦截挂起函数（如 coroutineScope, withContext, await）在恢复执行时抛出的同步异常；
 * 无法拦截协程构建器（如 launch）因任务失败而直接分发给所在线程未捕获异常处理器（UncaughtExceptionHandler）的异步异常。
 * 2. 异常向上传播的阻断与同步化 (coroutineScope / withContext)：底层基于 ScopeCoroutine 实现，其重写了 isScopedCoroutine = true 属性。
 * 当其内部发生未捕获异常时，不仅会向下触发对所有活动兄弟协程的取消（Cancellation），同时会在向上执行 cancelParent 时被底层逻辑拦截，
 * 阻断异常沿 Job 树继续向父级节点传播。原本会导致整棵 Job 树级联取消的异步异常，被收敛并转换为挂起函数层面的同步抛出。
 * 这使得调用方可通过标准的 try-catch 结构对该作用域进行局部异常捕获。
 * 3. 异常单向隔离与责任下放 (supervisorScope)：底层基于 SupervisorCoroutine 实现。其重写了 childCancelled 方法并强制返回 false。
 * 当接收到子协程的异常汇报时，该作用域拒绝接管异常，且不提供异常阻断与转换为同步抛出的功能。因此，单一子协程的失败不会导致作用域自身的级联取消。
 * 在此机制下，异常处理责任被完全下放至引发异常的子协程。该子协程被迫执行 handleJobException 兜底逻辑。若该子协程由 launch 构建，
 * 未处理的异常将直接交由线程池处理，可能导致进程异常终止。开发者必须在 supervisorScope 内部的各协程中独立处理异常（通过 try-catch 或 CEH）。
 * 4. 协程构建器行为规范与“有效根协程”判定 (launch vs async)：构建器对异常的处理行为，取决于其是否在 Job 树中处于“有效根协程”的地位。
 * 有效根协程的判定：当协程执行 cancelParent(cause) 向上汇报异常且得到 false 返回值时，该协程即降级为有效根协程。
 * 触发此条件的父节点环境包括：(1) parent 为 null（如 GlobalScope）；(2) 父节点为 SupervisorJob；
 * (3) 父节点为不干预异常的纯管理节点 JobImpl（如 CoroutineScope(EmptyCoroutineContext) 启动的协程）。行为分歧：
 * 作为普通子协程时：无论 launch 还是 async，遇到未捕获异常均立刻向上传播，触发父节点的级联取消。同步等待方法（如 await）不改变其异常分发的异步传播路径。
 * 作为有效根协程时：(1) launch (StandaloneCoroutine)：重写了 handleJobException。会主动寻址并触发上下文中的 CoroutineExceptionHandler；
 * 若无配置，则交由线程的 UncaughtExceptionHandler 处理。(2) async (DeferredCoroutine)：未重写该方法（沿用默认实现返回 false）。
 * 异常作为 CompletedExceptionally 状态被静默封存于底层的 _state 变量中，不会触发线程的未捕获异常处理机制。仅在显式调用 await() 时，才会在调用点所在的物理执行流中同步抛出该异常。
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    scope.launch {
        supervisorScope {

        }
        val name = try {
            coroutineScope {
                val deferred1 = async { "running" }
                // val deferred2 = async { "pig" }
                val deferred2: Deferred<String> = async {
                    throw RuntimeException("Error!")
                }
                deferred1.await() + deferred2.await()
            }
        } catch (e: Exception) {
            e.message
        }
        println("Full name: $name")
        val startTime1 = System.currentTimeMillis()
        coroutineScope {
            launch {
                delay(2000)
            }
            delay(1000)
            println("Duration within coroutineScope: ${System.currentTimeMillis() - startTime1}")
        }
        println("Duration of coroutineScope: ${System.currentTimeMillis() - startTime1}")
        val startTime2 = System.currentTimeMillis()
        launch {
            delay(1000)
            println("Duration within launch: ${System.currentTimeMillis() - startTime2}")
        }
        println("Duration of launch: ${System.currentTimeMillis() - startTime2}")
    }
    delay(10000)
}

private suspend fun someFun() =
    coroutineScope {
        launch {

        }
    }
