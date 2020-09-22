package com.epam.drill.test.agent


actual object TestListener {
    actual fun getData(): String {
        return TestListenerStub.getData()
    }

    actual fun reset() {
        TestListenerStub.reset()
    }
}