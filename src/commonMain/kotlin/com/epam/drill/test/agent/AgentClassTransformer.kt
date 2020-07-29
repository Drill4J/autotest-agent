package com.epam.drill.test.agent

expect object AgentClassTransformer {
    fun transform(className: String, classBytes: ByteArray): ByteArray?
}