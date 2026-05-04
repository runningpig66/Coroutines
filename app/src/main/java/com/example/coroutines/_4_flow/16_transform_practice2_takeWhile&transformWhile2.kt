package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author runningpig66
 * @date 2026-04-28
 * @time 23:45
 *
 * 050.4.16-transform() 系列操作符
 *
 * 验证：无限流环境下的生命周期截断与挂起泄漏风险分析
 * 模拟真实工程中长连接、传感器常驻监听等“无限流”场景，对比研究 [takeWhile] 与 [transformWhile] 在触发资源释放行为上的底层物理差异。
 * 1. takeWhile 的被动触发与挂起泄漏缺陷：由于 [takeWhile] 必须依赖接收到下一个“不满足判定条件”的越界元素，才能触发内部的逻辑熔断。
 * 在上游流处于永久挂起（如等待外部网络信号）且不再发射新元素的极端场景下，该操作符会导致调用方协程陷入无效的挂起等待状态，无法自主结束。
 * 此现象即为“逻辑性挂起泄漏”，必须依赖外部 Job 的显式取消才能强制回收资源。
 * 2. transformWhile 的主动熔断与生命周期即时控制：[transformWhile] 具备基于当前处理元素的即时决策能力。
 * 在完成临界元素的发射后，能够立即通过返回值触发内部异常回溯（AbortFlowException），从而主动截断上游的执行闭包。
 * 即使上游后续逻辑包含永久挂起操作，该机制也能确保在业务逻辑达标的瞬时精准回收协程资源，无需依赖外部干预，实现了更高维度的内存安全。
 */
fun main() = runBlocking {
    val infiniteNetworkFlow = flow {
        emit(20)
        emit(50)
        emit(100)
        // 挂起当前协程直至被显式取消。此处用于精确模拟网络长连接或传感器常驻监听器的非阻塞永久等待状态。
        // 该函数专门用于响应协作式取消信号，一旦所属作用域被取消，将立即抛出 CancellationException。
        awaitCancellation()
    }

    val job1 = launch {
        try {
            infiniteNetworkFlow
                .takeWhile { it <= 100 }
                .collect { log("takeWhile received: $it%") }
        } finally {
            log("takeWhile 收集结束，资源释放！")
        }
    }
    // 设置观察期窗口以验证协程是否陷入挂起泄漏状态，随后通过显式调用 cancel 强制终止泄漏的 Job 以回收资源。
    delay(10000)
    job1.cancelAndJoin()

    val job2 = launch {
        try {
            infiniteNetworkFlow
                .transformWhile { emit(it); it < 100 }
                .collect { log("transformWhile received: $it%") }
        } finally {
            log("transformWhile 收集结束，资源释放！")
        }
    }
    job2.join()
}
/* Output:
0 [main] takeWhile received: 20%
13 [main] takeWhile received: 50%
14 [main] takeWhile received: 100%
9969 [main] takeWhile 收集结束，资源释放！ // 注：10秒后手动 cancel 结束
9975 [main] transformWhile received: 20%
9975 [main] transformWhile received: 50%
9975 [main] transformWhile received: 100%
9976 [main] transformWhile 收集结束，资源释放！
 */
