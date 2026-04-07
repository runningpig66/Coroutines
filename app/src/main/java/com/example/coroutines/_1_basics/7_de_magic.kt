package com.example.coroutines._1_basics

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.coroutines.R
import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import kotlinx.coroutines.launch

/**
 * @author runningpig66
 * @date 2026-04-04
 * @time 21:11
 */
class DeMagicActivity : ComponentActivity() {
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_1)
        infoTextView = findViewById(R.id.infoTextView)

        lifecycleScope.launch {
            // switchToBackground {
            //     gitHub.contributors("square", "retrofit")
            //     switchToMain {
            //         showContributors(contributors)
            //     }
            // }
        }
    }

    private fun coroutinesStyle() = lifecycleScope.launch {
        val contributors = gitHub.contributors("square", "retrofit")
        showContributors(contributors)
    }

    private fun showContributors(contributors: List<Contributor>) = contributors
        .map { "${it.login} (${it.contributions})" }
        .reduce { acc, s -> "$acc\n$s" }
        .let { infoTextView.text = it }
}
