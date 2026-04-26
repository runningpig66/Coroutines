package com.example.coroutines._4_flow

import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-19
 * @time 15:44
 *
 * 037.4.3-Channel 的工作模式详解
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    // BlockingQueue
    val channel = Channel<List<Contributor>>()
    scope.launch {
        channel.send(gitHub.contributors("square", "retrofit"))
    }
    scope.launch {
        while (isActive) {
            channel.receive()
        }
    }
    scope.launch {
        while (isActive) {
            channel.receive()
        }
    }
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
