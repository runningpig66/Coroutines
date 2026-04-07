package com.example.coroutines.common

import io.reactivex.rxjava3.core.Single
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.mock.BehaviorDelegate
import java.util.concurrent.CompletableFuture

/**
 * @author runningpig66
 * @date 2026-04-03
 * @time 22:47
 */
const val GITHUB_API: String = "https://api.github.com"

data class Contributor(val login: String, val contributions: Int)

interface GitHub {
    // https://api.github.com/repos/{owner}/{repo}/contributors
    @GET("/repos/{owner}/{repo}/contributors")
    fun contributorsCall(
        @Path("owner") owner: String, // square
        @Path("repo") repo: String, // retrofit
    ): Call<List<Contributor>>

    @GET("/repos/{owner}/{repo}/contributors")
    suspend fun contributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): List<Contributor>

    @GET("/repos/{owner}/{repo}/contributors")
    fun contributorsFuture(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): CompletableFuture<List<Contributor>>

    @GET("/repos/{owner}/{repo}/contributors")
    fun contributorsRx(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): Single<List<Contributor>>
}

/**
 * GitHub 接口的本地模拟（Mock）实现类。适用于网络受限或测试环境下，替代真实的 HTTP 请求以返回硬编码数据。
 * @param delegate Retrofit Mock 模块提供的行为代理实例。
 * 其核心作用是模拟真实的网络环境特性（如动态注入网络延迟、模拟丢包或 HTTP 错误状态码），
 * 并将原始数据类型安全地转换为接口定义的异步返回类型（如 Call, suspend, Single 等）。
 *
 * 关于 BehaviorDelegate 的底层机制说明：在通常的 Retrofit 使用中，接口的实现类是由底层通过 Proxy.newProxyInstance 动态生成的。
 * 但在 Mock 模式下，开发者需要手动提供接口的实现类（即当前的 MockGitHub）。BehaviorDelegate 的引入是为了解决一个核心问题：
 * 如何让完全同步的本地内存读取操作，表现得像一次真实的、需要耗时的异步网络请求？
 * 它允许开发者在构造 MockRetrofit 时配置 NetworkBehavior，从而控制代理对象返回数据时的延迟时间或错误概率。
 */
class MockGitHub(private val delegate: BehaviorDelegate<GitHub>) : GitHub {
    // 基于内存的双层嵌套哈希表，用作本地模拟数据库。使用 LinkedHashMap 以保证数据遍历时的插入顺序。
    // 数据结构层级：Owner(组织/拥有者) -> Repo(仓库名) -> List<Contributor>(贡献者列表)。
    private val ownerRepoContributors: MutableMap<String, MutableMap<String, MutableList<Contributor>>> = LinkedHashMap()

    init {
        addContributor("square", "retrofit", "John Doe", 12)
        addContributor("square", "retrofit", "Bob Smith", 2)
        addContributor("square", "retrofit", "Big Bird", 40)
        addContributor("square", "okhttp", "Proposition Joe", 39)
        addContributor("square", "okhttp", "Keiser Soze", 152)
    }

    /**
     * 数据写入的辅助方法。处理了多层 Map 在键值不存在时的空指针问题及初始化逻辑。
     */
    private fun addContributor(owner: String, repo: String, name: String?, contributions: Int) {
        var repoContributors = ownerRepoContributors[owner]
        if (repoContributors == null) {
            repoContributors = LinkedHashMap()
            ownerRepoContributors[owner] = repoContributors
        }
        var contributors = repoContributors[repo]
        if (contributors == null) {
            contributors = ArrayList()
            repoContributors[repo] = contributors
        }
        contributors.add(Contributor(name!!, contributions))
    }

    /**
     * 模拟常规的 Call 回调接口。
     */
    override fun contributorsCall(owner: String, repo: String): Call<List<Contributor>> {
        var response: List<Contributor>? = emptyList()
        // 1. 数据查询阶段：从本地双层 Map 中检索对应的贡献者列表
        val repoContributors: Map<String, MutableList<Contributor>>? = ownerRepoContributors[owner]
        if (repoContributors != null) {
            val contributors: List<Contributor>? = repoContributors[repo]
            if (contributors != null) {
                response = contributors
            }
        }
        // 2. 行为代理阶段：将查询结果交付给代理对象
        // delegate.returningResponse(response) 建立了一个预设响应。随后的 .contributorsCall(owner, repo) 并非递归调用自身，
        // 而是调用由 Delegate 在内部生成的真实 Proxy 对象。代理对象会施加网络延迟，最后返回 Call<List<Contributor>>。
        return delegate.returningResponse(response).contributorsCall(owner, repo)
    }

    /**
     * 模拟 Kotlin 协程挂起函数接口。
     */
    override suspend fun contributors(owner: String, repo: String): List<Contributor> {
        var response: List<Contributor>? = emptyList()
        val repoContributors: Map<String, MutableList<Contributor>>? = ownerRepoContributors[owner]
        if (repoContributors != null) {
            val contributors: List<Contributor>? = repoContributors[repo]
            if (contributors != null) {
                response = contributors
            }
        }
        // 代理机制同样适用于挂起函数。代理对象会在内部通过 delay() 模拟网络耗时，随后恢复（Resume）并返回解析后的数据。
        return delegate.returningResponse(response).contributors(owner, repo)
    }

    /**
     * 模拟 CompletableFuture 接口（适用于 Java 8+ 异步编程模型）。
     */
    override fun contributorsFuture(owner: String, repo: String): CompletableFuture<List<Contributor>> {
        var response: List<Contributor>? = emptyList()
        val repoContributors: Map<String, MutableList<Contributor>>? = ownerRepoContributors[owner]
        if (repoContributors != null) {
            val contributors: List<Contributor>? = repoContributors[repo]
            if (contributors != null) {
                response = contributors
            }
        }
        return delegate.returningResponse(response).contributorsFuture(owner, repo)
    }

    /**
     * 模拟 RxJava3 Single 接口。
     */
    override fun contributorsRx(owner: String, repo: String): Single<List<Contributor>> {
        var response: List<Contributor>? = emptyList()
        val repoContributors: Map<String, MutableList<Contributor>>? = ownerRepoContributors[owner]
        if (repoContributors != null) {
            val contributors: List<Contributor>? = repoContributors[repo]
            if (contributors != null) {
                response = contributors
            }
        }
        return delegate.returningResponse(response).contributorsRx(owner, repo)
    }
}
