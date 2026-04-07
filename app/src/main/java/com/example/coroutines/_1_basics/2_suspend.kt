package com.example.coroutines._1_basics

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.coroutines.R
import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SuspendActivity : ComponentActivity() {
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_1)
        infoTextView = findViewById(R.id.infoTextView)

        // callbackStyle()
        coroutinesStyle()
    }

    private fun coroutinesStyle() = CoroutineScope(Dispatchers.Main).launch {
        val retrofitContributors = gitHub.contributors("square", "retrofit")
        showContributors(retrofitContributors)
        val okhttpContributors = gitHub.contributors("square", "okhttp")
        showContributors(okhttpContributors)
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

    private fun showContributors(contributors: List<Contributor>) = contributors
        .map { "${it.login} (${it.contributions})" }
        .reduce { acc, s -> "$acc\n$s" }
        .let { infoTextView.text = it }
}
