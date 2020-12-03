package com.epam.drill.test.agent.instrumentation

import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.test.agent.instrumentation.http.apache.*
import com.epam.drill.test.agent.instrumentation.http.java.*
import com.epam.drill.test.agent.instrumentation.http.ok.*
import com.epam.drill.test.agent.instrumentation.http.selenium.*
import javassist.*
import java.io.File
import java.security.ProtectionDomain
import java.util.*
import java.util.jar.JarFile

@Kni
actual object StrategyManager {
    private val logger = Logging.logger(StrategyManager::class.java.name)

    internal var allStrategies: MutableMap<String, MutableSet<Strategy>> = mutableMapOf()
    private var strategies: MutableSet<Strategy> = HashSet()
    private var systemStrategies: MutableSet<Strategy> = HashSet()

    init {
        systemStrategies.add(OkHttpClient())
        systemStrategies.add(ApacheClient())
        systemStrategies.add(JavaHttpUrlConnection())
        systemStrategies.add(Selenium())
    }

    actual fun initialize(rawFrameworkPlugins: String) {
        hotLoad()
        val plugins = rawFrameworkPlugins.split(";".toRegex()).toTypedArray()
        strategies.addAll(allStrategies.filterKeys { plugins.contains(it) }.values.flatten())
        if (strategies.isEmpty()) {
            strategies.addAll(allStrategies.values.flatten())
        }
        strategies.addAll(systemStrategies)
        logger.debug { "Added strategies: ${strategies.map { it::class.simpleName }.joinToString()}" }
    }

    private fun hotLoad() {
        val pack = this::class.java.`package`.name.replace(".", "/")
        val removeSuffix = this::class.java.getResource("/$pack").file
            .removePrefix("file:")
            .removeSuffix("!/$pack")
        JarFile(File(removeSuffix)).use {
            it.entries().iterator().forEach {
                val name = it.name
                if (name.startsWith(pack) && name.endsWith(".class")) {
                    val replace = name.replace("/", ".").removeSuffix(".class")
                    runCatching { Class.forName(replace) }.onFailure { throwable ->
                        logger.error(throwable) { "Error while loading $replace class" }
                    }
                }
            }
        }
    }

    internal fun process(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        for (strategy in strategies) {
            if (strategy.permit(ctClass)) return strategy.instrument(ctClass,pool, classLoader, protectionDomain)
        }
        return null
    }
}
