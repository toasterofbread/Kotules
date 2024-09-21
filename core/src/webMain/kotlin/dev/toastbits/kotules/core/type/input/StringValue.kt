package dev.toastbits.kotules.core.type.input

import dev.toastbits.kotules.core.type.ValueType

external class StringValue: ValueType {
    val value: String
}

expect fun StringValue(value: String): StringValue
