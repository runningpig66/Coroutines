package com.example.coroutines._1_basics

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.coroutines.R
import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * @author runningpig66
 * @date 2026-04-05
 * @time 3:54
 */
class ParallelActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_1)
        infoTextView = findViewById(R.id.infoTextView)

        // 结构化并发：使用 async 并发派发两个带有返回值的子任务，底层 I/O 并行处理，最后通过 await 汇合数据结果。
        lifecycleScope.launch {
            coroutineScope {
                val deferred1 = async {
                    gitHub.contributors("square", "retrofit")
                }
                val deferred2 = async {
                    gitHub.contributors("square", "okhttp")
                }
                showContributors(deferred1.await() + deferred2.await())
            }
        }
        lifecycleScope.launch {
            val initJob = launch {
                // init() // 初始化工作
            }
            // 不依赖初始化的工作
            val contributors1 = gitHub.contributors("square", "retrofit")
            // 同步点：挂起当前协程，直到初始化子任务完成。
            initJob.join()
            // 后续依赖初始化完成才能继续的工作
            // processData()
        }
    }

    // 单协程串行：连续调用挂起函数，当前协程会在每次网络 I/O 时挂起等待，严格按顺序执行。
    private fun coroutinesStyle() = lifecycleScope.launch {
        val contributors1 = gitHub.contributors("square", "retrofit")
        val contributors2 = gitHub.contributors("square", "okhttp")
        showContributors(contributors1 + contributors2)
    }

    private fun completableFutureStyleMerge() {
        val future1 = gitHub.contributorsFuture("square", "retrofit")
        val future2 = gitHub.contributorsFuture("square", "okhttp")
        future1.thenCombine(future2) { contributors1, contributors2 ->
            contributors1 + contributors2
        }.thenAccept { mergedContributors ->
            handler.post {
                showContributors(mergedContributors)
            }
        }
    }

    private fun showContributors(contributors: List<Contributor>) = contributors
        .map { "${it.login} (${it.contributions})" }
        .reduce { acc, s -> "$acc\n$s" }
        .let { infoTextView.text = it }
}
