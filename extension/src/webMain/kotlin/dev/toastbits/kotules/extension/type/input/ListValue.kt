package dev.toastbits.kotules.extension.type.input

import dev.toastbits.kotules.extension.type.ValueType

@Suppress("EXPECTED_EXTERNAL_DECLARATION")
expect external class ListValue<T: ValueType?>: ValueType

expect fun <T: ValueType?> ListValue<T>.getListValue(): List<T>
