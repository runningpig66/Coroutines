package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import com.example.coroutines.common.unstableGitHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeoutException
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-29
 * @time 17:47
 *
 * 055.4.21-try_catch 和 Flow 的异常可见性
 *
 * Flow 的异常可见性（Exception Visibility）与执行链路保护机制:
 * 异常可见性是响应式数据流的底层架构规范，要求流的上游（生产者）不应跨层级拦截下游（消费者）抛出的异常。
 * 在工程实践中，这意味着在 [flow] 构建器或任何数据发射闭包内，避免使用 try-catch 包裹 [emit] 函数的调用。
 * 上游局部的 try-catch 仅适用于处理当前层级数据准备阶段的异常（如网络请求或数据库读取失败）；一旦调用 [emit]，异常的抛出与处理权应完整移交至下游。
 * 若上游在内部捕获了 [emit] 抛出的异常且未重新抛出，会导致下游的异常被静默拦截，外层的异常捕获逻辑将无法感知真实的运行错误。此外，这会引发内部状态机冲突：
 * 下游的底层收集器（SafeCollector）在抛出异常后会被标记为终止状态，若上游持续向其发送数据，底层机制将抛出 IllegalStateException 并导致程序异常退出。
 * 因此，合规的异常捕获应放置于数据流的终端（如包裹 collect 调用），或使用专用的 [catch] 操作符来处理整条链路的异常。
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flow {
        try {
            for (i in 1..5) {
                // 数据库读取
                // 网络请求
                emit(i)
            }
        } catch (e: Exception) {
            log("Error in flow(): $e")
            throw e
        }
        /*for (i in 1..5) {
            try {
                // 数据库读写
                // 网络请求
            } catch (e: Exception) {
                println("Error in flow(): $e")
            }
            emit(i)
        }*/
    }.map { throw NullPointerException() }
    /*.onEach { throw NullPointerException() }
    .transform<Int, Int> {
        val data = it * 2
        emit(data)
        emit(data)
    }*/
    // Exception Transparency
    scope.launch {
        try {
            flow1.collect { // 假设根据上游数据作为表单字段生成不同的网络请求
                val contributors = unstableGitHub.contributors("square", "retrofit")
                log("Contributors: $contributors")
            }
        } catch (e: TimeoutException) {
            log("Network error: $e")
        } catch (e: NullPointerException) {
            log("Null data: $e")
        }
    }
    delay(10000)
}

private fun fun1() {
    fun2()
}

private fun fun2() {
    fun3()
}

private fun fun3() {
    throw NullPointerException("User null")
}
