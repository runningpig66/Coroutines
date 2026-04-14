package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-11
 * @time 1:35
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    var childJob: Job? = null
    var childJob2: Job? = null
    val newParent = Job()
    val parentJob = scope.launch {
        // 0. NonCancellable 是 Kotlin 协程标准库提供的一个特殊的全局单例 Job，专为在已取消的上下文中执行挂起操作而设计。
        // 其内部强行重写了 Job 的生命周期属性。isActive 恒定返回 true，isCancelled 恒定返回 false，且 cancel() 方法被重写为空实现。
        // 这使其对任何取消信号具备绝对的免疫能力。当将其作为 context 传入协程构建器（如 launch 或 withContext）时，它会顶替原有的父 Job。
        // 为防止作为全局单例引发内存泄漏，NonCancellable 重写了底层的 attachChild 方法。它拒绝将任何子节点加入自身的 children 集合，
        // 并向子节点下发一个内部的空句柄（NonDisposableHandle）。这导致新建协程的 parent 属性获取结果为 null，在底层彻底切断了结构化并发的父子双向链接与事件广播机制。
        // 其符合规范的唯一用法是结合 withContext 进行状态逃逸。若用于 launch 构建游离的协程上下文，则严格违背结构化并发范式（Anti-pattern）。
        childJob = launch(NonCancellable) {
            println("Child started")
            delay(3000)
            println("Child stopped")
        }
        println("childJob parent: ${childJob.parent}")
        childJob2 = launch(newParent) {
            println("Child 2 started")
            writeInfo2()
            // 3. 非结构化旁路协程：触发与当前主业务解耦，且不应随主业务取消而中断的短生命周期任务（如数据埋点上传、关键日志记录）。
            // 向 launch 传入 NonCancellable 会在底层完全切断协程间的层级引用关系（父子双向均不持有对方引用）。
            // 因此，该新建协程不会因外层 parentJob 的取消而终止，而是会在后台独立调度直至执行完毕。
            // 注意：此用法打破了结构化并发的生命周期约束，仅建议用于执行极短且必须完成的旁路逻辑。若滥用于长耗时任务，极易引发内存泄漏与生命周期失控。
            launch(NonCancellable) {
                // Log
            }
            if (!isActive) {
                // 1. 安全收尾保护：在响应取消信号（!isActive 或 catch CancellationException）后的清理阶段。
                // 当协程处于 Cancelling 状态时，直接调用任何挂起函数（如 Room 数据库操作）都会立即抛出 CancellationException 导致清理工作非正常中断。
                // 通过 withContext(NonCancellable) 将执行流切换到一个 isActive 永远为 true 的安全上下文中，
                // 确保必要的挂起清理操作（如释放资源、关闭数据库连接）能够同步执行完毕，随后再手动抛出异常完成协作式取消。
                withContext(NonCancellable) {
                    // Write to database (Room suspend function)
                    delay(1000)
                }
                throw CancellationException()
            }
            try {
                delay(3000)
            } catch (e: CancellationException) {

                throw e
            }
            println("Child 2 stopped")
        }
        println("Parent started")
        delay(3000)
        println("Parent stopped")
    }
    delay(1500)
    NonCancellable.cancel()
    delay(10000)
}

// 2. 原子性业务挂起段：业务逻辑中存在不可分割的连贯执行流程（如复杂的数据拼装与持久化存储）。
// 通过注入 NonCancellable，在与 withContext 结合的作用域内构建一个免疫取消信号的执行区间。
// 即使外部在此期间下发了 cancel 信号，该作用域内部的所有挂起调用（如读写数据库、文件 I/O）都不会抛出 CancellationException。
// 这样可以避免编写复杂且易错的异常捕获与状态回滚逻辑（如 writeInfo1 所示），用短暂的延迟响应换取数据的绝对完整性。
suspend fun writeInfo2() = withContext(Dispatchers.IO + NonCancellable) {
    // write to file
    // read from database (Room suspend function)
    // write data to file
}

suspend fun writeInfo1() = withContext(Dispatchers.IO) {
    // write to file
    try {
        // read from database (Room suspend function)
    } catch (e: CancellationException) {
        // ... 撤销文件写入
        throw e
    }
    // write data to file
}

suspend fun uselessSuspendFun() {
    Thread.sleep(1000)
}
