package com.example.coroutines._3_scope_context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-18
 * @time 21:25
 *
 * withContext 挂起函数机制：
 * 1. 动态调度机制与执行路径 (Context Switching & Dispatching)：withContext 通过 newCoroutineContext
 * 合并外部上下文与传入的 context，并在源码内部基于 ContinuationInterceptor（调度器）的对比结果，划分为三条物理执行路径：
 * - Fast-Path #1（上下文实例一致）：若合并后的 newContext 与 oldContext 实例完全相同 (===)，
 * 底层实例化 ScopeCoroutine，并通过 startUndispatchedOrReturn 在当前线程调用栈同步执行闭包，无任何切换开销。
 * - Fast-Path #2（调度器一致，其他元素变更）：若上下文不同，但调度器未发生变更，底层实例化 UndispatchedCoroutine。
 * 通过 withCoroutineContext 更新当前线程的局部上下文变量后，依然在当前调用栈同步执行。
 * - Slow-Path（调度器变更）：若调度器发生改变，底层实例化 DispatchedCoroutine。当前协程被挂起，
 * 闭包代码被分发（Dispatch）至目标调度器的线程池中执行；执行完毕后，状态机触发恢复（Resume）。
 * 2. 异常传播与结构化同步边界：withContext 具备完整的结构化并发语义（行为等同于 coroutineScope）。当闭包内部子协程发生未捕获异常时，
 * 其底层节点会向下取消所有活动的子协程，并阻断异常沿 Job 树继续向父级异步传播。在确保所有子任务终止后，底层机制会拦截该异常，
 * 并通过 resumeWithException 将其在外部协程的当前物理调用栈中重新抛出。因此，外部调用方可以通过常规的 try-catch 结构捕获其内部的异步异常。
 * 3. 结构化约束与 Job 传递规范：在上下文合并规则中，右侧传入的 context 参数具有高优先级，会覆盖当前上下文中的同类型元素。
 * 开发规范要求避免向 withContext 显式传入 Job 实例。由于 withContext 的核心作用是建立局部的协程树节点，若传入显式的 Job，
 * 该操作会覆盖框架内部自动生成的层级链接，破坏正常的父子协程生命周期绑定，进而导致结构化并发的取消机制失效。
 * 4. 与 coroutineScope 的底层架构：coroutineScope 语义固定，不接受上下文参数合并，相当于固定不接收参数的 withContext。
 * 若显式调用 withContext(EmptyCoroutineContext) 或 withContext(coroutineContext)，二者在底层的物理执行路径（Fast-Path #1）完全等价，均生成 ScopeCoroutine。
 * coroutineScope 用于就地划分作用域边界并等待子任务，无切换调度器的能力；withContext 语义侧重于环境切换，其挂起/恢复机制将多线程异步回调在宏观代码形态上扁平化为了同步阻塞序列。
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    scope.launch {
        withContext(coroutineContext) {

        }
        coroutineScope {

        }
    }
    val name = try {
        withContext(EmptyCoroutineContext) {
            val deferred1 = async {
                delay(500)
                "running"
            }
            val deferred2 = async {
                delay(2000)
                // "pig"
                throw RuntimeException("Error!")
            }
            deferred1.await() + deferred2.await()
        }
    } catch (e: Exception) {
        e.message
    }
    println("Full name: $name")
    delay(10000)
}
