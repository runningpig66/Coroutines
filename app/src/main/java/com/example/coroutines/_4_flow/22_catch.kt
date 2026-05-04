package com.example.coroutines._4_flow

import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeoutException
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-05-01
 * @time 0:05
 *
 * 056.4.22-catch() 操作符
 *
 * catch(): Flow 异常可见性控制与状态恢复操作符
 * 该操作符的核心职责是严格控制流的异常可见性，仅拦截并处理发生于该操作符上游的运行时业务异常，并在捕获后提供状态恢复机制（如发射兜底数据或转换异常类型）。
 * 在底层异常流转中，它遵循严格的隔离与放行原则：首先，它绝不干涉下游消费端（即该操作符之后的流程或末端 collect 闭包）引发的异常，
 * 此类异常将直接穿透该操作符向上级作用域击穿；其次，它会自动放行 CancellationException，以保障协程结构化并发树能够被系统正常取消。
 * 一旦合法捕获到上游异常，原有的数据生产序列将宣告永久终止，执行权移交至 catch 闭包，闭包逻辑执行完毕后当前流即随之正常结束。
 *
 * notes: 4. Flow_catch 操作符与 catchImpl 源码剖析.md
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flow {
        for (i in 1..5) {
            // 数据库读取
            // 网络请求
            if (i == 3) {
                throw RuntimeException("flow() error")
            } else {
                emit(i)
            }
        }
    }.catch { // 不会捕获 CancellationException 和 下游异常，只捕获上游普通异常
        log("catch() 1: $it")
        emit(100)
        emit(200)
        emit(300)
        // throw RuntimeException("Exception from catch()")
    }/*.onEach {
        throw RuntimeException("Exception from onEach()")
    }.catch {
        log("catch() 2: $it")
    }*/
    scope.launch {
        try {
            flow1.collect {
                /*val contributors = unstableGitHub.contributors("square", "retrofit")
                log("Contributors: $contributors")*/
                log("Data: $it")
            }
        } catch (e: TimeoutException) {
            log("Network error: $e")
        }
    }
    delay(10000)
}
/* Output:
0 [DefaultDispatcher-worker-2] Data: 1
15 [DefaultDispatcher-worker-2] Data: 2
21 [DefaultDispatcher-worker-2] catch() 1: java.lang.RuntimeException: flow() error
21 [DefaultDispatcher-worker-2] Data: 100
22 [DefaultDispatcher-worker-2] Data: 200
22 [DefaultDispatcher-worker-2] Data: 300
 */
