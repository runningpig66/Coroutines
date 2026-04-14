package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author runningpig66
 * @date 2026-04-10
 * @time 3:38
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext) // Default
    val job = launch(Dispatchers.Default) {
        // 将传统回调转换为挂起函数的最简方式。协程会在 suspendCoroutine 处挂起，直到调用 continuation.resume 或 resumeWithException 后才会恢复。
        // 该函数不响应协程的取消信号：即使外部 Job 被取消，当前挂起点也不会抛出 CancellationException，而会继续等待回调完成。
        // 因此，在不可取消的场景下（例如必须执行完的清理操作）可以使用它；但对于大多数业务代码，它可能导致协程永久挂起并泄漏资源。
        suspendCoroutine<String> {
            // 立即唤醒协程放行
            it.resume("Hello")
        }
        // 支持取消的挂起函数转换器，是 suspendCoroutine 的增强版本。它会在挂起期间监听协程的取消状态，一旦检测到取消，立即抛出 CancellationException 并终止挂起。
        // 配合 invokeOnCancellation 可以在取消时执行资源释放逻辑（如关闭网络连接或文件流）。所有涉及 I/O、网络或不可控延迟的回调桥接都应优先使用此函数，
        // 以确保协程能安全融入结构化并发的取消传播链。
        suspendCancellableCoroutine<String> {
            // 立即唤醒协程放行
            it.resume("Hi")
        }
        /*var count = 0
        while (true) {
            ensureActive() // 结束协程时不需要清理
            if (!isActive) { // 结束协程时需要清理
                // clear
                // return@launch
                throw CancellationException()
            }
            count++
            if (count % 100_000_000 == 0) {
                println(count)
            }
            if (count % 1_000_000_000 == 0) {
                break
            }
        }*/
        // InterruptedException
        var count = 0
        while (true) {
            /*if (isActive) {
                // Clear
                return@launch
            }*/
            println("count: ${count++}")
            try {
                delay(500)
            }
            /*catch (e: CancellationException) {
                println("Cancelled")
                // Clear
                throw e
            }*/
            finally {
                // Clear
            }
        }
    }
    delay(1000)
    job.cancel()
    // thread.interrupt()
}
