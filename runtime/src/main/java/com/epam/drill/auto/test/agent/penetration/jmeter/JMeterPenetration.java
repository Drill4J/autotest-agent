package com.epam.drill.auto.test.agent.penetration.jmeter;

import com.epam.drill.auto.test.agent.AgentClassTransformer;
import com.epam.drill.auto.test.agent.penetration.Strategy;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;


@SuppressWarnings("all")
public class JMeterPenetration extends Strategy {

    private String testNameSourceClass = "org.apache.jmeter.protocol.http.sampler.HTTPHC4Impl";

    @Override
    public String id() {
        return "jmeter";
    }

    public boolean permit(CtClass ctClass) {
        return ctClass.getName().equals(testNameSourceClass);
    }

    public byte[] instrument(CtClass ctClass) throws CannotCompileException, IOException, NotFoundException {
        CtMethod setupRequestMethod = ctClass.getMethod(
                "setupRequest",
                "(Ljava/net/URL;Lorg/apache/http/client/methods/HttpRequestBase;" +
                        "Lorg/apache/jmeter/protocol/http/sampler/HTTPSampleResult;)V"
        );
        setupRequestMethod.insertBefore(
                "String drillTestName = $3.getSampleLabel();\n" +
                        AgentClassTransformer.CLASS_NAME + ".memorizeTestName(drillTestName);\n"
        );
        return ctClass.toBytecode();
    }

}
