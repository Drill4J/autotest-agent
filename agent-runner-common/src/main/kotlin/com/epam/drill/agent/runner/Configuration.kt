/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.runner

import java.io.File

abstract class Configuration {
    lateinit var adminHost: String
    var agentId: String? = null
    var groupId: String? = null
    var adminPort: Int = 8080
    var version: String = "+"
    var agentPath: File? = null
    var runtimePath: File? = null
    var logLevel: LogLevels = LogLevels.ERROR
    var logFile: File? = null
    var additionalParams: Map<String, String>? = null
    var jvmArgs: Set<String> = mutableSetOf()
    val artifactId: String = "agent"
    abstract val repositoryName: String
    fun toJvmArgs(): List<String> {
        val args = mutableMapOf<String, Any?>()
        args["drillInstallationDir"] = runtimePath
        args["adminAddress"] = "$adminHost:$adminPort"
        args[Configuration::agentId.name] = agentId
        args[Configuration::logLevel.name] = logLevel.name
        groupId?.let { args[Configuration::groupId.name] = it }
        logFile?.let { args[Configuration::logFile.name] = it.absolutePath }
        additionalParams?.let { args.putAll(it) }
        args.putAll(jvmArgs())

        return jvmArgs.toList() + ("-agentpath:${agentPath}=" + args.map { (k, v) -> "$k=$v" }.joinToString(","))
    }

    abstract fun jvmArgs(): Map<String, String>
}
