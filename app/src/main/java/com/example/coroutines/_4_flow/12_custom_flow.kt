package com.example.coroutines._4_flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-26
 * @time 16:10
 *
 * 046.4.12-自定义 Flow 操作符
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf(1, 2, 3)
    scope.launch {
        flow1.customOperator().collect { println("1: $it") }
        flow1.customOperator().collect(object : FlowCollector<Int> {
            override suspend fun emit(value: Int) {
                println("1.1: $value")
            }
        })
        flow1.double().collect { println("2: $it") }
    }
    delay(10000)
}

/* 自定义中间操作符的基础结构模板。该扩展函数在系统内部构建了一个充当数据拦截与转发代理的中间流节点。
其完整展现了响应式冷流（Cold Stream）“由下游消费者反向驱动上游生产者”的执行拓扑与挂起时序。*/
fun <T> Flow<T>.customOperator(): Flow<T> = flow { // this: FlowCollector<T> 下游的收集箱
    /* 阶段 1：获取下游分发凭证
    当且仅当下游终端操作符被调用时（如 main 函数中的 collect），当前 flow 构建器的闭包才会被唤醒执行。
    此时，当前代码块的隐式接收者（this）是系统动态注入的 FlowCollector<T> 实例。
    该实例在物理层面直接指向下一级数据处理节点，充当当前操作符向外输出数据的唯一信道。*/

    /* 阶段 2：向上游源头建立订阅
    this@customOperator 显式指代当前扩展函数所依附的源数据流（即上游生产者）。
    对其调用 collect 方法，等效于在此中间节点的内部，向上游源头注册并挂载了一个局部的监听回调（匿名内部收集器）。*/
    this@customOperator.collect { // 为上游定义收集箱

        /* 阶段 3：数据的透明透传与接力
        上游状态机每处理完毕并下发一个合法元素，即会同步触发该闭包。
        此处调用外层作用域中持有的下游收集器的 emit 方法，将原始数据对象精确推送至外部下一级链路。*/
        this.emit(it)
    }
}

fun Flow<Int>.double(): Flow<Int> = channelFlow {
    collect {
        send(it * 2)
    }
}
