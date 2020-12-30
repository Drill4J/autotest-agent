package com.epam.drill.test.agent

import com.epam.drill.kni.Kni
import com.epam.drill.logger.Logging
import com.epam.drill.test.agent.actions.TestInfo
import com.epam.drill.test.agent.actions.TestResult
import com.epam.drill.test.agent.actions.TestRun
import com.epam.drill.test.agent.config.stringify
import java.util.concurrent.ConcurrentHashMap

@Kni
actual object TestListener {

    private val _ml = ConcurrentHashMap<String, MutableMap<String, Any>?>()

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
            addTestInfo(
                test,
                TestInfo::name.name to test,
                TestInfo::startedAt.name to System.currentTimeMillis()
            )
            ThreadStorage.startSession(it)
            ThreadStorage.memorizeTestName(it)
        }
    }


    fun testFinished(test: String?, status: String) {
        test?.let {
            addTestInfo(
                test,
                TestInfo::finishedAt.name to System.currentTimeMillis(),
                TestInfo::result.name to TestResult.getByMapping(status)
            )
            logger.trace { "Test: $test FINISHED. Result:$status" }
        }
        ThreadStorage.stopSession()
    }

    fun testIgnored(test: String?) {
        test?.let {
            addTestInfo(
                test,
                TestInfo::name.name to test,
                TestInfo::result.name to TestResult.SKIPPED
            )
            logger.trace { "Test: $test FINISHED. Result:${TestResult.SKIPPED.name}" }
        }
    }

    actual fun getData(): String {
        val map = kotlin.runCatching {
            _ml.values.filterNotNull().map { u ->
                TestInfo.serializer().deserialize(PropertyDecoder(u))
            }
        }.getOrDefault(emptyList())


        return TestRun.serializer() stringify TestRun(
            "",
            map.filter { it.startedAt != 0L }.minBy { it.startedAt }?.startedAt ?: 0,
            map.maxBy { it.finishedAt }?.finishedAt ?: 0,
            map
        )
    }

    actual fun reset() {
//        _ml.clear()
    }
}
