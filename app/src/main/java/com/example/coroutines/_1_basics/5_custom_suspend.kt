 package com.example.coroutines._1_basics

import com.example.coroutines.common.Contributor
import com.example.coroutines.common.gitHub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author runningpig66
 * @date 2026-04-04
 * @time 17:40
 */
suspend fun getRetrofitContributors(): List<Contributor> {
    return gitHub.contributors("square", "retrofit")
}

suspend fun customSuspendFun() {
    // suspend fun <T> withContext
    withContext(Dispatchers.IO) {
        // Do something
    }
}
