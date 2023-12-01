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

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File
import java.util.*

abstract class AgentMojo : AbstractMojo() {

    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    lateinit var project: MavenProject

    abstract val config: Configuration

    override fun execute() {
        val name = "argLine"
        prepareAgent(config)
        val projectProperties: Properties = project.properties
        projectProperties.forEach { x, y ->
            println("$x: $y")
        }
        val oldValue = projectProperties.getProperty(name)
        val newValue: String = "${oldValue ?: ""} ${config.toJvmArgs().joinToString(separator = " ")}".trim()
        log.info("$name set to $newValue")
        projectProperties.setProperty(name, newValue)
    }

    private fun prepareAgent(ac: Configuration) {
        if (ac.agentPath == null && ac.runtimePath == null) {
            val dir = File(project.build.outputDirectory).parentFile
                .resolve("drill")
                .apply { mkdirs() }
            if (ac.version == "+") {
                ac.version = AgentLoader.getVersion(config)
                println("version is ${ac.version}")
            }
            val extractedDir = dir.resolve("$presetName-${ac.version}")
            if (!extractedDir.exists()) {
                AgentLoader.downloadAgent(ac, dir)
            }
            ac.agentPath = extractedDir.listFiles()?.first { file ->
                dynamicLibExtensions.any { it == file.extension }
            }
            ac.runtimePath = extractedDir
        }
    }
}
