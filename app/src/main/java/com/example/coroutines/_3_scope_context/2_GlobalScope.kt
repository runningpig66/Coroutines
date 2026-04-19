package com.example.coroutines._3_scope_context

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-14
 * @time 19:48
 *
 * GlobalScope 机制总结：
 * 1. 物理形态与内存分配：GlobalScope 在源码中被定义为 public object GlobalScope，是一个全局单例对象。
 * 它存在于 JVM 的静态区（方法区），在类加载时初始化，伴随整个应用程序进程的生命周期，永不被 GC 回收。
 * 2. 极简的上下文配置：它重写了 CoroutineScope 接口的 coroutineContext 属性，强制返回 EmptyCoroutineContext。
 * 这意味着它的上下文中默认不包含任何 Dispatcher（底层后备为 Dispatchers.Default），更关键的是，它不包含任何 Job。
 * 3. 剥离了统一取消机制 (No Root Job)：由于上下文中缺失 Job 元素，调用 GlobalScope.cancel() 会触发源码级的 IllegalStateException。
 * 它在物理架构上被故意剥夺了作为“统一管理节点”的能力。通过 GlobalScope 启动的多个协程之间毫无物理关联，无法被一键批量取消。
 * 4. 协程生命周期的彻底独立 (顶级协程)：通过 GlobalScope.launch 或 GlobalScope.async 启动的协程，在底层合并上下文时找不到父 Job。
 * 因此，创建出的协程实例（如 StandaloneCoroutine）其 parent 引用为 null，成为一棵独立的协程树（绝对的根协程）。
 * 其生命周期完全独立于调用方的组件（如 Activity / ViewModel），如果内部存在长耗时或死循环任务，
 * 开发者必须显式捕获并持久化持有该 launch 函数返回的 Job 句柄，通过手动调用 job.cancel() 来终止运行，
 * 否则该协程对象及其占据的物理线程资源，将一直存活至代码执行完毕或 App 进程消亡。
 * 5. 绝对根节点的异常流转机制：由于 parent 为 null，当协程因未捕获异常进入 finalizeFinishingState 阶段时，
 * 底层短路逻辑 cancelParent(cause) 必然直接返回 false，从而强制当前协程执行 handleJobException(cause)。
 * 这意味着它不仅是生命周期的孤岛，也是异常处理的终点。若为 launch 构建器，它会直接触发自身的 CoroutineExceptionHandler 或导致线程崩溃；
 * 若为 async 构建器（DeferredCoroutine），由于其沿用默认逻辑返回 false，异常不会被外部拦截。
 * 在底层的无锁状态机流转中，框架会将原 _state 变量上挂载的监听器链表（NodeList）提取至局部变量用于后续分发通知，
 * 并通过 CAS 操作将 _state 的最终值原子性地替换为 CompletedExceptionally(cause) 对象。
 * 异常由此被物理封存于 Deferred 的内存空间中，程序静默终结，仅在后续显式调用 await() 读取该状态时才会被同步抛出。
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun main() = runBlocking<Unit> {
    CoroutineScope(EmptyCoroutineContext).launch {

    }
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught in handler: $exception")
    }
    // GlobalScope.coroutineContext[Job] == null
    //- GlobalScope.cancel() // Error: Scope cannot be cancelled because it does not have a job
    val job = GlobalScope.launch(handler) {
        delay(1000)
        throw RuntimeException("Error!")
    }
    println("job parent : ${job.parent}")
    delay(10000)
}
