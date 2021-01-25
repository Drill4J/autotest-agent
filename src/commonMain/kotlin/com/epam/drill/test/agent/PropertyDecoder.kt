package com.epam.drill.test.agent

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*

class PropertyDecoder(val map: Map<String, Any>) : NamedValueDecoder() {
    private var currentIndex = 0

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return decodeTaggedInt(nested("size"))
    }

    override fun decodeTaggedValue(tag: String): Any {
        return map.getValue(tag)
    }

    override fun decodeTaggedBoolean(tag: String): Boolean {
        return map.getValue(tag) as Boolean
    }

    override fun decodeTaggedLong(tag: String): Long {
        return map.getValue(tag) as Long
    }

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int {
        return (map.getValue(tag) as Enum<*>).ordinal
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val tag = nested("size")
        val size = if (map.containsKey(tag)) decodeTaggedInt(tag) else descriptor.elementsCount
        while (currentIndex < size) {
            val name = descriptor.getTag(currentIndex++)
            if (map.keys.any { it.startsWith(name) }) return currentIndex - 1
        }
        return CompositeDecoder.DECODE_DONE
    }
}
