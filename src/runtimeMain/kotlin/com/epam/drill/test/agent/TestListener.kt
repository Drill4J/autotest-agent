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

    private val _testInfo = atomic(persistentHashMapOf<String, PersistentMap<String, Any>>())

    private fun addTestInfo(
        testId: String,
        vararg vals: Pair<String, Any>
    ) = _testInfo.update { testProperties ->
        val currentInfo = testProperties[testId] ?: persistentHashMapOf()
        testProperties.put(testId, currentInfo + vals)
    }

    fun testStarted(test: String?) {
        test?.let {
            if (it !in _testInfo.value) {
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
            } else if (isFinalizeTestState(it)) {
                logger.trace { "Test: $it was repeated. Change status to UNKNOWN" }
                addTestInfo(
                    it,
                    TestInfo::result.name to TestResult.UNKNOWN,
                    TestInfo::startedAt.name to System.currentTimeMillis()
                )
            }
        }
    }

    fun testFinished(test: String?, status: String) {
        logger.trace { "Test: $test is finishing with status $status..." }
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
            logger.info { "Test: $test FINISHED. Result:$status" }
        }
        ThreadStorage.stopSession()
    }


    private fun isFinalizeTestState(test: String?): Boolean = !isNotFinalizeTestState(test)

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
                TestInfo::startedAt.name to 0L,
                TestInfo::finishedAt.name to 0L,
                TestInfo::result.name to TestResult.SKIPPED
            )
            logger.trace { "Test: $test FINISHED. Result:${TestResult.SKIPPED.name}" }
        }
    }

    actual fun getData(): String {
        logger.trace { "testInfo: ${_testInfo.value.values}" }
        val finished = runCatching {
            _testInfo.value.filterKeys { test -> isFinalizeTestState(test) }.values.map { properties ->
                TestInfo.serializer().deserialize(PropertyDecoder(properties))
            }
        }.getOrElse {
            logger.error(it) { "Can't get tests list. Reason:" }
            emptyList()
        }
        _testInfo.update { tests -> tests - finished.map { it.name } }

        return TestRun.serializer() stringify TestRun(
            startedAt = finished.filter { it.startedAt != 0L }.minByOrNull { it.startedAt }?.startedAt ?: 0,
            finishedAt = finished.maxByOrNull { it.finishedAt }?.finishedAt ?: 0,
            tests = finished
        )
    }

    actual fun reset() {
        _testInfo.update { it.clear() }
    }
}
