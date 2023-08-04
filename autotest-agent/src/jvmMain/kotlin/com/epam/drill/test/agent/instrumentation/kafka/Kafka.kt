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
package com.epam.drill.test.agent.instrumentation.kafka

import com.epam.drill.agent.instrument.*
import com.epam.drill.test.agent.*
import com.epam.drill.test.agent.instrumentation.http.*
import javassist.*
import org.objectweb.asm.*
import java.security.*

object Kafka : TransformStrategy() {

    private const val KAFKA_PRODUCER_INTERFACE = "org/apache/kafka/clients/producer/Producer"

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>) = interfaces.any { it == KAFKA_PRODUCER_INTERFACE }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        ctClass.getDeclaredMethods("send").forEach {
            it.insertBefore("""
                if ($ARE_DRILL_HEADERS_PRESENT) {
                    ${Log::class.java.name}.INSTANCE.${Log::injectHeaderLog.name}($TEST_NAME_VALUE_CALC_LINE,$SESSION_ID_VALUE_CALC_LINE);
                    $1.headers().add("$SESSION_ID_HEADER", $SESSION_ID_VALUE_CALC_LINE.getBytes());
                    $1.headers().add("$TEST_ID_HEADER", $TEST_NAME_VALUE_CALC_LINE.getBytes());
                }
            """.trimIndent())
        }
        return ctClass.toBytecode()
    }


}
