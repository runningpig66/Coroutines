package com.example.coroutines._4_flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-29
 * @time 14:35
 *
 * 052.4.18-reduce()、fold() 系列操作符
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf(1, 2, 3, 4, 5)
    val list = listOf(1, 2, 3, 4, 5)
    // fun <S, T : S> Iterable<T>.reduce(operation: (acc: S, T) -> S): S
    list.reduce { acc, value -> acc + value }.let { println("List reduce: $it") }
    // fun <S, T : S> Iterable<T>.runningReduce(operation: (acc: S, T) -> S): List<S>
    list.runningReduce { acc, value -> acc + value }.let { println("List runningReduce: $it") }
    // fun <T, R> Iterable<T>.fold(initial: R, operation: (acc: R, T) -> R): R
    list.fold(10) { acc, value -> acc + value }.let { println("List fold1: $it") }
    list.fold("Hello") { acc, value -> "$acc - $value" }.let { println("List fold2: $it") }
    // fun <T, R> Iterable<T>.runningFold(initial: R, operation: (acc: R, T) -> R): List<R>
    list.runningFold("Hi") { acc, value -> "$acc - $value" }.let { println("List runningFold: $it") }

    scope.launch {
        // 终端操作符 terminal operator
        // suspend fun <S, T : S> Flow<T>.reduce(operation: suspend (accumulator: S, value: T) -> S): S
        flow1.reduce { accumulator, value -> accumulator + value }.let { println("Flow reduce: $it") }
        // fun <T> Flow<T>.runningReduce(operation: suspend (accumulator: T, value: T) -> T): Flow<T>
        flow1.runningReduce { accumulator, value -> accumulator + value }
            .collect { println("Flow runningReduce: $it") }
        flow1.fold("Hello") { acc, value -> "$acc - $value" }.let { println("Flow fold: $it") }
        // suspend inline fun <T, R> Flow<T>.fold(initial: R, operation: suspend (acc: R, value: T) -> R): R
        flow1.runningFold("Hi") { accumulator, value -> "$accumulator - $value" }
            .collect { println("Flow runningFold: $it") }
        // Flow<T>.runningFold alias
        flow1.scan("Hi") { accumulator, value -> "$accumulator - $value" }
            .collect { println("Flow scan: $it") }
    }
    delay(10000)
}
/* Output:
List reduce: 15
List runningReduce: [1, 3, 6, 10, 15]
List fold1: 25
List fold2: Hello - 1 - 2 - 3 - 4 - 5
List runningFold: [Hi, Hi - 1, Hi - 1 - 2, Hi - 1 - 2 - 3, Hi - 1 - 2 - 3 - 4, Hi - 1 - 2 - 3 - 4 - 5]
Flow reduce: 15
Flow runningReduce: 1
Flow runningReduce: 3
Flow runningReduce: 6
Flow runningReduce: 10
Flow runningReduce: 15
Flow fold: Hello - 1 - 2 - 3 - 4 - 5
Flow runningFold: Hi
Flow runningFold: Hi - 1
Flow runningFold: Hi - 1 - 2
Flow runningFold: Hi - 1 - 2 - 3
Flow runningFold: Hi - 1 - 2 - 3 - 4
Flow runningFold: Hi - 1 - 2 - 3 - 4 - 5
Flow scan: Hi
Flow scan: Hi - 1
Flow scan: Hi - 1 - 2
Flow scan: Hi - 1 - 2 - 3
Flow scan: Hi - 1 - 2 - 3 - 4
Flow scan: Hi - 1 - 2 - 3 - 4 - 5
 */
