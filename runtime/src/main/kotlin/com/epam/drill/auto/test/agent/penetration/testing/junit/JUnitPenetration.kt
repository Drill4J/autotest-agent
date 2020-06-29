package com.epam.drill.auto.test.agent.penetration.testing.junit

import com.epam.drill.auto.test.agent.penetration.AnnotationStrategy

class JUnitPenetration : AnnotationStrategy() {
    override val id: String = "junit"

    init {
        supportedAnnotations.add("@org.junit.Test")
        supportedAnnotations.add("@org.junit.jupiter.api.Test")
        supportedAnnotations.add("@org.junit.jupiter.params.ParameterizedTest")
    }
}