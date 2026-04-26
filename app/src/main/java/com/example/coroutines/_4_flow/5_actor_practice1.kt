package com.example.coroutines._4_flow1

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * @author runningpig66
 * @date 2026-04-21
 * @time 14:32
 */
// 1. 定义消息协议：外部只能发送这两种指令，绝对无法越权
sealed class CounterMsg
object IncCounter : CounterMsg() // 递增指令，不需要返回值
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // 获取结果指令，携带一个回调凭证

@OptIn(ObsoleteCoroutinesApi::class)
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    // 2. 状态绝对封闭：counter 变量被幽禁在此协程内部，外部绝对不可见
    var counter = 0
    // 3. 串行处理：底层是一个 while(isActive) 的接收循环
    for (msg in channel) {
        when (msg) {
            is IncCounter -> counter++ // 单点写入，绝对不会发生并发冲突
            is GetCounter -> msg.response.complete(counter) // 将结果回传给外部
        }
    }
}

fun main() = runBlocking<Unit> {
    val counter = counterActor()
    // 模拟极端并发环境：启动 100 个独立协程，每个协程发送 1000 次递增指令
    measureTimeMillis {
        val jobs = List(100) {
            launch(Dispatchers.Default) {
                repeat(1000) {
                    counter.send(IncCounter)
                }
            }
        }
        jobs.joinAll() // 等待所有高并发任务完成
    }.also { println("Cost: $it ms") }

    // 获取最终结果
    val response = CompletableDeferred<Int>()
    counter.send(GetCounter(response))
    println("Counter = ${response.await()}") // 输出 100000，没有使用任何锁
    counter.close()
}
/* Output:
Cost: 332 ms
Counter = 100000
 */
