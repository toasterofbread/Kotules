package dev.toastbits.kotules.core.type.input

actual fun StringValue(value: String): StringValue =
    js("({ value: value })") as StringValue
