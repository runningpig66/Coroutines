package com.example.coroutines._4_flow1

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * @author runningpig66
 * @date 2026-04-27
 * @time 16:25
 *
 * 047.4.13-timeout、sample、debounce()
 */
@OptIn(FlowPreview::class)
fun main() = runBlocking<Unit> {
    flow {
        emit(1)
        delay(100)
        emit(2)
        delay(100)
        emit(3)
        delay(1000)
        emit(4)
    }
        /* fun <T> Flow<T>.timeout(timeout: Duration): Flow<T>
        @param timeout Timeout duration. If non-positive, the flow is timed out immediately
        Returns a flow that will emit a [TimeoutCancellationException] if the upstream doesn't emit an item within the given time.
        Note that delaying on the downstream doesn't trigger the timeout.
        限制上游 Flow 元素发射时间间隔的超时操作符：监控上游 Flow 的相邻两次元素发射（或自收集开始至首次发射）的时间间隔。
        若上游在该指定的时间窗口 [timeout] 内未发射任何新元素，则立刻协作式取消上游流，并向下游终端抛出 [TimeoutCancellationException] 异常。
        1. 作用域隔离（仅限上游监控）：该机制仅监控“等待上游生产数据”的时间。
        下游（如 collect、onEach 等终端或中间操作符）处理该元素所引发的挂起或耗时，绝对不会计入超时时间计算中。
        2. 局部区间重置机制：[timeout] 设定的并非整个 Flow 生命周期的总最大时长。
        在上游每次成功发射（emit）元素后，底层的超时计时器均会被瞬间重置，重新开始下一个周期的倒计时。
        3. 异常流转与降级处理（Fallback）：抛出的 [TimeoutCancellationException] 隶属于 CancellationException 体系。
        在标准协程控制流中，它被视为正常的取消信号而非致命错误。实战中，必须紧随其后使用 [catch] 操作符对其进行拦截捕获，以实现超时后的默认值下发或状态恢复，避免外部作用域异常终止。
        4. 临界值调度陷阱（避免竞态条件）：因底层操作系统的线程调度精度误差（通常在 1~15ms 浮动）以及协程 Continuation 的恢复开销，
        严禁将 [timeout] 阈值设定为与上游理论耗时（如 delay）绝对相等的值。在生产环境中，必须为超时设定合理的冗余缓冲期（Buffer），以规避偶发的并发调度时序反转现象。*/
        .timeout(100.milliseconds)
        .catch { exception ->
            if (exception is TimeoutCancellationException) {
                // Catch the TimeoutCancellationException emitted above.
                // Emit desired item on timeout.
                emit(-1)
            } else {
                // Throw other exceptions.
                throw exception
            }
        }
        .onEach {
            delay(300) // This will not cause a timeout
            log("$it")
        }
        .collect()
    /* ================================ */
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow2 = flow {
        delay(500)
        emit(1) // 此时总耗时 500ms
        delay(800)
        emit(2) // 此时总耗时 1300ms
        delay(100) // 快速连发
        emit(3) // 此时总耗时 1400ms
        delay(1200)
        emit(4) // 此时总耗时 2600ms
    }
    scope.launch {
        try {
            log("开始收集")
            flow2.timeout(1.seconds).collect {
                log("成功接收：$it")
            }
            log("收集正常结束") // 如果抛异常，这句根本走不到
        } catch (e: TimeoutCancellationException) {
            log("捕获到超时异常：${e.message}")
        }
    }.join()
}
/* Output 1:
0 [main] 1
337 [main] 2
648 [main] 3
1069 [main] -1
 */
/* Output 2: (注释掉 delay(300))
0 [main] 1
96 [main] 2
216 [main] -1
 */
/* Output 3: (====第三段代码====)
1094 [DefaultDispatcher-worker-1] 开始收集
1612 [DefaultDispatcher-worker-2] 成功接收：1
2410 [DefaultDispatcher-worker-1] 成功接收：2
2518 [DefaultDispatcher-worker-2] 成功接收：3
3531 [DefaultDispatcher-worker-2] 捕获到超时异常：Timed out waiting for 1s
 */
