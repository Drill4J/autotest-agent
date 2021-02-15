package com.epam.drill.test.agent

import com.epam.drill.test.agent.instrumentation.http.selenium.*

const val TEST_NAME_HEADER = "drill-test-name"
const val SESSION_ID_HEADER = "drill-session-id"

val TEST_NAME_VALUE_CALC_LINE = "((String)${ThreadStorage::class.qualifiedName}.INSTANCE.getStorage().get())"
val TEST_NAME_CALC_LINE = "\"$TEST_NAME_HEADER\", $TEST_NAME_VALUE_CALC_LINE"
val SESSION_ID_VALUE_CALC_LINE = "${ThreadStorage::class.qualifiedName}.INSTANCE.${ThreadStorage::sessionId.name}()"
val SESSION_ID_CALC_LINE = "\"$SESSION_ID_HEADER\", $SESSION_ID_VALUE_CALC_LINE"
val IF_CONDITION = "$TEST_NAME_VALUE_CALC_LINE != null && $SESSION_ID_VALUE_CALC_LINE != null"
val IS_HEADER_ADDED = "${DevToolsClientThreadStorage::class.java.name}.INSTANCE.${DevToolsClientThreadStorage::isHeadersAdded.name}()"

@Suppress("RedundantOverride")
class TTL : InheritableThreadLocal<String>() {
    override fun set(value: String?) {
        super.set(value)
    }

    override fun get(): String? {
        return super.get()
    }
}
