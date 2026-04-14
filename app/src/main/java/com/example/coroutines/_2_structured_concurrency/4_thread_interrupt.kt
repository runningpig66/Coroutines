package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * @author runningpig66
 * @date 2026-04-09
 * @time 4:24
 */
fun main() = runBlocking<Unit> {
    val thread = object : Thread() {
        override fun run() {
            println("Thread: I'm running!")
            // return@thread
            try {
                Thread.sleep(2000)
            } catch (_: InterruptedException) {
                // Java 的中断机制是协作式的：调用 thread.interrupt() 仅会设置线程的中断标志位，而不会强制终止线程，线程需要主动检查并响应这个信号。
                // 当线程处于 Thread.sleep(), Object.wait() 或 Thread.join() 等阻塞状态时，收到中断信号会立即抛出 InterruptedException，
                // 同时 JVM 会自动清除中断标志位（重置为 false），因此在 catch 块中调用 isInterrupted() 必然返回 false。
                // 这一设计的目的是让异常处理逻辑完全接管中断后的清理工作，避免后续代码误判中断状态。捕获 InterruptedException 即表示收到了取消请求，
                // 此时应执行必要的资源释放（如关闭流、释放锁等），然后通过 return 正常结束 run() 方法，实现线程的安全退出。
                println("isInterrupted: $isInterrupted")
                println("Clearing ...") // ... 结束前的清理工作
                return
            }
            val lock = Object()
            try {
                lock.wait()
            } catch (_: InterruptedException) {
                println("isInterrupted: $isInterrupted")
                println("Clearing ...") // ... 结束前的清理工作
                return
            }
            val newThread = thread {

            }
            newThread.join()
            val latch = CountDownLatch(3)
            latch.await()
            /*var count = 0
            while (true) {
                // Thread.currentThread().isInterrupted
                if (isInterrupted) {
                    // ... 结束前的清理工作
                    return
                }
                count++
                if (count % 100_000_000 == 0) {
                    println(count)
                }
                if (count % 1_000_000_000 == 0) {
                    break
                }
            }*/
            println("Thread: I'm done!")
        }
    }.apply { start() }
    Thread.sleep(1000)
    // thread.stop()
    thread.interrupt()
}
