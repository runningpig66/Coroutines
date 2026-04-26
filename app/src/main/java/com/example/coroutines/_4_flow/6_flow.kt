package com.example.coroutines._4_flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-21
 * @time 15:21
 *
 * 040.4.6-Flow 的功能定位
 *
 * notes: 6_flow_practice1.kt
 * notes: 1. Kotlin Flow 执行机制与上下文一致性（Flow Invariant）分析笔记.md
 * notes: 2. Flow 异步环境约束与上下文保留机制分析笔记.md
 */
fun main() = runBlocking<Unit> {
    val list = buildList {
//    while (true) {
        add(getData())
//    }
    }
    for (num in list) {
        println("List item: $num")
    }
    val nums = sequence {
        while (true) {
            yield(1)
        }
    }.map { "number $it" }
    for (num in nums) {
        println(num)
    }

    val numsFlow = flow {
        while (true) {
            emit(getData())
        }
    }.map { "number $it" }
    val scope = CoroutineScope(EmptyCoroutineContext)
    scope.launch {
        numsFlow.collect {
            println(it)
        }
    }
    delay(10000)
}

suspend fun getData(): Int {
    return 1
}
