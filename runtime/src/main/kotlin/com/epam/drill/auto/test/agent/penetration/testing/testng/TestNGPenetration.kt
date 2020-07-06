package com.epam.drill.auto.test.agent.penetration.testing.testng

import com.epam.drill.auto.test.agent.penetration.AnnotationStrategy

class TestNGPenetration : AnnotationStrategy() {

    override val id: String = "testng"

    init {
        supportedAnnotations.add("@org.testng.annotations.Test")
    }
}
