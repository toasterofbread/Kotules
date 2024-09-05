package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.extension.type.JsType

internal actual fun <T: JsType> newKotule(cls: JsType): T = js("new cls()").unsafeCast<T>()

internal actual fun getExtension(): JsType = js("extension").unsafeCast<JsType>()

private val getObjectEntries: (dynamic) -> Array<Array<dynamic>> =
    js("Object.entries") as (dynamic) -> Array<Array<dynamic>>

internal actual fun entriesOf(jsObject: dynamic): List<Pair<String, dynamic>> =
    getObjectEntries(jsObject)
        .map { entry -> entry[0] as String to entry[1] }
