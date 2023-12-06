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
package com.epam.drill.test.agent.configuration.jvm

import com.epam.drill.jvmapi.callNativeBooleanMethod
import com.epam.drill.jvmapi.callNativeStringMethod
import com.epam.drill.jvmapi.gen.JNIEnv
import com.epam.drill.jvmapi.gen.jobject
import com.epam.drill.test.agent.configuration.AgentConfig

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_proxyUrl")
fun proxyUrl(env: JNIEnv, thiz: jobject) = callNativeStringMethod(env, thiz, AgentConfig::proxyUrl)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_adminAddress")
fun adminAddress(env: JNIEnv, thiz: jobject) = callNativeStringMethod(env, thiz, AgentConfig::adminAddress)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_agentId")
fun agentId(env: JNIEnv, thiz: jobject) = callNativeStringMethod(env, thiz, AgentConfig::agentId)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_adminUserName")
fun adminUserName(env: JNIEnv, thiz: jobject) = callNativeStringMethod(env, thiz, AgentConfig::adminUserName)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_adminPassword")
fun adminPassword(env: JNIEnv, thiz: jobject) = callNativeStringMethod(env, thiz, AgentConfig::adminPassword)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_groupId")
fun groupId(env: JNIEnv, thiz: jobject) = callNativeStringMethod(env, thiz, AgentConfig::groupId)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_devToolsProxyAddress")
fun devToolsProxyAddress(env: JNIEnv, thiz: jobject) =
    callNativeStringMethod(env, thiz, AgentConfig::devToolsProxyAddress)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_withJsCoverage")
fun withJsCoverage(env: JNIEnv, thiz: jobject): UByte =
    callNativeBooleanMethod(env, thiz, AgentConfig::withJsCoverage)!!

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_launchType")
fun launchType(env: JNIEnv, thiz: jobject) = callNativeStringMethod(env, thiz, AgentConfig::launchType)

@Suppress("UNUSED")
@CName("Java_com_epam_drill_test_agent_configuration_AgentConfig_devtoolsAddressReplaceLocalhost")
fun devtoolsAddressReplaceLocalhost(env: JNIEnv, thiz: jobject) =
    callNativeStringMethod(env, thiz, AgentConfig::devtoolsAddressReplaceLocalhost)
