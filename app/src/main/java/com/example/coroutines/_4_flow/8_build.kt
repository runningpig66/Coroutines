package com.example.coroutines._4_flow

import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import com.example.coroutines.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * @author runningpig66
 * @date 2026-04-22
 * @time 15:04
 *
 * 042.4.8-Flow 的创建
 *
 * 基于通道转换数据流的冷热边界与运行特性：在响应式流架构中，界定冷流与热流的核心准则在于数据源的生命周期是否依赖于消费端点的订阅行为。
 * 基于通道转换的流对象在接口表现与底层运行时呈现冷热复合特征。此类构建函数返回的流实例在收集操作触发前处于惰性求值阶段，不主动拉取底层数据，
 * 在外部接口语义上符合冷流的延迟执行标准。然而，当收集链路建立后，其底层控制流直接接入独立运行的异步通道，其实际数据源的生命周期完全独立于当前的消费者。
 * 在多端并发订阅场景下，底层轮询机制会导致各协程对同一通道的缓冲序列产生扇出型竞态分配。该执行流无法像纯正冷流那样为每个收集器独立重放完整的生产模板，
 * 而是呈现热流固有的共享分发特征，各消费协程仅能截取源数据序列的局部切片。因此，该转换机制本质上构筑了由热数据源驱动的冷流抽象层。
 */
fun main() = runBlocking<Unit> {
    /* fun <T> flowOf(vararg elements: T): Flow<T>
    根据传入的可变参数集合构造并实例化一个具备有限状态的冷数据流对象。当下游端点触发收集操作时，
    底层控制流将在当前挂起函数的协程上下文中按参数列表的既定内存顺序进行同步迭代，并依次调用发射接口传递数据。
    参数集合遍历完毕且所有发射动作均执行返回后，该数据流的生命周期即刻正常终止。*/
    val flow1 = flowOf(1, 2, 3)
    /* fun <T> Iterable<T>.asFlow(): Flow<T>
    将同步的可迭代集合转换为基于协程机制的冷数据流。当下游执行收集操作时，底层控制流将在收集器所处的协程上下文中同步获取底层迭代器，
    并顺序读取集合内部元素进行逐个发射。集合元素遍历完毕后，该数据流的执行路径即刻终止。
    此转换过程不包含隐式的调度器切换，元素的提取与发射速率受制于下游消费端点的挂起与恢复周期。*/
    val flow2 = listOf(1, 2, 3).asFlow()
    val flow3 = setOf(1, 2, 3).asFlow()
    val flow4 = sequenceOf(1, 2, 3).asFlow()
    val channel = Channel<Int>()
    /* fun <T> ReceiveChannel<T>.consumeAsFlow(): Flow<T>
    将通道对象封装为具备生命周期绑定特性的单次消费型数据流。当下游触发收集操作时，控制流开始提取通道数据并执行同步发射。
    该流模型获取并持有底层通道的独占式消费所有权，严格排斥多协程并发收集或生命周期内的二次订阅，违背单一消费者约束将直接触发状态流转异常。
    在执行终态处理层面，无论收集逻辑因通道序列耗尽正常结束，或因下游逻辑抛出异常导致中断，底层控制流均会同步调用源通道的取消例程，强制切断双向通信链路并清理底层挂起资源。*/
    val flow5 = channel.consumeAsFlow()
    /* fun <T> ReceiveChannel<T>.receiveAsFlow(): Flow<T>
    将通道对象映射为与数据源生命周期完全解耦的数据流。收集操作触发后，底层逻辑直接调用源通道的标准接收接口执行连续的数据轮询与向下发射。
    该转换机制不接管通道的生命周期所有权，下游收集链路的正常结束或异常终止均不会向上游通道传播取消信号。在并发订阅架构中，
    多个执行收集的协程将对原始通道的未消费元素形成扇出型竞态分配。若源通道未接收到显式的关闭指令，其关联的收集逻辑将在当前执行上下文中持续保持挂起轮询状态。*/
    val flow6 = channel.receiveAsFlow()
    /* 基于 Channel 构建的冷数据流，专为需要并发发射数据的场景设计。当 collect 被调用时，框架会基于下游收集器所在的协程上下文，
    派生出一个作为直接子节点的生产者协程。构建器的闭包运行在具备 CoroutineScope 和 SendChannel 能力的上下文中，
    因此允许开发者在流的内部调用 launch 启动多个并发的子协程，并通过 send() 将数据安全地压入底层的通道缓冲区。
    下游的收集器随后会从该缓冲区串行读取数据。这种机制在保证下游线程安全（不抛出并发收集异常）的前提下，实现了上游的并发数据生产。*/
    val flow7 = channelFlow {
        this.launch {
            delay(2000)
            send(2)
        }
        delay(1000)
        send(1)
    }
    val flow8 = flow {
        /*this@runBlocking.*/
        launch {
            delay(2000)
            emit(2)
        }
        delay(1000)
        emit(1)
    }
    /* 专门用于将传统的基于回调（Callback）或事件监听的异步 API 转换为数据流的构建器。它在底层复用了 channelFlow 的通道机制。
    开发者可以在回调接口的响应方法中，通过 trySend() 将持续产生的事件安全地推送到流的缓冲区中。由于回调 API 通常是非阻塞的同步调用，
    为了防止流因代码块顺序执行完毕而过早结束，必须在闭包末尾显式调用 awaitClose 函数挂起当前协程，并在此处处理回调的反注册逻辑，以防止资源泄漏。*/
    val flow9 = callbackFlow {
        val call = gitHub.contributorsCall("square", "retrofit")
        call.enqueue(object : Callback<List<Contributor>> {
            override fun onResponse(call: Call<List<Contributor>?>, response: Response<List<Contributor>?>) {
                trySend(response.body())
                close()
            }

            override fun onFailure(call: Call<List<Contributor>?>, error: Throwable) {
                cancel(CancellationException(error))
            }
        })
        /* 用于在 callbackFlow 内部保持流持续存活并处理终态清理的核心挂起函数。执行此方法会立刻挂起当前的生产者协程，阻止代码流继续向下隐式返回，
        从而让底层的异步回调机制有充足的时间持续发送数据。该挂起点会一直保持阻塞，直到内部通道被主动调用 close()，或者外部的收集端协程触发了取消操作。
        此时协程将被唤醒，并同步执行传入的清理代码块。开发者必须在此代码块中注销异步监听器或取消网络请求，以确保协程的结构化取消机制能正确释放外部资源。*/
        awaitClose {
            call.cancel()
        }
    }
    // suspendCancellableCoroutine { } // 单次回调
    val scope = CoroutineScope(EmptyCoroutineContext)
    scope.launch {
        flow9.collect {
            log("channelFlow with callback: $it")
        }
        /*flow8.collect {
            log("channelFlow: $it")
        }*/
    }
    /*scope.launch {
        flow5.collect {
            log("Flow6 - 1: $it")
        }
    }
    scope.launch {
        flow5.collect {
            log("Flow6 - 2: $it")
        }
    }*/
    /*channel.send(1)
    channel.send(2)
    channel.send(3)
    channel.send(4)*/
    delay(10000)
}
