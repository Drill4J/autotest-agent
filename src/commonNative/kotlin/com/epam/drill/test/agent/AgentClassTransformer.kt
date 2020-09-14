package com.epam.drill.test.agent

actual object AgentClassTransformer {
    actual fun transform(className: String, classBytes: ByteArray, loader:Any?, protectionDomain: Any?): ByteArray? {
       return AgentClassTransformerStub.transform(className, classBytes, loader, protectionDomain)
    }
}