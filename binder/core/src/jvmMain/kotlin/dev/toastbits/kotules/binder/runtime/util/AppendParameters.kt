package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSValueParameter

fun StringBuilder.appendParameters(parameters: List<KSValueParameter>) {
    for ((index, parameter) in parameters.withIndex()) {
        append(parameter.name!!.asString())
        if (index + 1 != parameters.size) {
            append(", ")
        }
    }
}
