package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeoutException
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-05-03
 * @time 21:04
 *
 * 057.4.23-retry() 和 retryWhen() 操作符
 *
 * retry() 操作符是 Flow 提供的异常恢复与重试机制，其核心职责是在上游数据流生产过程中发生故障时，根据设定的条件（最大尝试次数与异常类型断言）
 * 决定是否重新触发数据的收集流程。在底层实现上，由于它深度依赖 catchImpl，因此严格遵循“异常可见性”原则：它仅能拦截发生在该操作符上游的纯业务层异常，
 * 绝不会捕获或干涉下游消费端（如末端 collect 闭包内）引发的崩溃，同时也会强制放行 CancellationException 以保障协程结构化并发树能够被系统正常取消。
 * 在运行机制层面，鉴于 Flow 的冷流本质，一旦重试条件满足，retry 会对上游重新发起完整的 collect 调用，这意味着上游数据源的整个生产闭包
 * （包括前置的网络请求、数据库查询或资源初始化等）都将从第一行代码开始被完全重新执行。在实际开发中使用该操作符时必须特别注意两点：
 * 第一，重试机制无法挽救由于下游渲染或消费逻辑错误引发的流中断；第二，为避免引发系统资源耗尽或服务端的雪崩效应，应避免使用默认的无条件无限重试，
 * 必须结合具体的异常类型（例如精准拦截 IOException 或 TimeoutException）并设定合理的重试次数上限来进行约束。
 *
 * notes: 4. Flow_catch 操作符与 catchImpl 源码剖析.md
 * notes: 5. 剖析 retry 与 retryWhen 操作符源码与底层流转机制.md
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flow {
        for (i in 1..5) {
            // 数据库读取
            // 网络请求
            if (i == 3) {
                throw RuntimeException("flow() error")
            } else {
                emit(i)
            }
        }
    }.map { it * 2 }.retry(1) {
        it is RuntimeException
    }
    scope.launch {
        try {
            flow1.collect {
                log("Data: $it")
            }
        } catch (e: TimeoutException) {
            log("Network error: $e")
        } catch (e: RuntimeException) {
            log("RuntimeException: $e")
        }
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-1] Data: 2
21 [DefaultDispatcher-worker-1] Data: 4
25 [DefaultDispatcher-worker-1] Data: 2
25 [DefaultDispatcher-worker-1] Data: 4
29 [DefaultDispatcher-worker-1] RuntimeException: java.lang.RuntimeException: flow() error
 */
