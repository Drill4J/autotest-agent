package com.epam.drill.test.agent.penetration.testing.junit

import com.epam.drill.test.agent.penetration.AnnotationStrategy

class JUnitPenetration : AnnotationStrategy() {
    override val id: String = "junit"

    init {
        supportedAnnotations.add("@org.junit.Test")
    }
}