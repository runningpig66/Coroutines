package com.example.coroutines._4_flow1

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
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
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flow {
        log("[上游] 数据流启动")

        // 阶段 1：高频发射测试 (验证采样窗口内的数据覆盖机制)
        // 时间轴：0 ~ 1000ms 之间
        delay(200)
        log("[上游] 发射数据: 1 (将被后续数据覆盖)")
        emit(1)

        delay(300)
        log("[上游] 发射数据: 2 (将被后续数据覆盖)")
        emit(2)

        delay(300)
        log("[上游] 发射数据: 3 (当前周期最新数据，将被采集)")
        emit(3)
        // 此时总耗时 800ms。等待 1000ms 时的第一次固定频率采样。

        // 阶段 2：常规发射测试 (验证正常周期内的数据采集)
        // 时间轴：1000ms ~ 2000ms 之间
        delay(600) // 此时总耗时 1400ms
        log("[上游] 发射数据: 4 (当前周期最新数据，将被采集)")
        emit(4)
        // 此时总耗时 1400ms。等待 2000ms 时的第二次固定频率采样。

        // 阶段 3：边界终止测试 (验证流提前结束时的数据丢弃机制)
        // 时间轴：2000ms 之后
        delay(800) // 此时总耗时 2200ms
        log("[上游] 发射数据: 5 (流即将终止，无法进入下一采样周期，将被丢弃)")
        emit(5)

        delay(300) // 此时总耗时 2500ms
        log("[上游] 数据流执行完毕，发送完成信号 (Completion Signal)")
        // 流在此处执行完毕。此时未达到第三个采样点 (3000ms)。
    }
    scope.launch {
        println("[下游] 开始收集 (采样周期参数：1000ms)")
        /* fun <T> Flow<T>.sample(period: Duration): Flow<T>
        基于固定时间窗口的数据流采样操作符：以指定的固定时间周期 [period] 对上游数据流进行离散采样。
        在每个时钟周期到达（触发中断）时，若该周期内上游存在新发射的数据，则向下游转储并发射该周期内的最新数据记录。
        1. 独立时钟中断机制：内部采样定时器在终端操作符（如 collect）触发流启动时即刻挂载，并严格遵循 [period] 设定的绝对时间轴运转。
        该定时器的执行节拍完全独立，不受上游生产者的发射频率、网络延迟或下游消费者的挂起耗时影响。
        2. 单元素引用与静默覆盖：操作符内部仅维护一个容量为 1 的原子状态引用。
        在同一个采样周期内，上游高频突发（Burst）的多个元素会在该内存地址上发生持续的静默覆盖。
        仅有时间线上最后到达的那个元素会被保留并在采样点发射，周期内较早到达的中间态数据将因失去强引用而被系统直接垃圾回收。
        3. 生命周期绑定与末端丢弃：[sample] 内部协程作用域的生命周期严格从属于上游数据流。
        当上游代码块执行完毕发送完成信号（Completion Signal），或因外部异常触发层级取消（Cancellation）时，内部定时器将立即被强制销毁。
        此时，若原子引用中存在尚未熬到下一个采样点的新数据，该数据将被执行末端丢弃，系统不会进行任何形式的补偿发射。
        4. 架构适用场景边界：强制约束：仅适用于数据具备“状态幂等性”且允许中间过程丢失的高频数据降级场景
        （例如：传感器坐标的高频采样、UI 进度条的高频帧同步、非关键性的状态心跳包）。
        严禁将此操作符应用于对完整性有硬性要求的事件流（如：支付流水指令、全量日志埋点上报），否则将导致不可逆的数据丢失异常。*/
        flow1.sample(1.seconds).collect {
            println("[下游] 成功采集数据：$it")
        }
        println("[下游] 收集正常结束")
    }.join()
}
/* Output:
[下游] 开始收集 (采样周期参数：1000ms)
0 [DefaultDispatcher-worker-1] [上游] 数据流启动
237 [DefaultDispatcher-worker-2] [上游] 发射数据: 1 (将被后续数据覆盖)
561 [DefaultDispatcher-worker-1] [上游] 发射数据: 2 (将被后续数据覆盖)
872 [DefaultDispatcher-worker-1] [上游] 发射数据: 3 (当前周期最新数据，将被采集)
[下游] 成功采集数据：3
1476 [DefaultDispatcher-worker-2] [上游] 发射数据: 4 (当前周期最新数据，将被采集)
[下游] 成功采集数据：4
2285 [DefaultDispatcher-worker-1] [上游] 发射数据: 5 (流即将终止，无法进入下一采样周期，将被丢弃)
2594 [DefaultDispatcher-worker-2] [上游] 数据流执行完毕，发送完成信号 (Completion Signal)
[下游] 成功采集数据：5
[下游] 收集正常结束
 */
