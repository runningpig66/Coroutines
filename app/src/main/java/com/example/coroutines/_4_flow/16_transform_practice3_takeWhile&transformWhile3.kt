package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author runningpig66
 * @date 2026-04-29
 * @time 00:22
 *
 * 050.4.16-transform() 系列操作符
 *
 * 验证：状态机数据流的边界截断缺陷与解决方案
 * 演示在处理状态机流（如 Sealed Class 构成的状态序列）时，若业务需要在遇到特定临界状态时终止流，
 * [takeWhile] 与 [transformWhile] 在状态传递完整性上的底层差异。
 * 1. takeWhile 的边界状态丢失（结构性缺陷）：采用“评估后放行”机制。
 * 当放行条件设定为 it is UiState.Loading 时，一旦临界状态（Success）到达，条件即评估为 false 并触发拦截。
 * 结果：上游虽被终止，但触发终止的临界状态（Success）被物理丢弃，导致下游业务逻辑出现状态断层。
 * 2. transformWhile 的状态分离与精准闭合：采用“发射与评估分离”机制。
 * 通过 emit(it) 确保当前状态（即使是临界的 Success）必定被传递至下游。随后执行布尔评估，
 * 一旦结果为 false，立即抛出控制流异常终止上游。在保障下游状态完整性的同时，彻底阻断上游后续的冗余数据产生。
 */
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val msg: String) : UiState()
}

fun main() = runBlocking<Unit> {
    val networkStateFlow = flow {
        emit(UiState.Loading)
        delay(500)
        emit(UiState.Success("api data"))
        delay(100)
        // 模拟流在达到业务终止条件后，上游仍在持续产生的越界/冗余状态
        emit(UiState.Loading)
    }

    val job1 = launch {
        networkStateFlow
            .takeWhile { it is UiState.Loading }
            // 条件评估为 false 触发拦截，导致边界状态 (Success) 被物理丢弃，下游产生状态断层
            .collect { log("takeWhile received: ${it.javaClass.simpleName}") }
    }
    job1.join()

    val job2 = launch {
        networkStateFlow
            .transformWhile { emit(it); it is UiState.Loading }
            // 先发射后评估，确保边界状态 (Success) 完整传递，并在当前调用帧内即时熔断上游冗余流转
            .collect { log("transformWhile received: ${it.javaClass.simpleName}") }
    }
    job2.join()
}
/* Output:
0 [main] takeWhile received: Loading
536 [main] transformWhile received: Loading
1049 [main] transformWhile received: Success
 */
