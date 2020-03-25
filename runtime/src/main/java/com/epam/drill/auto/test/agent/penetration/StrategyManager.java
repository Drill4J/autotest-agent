package com.epam.drill.auto.test.agent.penetration;

import com.epam.drill.auto.test.agent.penetration.jmeter.JMeterPenetration;
import com.epam.drill.auto.test.agent.penetration.junit.JUnitPenetration;
import com.epam.drill.auto.test.agent.penetration.testng.TestNGPenetration;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class StrategyManager {

    private static final String JUNIT = "junit";
    private static final String JMETER = "jmeter";
    private static final String TESTNG = "testng";

    public static Set<Strategy> strategies = new HashSet<>();

    public static void initialize(String rawFrameworkPlugins) {
        String[] plugins = rawFrameworkPlugins.split(";");
        for (String plugin : plugins) {
            matchStrategy(plugin);
        }
        if (strategies.isEmpty()) {
            enableAllStrategies();
        }
    }

    public static byte[] process(CtClass ctClass) throws NotFoundException, CannotCompileException, IOException {
        for (Strategy strategy : strategies) {
            if (strategy.permit(ctClass)) return strategy.instrument(ctClass);
        }
        return null;
    }

    private static void matchStrategy(String alias) {
        switch (alias) {
            case JUNIT: {
                strategies.add(new JUnitPenetration());
                break;
            }
            case JMETER: {
                strategies.add(new JMeterPenetration());
                break;
            }
            case TESTNG: {
                strategies.add(new TestNGPenetration());
                break;
            }
        }
    }

    private static void enableAllStrategies() {
        strategies.add(new JUnitPenetration());
        strategies.add(new JMeterPenetration());
        strategies.add(new TestNGPenetration());
    }

}
