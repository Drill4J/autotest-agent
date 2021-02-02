package com.epam.drill.test.agent.js

import com.epam.drill.logger.*
import com.epam.drill.test.agent.*
import okhttp3.*
import java.io.*

const val JS_EXTENSION_NAME = "drill4j-autotesting-extension.crx"

class ExtensionLoader {
    private val logger = Logging.logger { ExtensionLoader::class.java }
    private val storagePath = File(System.getProperty("java.io.tmpdir"))

    private val baseUrl = "https://oss.jfrog.org/artifactory"
    private val repo = "oss-release-local"
    private val artifactName = "auto-testing-browser-extension"

    fun loadExtension() {
        logger.info { "Loading extension..." }
        OkHttpClient.Builder().build().load()
    }

    private fun OkHttpClient.load() {
        val targetFile = storagePath.resolve(JS_EXTENSION_NAME)
        if (!targetFile.exists()) {
            val version: String = getVersion() ?: ""
            logger.info { "Loading extension $version..." }
            val targetFilename = "build.crx"
            val artifactPath = "com/epam/drill/$artifactName/$version/$targetFilename"
            downloadPlugin(artifactPath, targetFile)
        } else {
            logger.info { "Extension is already loaded" }
        }
    }

    private fun OkHttpClient.getVersion(): String? = runCatching {
        when (val version = AgentConfig.extensionVersion()) {
            "", "latest" -> {
                val url = "$baseUrl/api/search/latestVersion?" +
                        "g=com.epam.drill&" +
                        "a=$artifactName&" +
                        "repos=$repo"
                val response = newCall(Request.Builder().url(url).build()).execute()
                response.body()?.string()
            }
            else -> version
        }
    }.onFailure {
        logger.warn(it) { "Failed to get the latest version. Reason:" }
    }.getOrNull()

    private fun OkHttpClient.downloadPlugin(artifactPath: String, targetFile: File) {
        runCatching {
            logger.info { "Downloading $artifactPath from artifactory..." }
            val pluginBytes: ByteArray = newCall(
                Request.Builder()
                    .url("$baseUrl/$repo/$artifactPath")
                    .build()
            ).execute().body()?.bytes() ?: byteArrayOf()

            targetFile.writeBytes(pluginBytes)
            logger.info { "Extension loaded" }
        }.onFailure { logger.warn(it) { "Failed to download extension. Reason:" } }
    }
}
