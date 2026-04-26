package com.example.coroutines._4_flow

import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author runningpig66
 * @date 2026-04-20
 * @time 15:27
 *
 * 调用 Channel 的 close() 方法会将通道的发送功能标记为关闭。如果通道之前尚未关闭，此函数将执行关闭并返回 true；
 * 若已关闭则无操作并返回 false。调用此函数后，isClosedForSend 标志位会立刻返回 true。通道关闭会直接拦截后续的发送尝试，
 * 任何在 close() 之后新发起的 send 或 trySend 调用都会失败：如果 close() 为正常关闭（cause 为 null），
 * 新发送操作会抛出 ClosedSendChannelException 异常；如果 close() 携带了自定义的 cause 异常对象，
 * 则后续发送直接抛出该指定的异常。需要严格区分的是，在调用 close() 之前就已经发起、并且因缓冲区满而处于排队挂起状态的 send 调用，
 * 完全不受关闭操作的影响。它们依然有效，并在接收端消费数据腾出空间后，被依次唤醒并成功将数据存入通道。
 *
 * 通道在发送端关闭后，接收端（ReceiveChannel）依然保持开放状态以处理存量数据。通道缓冲区内原本存在的元素，
 * 以及那些在 close() 之前挂起并随后成功存入的元素，都会保留在通道中，并可以通过 receive() 方法或 for 循环被正常获取。
 * 只有当所有的存量数据被彻底接收完毕、通道真正变空之后，通道才会被视为在接收层面也已关闭，
 * 此时 isClosedForReceive 才会变为 true，接收方的 for 循环也会随之结束。在通道彻底清空并关闭接收端之后，
 * 如果通道是正常关闭的，继续调用 receive() 将抛出 ClosedReceiveChannelException 异常；
 * 如果通道是通过自定义 cause 异常关闭的，那么在元素耗尽后的所有接收尝试，均会直接抛出该自定义异常。
 *
 * 调用 ReceiveChannel 的 cancel() 方法将触发通道的立即双向关闭。此函数执行后，内部状态会发生同步变更，
 * isClosedForSend 与 isClosedForReceive 标志位均立刻返回 true。此操作将彻底阻断后续的数据传输链路，
 * 任何在此之后新发起的 send 或 receive 调用均会被拦截，并抛出 CancellationException 异常。
 * 该方法允许传入一个可选的 cause 参数以指定取消细节，若未显式指定，底层将默认实例化并抛出标准的 CancellationException。
 * 与平滑的正常关闭机制不同，cancel() 会对通道内部数据结构执行强制性的清理。它将直接抹除当前驻留在缓冲区内的所有存量数据。
 * 同时，底层状态机会立即介入处理所有正处于排队等待状态的延续体，无论是挂起在 receive 操作的接收端，
 * 还是因容量限制而挂起在 send 操作的发送端，均会被强制唤醒并统一注入 CancellationException 异常予以中断。
 *
 * 在数据被强制清除的过程中，底层机制会处理被废弃对象的引用状态以防资源泄漏。若在实例化 Channel 时通过第三个参数配置了
 * onUndeliveredElement 回调函数，cancel() 底层逻辑会在清空缓冲区残留元素以及阻断挂起发送方所持有的元素时，
 * 针对每一个被丢弃的数据体依次触发该回调执行收尾逻辑。若当前通道未注册此回调函数，所有被强行清除的元素引用将被直接解除，交由系统垃圾回收器自动处理。
 *
 * Tips: 参数求值优先于函数调用
 * 在你编写的 channel.send(gitHub.contributors("square", "retrofit")) 这一行代码中，实际包含两个挂起操作，其严格的执行顺序如下：
 * 优先执行参数：协程首先调用 gitHub.contributors(...) 这个挂起函数去发起网络请求。此时，当前协程被挂起，等待网络响应（如你设定的约 1000 毫秒）。
 * 执行外层调用：只有当网络请求完成，返回了 List<Contributor> 数据结果后，协程才会恢复执行，并将这组数据作为参数，正式去调用 channel.send(data)。
 * 在这个机制下，前 1000 毫秒内，通道（Channel）内部是完全空置的。没有任何一个发送者真正触碰到了通道的入口，它们全部阻塞在“准备发送数据”的网络请求阶段。
 */
@OptIn(DelicateCoroutinesApi::class)
fun main() = runBlocking<Unit> {
    log("Hello. Hi") // 初始化日志时间0基准
    val handler = CoroutineExceptionHandler { context, exception ->
        log("Caught in handler: $context $exception")
    }
    val scope = CoroutineScope(handler)
    val channel = Channel<List<Contributor>>(1)
    val job0 = scope.launch {
        delay(3000)
        channel.close()
        // channel.close(IllegalStateException("Data error!"))
    }
    val job1 = scope.launch {
        channel.send(gitHub.contributors("square", "retrofit"))
        log("job1 finish: ${this.coroutineContext.job}")
    }
    val job2 = scope.launch {
        channel.send(gitHub.contributors("square", "retrofit"))
        log("job2 finish: ${this.coroutineContext.job}")
    }
    val job3 = scope.launch {
        channel.send(gitHub.contributors("square", "retrofit"))
        log("job3 finish: ${this.coroutineContext.job}")
    }
    val job4 = scope.launch {
        channel.send(gitHub.contributors("square", "retrofit"))
        log("job4 finish: ${this.coroutineContext.job}")
    }
    val job5 = scope.launch {
        channel.send(gitHub.contributors("square", "retrofit"))
        log("job5 finish: ${this.coroutineContext.job}")
    }
    launch {
        for (data in channel) {
            log("sendChannel is closed: ${channel.isClosedForSend}") // false
            log("receiveChannel is closed: ${channel.isClosedForReceive}") // false
            delay(2000)
            log("job0 is active: $job0")
            log("job1 is active: $job1")
            log("job2 is active: $job2")
            log("job3 is active: $job3")
            log("job4 is active: $job4")
            log("job5 is active: $job5")
            log("sendChannel is closed: ${channel.isClosedForSend}") // true
            log("receiveChannel is closed: ${channel.isClosedForReceive}") // false
            log("Contributors: $data")
        }
        log("sendChannel is closed: ${channel.isClosedForSend}")
        log("receiveChannel is closed: ${channel.isClosedForReceive}")
    }
    scope.launch {
        delay(1800)
        log("job0 is active: $job0")
        log("job1 is active: $job1")
        log("job2 is active: $job2")
        log("job3 is active: $job3")
        log("job4 is active: $job4")
        log("job5 is active: $job5")
    }
}
/* Output:
0 [main] Hello. Hi
1726 [main] sendChannel is closed: false
1726 [main] receiveChannel is closed: false
1727 [DefaultDispatcher-worker-5] job2 finish: StandaloneCoroutine{Active}@bbf366c
1731 [DefaultDispatcher-worker-5] job4 finish: StandaloneCoroutine{Active}@79476612
1877 [DefaultDispatcher-worker-5] job0 is active: StandaloneCoroutine{Active}@1b3fe2d0
1877 [DefaultDispatcher-worker-5] job1 is active: StandaloneCoroutine{Active}@6941bb8d
1877 [DefaultDispatcher-worker-5] job2 is active: StandaloneCoroutine{Completed}@bbf366c
1878 [DefaultDispatcher-worker-5] job3 is active: StandaloneCoroutine{Active}@172372df
1878 [DefaultDispatcher-worker-5] job4 is active: StandaloneCoroutine{Completed}@79476612
1878 [DefaultDispatcher-worker-5] job5 is active: StandaloneCoroutine{Active}@5596f1df
3728 [main] job0 is active: StandaloneCoroutine{Completed}@1b3fe2d0
3728 [main] job1 is active: StandaloneCoroutine{Active}@6941bb8d
3728 [main] job2 is active: StandaloneCoroutine{Completed}@bbf366c
3728 [main] job3 is active: StandaloneCoroutine{Active}@172372df
3729 [main] job4 is active: StandaloneCoroutine{Completed}@79476612
3729 [main] job5 is active: StandaloneCoroutine{Active}@5596f1df
3730 [main] sendChannel is closed: true
3730 [main] receiveChannel is closed: false
3736 [main] Contributors: [Contributor(login=John Doe, contributions=12), Contributor(login=Bob Smith, contributions=2), Contributor(login=Big Bird, contributions=40)]
3736 [DefaultDispatcher-worker-5] job3 finish: StandaloneCoroutine{Active}@172372df
3737 [main] sendChannel is closed: true
3737 [main] receiveChannel is closed: false
5747 [main] job0 is active: StandaloneCoroutine{Completed}@1b3fe2d0
5747 [main] job1 is active: StandaloneCoroutine{Active}@6941bb8d
5747 [main] job2 is active: StandaloneCoroutine{Completed}@bbf366c
5747 [main] job3 is active: StandaloneCoroutine{Completed}@172372df
5747 [main] job4 is active: StandaloneCoroutine{Completed}@79476612
5747 [main] job5 is active: StandaloneCoroutine{Active}@5596f1df
5747 [main] sendChannel is closed: true
5747 [main] receiveChannel is closed: false
5747 [main] Contributors: [Contributor(login=John Doe, contributions=12), Contributor(login=Bob Smith, contributions=2), Contributor(login=Big Bird, contributions=40)]
5747 [main] sendChannel is closed: true
5747 [main] receiveChannel is closed: false
5747 [DefaultDispatcher-worker-5] job5 finish: StandaloneCoroutine{Active}@5596f1df
7754 [main] job0 is active: StandaloneCoroutine{Completed}@1b3fe2d0
7754 [main] job1 is active: StandaloneCoroutine{Active}@6941bb8d
7754 [main] job2 is active: StandaloneCoroutine{Completed}@bbf366c
7754 [main] job3 is active: StandaloneCoroutine{Completed}@172372df
7754 [main] job4 is active: StandaloneCoroutine{Completed}@79476612
7754 [main] job5 is active: StandaloneCoroutine{Completed}@5596f1df
7754 [main] sendChannel is closed: true
7754 [main] receiveChannel is closed: false
7754 [main] Contributors: [Contributor(login=John Doe, contributions=12), Contributor(login=Bob Smith, contributions=2), Contributor(login=Big Bird, contributions=40)]
7754 [main] sendChannel is closed: true
7754 [main] receiveChannel is closed: false
7754 [DefaultDispatcher-worker-5] job1 finish: StandaloneCoroutine{Active}@6941bb8d
9762 [main] job0 is active: StandaloneCoroutine{Completed}@1b3fe2d0
9762 [main] job1 is active: StandaloneCoroutine{Completed}@6941bb8d
9762 [main] job2 is active: StandaloneCoroutine{Completed}@bbf366c
9762 [main] job3 is active: StandaloneCoroutine{Completed}@172372df
9762 [main] job4 is active: StandaloneCoroutine{Completed}@79476612
9762 [main] job5 is active: StandaloneCoroutine{Completed}@5596f1df
9762 [main] sendChannel is closed: true
9762 [main] receiveChannel is closed: false
9762 [main] Contributors: [Contributor(login=John Doe, contributions=12), Contributor(login=Bob Smith, contributions=2), Contributor(login=Big Bird, contributions=40)]
9762 [main] sendChannel is closed: true
9762 [main] receiveChannel is closed: true
11771 [main] job0 is active: StandaloneCoroutine{Completed}@1b3fe2d0
11771 [main] job1 is active: StandaloneCoroutine{Completed}@6941bb8d
11771 [main] job2 is active: StandaloneCoroutine{Completed}@bbf366c
11771 [main] job3 is active: StandaloneCoroutine{Completed}@172372df
11771 [main] job4 is active: StandaloneCoroutine{Completed}@79476612
11771 [main] job5 is active: StandaloneCoroutine{Completed}@5596f1df
11771 [main] sendChannel is closed: true
11771 [main] receiveChannel is closed: true
11771 [main] Contributors: [Contributor(login=John Doe, contributions=12), Contributor(login=Bob Smith, contributions=2), Contributor(login=Big Bird, contributions=40)]
11771 [main] sendChannel is closed: true
11771 [main] receiveChannel is closed: true
 */
