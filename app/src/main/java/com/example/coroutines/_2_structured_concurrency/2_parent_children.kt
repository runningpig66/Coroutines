package com.example.coroutines._2_structured_concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-09
 * @time 2:00
 */
@Suppress("CoroutineContextWithJob")
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val initJob = scope.launch {
        launch { }
        launch { }
    }
    scope.launch {
        // ... 前置不依赖初始化流程结果的代码
        initJob.join()
        // ... 后续开启依赖初始化流程结果的代码
    }
    var innerJob: Job? = null
    val job = scope.launch {
        launch(Job()) {
            delay(100)
        }
        // 协程上下文合并与 Job 继承机制的核心原则如下：通过 launch 或 async 创建的协程会生成一个全新的 Job 实例，任何传入的上下文参数都无法覆盖该实例。
        // 对于上下文中的常规元素（如 Dispatcher 或拦截器），遵循属性覆盖原则，传入的元素会替换从父级继承的默认行为并直接作用于新协程自身；
        // 而对于 Job 这一特殊元素，则遵循拓扑重定向原则，传入的 Job 仅会被作为新协程的父节点消费，而绝不会覆盖新协程自身的 Job 实例。
        // 在本例中，显式传入 scope.coroutineContext[Job] 打破了默认的父子垂直挂载关系，
        // 将内部协程 innerJob 强行挂载到外层 scope 的根 Job 下，最终导致内部协程与外层 job 成为拥有共同父节点的兄弟关系。
        innerJob = this.launch(scope.coroutineContext[Job]!!) {
            delay(100)
        }
    }
    val startTime = System.currentTimeMillis()
    job.join()
    val duration = System.currentTimeMillis() - startTime
    println("duration: $duration")
    // val children = job.children
    // println("children count: ${children.count()}")
    // println("innerJob === children.first(): ${innerJob === children.first()}")
    // println("innerJob.parent === job: ${innerJob?.parent === job}")
}
