package com.epam.drill.test.agent

actual object AgentClassTransformer {
    actual fun transform(className: String, classBytes: ByteArray): ByteArray? {
       return AgentClassTransformerStub.transform(className, classBytes)
    }
}