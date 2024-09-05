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
package com.epam.drill.test.agent.runner

import java.io.File

abstract class Configuration {
    var apiUrl: String? = null
    var apiKey: String? = null
    var groupId: String? = null
    var appId: String? = null

    var jsAgentBuildVersion: String? = null
    var jsAgentId: String? = null
    var version: String = "+"
    var agentPath: File? = null
    var runtimePath: File? = null
    var logLevel: String = "ERROR"
    var logFile: File? = null
    var additionalParams: Map<String, String>? = null
    var jvmArgs: Set<String> = mutableSetOf()
    var directUrlToZip: String? = null
    var directLocalPathToZip: String? = null
    val artifactId: String = "agent"
    abstract val repositoryName: String
    fun toJvmArgs(): List<String> {
        val args = mutableMapOf<String, Any?>()
        args["drillInstallationDir"] = runtimePath
        args[Configuration::apiUrl.name] = apiUrl
        apiKey?.let { args[Configuration::apiKey.name] = it }
        args[Configuration::groupId.name] = groupId
        args[Configuration::appId.name] = appId
        args[Configuration::logLevel.name] = logLevel
        args[Configuration::directUrlToZip.name] = directUrlToZip
        args[Configuration::directLocalPathToZip.name] = directLocalPathToZip
        jsAgentBuildVersion?.let { args[Configuration::jsAgentBuildVersion.name] = it }
        jsAgentId?.let { args[Configuration::jsAgentId.name] = it }
        logFile?.let { args[Configuration::logFile.name] = it.absolutePath }
        additionalParams?.let { args.putAll(it) }
        args.putAll(jvmArgs())

        return jvmArgs.toList() + ("-agentpath:${agentPath}=" + args.map { (k, v) -> "$k=$v" }.joinToString(","))
    }

    abstract fun jvmArgs(): Map<String, String>
}
