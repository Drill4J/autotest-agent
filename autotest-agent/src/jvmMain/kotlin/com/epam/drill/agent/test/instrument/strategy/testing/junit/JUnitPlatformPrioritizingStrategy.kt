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
package com.epam.drill.agent.test.instrument.strategy.testing.junit

import com.epam.drill.agent.test.instrument.strategy.AbstractTestStrategy
import com.epam.drill.agent.test.prioritization.RecommendedTests
import com.epam.drill.agent.test2code.api.TestDetails
import javassist.*
import mu.KotlinLogging
import java.security.ProtectionDomain

private const val PostDiscoveryFilter = "org.junit.platform.launcher.PostDiscoveryFilter"
private const val FilterResult = "org.junit.platform.engine.FilterResult"
private const val TestDescriptor = "org.junit.platform.engine.TestDescriptor"
private const val Segment = "org.junit.platform.engine.UniqueId.Segment"
private const val LauncherDiscoveryRequest = "org.junit.platform.launcher.LauncherDiscoveryRequest"
private const val ConfigurationParameters = "org.junit.platform.engine.ConfigurationParameters"

@Suppress("unused")
object JUnitPlatformPrioritizingStrategy : AbstractTestStrategy() {

    private val logger = KotlinLogging.logger {}
    private val DrillJUnit5Filter = "${this.javaClass.`package`.name}.gen.DrillJUnit5Filter"
    private val LauncherDiscoveryRequestAdapter = "${this.javaClass.`package`.name}.gen.LauncherDiscoveryRequestAdapter"

    override val id: String
        get() = "junit5Prioritizing"

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean {
        return className == "org/junit/platform/launcher/core/DefaultLauncher"
    }

    override fun instrument(
        ctClass: CtClass,
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): ByteArray? {
        createRecommendedTestsFilterClass(pool, classLoader, protectionDomain)
        createLauncherDiscoveryRequestAdapterClass(pool, classLoader, protectionDomain)
        instrumentDiscoverMethod(ctClass)
        instrumentExecuteMethod(ctClass)
        return ctClass.toBytecode()
    }

    private fun instrumentDiscoverMethod(ctClass: CtClass) {
        ctClass.getMethod(
            "discover",
            "(Lorg/junit/platform/launcher/LauncherDiscoveryRequest;)Lorg/junit/platform/launcher/TestPlan;"
        ).insertBefore(
            """                
                $1 = new $LauncherDiscoveryRequestAdapter($1, new $DrillJUnit5Filter());
            """.trimIndent()
        )
    }

    private fun instrumentExecuteMethod(ctClass: CtClass) {
        ctClass.getMethod(
            "execute",
            "(Lorg/junit/platform/launcher/LauncherDiscoveryRequest;[Lorg/junit/platform/launcher/TestExecutionListener;)V")
            .insertBefore(
            """
                $1 = new $LauncherDiscoveryRequestAdapter($1, new $DrillJUnit5Filter());
            """.trimIndent()
        )
    }

    private fun createRecommendedTestsFilterClass(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): CtClass {
        val cc: CtClass = pool.makeClass(DrillJUnit5Filter)
        cc.interfaces = arrayOf(pool.get(PostDiscoveryFilter))
        cc.addMethod(
            CtMethod.make(
                """
                       public $FilterResult apply(java.lang.Object object) {                            
                            $TestDescriptor descriptor = ($TestDescriptor)object;
                            if (!descriptor.isTest())
                               return $FilterResult.included("");
                            java.util.Map testMetadata = new java.util.HashMap();
                            for (int i = 0; i < descriptor.getUniqueId().getSegments().size(); i++) {
                                java.lang.String key = (($Segment)descriptor.getUniqueId().getSegments().get(i)).getType();
                                java.lang.String value = (($Segment)descriptor.getUniqueId().getSegments().get(i)).getValue();
                                testMetadata.put(key, value);                                
                            }                                         
                            ${TestDetails::class.java.name} testDetails = ${this::class.java.name}.INSTANCE.${this::convertToTestDetails.name}(testMetadata, descriptor.getDisplayName());
                            boolean shouldSkip = testDetails != null && ${RecommendedTests::class.java.name}.INSTANCE.${RecommendedTests::shouldSkipByTestDetails.name}(testDetails);
                            if (shouldSkip) {                                
                                return $FilterResult.excluded("not recommended by Drill4J");
                            } else {
                                return $FilterResult.included("recommended by Drill4J");
                            }                                                    		                    
	                   }
                """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)
        return cc
    }

    private fun createLauncherDiscoveryRequestAdapterClass(
        pool: ClassPool,
        classLoader: ClassLoader?,
        protectionDomain: ProtectionDomain?,
    ): CtClass {
        val cc: CtClass = pool.makeClass(LauncherDiscoveryRequestAdapter)
        cc.interfaces = arrayOf(pool.get(LauncherDiscoveryRequest))
        cc.addField(CtField.make("$LauncherDiscoveryRequest delegate = null;", cc))
        cc.addField(CtField.make("$PostDiscoveryFilter additionalFilter = null;", cc))
        cc.addConstructor(
            CtNewConstructor.make(
                """
                    public LauncherDiscoveryRequestAdapter($LauncherDiscoveryRequest delegate, $PostDiscoveryFilter additionalFilter) {
                        this.delegate = delegate;
                        this.additionalFilter = additionalFilter;
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                    public java.util.List getEngineFilters() {
                        return delegate.getEngineFilters();
                    }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                public java.util.List getPostDiscoveryFilters() {
                    java.util.ArrayList modifiedList = new java.util.ArrayList(delegate.getPostDiscoveryFilters());
                    modifiedList.add(additionalFilter);
                    return modifiedList;                                          
                }
            """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                public java.util.List getSelectorsByType(java.lang.Class selectorType) {
                    return delegate.getSelectorsByType(selectorType);
                }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                public java.util.List getFiltersByType(java.lang.Class filterType) {
                    return delegate.getFiltersByType(filterType);
                }
                """.trimIndent(),
                cc
            )
        )
        cc.addMethod(
            CtMethod.make(
                """
                public $ConfigurationParameters getConfigurationParameters() {
                    return delegate.getConfigurationParameters();
                }
                """.trimIndent(),
                cc
            )
        )
        cc.toClass(classLoader, protectionDomain)
        return cc
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun convertToTestDetails(testMetadata: Map<String, String>, displayName: String?): TestDetails? {
        val testPath = testMetadata["class"] ?: testMetadata["feature"] ?: testMetadata["suite"]
        val testName = testMetadata["method"]?.substringBefore("(") ?: displayName
        if (testPath == null || testName == null) {
            logger.error { "Failed to convert test metadata to TestDetails: $testMetadata" }
            return null
        }
        return TestDetails(
            engine = testMetadata["engine"] ?: "junit",
            path = testPath,
            testName = testName,
            metadata = testMetadata,
        )
    }
}