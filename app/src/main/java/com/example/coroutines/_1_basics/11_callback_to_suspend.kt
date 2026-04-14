package com.example.coroutines._1_basics

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.coroutines.R
import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @author runningpig66
 * @date 2026-04-07
 * @time 2:03
 */
class CallbackToSuspendActivity : ComponentActivity() {
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_1)
        infoTextView = findViewById(R.id.infoTextView)

        val job = lifecycleScope.launch {
            try {
                val contributors = callbackToCancellableSuspend()
                showContributors(contributors)
            } catch (e: Exception) {
                infoTextView.text = e.message
            }
        }

        // lifecycleScope.launch {
        //     suspendCancellableCoroutine {
        //
        //     }
        // }

        // val job = lifecycleScope.launch {
        //     println("Coroutine cancel: 1")
        //     delay(500) // delay() 是挂起函数，内部会响应并检查协程的取消状态（协作式取消）。
        //     // Thread.sleep(500) // Thread.sleep() 则是物理阻塞线程，会完全无视外层的 job.cancel() 信号。
        //     println("Coroutine cancel: 2")
        // }

        lifecycleScope.launch {
            delay(200)
            job.cancel()
        }
    }

    /* suspendCoroutine 是最基础的回调转挂起工具，协程在此挂起后只能通过 resume 或 resumeWithException 唤醒，完全无视外部取消信号。
       这意味着即使协程作用域被取消，它仍会死等回调结果，容易造成永久挂起和内存泄漏，仅适用于无需响应取消的极简场景。*/
    private suspend fun callbackToSuspend() = suspendCoroutine {
        gitHub.contributorsCall("square", "retrofit")
            .enqueue(object : Callback<List<Contributor>> {
                override fun onResponse(
                    call: Call<List<Contributor>>, response: Response<List<Contributor>>
                ) {
                    it.resume(response.body()!!)
                }

                override fun onFailure(call: Call<List<Contributor>>, t: Throwable) {
                    it.resumeWithException(t)
                }
            })
    }

    /* suspendCancellableCoroutine 在 suspendCoroutine 的基础上加入了取消感知能力。一旦检测到协程被取消，它会自动抛出 CancellationException 来提前结束挂起，
       并允许通过 invokeOnCancellation 注册清理逻辑。这是官方推荐的回调桥接方案，能确保异步任务与结构化并发的取消机制协同工作。*/
    private suspend fun callbackToCancellableSuspend() = suspendCancellableCoroutine {
        // 1. 抽离并保存真实的底层网络请求句柄（Call 对象）
        val call = gitHub.contributorsCall("square", "retrofit")
        // 2. 注册协程取消时的清理回调。当父级 Job 取消导致协程进入终止状态时，系统会同步回调此代码块。
        it.invokeOnCancellation {
            call.cancel() // 显式触发底层的物理取消机制，强行释放 OkHttp 线程池中的该条 TCP 连接
        }
        // 3. 正常发起异步请求
        gitHub.contributorsCall("square", "retrofit")
            .enqueue(object : Callback<List<Contributor>> {
                override fun onResponse(
                    call: Call<List<Contributor>>, response: Response<List<Contributor>>
                ) {
                    it.resume(response.body()!!) // 恢复状态机，交回执行流
                }

                override fun onFailure(call: Call<List<Contributor>>, t: Throwable) {
                    it.resumeWithException(t) // 恢复状态机并抛出异常
                }
            })
    }

    private fun callbackStyle() {
        gitHub.contributorsCall("square", "retrofit")
            .enqueue(object : Callback<List<Contributor>> {
                override fun onResponse(
                    call: Call<List<Contributor>>, response: Response<List<Contributor>>
                ) {
                    showContributors(response.body()!!)
                }

                override fun onFailure(call: Call<List<Contributor>>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun showContributors(contributors: List<Contributor>) = contributors
        .map { "${it.login} (${it.contributions})" }
        .reduce { acc, s -> "$acc\n$s" }
        .let { infoTextView.text = it }
}
