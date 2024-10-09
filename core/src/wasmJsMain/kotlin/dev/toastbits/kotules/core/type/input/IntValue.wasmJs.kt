package dev.toastbits.kotules.core.type.input

actual fun IntValue(_value: Int): IntValue = js("({ value: _value })")
