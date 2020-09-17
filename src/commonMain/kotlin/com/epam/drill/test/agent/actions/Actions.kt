package com.epam.drill.test.agent.actions

import kotlinx.serialization.*

enum class Actions {
    START,
    STOP
}

@Serializable
data class StartSession(val type: String = Actions.START.name, val payload: StartSessionPayload = StartSessionPayload())

@Serializable
data class StartSessionPayload(
    val sessionId: String = "",
    val testType: String = "AUTO",
    val testName: String? = null,
    val isRealtime: Boolean = false,
    val isGlobal: Boolean = false
)

@Serializable
data class StartSessionResponse(val code: Int, val data: StartSessionResponseData)

@Serializable
data class StartSessionResponseData(val payload: StartSessionPayload)

@Serializable
data class StopSession(val type: String = Actions.STOP.name, val payload: StopSessionPayload)

fun stopAction(sessionId: String, testRun: TestRun? = null) = StopSession(
    payload = StopSessionPayload(sessionId, testRun)
)


@Serializable
data class StopSessionPayload(
    val sessionId: String,
    val testRun: TestRun? = null
)

@Serializable
data class TestRun(
    val name: String = "",
    val startedAt: Long,
    val finishedAt: Long,
    val tests: List<TestInfo>
)

@Serializable
data class TestInfo(
    val name: String,
    val result: TestResult,
    val startedAt: Long = 0,
    val finishedAt: Long = 0
)

enum class TestResult {
    PASSED,
    FAILED,
    SKIPPED,
    ERROR;

    companion object {
        fun getByMapping(value: String): TestResult {
            if (value == "SUCCESSFUL") return PASSED
            return valueOf(value)
        }
    }
}