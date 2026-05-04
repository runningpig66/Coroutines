package com.example.coroutines._4_flow1

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author runningpig66
 * @date 2026-04-27
 * @time 16:25
 *
 * 047.4.13-timeout、sample、debounce()
 */
@OptIn(FlowPreview::class)
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flow {
        log("[上游] 数据流启动")

        // 阶段 1：高频数据突发区 (验证防抖倒计时器的重置与旧数据静默覆盖机制)
        delay(100)
        log("[上游] 发射数据: 1 (被拦截，触发 500ms 防抖倒计时)")
        emit(1)

        delay(200) // 耗时 200ms < 500ms 阈值，导致数据 1 的倒计时被强行中断
        log("[上游] 发射数据: 2 (覆盖数据 1，重新触发 500ms 防抖倒计时)")
        emit(2)

        delay(200) // 耗时 200ms < 500ms 阈值，导致数据 2 的倒计时被强行中断
        log("[上游] 发射数据: 3 (覆盖数据 2，重新触发 500ms 防抖倒计时)")
        emit(3)

        // 阶段 2：静默等待区 (验证上游暂停发射时，倒计时结束的数据放行机制)
        log("[上游] 进入静默期，挂起等待 800ms...")
        delay(800)
        // 在这 800ms 的挂起期间，数据 3 的 500ms 倒计时将顺利走完，并被成功发射至下游！

        // 阶段 3：生命周期完结区 (验证流终止时，末端数据的强制刷新机制)
        log("[上游] 发射数据: 4 (流即将终止，不再等待倒计时，强制发射)")
        emit(4)

        delay(100) // 耗时 100ms < 500ms 阈值，但紧接着流生命周期结束
        log("[上游] 数据流执行完毕，发送完成信号 (Completion Signal)")
    }
    scope.launch {
        log("[下游] 开始收集 (防抖时间窗口：500ms)")
        /* fun <T> Flow<T>.debounce(timeout: Duration): Flow<T>
        基于动态重置时间窗口的防抖（去抖动）操作符：过滤掉那些在指定时间窗口 [timeout] 内被后续新数据迅速覆盖的瞬时中间态数据。
        仅当上游数据源经历一段大于等于 [timeout] 的“静默期”（即无新数据发射）后，操作符才会向下游发射当前缓冲区内缓存的最新元素。
        1. 动态重置倒计时：操作符内部维护一个动态的挂起倒计时器与一个单元素原子引用。
        每次上游发射新元素时，内部状态机会立即静默覆盖旧元素，并强制取消（Cancel）上一轮的倒计时协程，
        随后重新启动一个新的 [timeout] 倒计时。只有当倒计时完整走完且未被新元素打断时，数据才会被放行。
        2. 完结状态的强制刷新：这是 [debounce] 与 [sample] 操作符在底层最核心的物理差异。
        当上游流正常执行完毕（发送 Completion Signal）时，若防抖缓冲区内仍残留尚未达到倒计时阈值的最新数据，
        [debounce] 会立即中止等待，触发强制刷新（Flush），将该末端数据安全发射至下游，确保最终状态绝对不丢失。（注：[sample] 在流结束时会直接丢弃未到时间点的数据）。
        3. 架构级致命陷阱：无限饥饿：需极度警惕的高频并发风险。若上游生产者持续不断地以严格小于 [timeout] 的时间间隔高频发射数据，
        防抖倒计时将被无限次打断并重置。在物理表象上，这将导致下游消费者长时间（甚至永久）处于“饥饿”状态，接收不到任何数据流，直至上游进入静默期或流彻底结束。
        4. 架构适用场景边界：强制约束：专用于需屏蔽高频无意义操作、仅关注阶段性“稳定最终态”的业务场景。
        （实战用例：输入框高频打字的联想搜索防抖、UI 按钮连续快速点击的指令节流、滑动进度条结束时的位置提交）。*/
        flow1.debounce(500.milliseconds).collect {
            log("[下游] 成功采集防抖校验后数据：$it")
        }
        log("[下游] 收集正常结束")
    }.join()
}
/* Output:
0 [DefaultDispatcher-worker-1] [下游] 开始收集 (防抖时间窗口：500ms)
103 [DefaultDispatcher-worker-2] [上游] 数据流启动
208 [DefaultDispatcher-worker-1] [上游] 发射数据: 1 (被拦截，触发 500ms 防抖倒计时)
426 [DefaultDispatcher-worker-1] [上游] 发射数据: 2 (覆盖数据 1，重新触发 500ms 防抖倒计时)
629 [DefaultDispatcher-worker-2] [上游] 发射数据: 3 (覆盖数据 2，重新触发 500ms 防抖倒计时)
629 [DefaultDispatcher-worker-2] [上游] 进入静默期，挂起等待 800ms...
1144 [DefaultDispatcher-worker-1] [下游] 成功采集防抖校验后数据：3
1435 [DefaultDispatcher-worker-1] [上游] 发射数据: 4 (流即将终止，不再等待倒计时，强制发射)
1543 [DefaultDispatcher-worker-2] [上游] 数据流执行完毕，发送完成信号 (Completion Signal)
1554 [DefaultDispatcher-worker-2] [下游] 成功采集防抖校验后数据：4
1555 [DefaultDispatcher-worker-2] [下游] 收集正常结束
 */
