package dev.toastbits.kotules.extension.type.input

import dev.toastbits.kotules.extension.type.ValueType

expect class StringValue: ValueType {
    val value: String
}