package com.epam.drill.test.agent.instrumentation.testing

import com.epam.drill.logger.Logging
import com.epam.drill.test.agent.ThreadStorage

object TestListener {
    val _ml = mutableMapOf<String, MutableMap<String, Any>?>()

    private fun addTestInfo(testId: String, vararg vals: Pair<String, Any>) {
        vals.forEach {
            val (paramName, value) = it
            val meta = _ml[testId] ?: _ml.run {
                val mmo = mutableMapOf<String, Any>()
                put(testId, mmo)
                mmo
            }
            meta[paramName] = value
        }
    }

    private val logger = Logging.logger(TestListener::class.java.simpleName)
    fun testStarted(test: String?) {
        test?.let {
            logger.trace { "Test: $it STARTED" }
            addTestInfo(test, "startTime" to System.currentTimeMillis())
            ThreadStorage.memorizeTestName(it)
        }
    }

    fun testFinished(test: String?, status: String) {
        test?.let {
            if (_ml[test] != null) {
                addTestInfo(
                    test,
                    "stopTime" to System.currentTimeMillis(),
                    "status" to status
                )
                logger.trace { "Test: $test FINISHED. Result:$status" }
            }
        }
    }
}