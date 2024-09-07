package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSValueParameter

fun StringBuilder.appendParameters(parameters: List<KSValueParameter>, transform: (String) -> String = { it }) {
    for ((index, parameter) in parameters.withIndex()) {
        append(transform(parameter.name!!.asString()))
        if (index + 1 != parameters.size) {
            append(", ")
        }
    }
}
