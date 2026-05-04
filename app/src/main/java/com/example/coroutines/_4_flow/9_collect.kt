package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-22
 * @time 16:20
 *
 * 043.4.9-Flow 的收集
 *
 * notes: 2. Flow 异步环境约束与上下文保留机制分析笔记.md
 */
fun main() = runBlocking<Unit> {
    val flow = flow {
        /*launch(Dispatchers.IO) {
            delay(2000)
            emit(2)
        }*/
        delay(1000)
        emit(1)
    }
    val scope = CoroutineScope(EmptyCoroutineContext)

    flow.onEach {
        println("flow: $it")
    }.launchIn(scope)
    scope.launch {
        flow.collect {
            println("flow: $it")
        }
    }

    scope.launch(Dispatchers.Default) {
        flow.collect {
            println("flow: $it")
        }
        flow.collectIndexed { index, value ->
            log("1: $index - $value")
        }
        flow.collectLatest { } // TODO
    }
    delay(10000)
}
