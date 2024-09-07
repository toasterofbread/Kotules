package dev.toastbits.kotules.extension.type.input

import dev.toastbits.kotules.extension.type.ValueType

expect class IntValue: ValueType {
    val value: Int
}