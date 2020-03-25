package com.epam.drill.auto.test.agent.penetration.testng;

import com.epam.drill.auto.test.agent.penetration.AnnotationStrategy;

public class TestNGPenetration extends AnnotationStrategy {

    public TestNGPenetration() {
        supportedAnnotations.add("@org.testng.annotations.Test");
    }

    @Override
    public String id() {
        return "testng";
    }
}
