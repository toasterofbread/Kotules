package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.core.type.ValueType

internal actual fun <T: ValueType> new(constructor: ValueType): T = js("new constructor()")

internal actual fun getExtension(): ValueType = js("extension")

private val getObjectEntries: (JsAny) -> JsArray<JsArray<JsAny?>> = js("Object.entries")

internal actual fun entriesOf(jsObject: ValueType): List<Pair<String, ValueType?>> =
    getObjectEntries(jsObject)
        .map { entry -> (entry[0] as JsString).toString() to entry[1]?.unsafeCast() }

@Suppress("UNCHECKED_CAST")
private fun <T: JsAny?, O> JsArray<T>.map(mapper: (T) -> O): List<O> =
    List(length) { i ->
        mapper(this[i] as T)
    }
