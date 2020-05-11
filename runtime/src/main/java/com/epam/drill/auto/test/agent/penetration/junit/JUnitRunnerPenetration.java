package com.epam.drill.auto.test.agent.penetration.junit;

import com.epam.drill.auto.test.agent.AgentClassTransformer;
import com.epam.drill.auto.test.agent.penetration.Strategy;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;


@SuppressWarnings("all")
public class JUnitRunnerPenetration extends Strategy {


    @Override
    public String id() {
        return "junit-runner";
    }

    public boolean permit(CtClass ctClass) {
        try {
            CtClass[] interfaces = ctClass.getInterfaces();
            for (CtClass in : interfaces) {
                if (in.getName().equals("org.junit.platform.engine.support.hierarchical.Node$DynamicTestExecutor")) {
                    return true;
                }
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public byte[] instrument(CtClass ctClass) throws CannotCompileException, IOException, NotFoundException {
        CtMethod runChild = ctClass.getDeclaredMethod("execute");
        runChild.insertBefore("String drillTestName = $1.getDisplayName();\n" +
                AgentClassTransformer.CLASS_NAME + ".memorizeTestName(drillTestName);\n"
        );
        return ctClass.toBytecode();
    }

}
