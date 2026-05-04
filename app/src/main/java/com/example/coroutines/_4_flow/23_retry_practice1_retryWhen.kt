package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-05-03
 * @time 21:04
 *
 * 057.4.23-retry() 和 retryWhen() 操作符
 *
 * retryWhen()：Flow 高阶异常重试与状态拦截操作符：该操作符是 Flow 重试机制的底层核心实现（retry 操作符即基于此构建）。
 * 相较于机械的定次重试，retryWhen 的核心优势在于其闭包提供了挂起（suspend）能力，并同时暴露了下游发射器（FlowCollector）、
 * 当前异常实例（cause）与已尝试次数（attempt）。这一设计赋予了开发者在重试执行前的绝对控制权，常用于实现复杂的重试工作流：
 * 例如结合 delay() 实现指数退避（Exponential Backoff）重试策略，或在真正发起重试前向下游 emit 临时状态（如 UI 提示信息）。
 * 在底层流转上，它严格复用 catchImpl 的异常可见性规则：仅拦截发生于上游的纯业务异常，绝不干涉下游消费端的崩溃，且无条件放行 CancellationException。
 * 由于 Flow 的冷流特性，一旦闭包断言返回 true，该操作符将重新触发对上游数据源的完整 collect 调用，令生产闭包从头开始重新执行；若返回 false，重试流程终止，异常将继续向下游作用域击穿。
 *
 * notes: 4. Flow_catch 操作符与 catchImpl 源码剖析.md
 * notes: 5. 剖析 retry 与 retryWhen 操作符源码与底层流转机制.md
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    // 模拟一个极其不稳定的上游数据源（比如一个网络接口）
    val remoteDataFlow = flow<String> {
        log("[上游] 准备发起网络请求...")
        delay(500)
        throw IOException("服务器连接超时")
        // emit("请求成功的数据: JSON") // 正常情况下应该发送数据，但目前走不到这里
    }
    scope.launch {
        remoteDataFlow.retryWhen { cause, attempt -> // attempt 从 0 开始递增
            if (cause is IOException && attempt < 3) {
                // 1. 向下游发射中间状态
                emit("[中间状态] 检测到网络异常，准备进行第 ${attempt + 1} 次重试...")
                // 2. 挂起控制流，实现指数退避延迟
                val delayTime = (1000L * (1 shl attempt.toInt())) // 1 shl attempt 相当于 2 的 attempt 次方
                log("[拦截器] 挂起当前协程，等待 ${delayTime}ms 后发起重试...\n")
                delay(delayTime)
                return@retryWhen true
            } else {
                log("[拦截器] 重试次数已耗尽，或遇到非网络错误，停止重试。")
                return@retryWhen false
            }
        }.catch {
            // 如果 retryWhen 返回了 false，异常就会掉到这里
            log("[下游catch操作符] 最终请求失败: ${it.message}")
        }.collect { data ->
            log("[下游接收到数据更新] -> $data")
        }
    }.join()
}
/* Output:
0 [DefaultDispatcher-worker-1] [上游] 准备发起网络请求...
561 [DefaultDispatcher-worker-1] [下游接收到数据更新] -> [中间状态] 检测到网络异常，准备进行第 1 次重试...
561 [DefaultDispatcher-worker-1] [拦截器] 挂起当前协程，等待 1000ms 后发起重试...

1569 [DefaultDispatcher-worker-1] [上游] 准备发起网络请求...
2078 [DefaultDispatcher-worker-1] [下游接收到数据更新] -> [中间状态] 检测到网络异常，准备进行第 2 次重试...
2078 [DefaultDispatcher-worker-1] [拦截器] 挂起当前协程，等待 2000ms 后发起重试...

4085 [DefaultDispatcher-worker-1] [上游] 准备发起网络请求...
4591 [DefaultDispatcher-worker-1] [下游接收到数据更新] -> [中间状态] 检测到网络异常，准备进行第 3 次重试...
4591 [DefaultDispatcher-worker-1] [拦截器] 挂起当前协程，等待 4000ms 后发起重试...

8599 [DefaultDispatcher-worker-1] [上游] 准备发起网络请求...
9106 [DefaultDispatcher-worker-1] [拦截器] 重试次数已耗尽，或遇到非网络错误，停止重试。
9107 [DefaultDispatcher-worker-1] [下游catch操作符] 最终请求失败: 服务器连接超时
 */
