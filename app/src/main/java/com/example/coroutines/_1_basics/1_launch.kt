package com.example.coroutines._1_basics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.coroutines.ui.theme.CoroutinesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class LaunchCoroutineActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoroutinesTheme {
            }
        }

        thread {
        }

        println("Main thread: ${Thread.currentThread().name}")
        val executor = Executors.newCachedThreadPool()
        executor.execute {
            println("Executor thread: ${Thread.currentThread().name}")
        }

        val context = newFixedThreadPoolContext(20, "MyThread")
        val context1 = newSingleThreadContext("MyThread")
        val scope = CoroutineScope(context)
        context.close()
        context1.close()
        scope.launch {
            println("Coroutine thread: ${Thread.currentThread().name}")
        }
        scope.launch {
            println("Coroutine thread: ${Thread.currentThread().name}")
        }
    }
}
