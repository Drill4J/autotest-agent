package com.epam.drill.auto.test.agent.penetration.junit;

import com.epam.drill.auto.test.agent.penetration.AnnotationStrategy;

public class JUnitPenetration extends AnnotationStrategy {

    public JUnitPenetration() {
        supportedAnnotations.add("@org.junit.Test");
        supportedAnnotations.add("@org.junit.jupiter.api.Test");
        supportedAnnotations.add("@org.junit.jupiter.params.ParameterizedTest");
    }

    @Override
    public String id() {
        return "junit";
    }
}
