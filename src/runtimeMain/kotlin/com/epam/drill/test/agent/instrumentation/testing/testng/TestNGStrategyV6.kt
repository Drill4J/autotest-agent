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
object TestNGStrategyV6 : TestNGStrategy() {
    override val versionRegex: Regex = "6\\.[0-9]+(\\.[0-9]+)*".toRegex()

    override fun getIgnoredTests(ctClass: CtClass, pool: ClassPool) {
        ctClass.getDeclaredMethod("run").insertAfter(
            """
                    java.util.Iterator disabledTests = getExcludedMethods().iterator();
                    while(disabledTests.hasNext()) {
                        java.lang.Object baseMethod = disabledTests.next();
                        if (baseMethod instanceof $TestNGMethod) {
                            $TestNGMethod test = ($TestNGMethod) baseMethod;
                            ${TestListener::class.java.name}.INSTANCE.${TestListener::testIgnored.name}("$engineSegment/[class:" + test.getTestClass().getName() + "]/[method:" + test.getMethodName() + "()]");     
                        }
                    }
                """.trimIndent()
        )
    }

    override fun getFactoryParams(): String = """
        private String getFactoryParams($ITestResult result){
            Object[] instances = result.getTestClass().getInstances(false);
            String params = "";
            if (instances.length > 1){
                params += "(";
                Object instance = result.getInstance();
                java.lang.reflect.Field[] fields = instance.getClass().getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    java.lang.reflect.Field field = fields[i];
                    String classname = field.getType().getSimpleName();
                    params += classname;
                    if (fields.length > 1) params += " ,";
                }
                params += ")";
                int i = 0;
                while (i < instances.length) {
                    if (instances[i] == instance) break;
                    i++;
                }
                params += ("[" + i + "]");
            }
            return params;
        }
    """.trimIndent()
}
