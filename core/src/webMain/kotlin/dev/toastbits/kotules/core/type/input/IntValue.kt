package dev.toastbits.kotules.core.type.input

import dev.toastbits.kotules.core.type.ValueType

external class IntValue: ValueType {
    val value: Int
}

expect fun IntValue(_value: Int): IntValue
