package com.example.coroutines.common

import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author runningpig66
 * @date 2026-04-03
 * @time 22:47
 */
// 构建基础的 Retrofit 实例。即使在 Mock 模式下，MockRetrofit 也需要依赖此基础实例来获取全局的序列化配置（Converter）
// 以及异步调用适配器（CallAdapter）。这是为了确保模拟返回的数据结构与真实的线上环境严格保持一致。
private val retrofit = Retrofit.Builder().baseUrl(GITHUB_API)
    .addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // 桥接 RxJava3
    .addConverterFactory(GsonConverterFactory.create()) // 桥接 JSON 反序列化
    .build()

// val gitHub: GitHub = retrofit.create(GitHub::class.java)


// 实例化网络行为控制器 (NetworkBehavior)。
// 默认配置下，它会模拟一个表现良好的网络连接（如：默认带来约 2000ms 的网络延迟，0% 的失败率，0% 的错误率）。
private val behavior = NetworkBehavior.create().apply {
    setFailurePercent(0)
    setDelay(1000L, TimeUnit.MILLISECONDS)
}

// 结合基础 Retrofit 引擎与网络行为控制器，构建 MockRetrofit 实例。
private val mockRetrofit = MockRetrofit.Builder(retrofit)
    .networkBehavior(behavior)
    .build()

// 与真实 Retrofit 创建接口实现类不同，MockRetrofit.create() 生成的是一个 BehaviorDelegate（行为代理）。
// 该代理并不直接实现 GitHub 接口的方法，而是作为一个拦截器，专门负责向返回结果中注入延迟或异常。
private val delegate = mockRetrofit.create(GitHub::class.java)

// 将行为代理注入到我们手动编写的本地数据源实现（MockGitHub）中，最终暴露给外部的 gitHub 对象，既具备了本地读取数据的能力，又具备了网络延迟的物理特征。
val gitHub: GitHub = MockGitHub(delegate)


// 实例化一个定制化的网络行为控制器，用于模拟恶劣的网络环境。
private val unstableBehavior = NetworkBehavior.create().apply {
    // 设置网络请求的失败率为 40%
    setFailurePercent(40)
    // 指定当模拟失败发生时，底层抛出的异常类型为 TimeoutException。用于测试协程的 try-catch 块或 CoroutineExceptionHandler
    setFailureException(TimeoutException("Connection time out!"))
}

// 使用恶劣网络行为控制器，构建不稳定的 MockRetrofit 引擎。
private val mockUnstableRetrofit = MockRetrofit.Builder(retrofit)
    .networkBehavior(unstableBehavior)
    .build()

// 生成带有高故障率特性的行为代理。
private val unstableDelegate = mockUnstableRetrofit.create(GitHub::class.java)

// 暴露给外部调用的不稳定 GitHub API 实例。当业务层调用该实例的挂起函数时，有 40% 的概率不会获得数据，而是直接抛出 TimeoutException。
val unstableGitHub: GitHub = MockGitHub(unstableDelegate)
