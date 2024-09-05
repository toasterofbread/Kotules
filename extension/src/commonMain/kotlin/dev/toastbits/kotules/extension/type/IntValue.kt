package dev.toastbits.kotules.extension.type

import dev.toastbits.kotules.extension.PlatformJsExport

expect class IntValue: ValueType {
    val value: Int
}

@PlatformJsExport
class OutJsInt(val value: Int)
