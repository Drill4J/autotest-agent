package com.epam.drill.test.agent.penetration

import com.epam.drill.test.agent.*
import javassist.CannotCompileException
import javassist.CtClass
import javassist.CtMethod
import java.io.IOException
import java.util.*

abstract class AnnotationStrategy : AbstractTestStrategy() {
    internal var supportedAnnotations: MutableSet<String> = mutableSetOf()
    private var lastScannedMethods = ThreadLocal<List<CtMethod>>()
    override fun permit(ctClass: CtClass): Boolean {
        lastScannedMethods.set(scanMethods(ctClass))
        return lastScannedMethods.get().isNotEmpty()
    }

    @Throws(CannotCompileException::class, IOException::class)
    override fun instrument(ctClass: CtClass): ByteArray? {
        for (method in lastScannedMethods.get()) {
            method.insertBefore(ThreadStorage::class.java.name + ".INSTANCE.${ThreadStorage::memorizeTestName.name}(\"" + method.name + "\");")
        }
        return ctClass.toBytecode()
    }

    private fun scanMethods(ctClass: CtClass): List<CtMethod> {
        val ctMethods = ArrayList<CtMethod>()
        for (m in ctClass.methods) {
            try {
                for (an in m.annotations) {
                    if (annotationSupported(an.toString())) {
                        ctMethods.add(m)
                        break
                    }
                }
            } catch (cnfe: ClassNotFoundException) {
                //TODO: process if needed
            }
        }
        return ctMethods
    }

    private fun annotationSupported(annotation: String): Boolean {
        return supportedAnnotations.any { annotation.startsWith(it) }
    }
}
