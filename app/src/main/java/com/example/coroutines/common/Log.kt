package com.example.coroutines.common

/**
 * @author runningpig66
 * @date 2026-04-20
 * @time 17:07
 */
private var zeroTime = System.currentTimeMillis()
fun log(message: Any?) =
    println("${System.currentTimeMillis() - zeroTime} " + "[${Thread.currentThread().name}] $message")
