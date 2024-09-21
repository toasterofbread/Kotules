package dev.toastbits.kotules.core.type.input

import dev.toastbits.kotules.core.type.ValueType

@Suppress("EXPECTED_EXTERNAL_DECLARATION")
expect external class ByteArrayValue: ValueType

expect fun ByteArrayValue(value: ByteArray): ByteArrayValue

expect fun ByteArrayValue.toByteArray(): ByteArray
