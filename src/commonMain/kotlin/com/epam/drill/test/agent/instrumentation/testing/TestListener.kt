package com.epam.drill.test.agent

expect object TestListener {
    fun getData(): String
    fun reset()
}