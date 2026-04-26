package com.example.coroutines._4_flow2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * @author runningpig66
 * @date 2026-04-21
 * @time 14:32
 */
// 1. 定义消息协议（Intent/Action）：限制外部对状态的操作类型
sealed class CounterIntent
object IncCounter : CounterIntent()
object DecCounter : CounterIntent()

// 2. 状态容器（类似于 MVI 架构中的 ViewModel）：封装状态与意图处理逻辑
class CounterManager {
    // 内部私有的可变状态流，初始值为 0
    private val _state = MutableStateFlow(0)

    // 对外暴露的只读状态流，确保外部无法绕过 process 直接修改状态
    val state = _state.asStateFlow()

    // 统一的状态变更入口：接收并处理外部发来的意图
    fun process(intent: CounterIntent) {
        when (intent) {
            is IncCounter -> {
                // update 函数底层采用无锁的 CAS（Compare-And-Swap）加自旋机制
                // 在高并发场景下能确保状态更新的原子性与绝对线程安全，避免数据丢失
                _state.update { currentState -> currentState + 1 }
            }

            is DecCounter -> {
                _state.update { currentState -> currentState - 1 }
            }
        }
    }
}

fun main() = runBlocking<Unit> {
    val manager = CounterManager()

    // 模拟高并发环境：启动 100 个独立的协程，每个协程执行 1000 次递增指令
    measureTimeMillis {
        val jobs = List(100) {
            launch(Dispatchers.Default) {
                repeat(1000) {
                    manager.process(IncCounter)
                }
            }
        }
        jobs.joinAll() // 等待所有并发任务执行完毕
    }.also { println("并发执行耗时: $it ms") }
    // 验证并发安全性：在无锁机制下，最终结果应精确无误
    println("最终状态结果: ${manager.state.value}")
}
/* Output:
并发执行耗时: 45 ms
最终状态结果: 100000
 */
