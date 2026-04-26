package com.example.coroutines._4_flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-21
 * @time 11:51
 *
 * 039.4.5-actor()：把 SendChannel 暴露出来
 */
@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val sendChannel = scope.actor<Int> {
        for (num in this) {
            println("Number: $num")
        }
    }
    scope.launch {
        for (num in 1..100) {
            sendChannel.send(num)
            delay(1000)
        }
    }
    /*val receiveChannel = scope.produce {
        for (num in 1..10) {
            send(num)
            delay(1000)
        }
    }
    scope.launch {
        for (num in receiveChannel) {
            println("Number: $num")
        }
    }*/
    delay(10000)
}
