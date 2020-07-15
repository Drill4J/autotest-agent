package com.epam.drill.test.agent.penetration.testing.testng

import com.epam.drill.test.agent.penetration.AnnotationStrategy

class TestNGPenetration : AnnotationStrategy() {

    override val id: String = "testng"

    init {
        supportedAnnotations.add("@org.testng.annotations.Test")
    }
}
