package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-11
 * @time 4:57
 *
 * 协程常规异常传播与状态机连带取消全链路机制：协程的普通异常传播与正常取消（CancellationException）在底层共享同一套状态机体系，
 * 两者的核心分支点仅在于向父节点汇报时的类型校验策略。当 Job 树中某节点抛出常规异常时，底层执行流将按以下时序进行状态同步与递归传播：
 * 1. 状态跃迁与向下传播（局部子树处理）：抛出异常的节点首先会触发自身的 cancelImpl(cause) 方法，其内部状态由 Active 变更为 Cancelling。
 * 随后，该节点调用 notifyCancelling 遍历自身维护的子节点双向链表，依次调用所有子节点的 cancel(cause) 方法。
 * 此操作使该异常节点的所有直接和间接子孙节点同步进入 Cancelling 状态。
 * 2. 向上汇报与父节点干预：在完成向下广播的同时，该节点通过其持有的 parentHandle 引用，调用父节点的 childCancelled(cause) 方法。
 * 在该方法内部，父节点执行异常类型判定：若 cause 为 CancellationException（正常取消），父节点直接返回 true 拦截传播；
 * 若为常规异常，父节点则强制调用自身的 cancelImpl(cause)。
 * 3. 横向蔓延与整树递归：父节点调用自身的 cancelImpl 后，自身亦进入 Cancelling 状态，并触发其自身的 notifyCancelling。
 * 此时，父节点其余正常运行的子节点（即原异常节点的兄弟节点）被迫接收 cancel 信号并转入 Cancelling 状态。
 * 随后，父节点继续向其上一级父节点调用 childCancelled，此过程沿树形结构不断向上递归，直至抵达根节点，最终导致整棵关联的 Job 树统一变更为 Cancelling 状态。
 * 4. 幂等性与双向通信死锁阻断：在上述父调子与子调父的并发双向传播中，无限递归的截断依赖于 cancelImpl 内部的状态幂等性校验。
 * 当任意节点再次被调用 cancelImpl 时，只要检测到其自身已处于 Cancelling 或后续状态，函数便直接返回 false 并跳过任何进一步的状态修改与遍历逻辑。
 * 5. 状态同步与物理终止分离：上述整树的递归传播仅限于内存中状态机的同步修改。物理线程执行流的真正终止，仍严格遵循协作式并发规范：
 * 即各节点在进入 Cancelling 状态后，需在其后续代码的挂起点处（或手动进行状态检查时）抛出异常，执行各自的收尾清理工作，
 * 待所有相关子节点彻底结束，该节点的状态才会最终定格为 Cancelled。
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    var childJob: Job? = null
    val parentJob = scope.launch {
        println("Parent started")
        childJob = launch {
            launch {
                println("Grandchild started")
                delay(3000)
                println("Grandchild done")
            }
            delay(1000)
            throw IllegalStateException("User invalid!")
        }
        delay(3000)
        println("Parent done")
    }
    delay(500)
    // CoroutineExceptionHandler
    // parentJob.cancel()
    // println("isActive: parent - ${parentJob.isActive}, child - ${childJob?.isActive}")
    // println("isCancelled: parent - ${parentJob.isCancelled}, child - ${childJob?.isCancelled}")
    // delay(1500)
    // println("isActive: parent - ${parentJob.isActive}, child - ${childJob?.isActive}")
    // println("isCancelled: parent - ${parentJob.isCancelled}, child - ${childJob?.isCancelled}")
    delay(10000)
}
