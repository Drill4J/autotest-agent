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

import java.io.*
import java.net.*
import java.util.zip.*

const val GITHUB_URL = "https://api.github.com"
const val GITHUB_REPOS = "repos"
const val DRILL_OWNER = "Drill4J"

object AgentLoader {
    fun getVersion(config: Configuration): String = runCatching {
        val versionUrl = "$GITHUB_URL/$GITHUB_REPOS/$DRILL_OWNER/${config.repositoryName}" +
                "/releases?prerelease=true"
        println("Url to get version: $versionUrl")
        URL(versionUrl).readText()
            .substringAfter("\"tag_name\":\"")
            .substringBefore("\",")
            .replace("v", "")
    }.getOrNull() ?: ""

    fun downloadAgent(
        config: Configuration,
        dir: File
    ) = runCatching {
        val zipPath: File
        val directLocalPathToZip = config.directLocalPathToZip
        if (directLocalPathToZip != null) {
            zipPath = dir.resolve(directLocalPathToZip)
        } else {
            val url = config.directUrlToZip ?: getDefaultRepositoryUrl(config)
            val bytes = URL(url).openStream().readBytes()
            zipPath = dir.resolve(getArtifactName(config))
            zipPath.createNewFile()
            zipPath.writeBytes(bytes)
        }
        unzip(zipPath, dir)
    }.onFailure {
        println(
            "Failed to locate or download autotest agent zip file. Reason: ${it.message}." +
                    "Stacktrace: ${it.stackTrace.joinToString(separator = System.lineSeparator())}"
        )
    }

    private fun getDefaultRepositoryUrl(config: Configuration) =
        "${GITHUB_URL.replace("api.", "")}/$DRILL_OWNER/" +
                "${config.repositoryName}/releases/download/v" +
                "${config.version}/${getArtifactName(config)}"

    private fun getArtifactName(config: Configuration) = "${config.artifactId}-$presetName-${config.version}.zip"

    private fun unzip(file: File, dir: File) {
        ZipFile(file).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.isDirectory) {
                    File(dir, entry.name).mkdirs()
                } else {
                    zip.getInputStream(entry).use { input ->
                        File(dir, entry.name).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }
}