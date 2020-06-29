package com.epam.drill.auto.test.agent.penetration;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

public abstract class Strategy {
    public abstract String id();

    public abstract boolean permit(CtClass ctClass);

    public abstract byte[] instrument(CtClass ctClass) throws CannotCompileException, IOException, NotFoundException;

}
