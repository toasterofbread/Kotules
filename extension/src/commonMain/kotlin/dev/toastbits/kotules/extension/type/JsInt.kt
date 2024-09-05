package dev.toastbits.kotules.extension.type

import dev.toastbits.kotules.extension.PlatformJsExport

external class JsInt: JsType {
    val value: Int
}

@PlatformJsExport
class OutJsInt(val value: Int)
