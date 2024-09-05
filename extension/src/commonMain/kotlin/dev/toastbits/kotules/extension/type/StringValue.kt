package dev.toastbits.kotules.extension.type

import dev.toastbits.kotules.extension.PlatformJsExport

expect class StringValue: ValueType {
    val value: String
}

@PlatformJsExport
class OutStringValue(val value: String)
