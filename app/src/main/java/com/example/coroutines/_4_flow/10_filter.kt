package com.example.coroutines._4_flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author runningpig66
 * @date 2026-04-24
 * @time 16:24
 *
 * 044.4.10-filter() 系列操作符
 */
fun main() = runBlocking<Unit> {
    val scope = CoroutineScope(EmptyCoroutineContext)
    val flow1 = flowOf(1, 2, null, 3, null, 4, 5) // <Int?>
    val flow2 = flowOf(1, 2, 3, "running", "pig", listOf("A", "B"), listOf("C", "D"), listOf(1, 2))
    scope.launch {
        // fun <T> Flow<T>.filter(predicate: suspend (T) -> Boolean): Flow<T>
        flow1.filter { it?.rem(2) == 0 }.collect { println("1: $it") }
        flow1.filter { it != null && it % 2 == 0 }.collect { println("2: $it") } // 注意：使用filter自定义排空结果仍然是可空元素
        // fun <T> Flow<T>.filterNot(predicate: suspend (T) -> Boolean): Flow<T>
        flow1.filterNot { it?.rem(2) == 0 }.collect { println("3: $it") }
        flow1.filterNot { it != null && it % 2 == 0 }.collect { println("4: $it") }
        // fun <T: Any> Flow<T?>.filterNotNull(): Flow<T>
        flow1.filterNotNull().filterNot { it % 2 == 0 }.collect { println("5: $it") }
        // fun <reified R> Flow<*>.filterIsInstance(): Flow<R>
        flow2.filterIsInstance<List<String>>().collect { println("6: $it") } // 无法过滤泛型实参
        // fun <R : Any> Flow<*>.filterIsInstance(klass: KClass<R>): Flow<R>
        flow2.filterIsInstance(List::class).collect { println("7: $it") }

        /* 常规条件过滤操作。该实现依赖闭包内部的类型判断符号对数据流元素进行布尔求值。编译器仅在当前判定闭包的局部作用域内执行智能类型转换。
        该操作流转结束后，原始数据流的泛型边界未发生实质改变，下游节点接收到的元素类型仍保持初始状态。
        此机制导致类型推导信息无法沿操作符链路向下传递，后续处理逻辑需承担额外的显式类型转换开销。*/
        flow2.filter { it is List<*> && it.firstOrNull()?.let { item -> item is String } == true }
            .collect { println("8: $it") }

        /* 基于具体化泛型参数的内联类型过滤与转换机制。该操作符利用编译器内联与 reified 特性，在编译阶段将确定的类型校验指令直接展开至调用处字节码，
        绕过运行时的泛型擦除限制，实现无额外计算损耗的精确类型断言。执行完成后，该节点会重构数据流的泛型签名，
        输出与目标泛型严格匹配的安全数据流，为后续链式调用提供完整的静态类型推导环境，是处理流式类型过滤的标准首选路径。*/
        flow2.filterIsInstance<List<*>>()
            .filter { it.firstOrNull() is String }
            .collect { println("9: $it") } // THE BEST WAY

        /* 基于动态反射机制的类实例过滤实现。该方法依赖显式传入的类引用作为判定元数据，底层调用系统反射接口执行实例归属校验。
        该节点同样具备向下游传递精确类型边界的能力，但反射模块的介入会在程序运行阶段引入额外的调用链与指令开销。
        在高频分发或对响应延迟极度敏感的异步数据流引擎中，该路径的执行效率弱于编译期内联技术，通常仅约束在类型参数只能于运行时动态获取的特定分发场景下使用。*/
        flow2.filterIsInstance(List::class)
            .filter { it.firstOrNull() is String }
            .collect { println("10: $it") }
    }
    delay(10000)
}
