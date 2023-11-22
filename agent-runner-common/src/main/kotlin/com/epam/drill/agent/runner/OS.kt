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

import java.util.*

object OS {
    const val FAMILY_MAC = "mac"
    const val FAMILY_WINDOWS = "windows"
    const val FAMILY_UNIX = "unix"

    private var OS_NAME: String = System.getProperty("os.name").lowercase(Locale.ENGLISH)


    fun isFamily(st: String): Boolean {
        return OS_NAME.contains(st)
    }
}


val presetName: String =
    when {
        OS.isFamily(OS.FAMILY_MAC) -> "macosX64"
        OS.isFamily(OS.FAMILY_WINDOWS) -> "mingwX64"
        else -> "linuxX64"
    }

val dynamicLibExtensions = setOf(
    "dylib",
    "so",
    "dll",
    "wasm"
)
