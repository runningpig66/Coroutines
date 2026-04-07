package com.example.coroutines._1_basics

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.coroutines.R
import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * @author runningpig66
 * @date 2026-04-04
 * @time 23:15
 */
class StructuredConcurrencyActivity : ComponentActivity() {
    private lateinit var infoTextView: TextView
    private var disposable: Disposable? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_1)
        infoTextView = findViewById(R.id.infoTextView)

        // Structured concurrency
        // 内存泄露（泄漏） memory leak
        // GC（垃圾回收器） garbage collector
        // static
        // JNI
        // Android 内存泄露 弱引用
        // RxJava

        // disposable = rxStyle()
        job = coroutinesStyle()
        coroutinesStyle()
    }

    override fun onDestroy() {
        super.onDestroy()
        // disposable?.dispose()
        // job?.cancel()
        lifecycleScope.cancel()
    }

    private fun callbackStyle() {
        gitHub.contributorsCall("square", "retrofit")
            .enqueue(object : Callback<List<Contributor>> {
                override fun onResponse(
                    call: Call<List<Contributor>>, response: Response<List<Contributor>>
                ) {
                    showContributors(response.body()!!)
                    // 二次请求
                    gitHub.contributorsCall("square", "okhttp")
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

                override fun onFailure(call: Call<List<Contributor>>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun coroutinesStyle() = lifecycleScope.launch {
        launch {

        }
        val contributors = gitHub.contributors("square", "retrofit")
        val filtered = contributors.filter { it.contributions > 10 }
        showContributors(filtered)
    }

    private fun rxStyle() = gitHub.contributorsRx("square", "retrofit")
        .map { list -> list.filter { it.contributions > 10 } }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(::showContributors)

    private fun showContributors(contributors: List<Contributor>) = contributors
        .map { "${it.login} (${it.contributions})" }
        .reduce { acc, s -> "$acc\n$s" }
        .let { infoTextView.text = it }
}
