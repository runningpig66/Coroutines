package com.example.coroutines._4_flow

import com.example.coroutines.common.gitHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-19
 * @time 14:55
 *
 * 036.4.2-用 produce() 来提供跨协程的事件流
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val receiver = scope.produce {
        while (isActive) {
            val data = gitHub.contributors("square", "retrofit")
            send(data)
        }
    }
    launch {
        delay(5000)
        while (isActive) {
            println("Contributors: ${receiver.receive()}")
        }
    }
    delay(10000)
}
