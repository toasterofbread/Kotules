package dev.toastbits.kotules.extension.type

import dev.toastbits.kotules.extension.PlatformJsExport

external class JsString: JsType {
    val value: String
}

@PlatformJsExport
class OutJsString(val value: String)
