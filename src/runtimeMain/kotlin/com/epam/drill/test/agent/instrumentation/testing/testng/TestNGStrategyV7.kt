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
package com.epam.drill.test.agent.instrumentation.testing.testng

import com.epam.drill.test.agent.*
import javassist.*

@Suppress("unused")
object TestNGStrategyV7 : TestNGStrategy() {
    private const val IIgnoreAnnotation = "org.testng.annotations.IIgnoreAnnotation"
    override val versionRegex: Regex = "7\\.[0-9]+(\\.[0-9]+)*".toRegex()

    override fun getIgnoredTests(ctClass: CtClass, pool: ClassPool) {
        val annotationHelper = pool.getOrNull("org.testng.internal.annotations.AnnotationHelper")
        pool.getOrNull(IIgnoreAnnotation)?.also {
            annotationHelper?.getMethod(
                "isAnnotationPresent",
                "(Lorg/testng/internal/annotations/IAnnotationFinder;Ljava/lang/reflect/Method;Ljava/lang/Class;)Z"
            )?.insertAfter(
                """ 
                    if ($3 == ${IIgnoreAnnotation}.class && ${'$'}_) {
                        ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("${engineSegment}/[class:" + $2.getDeclaringClass().getName() + "]/[method:" + $2.getName() + "()]");
                    }
                """.trimIndent()
            )
        }
    }

    override fun getFactoryParams(): String = """
        private String getFactoryParams($ITestResult result){
            org.testng.internal.IParameterInfo[] instances = result.getMethod().getTestClass().getInstances(false);
            String params = "";
            if (instances.length > 1){
                params += "(";
                org.testng.internal.IParameterInfo parameterInfo = result.getMethod().getFactoryMethodParamsInfo();
                Object[] fields = parameterInfo.getParameters();
                Object instance = parameterInfo.getInstance();
                for (int i = 0; i < fields.length; i++) {
                    String name = fields[i].getClass().getSimpleName();
                    params += name;
                    if (fields.length > 1) params += " ,";
                }
                params += ")";
                int i = 0;
                while (i < instances.length) {
                    if (instances[i].getInstance() == instance) break;
                    i++;
                }
                params += ("[" + i + "]");
            }
            return params;
        }
    """.trimIndent()
}
