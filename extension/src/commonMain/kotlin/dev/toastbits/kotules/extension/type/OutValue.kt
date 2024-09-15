package dev.toastbits.kotules.extension.type

import dev.toastbits.kotules.core.util.PRIMITIVE_TYPE_CLASSES
import dev.toastbits.kotules.extension.PlatformJsExport

fun <T: Any> OutValue(value: T): OutValueContainer<*> {
    require(PRIMITIVE_TYPE_CLASSES.contains(value::class)) {
        "Value passed to OutValue is not of a primitive type (${value::class})"
    }

    if (value is List<*>) {
        return OutValueContainer(value.toTypedArray())
    }

    return OutValueContainer(value)
}

@PlatformJsExport
class OutValueContainer<T>(val value: T)
