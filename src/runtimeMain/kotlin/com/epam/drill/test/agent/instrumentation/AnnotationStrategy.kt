/**
 * Copyright 2020 EPAM Systems
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
package com.epam.drill.test.agent.instrumentation

import com.epam.drill.test.agent.*
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import java.io.IOException
import java.security.ProtectionDomain
import java.util.*

abstract class AnnotationStrategy : AbstractTestStrategy() {
    internal var supportedAnnotations: MutableSet<String> = mutableSetOf()
    private var lastScannedMethods = ThreadLocal<List<CtMethod>>()
    override fun permit(ctClass: CtClass): Boolean {
        lastScannedMethods.set(scanMethods(ctClass))
        return lastScannedMethods.get().isNotEmpty()
    }

    @Throws(CannotCompileException::class, IOException::class)
    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?
    ): ByteArray? {
        for (method in lastScannedMethods.get()) {
            method.insertBefore(ThreadStorage::class.java.name + ".INSTANCE.${ThreadStorage::memorizeTestName.name}(\"" + method.name + "\");")
        }
        return ctClass.toBytecode()
    }

    private fun scanMethods(ctClass: CtClass): List<CtMethod> {
        val ctMethods = ArrayList<CtMethod>()
        for (m in ctClass.methods) {
            try {
                for (an in m.annotations) {
                    if (annotationSupported(an.toString())) {
                        ctMethods.add(m)
                        break
                    }
                }
            } catch (cnfe: ClassNotFoundException) {
                //TODO: process if needed
            }
        }
        return ctMethods
    }

    private fun annotationSupported(annotation: String): Boolean {
        return supportedAnnotations.any { annotation.startsWith(it) }
    }
}
