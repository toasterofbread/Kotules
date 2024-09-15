package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter

fun StringBuilder.appendParameters(
    parameters: List<KSValueParameter>,
    transform: KSType.(String) -> String = { it }
) {
    for ((index, parameter) in parameters.withIndex()) {
        with (parameter.type.resolve()) {
            append(transform(parameter.name!!.asString()))
        }
        if (index + 1 != parameters.size) {
            append(", ")
        }
    }
}
