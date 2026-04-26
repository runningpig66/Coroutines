package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-21
 * @time 16:31
 *
 * 041.4.7-Flow 的工作原理和应用场景
 *
 * Flow作为冷数据流的核心特征在于其声明式延期执行机制。调用flow构建器仅在内存中封装数据生产的执行模板，不消耗计算资源亦不触发状态流转。
 * 下游对该流对象的单次收集操作，将触发执行模板在当前收集器协程上下文中的独立顺序运行，从而在物理隔离的执行环境中建立互不干扰的运算链路，
 * 从底层架构上规避共享状态引发的并发竞争。在系统架构层面，此机制实现了异步任务中数据生产源与消费端点的逻辑解耦。配合挂起函数的非阻塞特性，
 * 流管道内部可装载标准的中间拦截逻辑，依托操作符机制实现跨越异步时间周期的数据变换、状态过滤与异常降级，为构建高内聚的响应式系统提供底层通信原语。
 *
 * Channel: hot send() receive()
 * Flow: cold collect()
 */
fun main() = runBlocking<Unit> {
    val numsFlow = flow {
        emit(1)
        delay(100)
        emit(2)
    }
    val scope = CoroutineScope(EmptyCoroutineContext)
    scope.launch {
        // showWeather(weatherFlow)
        weatherFlow.collect {
            println("Weather: $it")
        }
        // log("done")
        /*while (true) {
            println("Weather: ${getWeather()}")
            delay(60000)
        }*/
        /*numsFlow.collect {
            log("A: $it")
        }*/
    }
    scope.launch {
        delay(50)
        numsFlow.collect {
            log("B: $it")
        }
    }
    delay(10000)
}

val weatherFlow = flow {
    while (true) {
        emit(getWeather())
        delay(60000)
    }
}

suspend fun showWeather(flow: Flow<String>) {
    flow.collect {
        println("Weather: $it")
    }
}

suspend fun getWeather() = withContext(Dispatchers.IO) {
    "Sunny"
}
