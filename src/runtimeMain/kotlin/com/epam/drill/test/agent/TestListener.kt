package com.epam.drill.test.agent

import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.test.agent.actions.*
import com.epam.drill.test.agent.config.*
import com.epam.drill.test.agent.instrumentation.http.selenium.*
import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*

@Kni
actual object TestListener {

    private val logger = Logging.logger(TestListener::class.java.simpleName)

    private val _testInfo = atomic(persistentHashMapOf<String, PersistentMap<String, Any>?>())

    private fun addTestInfo(
        testId: String,
        vararg vals: Pair<String, Any>
    ) = _testInfo.update { testProperties ->
        val currentInfo = testProperties[testId] ?: persistentHashMapOf()
        testProperties.put(testId, currentInfo + vals)
    }


    fun testStarted(test: String?) {
        test?.takeIf { it !in _testInfo.value }?.let {
            logger.info { "Test: $it STARTED" }
            addTestInfo(
                test,
                TestInfo::name.name to test,
                TestInfo::startedAt.name to System.currentTimeMillis()
            )
            DevToolsClientThreadStorage.getDevTool()?.apply {
                logger.debug { "Thread id=${Thread.currentThread().id}, devTool instance=${this}, Test = $it" }
                addHeaders(mapOf(TEST_NAME_HEADER to it, SESSION_ID_HEADER to (ThreadStorage.sessionId() ?: "")))
            }
            ThreadStorage.startSession(it)
            ThreadStorage.memorizeTestName(it)
        }
    }

    fun testFinished(test: String?, status: String) {
        if (isNotFinalizeTestState(test)) {
            addTestResult(test, status)
        }
    }

    private fun addTestResult(test: String?, status: String) {
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


    private fun isNotFinalizeTestState(test: String?): Boolean = _testInfo.value[test]?.let { testProperties ->
        testProperties[TestInfo::result.name]?.let { result ->
            result as TestResult == TestResult.UNKNOWN
        }
    } ?: true


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
            _testInfo.value.values.filterNotNull().map { testProperties ->
                TestInfo.serializer().deserialize(PropertyDecoder(testProperties))
            }
        }.getOrDefault(emptyList())


        return TestRun.serializer() stringify TestRun(
            "",
            map.filter { it.startedAt != 0L }.minByOrNull { it.startedAt }?.startedAt ?: 0,
            map.maxByOrNull { it.finishedAt }?.finishedAt ?: 0,
            map
        )
    }

    actual fun reset() {
        _testInfo.update { it.clear() }
    }
}
