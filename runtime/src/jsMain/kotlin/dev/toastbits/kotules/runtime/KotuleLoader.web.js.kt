package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.extension.type.ValueType

internal actual fun <T: ValueType> newKotule(cls: ValueType): T = js("new cls()").unsafeCast<T>()

internal actual fun getExtension(): ValueType = js("extension").unsafeCast<ValueType>()

private val getObjectEntries: (dynamic) -> Array<Array<dynamic>> =
    js("Object.entries") as (dynamic) -> Array<Array<dynamic>>

internal actual fun entriesOf(jsObject: dynamic): List<Pair<String, dynamic>> =
    getObjectEntries(jsObject)
        .map { entry -> entry[0] as String to entry[1] }
