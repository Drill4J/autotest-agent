package com.epam.drill.test.agent.js

import com.epam.drill.logger.*
import com.epam.drill.test.agent.*
import kotlinx.serialization.json.*
import okhttp3.*
import java.io.*

class ExtensionLoader {
    private val logger = Logging.logger { ExtensionLoader::class.java }
    private val storagePath = File(System.getProperty("java.io.tmpdir"))

    private val baseUrl = "https://api.github.com"
    private val basePath = "repos"
    private val repo = "Drill4J"
    private val repositoryName = "auto-testing-browser-extension"
    private val targetFilename = "build.crx"

    fun loadExtension(): File {
        logger.info { "Loading extension..." }
        return OkHttpClient.Builder().build().load()
    }

    private fun OkHttpClient.load(): File {
        val version: String = getVersion() ?: ""
        val targetFile = storagePath.resolve("drill4j-autotesting-extension-$version.crx")
        if (!targetFile.exists()) {
            logger.info { "Loading extension $version..." }
            val artifactPath = "$repositoryName/releases/download/$version/$targetFilename"
            downloadPlugin(artifactPath, targetFile)
        } else {
            logger.info { "Extension is already loaded" }
        }
        return targetFile
    }

    private fun OkHttpClient.getVersion(): String? = runCatching {
        when (val version = AgentConfig.extensionVersion()) {
            "", "latest" -> {
                val url = "$baseUrl/$basePath/$repo/$repositoryName/releases"
                val response = newCall(Request.Builder().url(url).build()).execute()
                val releases = response.body()?.string()?.let { Json.parseToJsonElement(it) }
                releases?.jsonArray?.filterNot {
                    it.primitive("draft")?.boolean ?: false || it.primitive("prerelease")?.boolean ?: false
                }?.mapNotNull {
                    it.primitive("tag_name")?.content
                }?.first()?.replaceFirst("v", "") ?: ""
            }
            else -> version
        }
    }.onFailure {
        logger.warn(it) { "Failed to get the latest version. Reason:" }
    }.getOrNull()

    private fun JsonElement.primitive(key: String) = jsonObject[key]?.jsonPrimitive

    private fun OkHttpClient.downloadPlugin(artifactPath: String, targetFile: File) {
        runCatching {
            logger.info { "Downloading $artifactPath from artifactory..." }
            val url = "${baseUrl.replace("api.", "")}/$repo/$artifactPath"
            val pluginBytes: ByteArray? = newCall(
                Request.Builder()
                    .url(url)
                    .build()
            ).execute().takeIf { it.isSuccessful }?.body()?.bytes()

            pluginBytes?.let {
                targetFile.writeBytes(it)
                logger.info { "Extension loaded" }
            } ?: logger.error { "Can't load extension for js coverage collecting" }
        }.onFailure { logger.warn(it) { "Failed to download extension. Reason:" } }
    }
}
