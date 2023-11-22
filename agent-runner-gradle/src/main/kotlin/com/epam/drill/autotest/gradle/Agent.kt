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
@file:Suppress("unused")

package com.epam.drill.autotest.gradle

import com.epam.drill.agent.runner.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.gradle.process.JavaForkOptions
import kotlin.reflect.KClass

private const val EXTENSION_NAME = "drill"

abstract class Agent : Plugin<Project> {

    abstract val extensionClass: KClass<out Configuration>
    abstract val taskType: Set<KClass<out JavaForkOptions>>

    private fun TaskContainer.configure() {
        filter { task -> taskType.any { it.java.isInstance(task) } }.map { it as JavaForkOptions }.forEach {
            println("Task ${(it as Task).name} is modified by Drill")
            with(it) {
                (it as Task).doFirst {
                    with(project) {
                        prepare()
                        val toJvmArgs: List<String> = config.toJvmArgs()
                        println("Drill agent line: $toJvmArgs")
                        it.setJvmArgs(toJvmArgs.asIterable())
                    }
                }
            }
        }
    }

    override fun apply(target: Project) = target.run {
        extensions.create(EXTENSION_NAME, extensionClass.java)

        afterEvaluate {
            with(tasks) {
                configure()
            }
        }
    }

    private fun Project.prepare() {
        if (config.agentPath == null && config.runtimePath == null) {
            val drillDist = rootProject.buildDir.resolve("drill").apply { mkdirs() }
            if (config.version == "+") {
                config.version = AgentLoader.getVersion(config)
                println("version is ${config.version}")
            }
            val extractedDir = drillDist.resolve("$presetName-${config.version}")
            if (!extractedDir.exists() || extractedDir.listFiles()!!.isEmpty()) {
                AgentLoader.downloadAgent(config, drillDist)
            }
            config.runtimePath = extractedDir
            config.agentPath = extractedDir.listFiles()?.first { file ->
                dynamicLibExtensions.any { it == file.extension }
            } ?: throw GradleException("can't find agent")
        }
    }
}

private val Project.config: Configuration
    get() = extensions.findByName(EXTENSION_NAME) as Configuration

