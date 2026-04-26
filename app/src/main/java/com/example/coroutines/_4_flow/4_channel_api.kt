package com.example.coroutines._4_flow

import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileWriter
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-20
 * @time 13:52
 *
 * 038.4.4-Channel API 详解
 *
 * Channel.UNLIMITED
 * 设置通道的缓冲区容量为无限大。底层采用链表结构动态分配内存。在此模式下，发送方的 send 操作永远不会被挂起，直至因内存耗尽引发系统异常。
 * 接收方的 receive 操作在通道内无数据时会被挂起，直到有新数据到达。此配置适用于必须保留所有已发送数据且硬件内存允许的场景。
 * Channel.RENDEZVOUS
 * 设置通道的缓冲区容量为零，此为未显式指定容量时的默认配置。该模式强制要求数据交换必须在发送方与接收方均处于就绪状态时发生。
 * 具体表现为：当发送方调用 send 时，若此时无接收方正处于等待接收的状态，则发送方协程将被挂起；
 * 当接收方调用 receive 时，若此时无发送方正处于等待发送的状态，则接收方协程将被挂起。数据交接仅在收发双方发生交汇时瞬间完成。
 * Channel.CONFLATED
 * 设置通道的缓冲区容量为 1，并强制绑定丢弃最旧数据的溢出策略 (DROP_OLDEST)。通道内部最多仅保留一个最新发送的元素。
 * 当新数据到达时，若缓冲区内已有未被消费的旧数据，旧数据将被直接覆盖。在此机制下，发送方的 send 操作永远不会被挂起。
 * 接收方的 receive 操作在通道为空时会被挂起，一旦有数据写入即可恢复执行并获取最新元素。此配置适用于接收方仅需获取最新状态而允许丢失中间状态的场景。
 * Channel.BUFFERED
 * 设置通道的缓冲区容量为系统默认大小，该默认数值通常为 64，可通过系统属性进行重写。在此模式下，当缓冲区存在空闲空间时，
 * 发送方的 send 操作可立即完成而不被挂起；当缓冲区已满时，后续的 send 操作将根据通道配置的溢出策略执行，
 * 若未显式配置，则默认触发挂起等待。接收方的 receive 操作在缓冲区为空时将被挂起，直到缓冲区中被写入新数据。
 *
 * BufferOverflow.SUSPEND
 * 作为固定容量通道在发生缓冲区溢出时的默认处理策略。当通道的内部缓冲区达到设定的最大容量限制时，任何继续调用 send 方法
 * 尝试写入新数据的发送方协程都会被底层状态机挂起。直到接收方协程调用 receive 方法消费了部分数据，使缓冲区重新释放出空闲空间后，* 被挂起的发送操作才会被恢复执行并完成数据入队。此策略通过挂起发送方来严格保证数据流的完整性，防止任何元素在传输过程中丢失。
 * BufferOverflow.DROP_OLDEST
 * 一种非阻塞的缓冲区溢出处理策略。当通道缓冲区已满且发送方尝试通过 send 写入新数据时，通道底层结构会自动出队并丢弃队列头部最旧的元素
 * （即最早进入缓冲区且尚未被消费的数据），从而为新元素腾出空间，并将新元素正常追加至队列尾部。由于溢出发生时底层会自动执行旧数据清理操作，
 * 配置此策略的通道其发送操作永远不会引发协程挂起。Channel.CONFLATED 的底层实现本质上就是将缓冲区容量强制设定为 1，并强行绑定此策略。
 * BufferOverflow.DROP_LATEST
 * 一种非阻塞的缓冲区溢出处理策略。当通道缓冲区已满且发送方尝试通过 send 写入新数据时，当前正在请求入队的这个最新元素将被直接丢弃，
 * 而缓冲区内原有的存量数据队列状态保持绝对不变。与 DROP_OLDEST 策略在执行流程上具有相似性，配置此策略的通道同样具备自清理特性，
 * 发送方的 send 操作永远不会触发挂起行为，适用于接收端对瞬时突发峰值数据不敏感且允许丢弃最新到达元素的场景。
 *
 * SendChannel
 * isClosedForSend
 * close()
 *
 * ReceiveChannel
 * isClosedForReceive
 * cancel()
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val fileChannel = Channel<FileWriter> { it.close() }
    fileChannel.send(FileWriter("test.txt"))
    // val channel = Channel<List<Contributor>>(8, BufferOverflow.DROP_OLDEST)
    // val channel = Channel<List<Contributor>>(1, BufferOverflow.DROP_OLDEST)
    val channel = Channel<List<Contributor>>(Channel.CONFLATED)
    scope.launch {
        channel.send(gitHub.contributors("square", "retrofit"))
        channel.close()
        channel.close(IllegalStateException("Data error!"))
        channel.receive()
        channel.receive()
        channel.send(gitHub.contributors("square", "retrofit"))
        // public fun trySend(element: E): ChannelResult<Unit>v
        channel.trySend(gitHub.contributors("square", "retrofit"))
        // public fun tryReceive(): ChannelResult<E>
        channel.tryReceive()
    }
    launch {
        for (data in channel) {
            println("Contributors: $data")
        }
        /*while (isActive) {
          val contributors = channel.receive()
          println("Contributors: $contributors")
        }*/
    }
    delay(1000)
    channel.cancel()
    delay(10000)
}
