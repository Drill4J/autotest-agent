package com.epam.drill.test.agent.instrumentation.http.selenium

object WebDriverThreadStorage {
    private val remoteWebDriver = InheritableThreadLocal<Any>()

    fun set(obj: Any) = remoteWebDriver.set(obj)

    fun addCookies() {
        runCatching {
            remoteWebDriver.get()?.let {
                it.javaClass.getMethod("addDrillCookies").invoke(it)
            }
        }.onFailure { println("Can't get method") }
    }
}
