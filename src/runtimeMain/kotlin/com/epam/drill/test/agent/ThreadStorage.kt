package com.epam.drill.test.agent

const val TEST_NAME_HEADER = "drill-test-name"
const val SESSION_ID_HEADER = "drill-session-id"

val TEST_NAME_VALUE_CALC_LINE = "((String)${ThreadStorage::class.qualifiedName}.INSTANCE.getStorage().get())"
val TEST_NAME_CALC_LINE = "\"$TEST_NAME_HEADER\", $TEST_NAME_VALUE_CALC_LINE"
val SESSION_ID_VALUE_CALC_LINE = "${ThreadStorage::class.qualifiedName}.INSTANCE.${ThreadStorage::sessionId.name}()"
val SESSION_ID_CALC_LINE = "\"$SESSION_ID_HEADER\", $SESSION_ID_VALUE_CALC_LINE"
val IF_CONDITION = "$TEST_NAME_VALUE_CALC_LINE != null && $SESSION_ID_VALUE_CALC_LINE != null"

object ThreadStorage {
    val storage = TTL()

    @Suppress("unused")
    fun memorizeTestName(testName: String?) {
        storage.set(testName)
        memorizeTestNameNative(testName)
    }

    private external fun memorizeTestNameNative(testName: String?)

    @Suppress("unused")
    external fun sessionId(): String?

    @Suppress("unused")
    external fun proxyUrl(): String?
}

@Suppress("RedundantOverride")
class TTL : InheritableThreadLocal<String>() {
    override fun set(value: String?) {
        super.set(value)
    }

    override fun get(): String? {
        return super.get()
    }
}
